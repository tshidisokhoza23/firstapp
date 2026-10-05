package com.tpsk.tj.common.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import com.tpsk.tj.R
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.data.ads.FeedAdSlots
import com.tpsk.tj.common.data.ads.NativeAdPool

@Suppress("UNUSED_PARAMETER")
@Composable
fun DynamicAdCard(
    adManager: AdManager,
    adUnitId: String,
    modifier: Modifier = Modifier,
    isBanner: Boolean = false,
    isShortsAd: Boolean = false,
    slotKey: String = adUnitId,
    playMedia: Boolean = false,
    reserveSlot: Boolean = false,
    onAdRevenuePaid: ((Double) -> Unit)? = null
) {
    if (isBanner) {
        BannerAdCard(adManager, adUnitId, modifier)
        return
    }
    if (isShortsAd) {
        FullScreenPooledAd(
            adManager = adManager,
            adUnitId = adUnitId,
            slotKey = slotKey,
            playMedia = playMedia,
            layoutResId = R.layout.item_short_native_ad,
            modifier = modifier
        )
        return
    }
    FeedPooledAd(
        adManager = adManager,
        adUnitId = adUnitId,
        slotKey = slotKey,
        reserveSlot = reserveSlot,
        modifier = modifier
    )
}

@Composable
private fun BannerAdCard(
    adManager: AdManager,
    adUnitId: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var adObject by remember(adUnitId) { mutableStateOf<Any?>(null) }
    DisposableEffect(adUnitId) {
        adManager.loadBannerAd(
            context = context,
            adUnitId = adUnitId,
            onAdLoaded = { adObject = it },
            onAdFailed = { }
        )
        onDispose { adManager.destroyAd(adObject) }
    }
    val banner = adObject as? AdView ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp),
        factory = { _ ->
            (banner.parent as? ViewGroup)?.removeView(banner)
            banner
        }
    )
}

@Composable
private fun FullScreenPooledAd(
    adManager: AdManager,
    adUnitId: String,
    slotKey: String,
    playMedia: Boolean,
    layoutResId: Int,
    modifier: Modifier
) {
    val ad = awaitPooledNativeAd(
        adManager = adManager,
        unitId = adUnitId,
        portrait = true,
        slotKey = slotKey
    )
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (ad == null) {
            NativeAdShimmer(Modifier.fillMaxSize())
        } else {
            ReadyNativeAd(
                adObject = ad,
                modifier = Modifier.fillMaxSize(),
                layoutResId = layoutResId,
                playMedia = playMedia,
                cornerRadius = 0
            )
        }
    }
}

@Composable
private fun FeedPooledAd(
    adManager: AdManager,
    adUnitId: String,
    slotKey: String,
    reserveSlot: Boolean,
    modifier: Modifier
) {
    val context = LocalContext.current
    val claimed = remember(slotKey) {
        NativeAdPool.take(adManager, context, adUnitId, portrait = false)
    }
    val show = reserveSlot || FeedAdSlots.reserveIfReady(slotKey, claimed != null)
    val ad = awaitPooledNativeAd(
        adManager = adManager,
        unitId = adUnitId,
        portrait = false,
        slotKey = slotKey,
        initial = claimed,
        fetch = show && claimed == null
    )
    if (!show) return
    Box(
        modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (ad == null) {
            NativeAdShimmer(Modifier.fillMaxSize())
        } else {
            ReadyNativeAd(
                adObject = ad,
                modifier = Modifier.fillMaxSize(),
                layoutResId = R.layout.item_media_native_ad,
                playMedia = false
            )
        }
    }
}
