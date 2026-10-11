package com.riccardopinato.inviaalpc.monetization

import com.riccardopinato.inviaalpc.transfer.TransferOperationSummary

object InterstitialPolicy {
    const val MIN_DURATION_MS = 15_000L
    const val MIN_BYTES = 20L * 1024L * 1024L
    const val MIN_FILES = 2
    const val COOLDOWN_MS = 5L * 60L * 1000L

    fun shouldShow(
        summary: TransferOperationSummary,
        lastShownAtMillis: Long,
        alreadyShownForSession: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (alreadyShownForSession) return false
        if (summary.completedFiles <= 0) return false

        if (
            lastShownAtMillis > 0L &&
            nowMillis - lastShownAtMillis < COOLDOWN_MS
        ) {
            return false
        }

        return summary.durationMillis >= MIN_DURATION_MS ||
            summary.completedBytes >= MIN_BYTES ||
            summary.completedFiles >= MIN_FILES
    }
}
