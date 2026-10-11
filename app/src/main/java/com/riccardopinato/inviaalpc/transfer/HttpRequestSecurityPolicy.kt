package com.riccardopinato.inviaalpc.transfer

object HttpRequestSecurityPolicy {

    fun isExpectedHost(
        hostHeader: String?,
        localIp: String,
        port: Int
    ): Boolean {
        val host =
            hostHeader
                ?.trim()
                ?.lowercase()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false

        val expected =
            localIp
                .trim()
                .lowercase()

        return host ==
            "$expected:$port" ||
            (
                port == 443 &&
                    host == expected
                )
    }

    fun isAllowedFetchSite(
        method: String,
        secFetchSite: String?
    ): Boolean {
        if (
            method.uppercase() !=
            "POST"
        ) {
            return true
        }

        val value =
            secFetchSite
                ?.trim()
                ?.lowercase()
                .orEmpty()

        return value.isBlank() ||
            value == "same-origin" ||
            value == "none"
    }
}
