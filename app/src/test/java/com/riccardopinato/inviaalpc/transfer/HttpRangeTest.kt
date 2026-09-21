package com.riccardopinato.inviaalpc.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpRangeTest {

    @Test
    fun parsesClosedRange() {
        assertEquals(
            100L..199L,
            HttpRange.parse(
                "bytes=100-199",
                1000L
            )
        )
    }

    @Test
    fun parsesOpenEndedRange() {
        assertEquals(
            900L..999L,
            HttpRange.parse(
                "bytes=900-",
                1000L
            )
        )
    }

    @Test
    fun parsesSuffixRange() {
        assertEquals(
            900L..999L,
            HttpRange.parse(
                "bytes=-100",
                1000L
            )
        )
    }

    @Test
    fun clampsEndToFileSize() {
        assertEquals(
            950L..999L,
            HttpRange.parse(
                "bytes=950-5000",
                1000L
            )
        )
    }

    @Test
    fun rejectsOutOfBoundsStart() {
        assertNull(
            HttpRange.parse(
                "bytes=1000-",
                1000L
            )
        )
    }

    @Test
    fun rejectsMalformedRange() {
        assertNull(
            HttpRange.parse(
                "items=1-2",
                1000L
            )
        )

        assertNull(
            HttpRange.parse(
                "bytes=abc-def",
                1000L
            )
        )
    }
}
