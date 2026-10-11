package com.riccardopinato.inviaalpc.transfer

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.security.auth.x500.X500Principal

/**
 * Persistent local TLS identity.
 *
 * The private key never leaves Android Keystore. The generated X.509
 * certificate is self-signed, so browsers can require an explicit local
 * certificate exception. Its SHA-256 fingerprint is shown on the phone so
 * the user can compare the certificate identity out-of-band.
 */
class LocalTlsIdentity {

    private val secureRandom = SecureRandom()

    private val entry: KeyStore.PrivateKeyEntry =
        loadOrCreateEntry()

    val certificateFingerprintSha256: String =
        sha256Fingerprint(
            entry.certificate as X509Certificate
        )

    private val sslContext: SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(
                arrayOf(
                    AndroidKeyStoreKeyManager(
                        entry
                    )
                ),
                null,
                secureRandom
            )
        }

    fun createServerSocket(
        localIp: String,
        port: Int
    ): SSLServerSocket {
        val socket =
            sslContext
                .serverSocketFactory
                .createServerSocket()
                as SSLServerSocket

        val modernProtocols =
            socket.supportedProtocols
                .filter {
                    it == "TLSv1.3" ||
                        it == "TLSv1.2"
                }
                .toTypedArray()

        if (modernProtocols.isNotEmpty()) {
            socket.enabledProtocols =
                modernProtocols
        }

        socket.useClientMode = false
        socket.needClientAuth = false
        socket.reuseAddress = true
        socket.bind(
            InetSocketAddress(
                InetAddress.getByName(localIp),
                port
            )
        )

        return socket
    }

    private fun loadOrCreateEntry():
        KeyStore.PrivateKeyEntry {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        fun currentEntry():
            KeyStore.PrivateKeyEntry? =
            runCatching {
                keyStore.getEntry(
                    KEY_ALIAS,
                    null
                ) as? KeyStore.PrivateKeyEntry
            }.getOrNull()
                ?.takeIf { candidate ->
                    runCatching {
                        (
                            candidate.certificate
                                as X509Certificate
                            ).checkValidity()
                    }.isSuccess
                }

        currentEntry()?.let {
            return it
        }

        if (
            keyStore.containsAlias(
                KEY_ALIAS
            )
        ) {
            keyStore.deleteEntry(
                KEY_ALIAS
            )
        }

        generateKeyPair()

        keyStore.load(null)

        return requireNotNull(
            currentEntry()
        ) {
            "Unable to create local TLS identity"
        }
    }

    private fun generateKeyPair() {
        val now =
            System.currentTimeMillis()

        val notBefore =
            Date(
                now -
                    ONE_DAY_MS
            )

        val notAfter =
            Date(
                now +
                    CERT_VALIDITY_MS
            )

        val serial =
            BigInteger(
                128,
                secureRandom
            ).let {
                if (
                    it == BigInteger.ZERO
                ) {
                    BigInteger.ONE
                } else {
                    it
                }
            }

        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                )
                .setSignaturePaddings(
                    KeyProperties
                        .SIGNATURE_PADDING_RSA_PKCS1,
                    KeyProperties
                        .SIGNATURE_PADDING_RSA_PSS
                )
                .setCertificateSubject(
                    X500Principal(
                        CERT_SUBJECT
                    )
                )
                .setCertificateSerialNumber(
                    serial
                )
                .setCertificateNotBefore(
                    notBefore
                )
                .setCertificateNotAfter(
                    notAfter
                )
                .setKeyValidityStart(
                    notBefore
                )
                .setKeyValidityForOriginationEnd(
                    notAfter
                )
                .setUserAuthenticationRequired(
                    false
                )
                .build()

        KeyPairGenerator
            .getInstance(
                KeyProperties
                    .KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            .apply {
                initialize(spec)
                generateKeyPair()
            }
    }

    companion object {
        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "send_to_pc_local_tls_v1"

        private const val CERT_SUBJECT =
            "CN=Send to PC Local"

        private const val ONE_DAY_MS =
            24L * 60L * 60L * 1000L

        private const val CERT_VALIDITY_MS =
            10L * 365L *
                ONE_DAY_MS

        fun sha256Fingerprint(
            certificate: X509Certificate
        ): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    certificate.encoded
                )
                .joinToString(":") {
                    "%02X".format(it)
                }
    }

    private class AndroidKeyStoreKeyManager(
        private val entry:
            KeyStore.PrivateKeyEntry
    ) : X509ExtendedKeyManager() {

        private val chain =
            entry.certificateChain
                .map {
                    it as X509Certificate
                }
                .toTypedArray()

        override fun getClientAliases(
            keyType: String?,
            issuers: Array<out Principal>?
        ): Array<String>? =
            null

        override fun chooseClientAlias(
            keyType: Array<out String>?,
            issuers: Array<out Principal>?,
            socket: java.net.Socket?
        ): String? =
            null

        override fun getServerAliases(
            keyType: String?,
            issuers: Array<out Principal>?
        ): Array<String>? =
            if (
                supports(
                    keyType
                )
            ) {
                arrayOf(
                    KEY_ALIAS
                )
            } else {
                null
            }

        override fun chooseServerAlias(
            keyType: String?,
            issuers: Array<out Principal>?,
            socket: java.net.Socket?
        ): String? =
            KEY_ALIAS.takeIf {
                supports(
                    keyType
                )
            }

        override fun chooseEngineServerAlias(
            keyType: String?,
            issuers: Array<out Principal>?,
            engine: SSLEngine?
        ): String? =
            KEY_ALIAS.takeIf {
                supports(
                    keyType
                )
            }

        override fun getCertificateChain(
            alias: String?
        ): Array<X509Certificate>? =
            chain.takeIf {
                alias == KEY_ALIAS
            }

        override fun getPrivateKey(
            alias: String?
        ): PrivateKey? =
            entry.privateKey
                .takeIf {
                    alias ==
                        KEY_ALIAS
                }

        private fun supports(
            keyType: String?
        ): Boolean =
            keyType
                ?.uppercase()
                ?.contains("RSA")
                ?: true
    }
}
