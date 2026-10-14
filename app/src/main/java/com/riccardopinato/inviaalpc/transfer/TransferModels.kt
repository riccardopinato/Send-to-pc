package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class TransferDirection {
    PHONE_TO_PC,
    PC_TO_PHONE
}

enum class TransferContentKind {
    FILE,
    TEXT,
    LINK
}

enum class TransferStatus {
    WAITING,
    CONNECTED,
    TRANSFERRING,
    COMPLETED,
    EXPIRED,
    STOPPED,
    ERROR
}

enum class SessionEndReason {
    USER_STOP,
    EXPIRED,
    NETWORK_LOST,
    IP_CHANGED,
    ANDROID_TIMEOUT,
    SERVER_ERROR
}

data class SharedItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?
)

data class TransferProgress(
    val itemId: String,
    val fileName: String,
    val transferredBytes: Long,
    val totalBytes: Long?,
    val bytesPerSecond: Long,
    val direction: TransferDirection
) {
    val percentage: Int?
        get() {
            val total = totalBytes ?: return null
            if (total <= 0L) return null
            return ((transferredBytes.toDouble() / total.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        }
}

data class TransferHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,
    val fileName: String,
    val sizeBytes: Long?,
    val direction: TransferDirection,
    val kind: TransferContentKind = TransferContentKind.FILE,
    val contentUri: String? = null,
    val mimeType: String? = null,
    val textContent: String? = null,
    val completedAtMillis: Long = System.currentTimeMillis()
)

data class TransferOperationSummary(
    val sessionId: String,
    val startedAtMillis: Long,
    val firstTransferAtMillis: Long? = null,
    val lastCompletedAtMillis: Long? = null,
    val completedFiles: Int = 0,
    val completedBytes: Long = 0L,
    val phoneToPcFiles: Int = 0,
    val pcToPhoneFiles: Int = 0,
    val expectedFiles: Int? = null,
    val expectedBytes: Long? = null
) {
    val durationMillis: Long
        get() {
            val start =
                firstTransferAtMillis
                    ?: startedAtMillis
            val end =
                lastCompletedAtMillis
                    ?: System.currentTimeMillis()

            return (end - start)
                .coerceAtLeast(0L)
        }
}

data class TransferSession(
    val id: String = UUID.randomUUID().toString(),
    val token: String,
    val pin: String,
    val quickCode: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val port: Int,
    val localIp: String,
    val tlsFingerprintSha256: String,
    val sharedItems: List<SharedItem> = emptyList(),
    val sharedText: String? = null,
    val sharedLink: String? = null,
    val status: TransferStatus = TransferStatus.WAITING
) {
    val url: String
        get() = "https://$localIp:$port/s/$token"

    val quickUrl: String
        get() = "https://$localIp:$port"

    val quickCodeDisplay: String
        get() = quickCode.chunked(4).joinToString("-")

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now >= expiresAtMillis
}

object TransferLimits {
    const val MAX_SINGLE_UPLOAD_BYTES = 8L * 1024L * 1024L * 1024L
    const val STORAGE_HEADROOM_BYTES = 100L * 1024L * 1024L
    const val MAX_FILE_NAME_LENGTH = 180
    const val MAX_SHARED_TEXT_LENGTH = 250_000
    const val MAX_URL_LENGTH = 4_096
    const val SESSION_DURATION_MS = 10L * 60L * 1000L
    const val MAX_SELECTED_FILES = 100
}

object FileNameUtils {
    fun sanitize(raw: String): String {
        val cleaned = raw
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .replace("..", "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(TransferLimits.MAX_FILE_NAME_LENGTH)

        return cleaned.ifBlank { "file_${System.currentTimeMillis()}" }
    }

    fun containsHeaderInjection(value: String): Boolean =
        value.contains('\r') || value.contains('\n')
}

object UrlValidator {
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank() || value.length > TransferLimits.MAX_URL_LENGTH) return null

        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null

        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null) return null

        return uri.normalize().toASCIIString()
    }
}

object SessionSecurity {
    private val random = SecureRandom()
    private const val MAX_ATTEMPTS = 10
    private const val BLOCK_MS = 60_000L
    private const val MAX_TRACKED_CLIENTS = 256
    private const val QUICK_CODE_LENGTH = 8
    private const val QUICK_CODE_ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private data class AttemptState(
        var attempts: Int = 0,
        var blockedUntil: Long = 0L,
        var lastTouchedAt: Long = 0L
    )

    private val attempts = ConcurrentHashMap<String, AttemptState>()

    fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generatePin(): String =
        random.nextInt(10_000).toString().padStart(4, '0')

    fun generateQuickCode(): String =
        buildString(QUICK_CODE_LENGTH) {
            repeat(QUICK_CODE_LENGTH) {
                append(
                    QUICK_CODE_ALPHABET[
                        random.nextInt(
                            QUICK_CODE_ALPHABET.length
                        )
                    ]
                )
            }
        }

    fun validatePin(
        clientKey: String,
        expectedPin: String,
        suppliedPin: String
    ): Boolean =
        validateSecret(
            clientKey = "pin:$clientKey",
            expected = expectedPin,
            supplied = suppliedPin
        )

    fun validateQuickCode(
        clientKey: String,
        expectedCode: String,
        suppliedCode: String
    ): Boolean {
        val normalized =
            suppliedCode
                .trim()
                .replace("-", "")
                .replace(" ", "")
                .uppercase(Locale.US)

        if (normalized.length != QUICK_CODE_LENGTH) {
            return false
        }

        if (normalized.any { it !in QUICK_CODE_ALPHABET }) {
            return false
        }

        return validateSecret(
            clientKey = "quick:$clientKey",
            expected = expectedCode,
            supplied = normalized
        )
    }

    private fun validateSecret(
        clientKey: String,
        expected: String,
        supplied: String
    ): Boolean {
        val now = System.currentTimeMillis()
        val existing = attempts[clientKey]

        val state =
            existing ?: run {
                pruneAttempts(now)

                if (attempts.size >= MAX_TRACKED_CLIENTS) {
                    return false
                }

                attempts.computeIfAbsent(clientKey) {
                    AttemptState(
                        lastTouchedAt = now
                    )
                }
            }

        synchronized(state) {
            state.lastTouchedAt = now

            if (state.blockedUntil > now) {
                return false
            }

            if (
                constantTimeEquals(
                    expected,
                    supplied
                )
            ) {
                attempts.remove(
                    clientKey,
                    state
                )
                return true
            }

            state.attempts++

            if (state.attempts >= MAX_ATTEMPTS) {
                state.attempts = 0
                state.blockedUntil = now + BLOCK_MS
            }
        }

        return false
    }

    private fun pruneAttempts(
        now: Long
    ) {
        if (
            attempts.size <
            MAX_TRACKED_CLIENTS
        ) {
            return
        }

        attempts.entries
            .removeIf {
                (_, value) ->
                value.blockedUntil <= now &&
                    now -
                    value.lastTouchedAt >
                    BLOCK_MS
            }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aa = a.toByteArray()
        val bb = b.toByteArray()
        var diff = aa.size xor bb.size
        val max = maxOf(aa.size, bb.size)

        for (i in 0 until max) {
            val av = if (i < aa.size) aa[i].toInt() else 0
            val bv = if (i < bb.size) bb[i].toInt() else 0
            diff = diff or (av xor bv)
        }

        return diff == 0
    }
}

private class TransferHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("transfer_history", Context.MODE_PRIVATE)

    fun load(): List<TransferHistoryEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        TransferHistoryEntry(
                            id = item.getString("id"),
                            sessionId =
                                item.optString("sessionId", "")
                                    .takeIf { it.isNotBlank() },
                            fileName = item.getString("name"),
                            sizeBytes =
                                if (item.isNull("size")) {
                                    null
                                } else {
                                    item.getLong("size")
                                },
                            direction =
                                TransferDirection.valueOf(
                                    item.getString("direction")
                                ),
                            kind =
                                runCatching {
                                    TransferContentKind.valueOf(
                                        item.optString(
                                            "kind",
                                            TransferContentKind.FILE.name
                                        )
                                    )
                                }.getOrDefault(
                                    TransferContentKind.FILE
                                ),
                            contentUri =
                                item.optString("uri", "")
                                    .takeIf { it.isNotBlank() },
                            mimeType =
                                item.optString("mime", "")
                                    .takeIf { it.isNotBlank() },
                            textContent =
                                item.optString("text", "")
                                    .takeIf { it.isNotBlank() },
                            completedAtMillis =
                                item.getLong("date")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(entries: List<TransferHistoryEntry>) {
        val array = JSONArray()

        entries.take(50).forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    if (entry.sessionId == null) {
                        put("sessionId", JSONObject.NULL)
                    } else {
                        put("sessionId", entry.sessionId)
                    }
                    put("name", entry.fileName)
                    if (entry.sizeBytes == null) {
                        put("size", JSONObject.NULL)
                    } else {
                        put("size", entry.sizeBytes)
                    }
                    put("direction", entry.direction.name)
                    put("kind", entry.kind.name)
                    if (entry.contentUri == null) {
                        put("uri", JSONObject.NULL)
                    } else {
                        put("uri", entry.contentUri)
                    }
                    if (entry.mimeType == null) {
                        put("mime", JSONObject.NULL)
                    } else {
                        put("mime", entry.mimeType)
                    }
                    if (entry.textContent == null) {
                        put("text", JSONObject.NULL)
                    } else {
                        put("text", entry.textContent)
                    }
                    put("date", entry.completedAtMillis)
                }
            )
        }

        prefs.edit().putString("entries", array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove("entries").apply()
    }
}

class TransferSessionManager(context: Context) {
    private val historyStore = TransferHistoryStore(context.applicationContext)

    private val _session = MutableStateFlow<TransferSession?>(null)
    val session: StateFlow<TransferSession?> = _session.asStateFlow()

    private val _progress = MutableStateFlow<TransferProgress?>(null)
    val progress: StateFlow<TransferProgress?> = _progress.asStateFlow()

    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<TransferHistoryEntry>> = _history.asStateFlow()

    private val _operationSummary =
        MutableStateFlow<TransferOperationSummary?>(null)
    val operationSummary:
        StateFlow<TransferOperationSummary?> =
        _operationSummary.asStateFlow()

    private val registeredBatchIds =
        mutableSetOf<String>()

    private val _lastEndReason =
        MutableStateFlow<SessionEndReason?>(null)
    val lastEndReason: StateFlow<SessionEndReason?> =
        _lastEndReason.asStateFlow()

    @Synchronized
    fun create(
        localIp: String,
        port: Int,
        tlsFingerprintSha256: String,
        sharedItems: List<SharedItem>,
        sharedText: String?,
        sharedLink: String?,
        lifetimeMillis: Long = TransferLimits.SESSION_DURATION_MS
    ): TransferSession {
        val now = System.currentTimeMillis()
        val value = TransferSession(
            token = SessionSecurity.generateToken(),
            pin = SessionSecurity.generatePin(),
            quickCode = SessionSecurity.generateQuickCode(),
            createdAtMillis = now,
            expiresAtMillis = now + lifetimeMillis,
            port = port,
            localIp = localIp,
            tlsFingerprintSha256 =
                tlsFingerprintSha256,
            sharedItems = sharedItems,
            sharedText = sharedText,
            sharedLink = sharedLink
        )
        _progress.value = null
        _lastEndReason.value = null
        registeredBatchIds.clear()
        val expectedFileCount =
            sharedItems.size
                .takeIf { it > 0 }

        val knownExpectedBytes =
            sharedItems
                .mapNotNull { it.sizeBytes }
                .sum()
                .takeIf {
                    sharedItems.isNotEmpty() &&
                        sharedItems.all {
                            item ->
                            item.sizeBytes != null
                        }
                }

        _operationSummary.value =
            TransferOperationSummary(
                sessionId = value.id,
                startedAtMillis = now,
                expectedFiles = expectedFileCount,
                expectedBytes = knownExpectedBytes
            )
        _session.value = value
        return value
    }

    fun updateStatus(status: TransferStatus) {
        val current = _session.value ?: return
        val now = System.currentTimeMillis()
        val extendSession =
            status == TransferStatus.CONNECTED ||
                status == TransferStatus.TRANSFERRING ||
                status == TransferStatus.COMPLETED

        _session.value =
            current.copy(
                status = status,
                expiresAtMillis =
                    if (extendSession) {
                        maxOf(
                            current.expiresAtMillis,
                            now + TransferLimits.SESSION_DURATION_MS
                        )
                    } else {
                        current.expiresAtMillis
                    }
            )
    }

    fun updateProgress(value: TransferProgress) {
        _progress.value = value

        val currentSummary =
            _operationSummary.value

        if (
            currentSummary != null &&
            currentSummary.firstTransferAtMillis == null
        ) {
            _operationSummary.value =
                currentSummary.copy(
                    firstTransferAtMillis =
                        System.currentTimeMillis()
                )
        }

        updateStatus(TransferStatus.TRANSFERRING)
    }

    @Synchronized
    fun registerExpectedBatch(
        batchId: String,
        fileCount: Int,
        totalBytes: Long
    ) {
        if (
            batchId.isBlank() ||
            fileCount <= 0 ||
            totalBytes < 0L ||
            !registeredBatchIds.add(batchId)
        ) {
            return
        }

        val current =
            _operationSummary.value
                ?: return

        _operationSummary.value =
            current.copy(
                expectedFiles =
                    (current.expectedFiles ?: 0) +
                        fileCount,
                expectedBytes =
                    (current.expectedBytes ?: 0L) +
                        totalBytes
            )
    }

    @Synchronized
    fun completeTransfer(
        fileName: String,
        sizeBytes: Long?,
        direction: TransferDirection,
        kind: TransferContentKind = TransferContentKind.FILE,
        contentUri: String? = null,
        mimeType: String? = null,
        textContent: String? = null
    ) {
        val updated = listOf(
            TransferHistoryEntry(
                sessionId =
                    _session.value?.id,
                fileName = fileName,
                sizeBytes = sizeBytes,
                direction = direction,
                kind = kind,
                contentUri = contentUri,
                mimeType = mimeType,
                textContent = textContent
            )
        ) + _history.value.take(49)

        _history.value = updated
        historyStore.save(updated)

        _operationSummary.value
            ?.let { current ->
                val completedSize =
                    sizeBytes
                        ?.coerceAtLeast(0L)
                        ?: 0L

                _operationSummary.value =
                    current.copy(
                        firstTransferAtMillis =
                            current.firstTransferAtMillis
                                ?: System.currentTimeMillis(),
                        lastCompletedAtMillis =
                            System.currentTimeMillis(),
                        completedFiles =
                            current.completedFiles + 1,
                        completedBytes =
                            current.completedBytes +
                                completedSize,
                        phoneToPcFiles =
                            current.phoneToPcFiles +
                                if (
                                    direction ==
                                    TransferDirection.PHONE_TO_PC
                                ) {
                                    1
                                } else {
                                    0
                                },
                        pcToPhoneFiles =
                            current.pcToPhoneFiles +
                                if (
                                    direction ==
                                    TransferDirection.PC_TO_PHONE
                                ) {
                                    1
                                } else {
                                    0
                                }
                    )
            }

        _progress.value = null
        updateStatus(TransferStatus.COMPLETED)
    }

    fun clearHistory() {
        historyStore.clear()
        _history.value = emptyList()
    }

    fun expire() {
        _progress.value = null
        updateStatus(TransferStatus.EXPIRED)
    }

    fun end(reason: SessionEndReason) {
        _progress.value = null
        _lastEndReason.value = reason

        val terminalStatus =
            when (reason) {
                SessionEndReason.EXPIRED ->
                    TransferStatus.EXPIRED
                SessionEndReason.USER_STOP ->
                    TransferStatus.STOPPED
                else ->
                    TransferStatus.ERROR
            }

        updateStatus(terminalStatus)
    }

    fun clearLastEndReason() {
        _lastEndReason.value = null
    }

    fun destroy() {
        _session.value = null
        _progress.value = null
        _operationSummary.value = null
        registeredBatchIds.clear()
    }
}
