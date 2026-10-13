package com.riccardopinato.inviaalpc.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.riccardopinato.inviaalpc.BuildConfig

class ConsentManager private constructor(
    context: Context
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(
            context.applicationContext
        )

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gather(
        activity: Activity,
        onComplete: (FormError?) -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        val paramsBuilder =
            ConsentRequestParameters.Builder()

        if (BuildConfig.ADMOB_DIAGNOSTIC_FORCE_EEA) {
            consentInformation.reset()

            val debugSettings =
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(
                        ConsentDebugSettings.DebugGeography
                            .DEBUG_GEOGRAPHY_EEA
                    )
                    .build()

            paramsBuilder.setConsentDebugSettings(
                debugSettings
            )

            Log.i(
                "Monetization",
                "UMP diagnostic geography forced to EEA"
            )
        }

        val params =
            paramsBuilder.build()

        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    try {
                        UserMessagingPlatform
                            .loadAndShowConsentFormIfRequired(
                                activity
                            ) { error ->
                                onComplete(error)
                            }
                    } catch (throwable: Throwable) {
                        onFailure(throwable)
                    }
                },
                { error ->
                    onComplete(error)
                }
            )
        } catch (throwable: Throwable) {
            onFailure(throwable)
        }
    }

    fun showPrivacyOptions(
        activity: Activity,
        onDismissed: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            onDismissed
        )
    }

    companion object {
        @Volatile
        private var instance: ConsentManager? = null

        fun getInstance(
            context: Context
        ): ConsentManager =
            instance
                ?: synchronized(this) {
                    instance
                        ?: ConsentManager(
                            context
                        ).also {
                            instance = it
                        }
                }
    }
}
