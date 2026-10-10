package com.jericx.trainr.presentation.common.components.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// One anchored banner sized to the slot it sits in, the shape Google reserves
// space for up front so the layout does not jump when the ad arrives. Measured
// against the slot's own width and never the screen's: the two agree only while
// the banner is as wide as the window, and the wrong width reserves the wrong
// height.
@Composable
fun AdBanner(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
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
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}
