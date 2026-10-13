package com.riccardopinato.inviaalpc.monetization

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AdaptiveBannerAd(
    adsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Bisect control: keep the v1.17 UI call-site intact
    // while removing the advertising SDK from the APK.
}
