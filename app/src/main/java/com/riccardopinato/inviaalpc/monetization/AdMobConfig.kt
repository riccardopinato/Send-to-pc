package com.riccardopinato.inviaalpc.monetization

import com.riccardopinato.inviaalpc.BuildConfig

object AdMobConfig {
    const val TEST_APP_ID =
        "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID =
        "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID =
        "ca-app-pub-3940256099942544/1033173712"

    val appId: String
        get() = BuildConfig.ADMOB_APP_ID

    val bannerId: String
        get() = BuildConfig.ADMOB_BANNER_ID

    val interstitialId: String
        get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    val productionConfigured: Boolean
        get() = BuildConfig.ADMOB_PRODUCTION_CONFIGURED
}
