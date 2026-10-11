package com.riccardopinato.inviaalpc.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRecoveryPolicyTest {

    @Test
    fun sameIpNeverExpiresGrace() {
        assertFalse(
            NetworkRecoveryPolicy.graceExpired(
                expectedIp = "192.168.1.10",
                observedIp = "192.168.1.10",
                mismatchSinceMillis = 1_000L,
                nowMillis = 30_000L
            )
        )
    }

    @Test
    fun temporaryLossStaysInsideGrace() {
        assertFalse(
            NetworkRecoveryPolicy.graceExpired(
                expectedIp = "192.168.1.10",
                observedIp = null,
                mismatchSinceMillis = 10_000L,
                nowMillis = 20_000L,
                graceMillis = 12_000L
            )
        )
    }

    @Test
    fun prolongedLossExpiresGrace() {
        assertTrue(
            NetworkRecoveryPolicy.graceExpired(
                expectedIp = "192.168.1.10",
                observedIp = null,
                mismatchSinceMillis = 10_000L,
                nowMillis = 22_001L,
                graceMillis = 12_000L
            )
        )
    }

    @Test
    fun changedIpExpiresAfterGrace() {
        assertTrue(
            NetworkRecoveryPolicy.graceExpired(
                expectedIp = "192.168.1.10",
                observedIp = "192.168.1.11",
                mismatchSinceMillis = 5_000L,
                nowMillis = 18_000L,
                graceMillis = 12_000L
            )
        )
    }
}
