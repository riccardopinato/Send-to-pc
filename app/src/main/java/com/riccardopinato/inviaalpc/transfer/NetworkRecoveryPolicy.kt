package com.riccardopinato.inviaalpc.transfer

object NetworkRecoveryPolicy {
    const val DEFAULT_GRACE_MS = 12_000L

    fun graceExpired(
        expectedIp: String,
        observedIp: String?,
        mismatchSinceMillis: Long?,
        nowMillis: Long,
        graceMillis: Long = DEFAULT_GRACE_MS
    ): Boolean {
        if (observedIp == expectedIp) {
            return false
        }

        val since = mismatchSinceMillis ?: nowMillis
        return nowMillis - since >= graceMillis
    }
}
