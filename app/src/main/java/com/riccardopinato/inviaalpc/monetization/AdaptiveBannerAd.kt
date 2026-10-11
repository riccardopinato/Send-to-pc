package com.riccardopinato.inviaalpc.monetization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult

@Composable
fun AdaptiveBannerAd(
    adsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!adsEnabled) {
        return
    }

    val context =
        LocalContext.current

    val activity =
        remember(context) {
            context.findActivity()
        } ?: return

    val widthDp =
        LocalConfiguration.current
            .screenWidthDp
            .coerceAtLeast(1)

    val isPreview =
        LocalInspectionMode.current

    var bannerAd by
        remember {
            mutableStateOf<
                BannerAd?
            >(null)
        }

    LaunchedEffect(
        adsEnabled,
        widthDp,
        AdMobConfig.bannerId
    ) {
        bannerAd?.destroy()
        bannerAd = null

        if (
            !adsEnabled ||
            isPreview
        ) {
            return@LaunchedEffect
        }

        val adSize =
            AdSize
                .getLargeAnchoredAdaptiveBannerAdSize(
                    context,
                    widthDp
                )

        when (
            val result =
                BannerAd.load(
                    BannerAdRequest
                        .Builder(
                            AdMobConfig.bannerId,
                            adSize
                        )
                        .build()
                )
        ) {
            is AdLoadResult.Success ->
                bannerAd =
                    result.ad

            is AdLoadResult.Failure ->
                Log.w(
                    "Monetization",
                    "Banner load failed: " +
                        result.error
                )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bannerAd?.destroy()
        }
    }

    bannerAd?.let { ad ->
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
        ) {
            AndroidView(
                modifier =
                    Modifier
                        .wrapContentSize(),
                factory = {
                    ad.getView(
                        activity
                    )
                }
            )
        }
    }
}

private tailrec fun Context.findActivity():
    Activity? =
    when (this) {
        is Activity ->
            this

        is ContextWrapper ->
            baseContext
                .findActivity()

        else ->
            null
    }
