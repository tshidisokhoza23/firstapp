package com.tpsk.tj.common.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
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
    layoutResId: Int = R.layout.native_ad_social_item,
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
        layoutResId = layoutResId,
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
    val cached = remember(slotKey) { SlotAdRegistry.get(slotKey) ?: FeedAdCache.get(slotKey) }
    val ad = awaitPooledNativeAd(
        adManager = adManager,
        unitId = adUnitId,
        portrait = true,
        slotKey = slotKey,
        initial = cached,
        fetch = cached == null
    ) ?: cached

    if (ad != null) {
        FeedAdCache.put(slotKey, ad)
        SlotAdRegistry.put(slotKey, ad)
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (ad != null) {
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

object FeedAdCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun get(slotKey: String): Any? = cache[slotKey]

    fun put(slotKey: String, ad: Any) {
        cache[slotKey] = ad
    }

    fun clear() {
        cache.clear()
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun FeedPooledAd(
    adManager: AdManager,
    adUnitId: String,
    slotKey: String,
    reserveSlot: Boolean,
    layoutResId: Int,
    modifier: Modifier
) {
    val context = LocalContext.current
    val cachedAd = remember(slotKey) { SlotAdRegistry.get(slotKey) ?: FeedAdCache.get(slotKey) }
    val ad = awaitPooledNativeAd(
        adManager = adManager,
        unitId = adUnitId,
        portrait = false,
        slotKey = slotKey,
        initial = cachedAd,
        fetch = cachedAd == null
    ) ?: cachedAd

    if (ad != null) {
        FeedAdCache.put(slotKey, ad)
        SlotAdRegistry.put(slotKey, ad)
    }

    if (ad == null && !reserveSlot) return

    Box(
        modifier
            .fillMaxWidth()
            .then(if (ad == null) Modifier.height(0.dp) else Modifier.wrapContentHeight())
    ) {
        if (ad != null) {
            ReadyNativeAd(
                adObject = ad,
                modifier = Modifier.fillMaxWidth(),
                layoutResId = layoutResId,
                playMedia = false,
                cornerRadius = 0,
                fillParent = false
            )
        } else if (reserveSlot) {
            NativeAdShimmer(modifier = Modifier.fillMaxWidth().height(280.dp))
        }
    }
}
