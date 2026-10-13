package com.riccardopinato.inviaalpc.monetization

import android.app.Activity
import android.content.Context

class ConsentManager private constructor() {
    val canRequestAds: Boolean
        get() = false

    val isPrivacyOptionsRequired: Boolean
        get() = false

    fun gather(
        activity: Activity,
        onComplete: (Any?) -> Unit
    ) {
        onComplete(null)
    }

    fun showPrivacyOptions(
        activity: Activity,
        onDismissed: (Any?) -> Unit
    ) {
        onDismissed(null)
    }

    companion object {
        private val instance =
            ConsentManager()

        fun getInstance(
            context: Context
        ): ConsentManager =
            instance
    }
}
