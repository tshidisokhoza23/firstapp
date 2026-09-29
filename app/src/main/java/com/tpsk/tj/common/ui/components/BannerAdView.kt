package com.tpsk.tj.common.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

import com.tpsk.tj.common.data.ads.AdUnitConfig
import com.tpsk.tj.BuildConfig

@Composable
fun BannerAdView(
    adUnitId: String = AdUnitConfig.BANNER_MAIN_ID,
    modifier: Modifier = Modifier
) {
    var adLoaded by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 6.dp)
            .background(if (BuildConfig.DEBUG && !adLoaded) Color.Red.copy(alpha = 0.05f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (!adLoaded && BuildConfig.DEBUG) {
            Text(
                text = adError ?: "Loading Banner Ad...",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val displayMetrics = ctx.resources.displayMetrics
                    val widthPixels = displayMetrics.widthPixels
                    val density = displayMetrics.density
                    val adWidth = (widthPixels / density).toInt()
                    val adaptiveAdSize = if (adWidth > 0) {
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
                    } else {
                        AdSize.BANNER
                    }
                    setAdSize(adaptiveAdSize)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            adLoaded = true
                            android.util.Log.d("BannerAdView", "SUCCESS: Banner loaded for $adUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            adError = "Ad Failed: ${error.message} (${error.code})"
                            android.util.Log.e("BannerAdView", "FAILURE: Banner failed to load: ${error.message} (code: ${error.code}) for $adUnitId")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { view ->
                if (view.adUnitId != adUnitId) {
                    view.adUnitId = adUnitId
                    view.loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
