package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address

object NetworkUtils {

    fun findLanIpv4(context: Context): String? {
        val manager =
            context.getSystemService(
                ConnectivityManager::class.java
            )

        return findLanIpv4FromManager(
            manager
        )
    }

    fun findLanIpv4FromManager(
        manager: ConnectivityManager
    ): String? {
        val active =
            manager.activeNetwork

        val candidates =
            buildList<Network> {
                active?.let {
                    add(it)
                }

                manager.allNetworks
                    .forEach { network ->
                        if (network != active) {
                            add(network)
                        }
                    }
            }

        return candidates
            .asSequence()
            .mapNotNull { network ->
                val capabilities =
                    manager.getNetworkCapabilities(
                        network
                    ) ?: return@mapNotNull null

                if (
                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_VPN
                    )
                ) {
                    return@mapNotNull null
                }

                val localTransport =
                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) ||
                        capabilities.hasTransport(
                            NetworkCapabilities.TRANSPORT_ETHERNET
                        )

                if (!localTransport) {
                    return@mapNotNull null
                }

                val properties =
                    manager.getLinkProperties(
                        network
                    ) ?: return@mapNotNull null

                properties.linkAddresses
                    .asSequence()
                    .map {
                        it.address
                    }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull {
                        !it.isLoopbackAddress &&
                            it.isSiteLocalAddress
                    }
                    ?.hostAddress
            }
            .firstOrNull()
    }
}
