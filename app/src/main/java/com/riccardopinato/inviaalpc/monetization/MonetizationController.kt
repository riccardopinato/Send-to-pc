package com.riccardopinato.inviaalpc.monetization

import android.app.Activity
import com.riccardopinato.inviaalpc.transfer.TransferOperationSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MonetizationState(
    val consentResolved: Boolean = true,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val mobileAdsInitialized: Boolean = false,
    val interstitialReady: Boolean = false,
    val productionConfigured: Boolean = false
)

object MonetizationController {
    private val _state =
        MutableStateFlow(
            MonetizationState()
        )

    val state: StateFlow<MonetizationState> =
        _state.asStateFlow()

    fun initialize(
        activity: Activity
    ) = Unit

    fun showPrivacyOptions(
        activity: Activity,
        onComplete: (String?) -> Unit = {}
    ) {
        onComplete(null)
    }

    fun preloadInterstitial() = Unit

    fun showInterstitialIfEligible(
        activity: Activity,
        summary: TransferOperationSummary
    ): Boolean =
        false
}
