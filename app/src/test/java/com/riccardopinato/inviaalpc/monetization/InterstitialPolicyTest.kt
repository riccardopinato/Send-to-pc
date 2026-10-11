package com.riccardopinato.inviaalpc.monetization

import com.riccardopinato.inviaalpc.transfer.TransferOperationSummary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialPolicyTest {

    private fun summary(
        files: Int = 1,
        bytes: Long = 1L,
        durationMillis: Long = 1_000L
    ): TransferOperationSummary {
        val start = 10_000L
        return TransferOperationSummary(
            sessionId = "session",
            startedAtMillis = start,
            firstTransferAtMillis = start,
            lastCompletedAtMillis =
                start + durationMillis,
            completedFiles = files,
            completedBytes = bytes
        )
    }

    @Test
    fun shortTinySingleFileDoesNotShow() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                summary = summary(),
                lastShownAtMillis = 0L,
                alreadyShownForSession = false,
                nowMillis = 100_000L
            )
        )
    }

    @Test
    fun longTransferCanShow() {
        assertTrue(
            InterstitialPolicy.shouldShow(
                summary =
                    summary(
                        durationMillis = 20_000L
                    ),
                lastShownAtMillis = 0L,
                alreadyShownForSession = false,
                nowMillis = 100_000L
            )
        )
    }

    @Test
    fun largeTransferCanShow() {
        assertTrue(
            InterstitialPolicy.shouldShow(
                summary =
                    summary(
                        bytes =
                            30L *
                                1024L *
                                1024L
                    ),
                lastShownAtMillis = 0L,
                alreadyShownForSession = false,
                nowMillis = 100_000L
            )
        )
    }

    @Test
    fun multiFileTransferCanShow() {
        assertTrue(
            InterstitialPolicy.shouldShow(
                summary =
                    summary(
                        files = 2
                    ),
                lastShownAtMillis = 0L,
                alreadyShownForSession = false,
                nowMillis = 100_000L
            )
        )
    }

    @Test
    fun cooldownBlocksInterstitial() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                summary =
                    summary(
                        files = 3
                    ),
                lastShownAtMillis = 90_000L,
                alreadyShownForSession = false,
                nowMillis = 100_000L
            )
        )
    }

    @Test
    fun sameSessionOnlyShowsOnce() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                summary =
                    summary(
                        files = 3
                    ),
                lastShownAtMillis = 0L,
                alreadyShownForSession = true,
                nowMillis = 100_000L
            )
        )
    }
}
