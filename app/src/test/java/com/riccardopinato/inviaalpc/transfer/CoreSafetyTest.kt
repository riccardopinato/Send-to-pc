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
