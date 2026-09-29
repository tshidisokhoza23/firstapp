package com.tpsk.tj.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tpsk.tj.common.data.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import com.tpsk.tj.R

import com.google.android.gms.ads.AdView

@Composable
fun DynamicAdCard(
    adManager: AdManager,
    adUnitId: String,
    modifier: Modifier = Modifier,
    isBanner: Boolean = false,
    isShortsAd: Boolean = false,
    onAdRevenuePaid: ((Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var adObject by remember { mutableStateOf<Any?>(null) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(adUnitId, isShortsAd, isBanner) {
        if (isBanner) {
            adManager.loadBannerAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { adObject = it },
                onAdFailed = { failed = true }
            )
        } else if (isShortsAd) {
            adManager.loadShortsNativeAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { adObject = it },
                onAdFailed = { failed = true },
                onAdRevenuePaid = onAdRevenuePaid
            )
        } else {
            adManager.loadNativeAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { adObject = it },
                onAdFailed = { failed = true },
                onAdRevenuePaid = onAdRevenuePaid
            )
        }
        onDispose {
            adManager.destroyAd(adObject)
        }
    }

    if (adObject == null) {
        if (failed || !isShortsAd) return
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    if (adObject is NativeAd) {
        val ad = adObject as NativeAd
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isShortsAd) Modifier.fillMaxSize() else Modifier.defaultMinSize(minHeight = 180.dp)),
            factory = { ctx ->
                val layoutRes = if (isShortsAd) R.layout.item_short_native_ad else R.layout.item_media_native_ad
                val view = LayoutInflater.from(ctx).inflate(layoutRes, null) as NativeAdView
                prepareNativeAdView(view, isShortsAd)
                bindNativeAd(view, ad)
                view
            },
            update = { view -> bindNativeAd(view, ad) }
        )
    }
else if (adObject is AdView) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { _ ->
                val adView = adObject as AdView
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView
            }
        )
    }
}
