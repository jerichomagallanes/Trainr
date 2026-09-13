package com.jericx.trainr.presentation.common.components.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// One anchored banner sized to the screen's width, the shape Google reserves
// space for up front so the layout does not jump when the ad arrives.
@Composable
fun AdBanner(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember(adUnitId, widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        adView.resume()
        onDispose {
            adView.pause()
            adView.destroy()
        }
    }
    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
}
