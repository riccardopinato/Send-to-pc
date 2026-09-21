package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class TrustedDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tokenHash: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastSeenAtMillis: Long = System.currentTimeMillis()
)

class TrustedDeviceStore(
    context: Context
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            "trusted_devices",
            Context.MODE_PRIVATE
        )

    private val _devices =
        MutableStateFlow(load())

    val devices: StateFlow<List<TrustedDevice>> =
        _devices.asStateFlow()

    @Synchronized
    fun create(name: String): Pair<TrustedDevice, String> {
        val rawToken = SessionSecurity.generateToken()
        val now = System.currentTimeMillis()

        val device =
            TrustedDevice(
                name = sanitizeName(name),
                tokenHash = hashToken(rawToken),
                createdAtMillis = now,
                lastSeenAtMillis = now
            )

        val updated =
            listOf(device) +
                _devices.value
                    .filterNot { it.name.equals(device.name, ignoreCase = true) }
                    .take(19)

        _devices.value = updated
        save(updated)

        return device to rawToken
    }

    @Synchronized
    fun validate(rawToken: String): TrustedDevice? {
        if (rawToken.isBlank() || rawToken.length > 256) return null

        val hash = hashToken(rawToken)
        val current =
            _devices.value.firstOrNull {
                constantTimeEquals(it.tokenHash, hash)
            } ?: return null

        val refreshed =
            current.copy(
                lastSeenAtMillis = System.currentTimeMillis()
            )

        val updated =
            _devices.value.map {
                if (it.id == current.id) refreshed else it
            }

        _devices.value = updated
        save(updated)

        return refreshed
    }

    @Synchronized
    fun remove(id: String) {
        val updated =
            _devices.value.filterNot {
                it.id == id
            }

        _devices.value = updated
        save(updated)
    }

    @Synchronized
    fun clear() {
        _devices.value = emptyList()
        prefs.edit().remove(KEY).apply()
    }

    private fun load(): List<TrustedDevice> {
        val raw =
            prefs.getString(KEY, null)
                ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)

                    add(
                        TrustedDevice(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            tokenHash = item.getString("tokenHash"),
                            createdAtMillis = item.getLong("createdAt"),
                            lastSeenAtMillis = item.getLong("lastSeenAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(devices: List<TrustedDevice>) {
        val array = JSONArray()

        devices.take(20).forEach { device ->
            array.put(
                JSONObject().apply {
                    put("id", device.id)
                    put("name", device.name)
                    put("tokenHash", device.tokenHash)
                    put("createdAt", device.createdAtMillis)
                    put("lastSeenAt", device.lastSeenAtMillis)
                }
            )
        }

        prefs.edit()
            .putString(KEY, array.toString())
            .apply()
    }

    private fun sanitizeName(raw: String): String =
        raw
            .trim()
            .replace(Regex("""[\p{Cntrl}]"""), "")
            .take(60)
            .ifBlank { "PC fidato" }

    private fun hashToken(rawToken: String): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(rawToken.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aa = a.toByteArray(Charsets.UTF_8)
        val bb = b.toByteArray(Charsets.UTF_8)

        return MessageDigest.isEqual(aa, bb)
    }

    companion object {
        private const val KEY = "devices"
    }
}
