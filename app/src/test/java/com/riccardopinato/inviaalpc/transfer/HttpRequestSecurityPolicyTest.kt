package com.riccardopinato.inviaalpc.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRequestSecurityPolicyTest {

    @Test
    fun acceptsExpectedLanHost() {
        assertTrue(
            HttpRequestSecurityPolicy
                .isExpectedHost(
                    hostHeader =
                        "192.168.1.42:8734",
                    localIp =
                        "192.168.1.42",
                    port = 8734
                )
        )
    }

    @Test
    fun rejectsUnexpectedHost() {
        assertFalse(
            HttpRequestSecurityPolicy
                .isExpectedHost(
                    hostHeader =
                        "attacker.example",
                    localIp =
                        "192.168.1.42",
                    port = 8734
                )
        )
    }

    @Test
    fun rejectsCrossSitePosts() {
        assertFalse(
            HttpRequestSecurityPolicy
                .isAllowedFetchSite(
                    method = "POST",
                    secFetchSite =
                        "cross-site"
                )
        )
    }

    @Test
    fun allowsSameOriginPosts() {
        assertTrue(
            HttpRequestSecurityPolicy
                .isAllowedFetchSite(
                    method = "POST",
                    secFetchSite =
                        "same-origin"
                )
        )
    }

    @Test
    fun allowsLegacyClientsWithoutFetchMetadata() {
        assertTrue(
            HttpRequestSecurityPolicy
                .isAllowedFetchSite(
                    method = "POST",
                    secFetchSite = null
                )
        )
    }

    @Test
    fun getRequestsAreNotBlockedByFetchMetadata() {
        assertTrue(
            HttpRequestSecurityPolicy
                .isAllowedFetchSite(
                    method = "GET",
                    secFetchSite =
                        "cross-site"
                )
        )
    }
}
