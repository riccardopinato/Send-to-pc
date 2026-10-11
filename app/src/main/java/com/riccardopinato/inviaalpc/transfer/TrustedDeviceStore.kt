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
    val lastSeenAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long =
        System.currentTimeMillis() +
            TrustedDeviceStore.TRUSTED_DEVICE_TTL_MS
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
        MutableStateFlow(loadActive())

    val devices: StateFlow<List<TrustedDevice>> =
        _devices.asStateFlow()

    @Synchronized
    fun create(
        name: String
    ): Pair<TrustedDevice, String> {
        val rawToken = SessionSecurity.generateToken()
        val now = System.currentTimeMillis()

        val device =
            TrustedDevice(
                name = sanitizeName(name),
                tokenHash = hashToken(rawToken),
                createdAtMillis = now,
                lastSeenAtMillis = now,
                expiresAtMillis =
                    now + TRUSTED_DEVICE_TTL_MS
            )

        val updated =
            listOf(device) +
                activeDevices(now)
                    .filterNot {
                        it.name.equals(
                            device.name,
                            ignoreCase = true
                        )
                    }
                    .take(19)

        _devices.value = updated
        save(updated)

        return device to rawToken
    }

    @Synchronized
    fun validateAndRotate(
        rawToken: String
    ): Pair<TrustedDevice, String>? {
        if (
            rawToken.isBlank() ||
            rawToken.length > 256
        ) {
            return null
        }

        val now = System.currentTimeMillis()
        val hash = hashToken(rawToken)

        val current =
            activeDevices(now)
                .firstOrNull {
                    constantTimeEquals(
                        it.tokenHash,
                        hash
                    )
                } ?: return null

        val rotatedRawToken =
            SessionSecurity.generateToken()

        val refreshed =
            current.copy(
                tokenHash =
                    hashToken(rotatedRawToken),
                lastSeenAtMillis = now,
                expiresAtMillis =
                    now + TRUSTED_DEVICE_TTL_MS
            )

        val updated =
            activeDevices(now)
                .map {
                    if (it.id == current.id) {
                        refreshed
                    } else {
                        it
                    }
                }

        _devices.value = updated
        save(updated)

        return refreshed to rotatedRawToken
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
        prefs.edit()
            .remove(KEY)
            .apply()
    }

    private fun activeDevices(
        now: Long
    ): List<TrustedDevice> {
        val active =
            _devices.value.filter {
                it.expiresAtMillis > now
            }

        if (active.size != _devices.value.size) {
            _devices.value = active
            save(active)
        }

        return active
    }

    private fun loadActive(): List<TrustedDevice> {
        val now = System.currentTimeMillis()

        val loaded =
            load().filter {
                it.expiresAtMillis > now
            }

        if (loaded.isNotEmpty()) {
            save(loaded)
        } else {
            prefs.edit()
                .remove(KEY)
                .apply()
        }

        return loaded
    }

    private fun load(): List<TrustedDevice> {
        val raw =
            prefs.getString(KEY, null)
                ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)

            buildList {
                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(
                            index
                        )

                    val createdAt =
                        item.getLong(
                            "createdAt"
                        )

                    add(
                        TrustedDevice(
                            id =
                                item.getString(
                                    "id"
                                ),
                            name =
                                item.getString(
                                    "name"
                                ),
                            tokenHash =
                                item.getString(
                                    "tokenHash"
                                ),
                            createdAtMillis =
                                createdAt,
                            lastSeenAtMillis =
                                item.getLong(
                                    "lastSeenAt"
                                ),
                            expiresAtMillis =
                                item.optLong(
                                    "expiresAt",
                                    createdAt +
                                        TRUSTED_DEVICE_TTL_MS
                                )
                        )
                    )
                }
            }
        }.getOrDefault(
            emptyList()
        )
    }

    private fun save(
        devices: List<TrustedDevice>
    ) {
        val array = JSONArray()

        devices.take(20)
            .forEach { device ->
                array.put(
                    JSONObject().apply {
                        put(
                            "id",
                            device.id
                        )
                        put(
                            "name",
                            device.name
                        )
                        put(
                            "tokenHash",
                            device.tokenHash
                        )
                        put(
                            "createdAt",
                            device.createdAtMillis
                        )
                        put(
                            "lastSeenAt",
                            device.lastSeenAtMillis
                        )
                        put(
                            "expiresAt",
                            device.expiresAtMillis
                        )
                    }
                )
            }

        prefs.edit()
            .putString(
                KEY,
                array.toString()
            )
            .apply()
    }

    private fun sanitizeName(
        raw: String
    ): String =
        raw
            .trim()
            .replace(
                Regex("""[\p{Cntrl}]"""),
                ""
            )
            .take(60)
            .ifBlank {
                "PC fidato"
            }

    private fun hashToken(
        rawToken: String
    ): String {
        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    rawToken.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }

    private fun constantTimeEquals(
        a: String,
        b: String
    ): Boolean =
        MessageDigest.isEqual(
            a.toByteArray(
                Charsets.UTF_8
            ),
            b.toByteArray(
                Charsets.UTF_8
            )
        )

    companion object {
        private const val KEY =
            "devices"

        const val TRUSTED_DEVICE_TTL_MS =
            30L * 24L * 60L * 60L * 1000L
    }
}
