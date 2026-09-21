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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class TransferDirection {
    PHONE_TO_PC,
    PC_TO_PHONE
}

enum class TransferStatus {
    WAITING,
    CONNECTED,
    TRANSFERRING,
    EXPIRED,
    STOPPED,
    ERROR
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
    val fileName: String,
    val sizeBytes: Long?,
    val direction: TransferDirection,
    val completedAtMillis: Long = System.currentTimeMillis()
)

data class TransferSession(
    val id: String = UUID.randomUUID().toString(),
    val token: String,
    val pin: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val port: Int,
    val localIp: String,
    val sharedItems: List<SharedItem> = emptyList(),
    val sharedText: String? = null,
    val sharedLink: String? = null,
    val status: TransferStatus = TransferStatus.WAITING
) {
    val url: String
        get() = "http://$localIp:$port/s/$token"

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now >= expiresAtMillis
}

object TransferLimits {
    const val MAX_SINGLE_UPLOAD_BYTES = 2L * 1024L * 1024L * 1024L
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

    private data class AttemptState(
        var attempts: Int = 0,
        var blockedUntil: Long = 0L
    )

    private val attempts = ConcurrentHashMap<String, AttemptState>()

    fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generatePin(): String =
        random.nextInt(10_000).toString().padStart(4, '0')

    fun validatePin(clientKey: String, expectedPin: String, suppliedPin: String): Boolean {
        val now = System.currentTimeMillis()
        val state = attempts.computeIfAbsent(clientKey) { AttemptState() }

        synchronized(state) {
            if (state.blockedUntil > now) return false

            if (constantTimeEquals(expectedPin, suppliedPin)) {
                attempts.remove(clientKey)
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
                            fileName = item.getString("name"),
                            sizeBytes = if (item.isNull("size")) null else item.getLong("size"),
                            direction = TransferDirection.valueOf(item.getString("direction")),
                            completedAtMillis = item.getLong("date")
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
                    put("name", entry.fileName)
                    if (entry.sizeBytes == null) put("size", JSONObject.NULL) else put("size", entry.sizeBytes)
                    put("direction", entry.direction.name)
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

    @Synchronized
    fun create(
        localIp: String,
        port: Int,
        sharedItems: List<SharedItem>,
        sharedText: String?,
        sharedLink: String?,
        lifetimeMillis: Long = TransferLimits.SESSION_DURATION_MS
    ): TransferSession {
        val now = System.currentTimeMillis()
        val value = TransferSession(
            token = SessionSecurity.generateToken(),
            pin = SessionSecurity.generatePin(),
            createdAtMillis = now,
            expiresAtMillis = now + lifetimeMillis,
            port = port,
            localIp = localIp,
            sharedItems = sharedItems,
            sharedText = sharedText,
            sharedLink = sharedLink
        )
        _progress.value = null
        _session.value = value
        return value
    }

    fun updateStatus(status: TransferStatus) {
        _session.value = _session.value?.copy(status = status)
    }

    fun updateProgress(value: TransferProgress) {
        _progress.value = value
        updateStatus(TransferStatus.TRANSFERRING)
    }

    @Synchronized
    fun completeTransfer(fileName: String, sizeBytes: Long?, direction: TransferDirection) {
        val updated = listOf(
            TransferHistoryEntry(
                fileName = fileName,
                sizeBytes = sizeBytes,
                direction = direction
            )
        ) + _history.value.take(49)

        _history.value = updated
        historyStore.save(updated)
        _progress.value = null
        updateStatus(TransferStatus.CONNECTED)
    }

    fun clearHistory() {
        historyStore.clear()
        _history.value = emptyList()
    }

    fun expire() {
        _progress.value = null
        updateStatus(TransferStatus.EXPIRED)
    }

    fun stop() {
        _progress.value = null
        updateStatus(TransferStatus.STOPPED)
    }

    fun destroy() {
        _session.value = null
        _progress.value = null
    }
}
