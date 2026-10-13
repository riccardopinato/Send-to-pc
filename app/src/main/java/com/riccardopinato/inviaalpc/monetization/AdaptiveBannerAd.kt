package com.riccardopinato.inviaalpc.monetization

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdaptiveBannerAd(
    adsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!adsEnabled) {
        return
    }

    val context = LocalContext.current
    val widthDp =
        LocalConfiguration.current
            .screenWidthDp
            .coerceAtLeast(1)
    val isPreview =
        LocalInspectionMode.current

    val adView =
        remember(
            context,
            AdMobConfig.bannerId
        ) {
            AdView(context).apply {
                adUnitId =
                    AdMobConfig.bannerId

                adListener =
                    object : AdListener() {
                        override fun onAdFailedToLoad(
                            error: LoadAdError
                        ) {
                            Log.w(
                                "Monetization",
                                "Banner load failed: " +
                                    error.message
                            )
                        }
                    }
            }
        }

    LaunchedEffect(
        adsEnabled,
        widthDp,
        AdMobConfig.bannerId
    ) {
        if (
            !adsEnabled ||
            isPreview
        ) {
            return@LaunchedEffect
        }

        try {
            adView.setAdSize(
                AdSize
                    .getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        widthDp
                    )
            )

            adView.loadAd(
                AdRequest
                    .Builder()
                    .build()
            )
        } catch (throwable: Throwable) {
            MonetizationController
                .disableForSession(
                    stage = "banner_load",
                    throwable = throwable
                )
        }
    }

    DisposableEffect(adView) {
        onDispose {
            runCatching {
                adView.destroy()
            }
        }
    }

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
                adView
            }
        )
    }
}
