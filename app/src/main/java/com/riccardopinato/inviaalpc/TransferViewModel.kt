package com.riccardopinato.inviaalpc

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riccardopinato.inviaalpc.transfer.LocalTransferServer
import com.riccardopinato.inviaalpc.transfer.NetworkUtils
import com.riccardopinato.inviaalpc.transfer.SharedItem
import com.riccardopinato.inviaalpc.transfer.SharedItemResolver
import com.riccardopinato.inviaalpc.transfer.TransferForegroundService
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
    DEVICES
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
    val showOnboarding: Boolean = false
)

class TransferViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = getApplication<Application>()
    private val manager = TransferRuntime.sessionManager

    val session = manager.session
    val progress = manager.progress
    val history = manager.history
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
                    )
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

    fun clearError() {
        _uiState.value =
            _uiState.value.copy(
                error = null
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
                        "Puoi condividere fino a " +
                            TransferLimits.MAX_SELECTED_FILES +
                            " file per sessione."
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
                            "Non riesco ad accedere ai file selezionati. Prova a sceglierli di nuovo dal selettore di sistema."

                        uris.size > remaining ->
                            "Sono stati aggiunti solo i primi " +
                                TransferLimits.MAX_SELECTED_FILES +
                                " file."

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

        if (
            state.selectedItems.isEmpty() &&
            state.sharedText.isBlank() &&
            state.sharedLink.isBlank()
        ) {
            _uiState.value =
                state.copy(
                    error =
                        "Seleziona almeno un file, una foto, un testo o un link."
                )
            return
        }

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
                        "Il link non è valido. Usa un indirizzo http:// o https://."
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
                        "Telefono e PC devono essere collegati alla stessa rete Wi-Fi."
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

            val port = server.start()
            TransferRuntime.attachServer(server)

            val created =
                manager.create(
                    localIp = ip,
                    port = port,
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
            TransferRuntime.stopEverything()

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

    private fun loadLastSection(): HomeSection =
        runCatching {
            HomeSection.valueOf(
                appPrefs.getString(
                    "last_section",
                    HomeSection.SEND.name
                ) ?: HomeSection.SEND.name
            )
        }.getOrDefault(HomeSection.SEND)

    fun openHistoryEntry(entry: TransferHistoryEntry) {
        val uriString = entry.contentUri

        if (uriString.isNullOrBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    error = "Il file non è più disponibile."
                )
            return
        }

        val uri = Uri.parse(uriString)

        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri,
                    entry.mimeType ?: "*/*"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        val opened =
            runCatching {
                app.startActivity(intent)
            }.isSuccess

        if (!opened) {
            _uiState.value =
                _uiState.value.copy(
                    error = "Nessuna app disponibile per aprire questo file."
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
                    error = "Il file originale non è più disponibile."
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
                    error = "Il file originale non è più accessibile. Selezionalo di nuovo."
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

    private fun friendlySessionError(
        throwable: Throwable
    ): String =
        when (throwable) {
            is SecurityException ->
                "Android ha bloccato l'avvio della sessione. Controlla i permessi dell'app e riprova."

            is java.net.BindException ->
                "La porta di rete è occupata. Ho provato anche una porta alternativa: chiudi eventuali sessioni precedenti e riprova."

            else ->
                "Non riesco ad avviare il trasferimento sulla rete locale. Verifica che telefono e PC siano sulla stessa Wi-Fi e riprova."
        }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
