package com.riccardopinato.inviaalpc

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riccardopinato.inviaalpc.transfer.LocalTransferServer
import com.riccardopinato.inviaalpc.transfer.NetworkUtils
import com.riccardopinato.inviaalpc.transfer.SharedItem
import com.riccardopinato.inviaalpc.transfer.SharedItemResolver
import com.riccardopinato.inviaalpc.transfer.SessionEndReason
import com.riccardopinato.inviaalpc.transfer.TransferForegroundService
import com.riccardopinato.inviaalpc.transfer.TransferContentKind
import com.riccardopinato.inviaalpc.transfer.TransferDirection
import com.riccardopinato.inviaalpc.transfer.TransferHistoryEntry
import com.riccardopinato.inviaalpc.transfer.TransferLimits
import com.riccardopinato.inviaalpc.transfer.TransferRuntime
import com.riccardopinato.inviaalpc.transfer.TrustedDevice
import com.riccardopinato.inviaalpc.transfer.UrlValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeSection {
    SEND,
    RECEIVE,
    RECENTS,
    DEVICES,
    SETTINGS
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class TransferUiState(
    val section: HomeSection = HomeSection.SEND,
    val selectedItems: List<SharedItem> = emptyList(),
    val sharedText: String = "",
    val sharedLink: String = "",
    val remainingSeconds: Long = 0L,
    val localIp: String? = null,
    val preparing: Boolean = false,
    val error: String? = null,
    val showOnboarding: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val languageOverrideCode: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class TransferViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = getApplication<Application>()
    private val manager = TransferRuntime.sessionManager

    val session = manager.session
    val progress = manager.progress
    val history = manager.history
    val operationSummary = manager.operationSummary
    val trustedDevices = TransferRuntime.trustedDeviceStore.devices

    private val appPrefs =
        app.getSharedPreferences(
            "app_preferences",
            Application.MODE_PRIVATE
        )

    private val _uiState =
        MutableStateFlow(
            TransferUiState(
                section = loadLastSection(),
                showOnboarding =
                    !appPrefs.getBoolean(
                        "onboarding_complete",
                        false
                    ),
                language =
                    AppLanguageStore.current(app),
                languageOverrideCode =
                    AppLanguageStore.overrideCode(app),
                themeMode =
                    loadThemeMode()
            )
        )

    val uiState: StateFlow<TransferUiState> =
        _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        refreshNetwork()

        viewModelScope.launch {
            session.collect { active ->
                if (active == null) {
                    countdownJob?.cancel()
                    _uiState.value =
                        _uiState.value.copy(
                            remainingSeconds = 0L,
                            preparing = false
                        )
                }
            }
        }

        viewModelScope.launch {
            manager.lastEndReason.collect { reason ->
                val message =
                    sessionEndMessage(reason)
                        ?: return@collect

                _uiState.value =
                    _uiState.value.copy(
                        error = message,
                        preparing = false
                    )
            }
        }
    }

    fun setSection(section: HomeSection) {
        appPrefs.edit()
            .putString("last_section", section.name)
            .apply()

        _uiState.value =
            _uiState.value.copy(
                section = section,
                error = null
            )
    }

    fun dismissOnboarding() {
        appPrefs.edit()
            .putBoolean(
                "onboarding_complete",
                true
            )
            .apply()

        _uiState.value =
            _uiState.value.copy(
                showOnboarding = false
            )
    }

    fun showOnboarding() {
        _uiState.value =
            _uiState.value.copy(
                showOnboarding = true
            )
    }

    fun setLanguageOverride(
        language: AppLanguage?
    ) {
        AppLanguageStore.setOverride(
            app,
            language
        )

        _uiState.value =
            _uiState.value.copy(
                language =
                    AppLanguageStore.current(app),
                languageOverrideCode =
                    language?.code
            )
    }

    fun setThemeMode(
        mode: AppThemeMode
    ) {
        appPrefs.edit()
            .putString(
                "theme_mode",
                mode.name
            )
            .apply()

        _uiState.value =
            _uiState.value.copy(
                themeMode = mode
            )
    }

    fun clearError() {
        _uiState.value =
            _uiState.value.copy(
                error = null
            )
    }

    fun reportLocalNetworkPermissionDenied() {
        _uiState.value =
            _uiState.value.copy(
                error =
                    t(
                        "err_local_network_permission"
                    )
            )
    }

    fun refreshNetwork() {
        _uiState.value =
            _uiState.value.copy(
                localIp = NetworkUtils.findLanIpv4(app)
            )
    }

    fun addUris(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val current = _uiState.value
        val remaining =
            (TransferLimits.MAX_SELECTED_FILES - current.selectedItems.size)
                .coerceAtLeast(0)

        if (remaining == 0) {
            _uiState.value =
                current.copy(
                    error =
                        t("err_file_limit", TransferLimits.MAX_SELECTED_FILES)
                )
            return
        }

        val resolved =
            SharedItemResolver.resolveAll(
                app,
                uris.take(remaining)
            )

        _uiState.value =
            current.copy(
                selectedItems =
                    (current.selectedItems + resolved)
                        .distinctBy { it.uri.toString() }
                        .take(TransferLimits.MAX_SELECTED_FILES),

                error =
                    when {
                        resolved.isEmpty() ->
                            t("err_file_access")

                        uris.size > remaining ->
                            t("err_first_files", TransferLimits.MAX_SELECTED_FILES)

                        else ->
                            null
                    }
            )
    }

    fun removeItem(id: String) {
        _uiState.value =
            _uiState.value.copy(
                selectedItems =
                    _uiState.value.selectedItems
                        .filterNot { it.id == id }
            )
    }

    fun setSharedText(value: String) {
        _uiState.value =
            _uiState.value.copy(
                sharedText =
                    value.take(
                        TransferLimits.MAX_SHARED_TEXT_LENGTH
                    ),
                error = null
            )
    }

    fun setSharedLink(value: String) {
        _uiState.value =
            _uiState.value.copy(
                sharedLink =
                    value.take(
                        TransferLimits.MAX_URL_LENGTH
                    ),
                error = null
            )
    }

    fun startSendSession() {
        val state = _uiState.value

        val normalizedLink =
            state.sharedLink
                .takeIf { it.isNotBlank() }
                ?.let { UrlValidator.normalize(it) }

        if (
            state.sharedLink.isNotBlank() &&
            normalizedLink == null
        ) {
            _uiState.value =
                state.copy(
                    error =
                        t("err_invalid_link")
                )
            return
        }

        startSession(
            items = state.selectedItems,
            text =
                state.sharedText
                    .trim()
                    .takeIf { it.isNotBlank() },
            link = normalizedLink
        )
    }

    fun startReceiveSession() {
        startSession(
            items = emptyList(),
            text = null,
            link = null
        )
    }

    private fun startSession(
        items: List<SharedItem>,
        text: String?,
        link: String?
    ) {
        countdownJob?.cancel()
        TransferRuntime.stopServer()

        val ip = NetworkUtils.findLanIpv4(app)

        if (ip == null) {
            _uiState.value =
                _uiState.value.copy(
                    localIp = null,
                    preparing = false,
                    error =
                        t("err_same_wifi")
                )
            return
        }

        _uiState.value =
            _uiState.value.copy(
                localIp = ip,
                preparing = true,
                error = null
            )

        try {
            val server =
                LocalTransferServer(
                    context = app,
                    sessionManager = manager
                )

            val port = server.start(ip)
            TransferRuntime.attachServer(server)

            val created =
                manager.create(
                    localIp = ip,
                    port = port,
                    tlsFingerprintSha256 =
                        server
                            .certificateFingerprintSha256,
                    sharedItems = items,
                    sharedText = text,
                    sharedLink = link
                )

            ContextCompat.startForegroundService(
                app,
                Intent(
                    app,
                    TransferForegroundService::class.java
                ).apply {
                    action =
                        TransferForegroundService.ACTION_START
                }
            )

            startCountdown()

            _uiState.value =
                _uiState.value.copy(
                    preparing = false
                )
        } catch (throwable: Throwable) {
            TransferRuntime.stopEverything(
                SessionEndReason.SERVER_ERROR
            )

            _uiState.value =
                _uiState.value.copy(
                    preparing = false,
                    error =
                        friendlySessionError(
                            throwable
                        )
                )
        }
    }

    fun stopSession() {
        countdownJob?.cancel()
        TransferRuntime.stopEverything()

        app.stopService(
            Intent(
                app,
                TransferForegroundService::class.java
            )
        )

        _uiState.value =
            _uiState.value.copy(
                remainingSeconds = 0L,
                preparing = false
            )
    }

    fun clearHistory() {
        manager.clearHistory()
    }

    fun removeTrustedDevice(device: TrustedDevice) {
        TransferRuntime.trustedDeviceStore.remove(device.id)
    }

    fun clearTrustedDevices() {
        TransferRuntime.trustedDeviceStore.clear()
    }

    private fun loadThemeMode():
        AppThemeMode =
        runCatching {
            AppThemeMode.valueOf(
                appPrefs.getString(
                    "theme_mode",
                    AppThemeMode.SYSTEM.name
                ) ?: AppThemeMode.SYSTEM.name
            )
        }.getOrDefault(
            AppThemeMode.SYSTEM
        )

    private fun loadLastSection(): HomeSection =
        runCatching {
            HomeSection.valueOf(
                appPrefs.getString(
                    "last_section",
                    HomeSection.SEND.name
                ) ?: HomeSection.SEND.name
            )
        }.getOrDefault(HomeSection.SEND)
            .let {
                if (it == HomeSection.RECEIVE) {
                    HomeSection.SEND
                } else {
                    it
                }
            }

    fun openHistoryEntry(entry: TransferHistoryEntry) {
        val intent =
            when (entry.kind) {
                TransferContentKind.FILE -> {
                    val uriString =
                        entry.contentUri

                    if (uriString.isNullOrBlank()) {
                        _uiState.value =
                            _uiState.value.copy(
                                error =
                                    t("err_file_missing")
                            )
                        return
                    }

                    Intent(
                        Intent.ACTION_VIEW
                    ).apply {
                        setDataAndType(
                            Uri.parse(uriString),
                            entry.mimeType ?: "*/*"
                        )
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }

                TransferContentKind.LINK -> {
                    val value =
                        entry.textContent
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return

                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(value)
                    )
                }

                TransferContentKind.TEXT ->
                    return
            }.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        val opened =
            runCatching {
                app.startActivity(intent)
            }.isSuccess

        if (!opened) {
            _uiState.value =
                _uiState.value.copy(
                    error =
                        t("err_no_app_open")
                )
        }
    }

    fun copyHistoryText(
        entry: TransferHistoryEntry
    ) {
        val value =
            entry.textContent
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return

        val clipboard =
            app.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                entry.fileName,
                value
            )
        )
    }

    fun shareHistoryEntry(
        entry: TransferHistoryEntry
    ) {
        val sendIntent =
            Intent(Intent.ACTION_SEND)

        when (entry.kind) {
            TransferContentKind.FILE -> {
                val uriString =
                    entry.contentUri
                        ?: return

                sendIntent.type =
                    entry.mimeType ?: "*/*"

                sendIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.parse(uriString)
                )

                sendIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            TransferContentKind.TEXT -> {
                sendIntent.type =
                    "text/plain"
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    entry.textContent
                        ?: return
                )
            }

            TransferContentKind.LINK -> {
                sendIntent.type =
                    "text/plain"
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    entry.textContent
                        ?: return
                )
            }
        }

        val chooser =
            Intent.createChooser(
                sendIntent,
                t("share")
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        val shared =
            runCatching {
                app.startActivity(
                    chooser
                )
            }.isSuccess

        if (!shared) {
            _uiState.value =
                _uiState.value.copy(
                    error =
                        t("err_no_app_open")
                )
        }
    }

    fun reuseHistoryEntry(entry: TransferHistoryEntry) {
        if (entry.direction != TransferDirection.PHONE_TO_PC) {
            return
        }

        val uriString = entry.contentUri

        if (uriString.isNullOrBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    error = t("err_original_missing")
                )
            return
        }

        val uri = Uri.parse(uriString)

        val readable =
            runCatching {
                app.contentResolver
                    .openAssetFileDescriptor(uri, "r")
                    ?.use { true }
                    ?: false
            }.getOrDefault(false)

        if (!readable) {
            _uiState.value =
                _uiState.value.copy(
                    error = t("err_original_unreadable")
                )
            return
        }

        addUris(listOf(uri))

        _uiState.value =
            _uiState.value.copy(
                section = HomeSection.SEND,
                error = null
            )
    }

    private fun startCountdown() {
        countdownJob?.cancel()

        countdownJob =
            viewModelScope.launch {
                while (true) {
                    val active =
                        manager.session.value
                            ?: break

                    val remaining =
                        (
                            active.expiresAtMillis -
                                System.currentTimeMillis()
                        ).coerceAtLeast(0L)

                    _uiState.value =
                        _uiState.value.copy(
                            remainingSeconds =
                                remaining / 1000L
                        )

                    if (remaining <= 0L) {
                        delay(1_000L)
                        continue
                    }

                    delay(1_000L)
                }
            }
    }

    private fun t(
        key: String,
        vararg args: Any
    ): String =
        AppText.get(
            _uiState.value.language,
            key,
            *args
        )

    private fun sessionEndMessage(
        reason: SessionEndReason?
    ): String? =
        when (reason) {
            null,
            SessionEndReason.USER_STOP ->
                null

            SessionEndReason.EXPIRED ->
                t("end_expired")

            SessionEndReason.NETWORK_LOST ->
                t("end_network_lost")

            SessionEndReason.IP_CHANGED ->
                t("end_ip_changed")

            SessionEndReason.ANDROID_TIMEOUT ->
                t("end_android_timeout")

            SessionEndReason.SERVER_ERROR ->
                t("end_server_error")
        }

    private fun friendlySessionError(
        throwable: Throwable
    ): String =
        when (throwable) {
            is SecurityException ->
                t("err_android_block")

            is java.net.BindException ->
                t("err_port_busy")

            else ->
                t("err_start_session")
        }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
