package com.odom.docreader.core.ads

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Anchored adaptive banner for the bottom of a screen. Takes no space until an ad has loaded, so
 * offline or without consent the screen looks as if there were no ad slot at all.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier, viewModel: AdsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.adsReady || LocalInspectionMode.current) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adView = remember { AdView(context) }
        var loaded by remember { mutableStateOf(false) }

        LaunchedEffect(adView, widthDp) {
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            val request = BannerAdRequest.Builder(viewModel.bannerAdUnitId, adSize).build()
            adView.loadAd(
                request,
                object : AdLoadCallback<BannerAd> {
                    // The SDK calls back on a background thread.
                    override fun onAdLoaded(ad: BannerAd) {
                        adView.post { loaded = true }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w("Ads", "Banner failed to load: ${adError.code}")
                    }
                },
            )
        }
        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }

        AndroidView(
            factory = { adView },
            modifier = if (loaded) Modifier.fillMaxWidth().navigationBarsPadding() else Modifier.height(0.dp),
        )
    }
}
