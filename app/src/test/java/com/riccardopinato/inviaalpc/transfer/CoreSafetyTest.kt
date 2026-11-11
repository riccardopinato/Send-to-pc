package com.riccardopinato.inviaalpc.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreSafetyTest {

    @Test
    fun urlValidatorAcceptsOnlyHttpAndHttps() {
        assertEquals(
            "https://example.com/path",
            UrlValidator.normalize(
                " https://example.com/path "
            )
        )

        assertNull(
            UrlValidator.normalize(
                "ftp://example.com/file"
            )
        )

        assertNull(
            UrlValidator.normalize(
                "https://user:pass@example.com"
            )
        )
    }

    @Test
    fun fileNameSanitizerRemovesUnsafeCharacters() {
        val sanitized =
            FileNameUtils.sanitize(
                "../report:2026?.pdf"
            )

        assertFalse(
            sanitized.contains("..")
        )
        assertFalse(
            sanitized.contains(':')
        )
        assertFalse(
            sanitized.contains('?')
        )
        assertTrue(
            sanitized.endsWith(".pdf")
        )
    }

    @Test
    fun headerInjectionIsRejected() {
        assertTrue(
            FileNameUtils.containsHeaderInjection(
                "file.txt\r\nX-Test: 1"
            )
        )

        assertFalse(
            FileNameUtils.containsHeaderInjection(
                "file.txt"
            )
        )
    }


    @Test
    fun transferSessionUsesHttpsOnly() {
        val session =
            TransferSession(
                token = "token",
                pin = "1234",
                quickCode = "ABCDEFGH",
                createdAtMillis = 1L,
                expiresAtMillis = 2L,
                port = 8734,
                localIp = "192.168.1.42",
                tlsFingerprintSha256 = "AA:BB"
            )

        assertEquals(
            "https://192.168.1.42:8734/s/token",
            session.url
        )

        assertEquals(
            "https://192.168.1.42:8734",
            session.quickUrl
        )

        assertEquals(
            "ABCD-EFGH",
            session.quickCodeDisplay
        )
    }

    @Test
    fun pinRateLimitBlocksAfterTenFailures() {
        val client =
            "unit-test-rate-limit"

        repeat(10) {
            assertFalse(
                SessionSecurity.validatePin(
                    clientKey = client,
                    expectedPin = "1234",
                    suppliedPin = "0000"
                )
            )
        }

        assertFalse(
            SessionSecurity.validatePin(
                clientKey = client,
                expectedPin = "1234",
                suppliedPin = "1234"
            )
        )
    }

    @Test
    fun quickCodeHasHumanSafeFortyBitShape() {
        val code =
            SessionSecurity
                .generateQuickCode()

        assertEquals(
            8,
            code.length
        )
        assertTrue(
            code.matches(
                Regex(
                    "[A-HJ-NP-Z2-9]{8}"
                )
            )
        )
        assertTrue(
            SessionSecurity
                .validateQuickCode(
                    clientKey = "unit-test-quick-ok",
                    expectedCode = code,
                    suppliedCode =
                        code.take(4) +
                            "-" +
                            code.drop(4)
                )
        )
    }

    @Test
    fun sessionTokenHasStrongEntropyShape() {
        val token =
            SessionSecurity.generateToken()

        assertTrue(
            token.length >= 40
        )
        assertFalse(
            token.contains('=')
        )
    }
}
