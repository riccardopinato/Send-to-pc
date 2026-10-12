package com.riccardopinato.inviaalpc.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.riccardopinato.inviaalpc.transfer.TransferOperationSummary
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MonetizationState(
    val consentResolved: Boolean = false,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val mobileAdsInitialized: Boolean = false,
    val interstitialReady: Boolean = false,
    val productionConfigured: Boolean =
        AdMobConfig.productionConfigured
)

object MonetizationController {
    private const val TAG = "Monetization"
    private const val PREFS =
        "monetization_preferences"
    private const val KEY_LAST_INTERSTITIAL =
        "last_interstitial_at"

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    private val _state =
        MutableStateFlow(
            MonetizationState()
        )

    val state: StateFlow<MonetizationState> =
        _state.asStateFlow()

    private val shownSessionIds =
        Collections.synchronizedSet(
            mutableSetOf<String>()
        )

    @Volatile
    private var sdkInitializing = false

    @Volatile
    private var interstitialLoading = false

    @Volatile
    private var adsDisabledForSession = false

    @Volatile
    private var interstitialAd:
        InterstitialAd? = null

    private lateinit var appContext:
        Context

    private lateinit var consentManager:
        ConsentManager

    fun initialize(
        activity: Activity
    ) {
        if (adsDisabledForSession) {
            return
        }

        try {
            appContext =
                activity.applicationContext

            consentManager =
                ConsentManager.getInstance(
                    appContext
                )

            refreshConsentState(
                resolved = false
            )

            consentManager.gather(
                activity = activity,
                onComplete = { error ->
                    error?.let {
                        Log.w(
                            TAG,
                            "Consent update: " +
                                it.message
                        )
                    }

                    refreshConsentState(
                        resolved = true
                    )

                    if (
                        !adsDisabledForSession &&
                        consentManager
                            .canRequestAds
                    ) {
                        initializeMobileAds()
                    }
                },
                onFailure = {
                    disableForSession(
                        stage = "ump_gather",
                        throwable = it
                    )
                }
            )
        } catch (throwable: Throwable) {
            disableForSession(
                stage = "monetization_initialize",
                throwable = throwable
            )
        }
    }

    fun disableForSession(
        stage: String,
        throwable: Throwable
    ) {
        adsDisabledForSession = true
        sdkInitializing = false
        interstitialLoading = false

        runCatching {
            interstitialAd?.destroy()
        }

        interstitialAd = null

        _state.value =
            _state.value.copy(
                consentResolved = true,
                canRequestAds = false,
                mobileAdsInitialized = false,
                interstitialReady = false
            )

        Log.e(
            TAG,
            "Ads disabled for this session at " +
                stage,
            throwable
        )
    }

    fun showPrivacyOptions(
        activity: Activity,
        onComplete: (String?) -> Unit =
            {}
    ) {
        if (
            adsDisabledForSession ||
            !::consentManager
                .isInitialized
        ) {
            onComplete(
                "Consent manager not ready"
            )
            return
        }

        try {
            consentManager.showPrivacyOptions(
                activity
            ) { error ->
                refreshConsentState(
                    resolved = true
                )

                if (
                    !adsDisabledForSession &&
                    consentManager
                        .canRequestAds
                ) {
                    initializeMobileAds()
                }

                onComplete(
                    error?.message
                )
            }
        } catch (throwable: Throwable) {
            disableForSession(
                stage = "privacy_options",
                throwable = throwable
            )
            onComplete(
                throwable.message
            )
        }
    }

    fun preloadInterstitial() {
        val current = _state.value

        if (
            adsDisabledForSession ||
            !current.canRequestAds ||
            !current.mobileAdsInitialized ||
            interstitialLoading ||
            interstitialAd != null
        ) {
            return
        }

        interstitialLoading = true

        try {
            val request =
                AdRequest.Builder(
                    AdMobConfig.interstitialId
                ).build()

            InterstitialAd.load(
                request,
                object :
                    AdLoadCallback<
                        InterstitialAd
                    > {
                    override fun onAdLoaded(
                        ad: InterstitialAd
                    ) {
                        interstitialLoading =
                            false

                        if (
                            adsDisabledForSession
                        ) {
                            runCatching {
                                ad.destroy()
                            }
                            return
                        }

                        interstitialAd = ad
                        _state.value =
                            _state.value.copy(
                                interstitialReady =
                                    true
                            )
                    }

                    override fun onAdFailedToLoad(
                        adError: LoadAdError
                    ) {
                        interstitialLoading =
                            false
                        interstitialAd = null
                        _state.value =
                            _state.value.copy(
                                interstitialReady =
                                    false
                            )

                        Log.w(
                            TAG,
                            "Interstitial load failed: " +
                                adError.message
                        )
                    }
                }
            )
        } catch (throwable: Throwable) {
            disableForSession(
                stage = "interstitial_preload",
                throwable = throwable
            )
        }
    }

    fun showInterstitialIfEligible(
        activity: Activity,
        summary: TransferOperationSummary
    ): Boolean {
        if (
            adsDisabledForSession ||
            !_state.value
                .canRequestAds ||
            !_state.value
                .mobileAdsInitialized
        ) {
            return false
        }

        return try {
            val prefs =
                appContext
                    .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                    )

            val lastShown =
                prefs.getLong(
                    KEY_LAST_INTERSTITIAL,
                    0L
                )

            val alreadyShown =
                shownSessionIds.contains(
                    summary.sessionId
                )

            if (
                !InterstitialPolicy
                    .shouldShow(
                        summary =
                            summary,
                        lastShownAtMillis =
                            lastShown,
                        alreadyShownForSession =
                            alreadyShown
                    )
            ) {
                preloadInterstitial()
                return false
            }

            val ad =
                interstitialAd
                    ?: run {
                        preloadInterstitial()
                        return false
                    }

            interstitialAd = null
            _state.value =
                _state.value.copy(
                    interstitialReady =
                        false
                )

            shownSessionIds.add(
                summary.sessionId
            )

            prefs.edit()
                .putLong(
                    KEY_LAST_INTERSTITIAL,
                    System.currentTimeMillis()
                )
                .apply()

            ad.adEventCallback =
                object :
                    InterstitialAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        preloadInterstitial()
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        fullScreenContentError:
                            FullScreenContentError
                    ) {
                        Log.w(
                            TAG,
                            "Interstitial show failed: " +
                                fullScreenContentError.message
                        )
                        preloadInterstitial()
                    }
                }

            ad.show(activity)
            true
        } catch (throwable: Throwable) {
            disableForSession(
                stage = "interstitial_show",
                throwable = throwable
            )
            false
        }
    }

    private fun refreshConsentState(
        resolved: Boolean
    ) {
        if (
            adsDisabledForSession ||
            !::consentManager
                .isInitialized
        ) {
            return
        }

        try {
            _state.value =
                _state.value.copy(
                    consentResolved =
                        resolved,
                    canRequestAds =
                        consentManager
                            .canRequestAds,
                    privacyOptionsRequired =
                        consentManager
                            .isPrivacyOptionsRequired
                )
        } catch (throwable: Throwable) {
            disableForSession(
                stage = "consent_state",
                throwable = throwable
            )
        }
    }

    @Synchronized
    private fun initializeMobileAds() {
        if (
            adsDisabledForSession ||
            sdkInitializing ||
            _state.value
                .mobileAdsInitialized ||
            !_state.value
                .canRequestAds
        ) {
            return
        }

        sdkInitializing = true

        scope.launch {
            try {
                MobileAds.initialize(
                    appContext,
                    InitializationConfig
                        .Builder(
                            AdMobConfig.appId
                        )
                        .build()
                ) {
                    sdkInitializing =
                        false

                    if (
                        !adsDisabledForSession
                    ) {
                        _state.value =
                            _state.value.copy(
                                mobileAdsInitialized =
                                    true
                            )

                        preloadInterstitial()
                    }
                }
            } catch (throwable: Throwable) {
                disableForSession(
                    stage = "gma_initialize",
                    throwable = throwable
                )
            }
        }
    }
}
