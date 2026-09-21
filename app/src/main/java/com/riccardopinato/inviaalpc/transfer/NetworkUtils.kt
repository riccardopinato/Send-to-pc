package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address

object NetworkUtils {

    fun findLanIpv4(context: Context): String? {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return findLanIpv4FromManager(manager)
    }

    fun findLanIpv4FromManager(manager: ConnectivityManager): String? {
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null

        val localTransport =
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        if (!localTransport) return null

        val properties = manager.getLinkProperties(network) ?: return null

        return properties.linkAddresses
            .asSequence()
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }
}
