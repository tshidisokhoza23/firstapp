package com.tpsk.tj.common.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.tpsk.tj.R
import com.tpsk.tj.common.data.ads.AdManager
import kotlinx.coroutines.delay

/**
 * Binds a native ad that is already loaded. The parent must already have a
 * fixed size. This composable never changes that size.
 */
@Composable
fun ReadyNativeAd(
    adObject: Any,
    modifier: Modifier = Modifier,
    layoutResId: Int = R.layout.item_media_native_ad,
    playMedia: Boolean = false,
    cornerRadius: Int = 12
) {
    val nativeAd = adObject as? NativeAd ?: return
    var bound by remember(adObject) { mutableStateOf(false) }

    Box(modifier) {
        if (!bound) {
            NativeAdShimmer(Modifier.fillMaxSize())
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius.dp)),
            factory = { context ->
                val container = FrameLayout(context)
                val adView = LayoutInflater.from(context).inflate(layoutResId, container, false) as NativeAdView
                adView.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                prepareNativeAdView(adView, fillParent = true)
                bindNativeAd(adView, nativeAd)
                container.addView(adView)
                container.post { bound = true }
                container
            },
            update = { container ->
                val adView = container.getChildAt(0) as? NativeAdView ?: return@AndroidView
                if (adView.tag != nativeAd) {
                    bindNativeAd(adView, nativeAd)
                }
                applyPlayback(adView, playMedia)
            },
            onRelease = { container ->
                container.removeAllViews()
                nativeAd.destroy()
            }
        )
    }
}

@Composable
fun NativeAdShimmer(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF1C1C1C)))
}

@Composable
fun awaitPooledNativeAd(
    adManager: AdManager,
    unitId: String,
    portrait: Boolean,
    slotKey: String,
    initial: Any? = null,
    fetch: Boolean = true
): Any? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ad by remember(slotKey) {
        mutableStateOf(
            initial ?: if (fetch) {
                com.tpsk.tj.common.data.ads.NativeAdPool.take(adManager, context, unitId, portrait)
            } else {
                null
            }
        )
    }
    LaunchedEffect(slotKey, unitId, portrait, fetch) {
        if (!fetch || ad != null) return@LaunchedEffect
        com.tpsk.tj.common.data.ads.NativeAdPool.warm(adManager, context, unitId, portrait)
        repeat(12) {
            delay(250)
            val next = com.tpsk.tj.common.data.ads.NativeAdPool.take(adManager, context, unitId, portrait)
            if (next != null) {
                ad = next
                return@LaunchedEffect
            }
        }
    }
    return ad
}

private fun applyPlayback(adView: NativeAdView, play: Boolean) {
    val media = adView.mediaView?.mediaContent ?: return
    if (!media.hasVideoContent()) return
    val controller = media.videoController ?: return
    if (play) {
        controller.mute(false)
        controller.play()
    } else {
        controller.pause()
        controller.mute(true)
    }
}


