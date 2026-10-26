package com.tpsk.tj.common.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

object SlotAdRegistry {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun get(slotKey: String): Any? = map[slotKey]

    fun put(slotKey: String, ad: Any) {
        map[slotKey] = ad
    }

    fun clear() {
        map.clear()
    }
}

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
    cornerRadius: Int = 12,
    fillParent: Boolean = true
) {
    val nativeAd = adObject as? NativeAd ?: return

    Box(modifier) {
        AndroidView(
            modifier = if (fillParent) {
                Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius.dp))
            } else {
                Modifier.fillMaxWidth().clip(RoundedCornerShape(cornerRadius.dp))
            },
            factory = { context ->
                val container = FrameLayout(context)
                try {
                    val inflated = LayoutInflater.from(context).inflate(layoutResId, container, false)
                    (inflated as? NativeAdView)?.let { adView ->
                        adView.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            if (fillParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        (adView.parent as? ViewGroup)?.removeView(adView)
                        prepareNativeAdView(adView, fillParent = fillParent)
                        bindNativeAd(adView, nativeAd)
                        container.addView(adView)
                    }
                } catch (e: RuntimeException) {
                    android.util.Log.w("ReadyNativeAd", "Native ad view skipped", e)
                }
                container
            },
            update = { container ->
                val adView = container.getChildAt(0) as? NativeAdView ?: return@AndroidView
                try {
                    if (adView.tag != nativeAd) {
                        bindNativeAd(adView, nativeAd)
                    }
                    if (playbackState[adView] != playMedia) {
                        playbackState[adView] = playMedia
                        applyPlayback(adView, playMedia)
                    }
                } catch (e: RuntimeException) {
                    android.util.Log.w("ReadyNativeAd", "Native ad update skipped", e)
                    adView.visibility = android.view.View.GONE
                }
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
            initial
                ?: SlotAdRegistry.get(slotKey)
                ?: if (fetch) {
                    com.tpsk.tj.common.data.ads.NativeAdPool.take(adManager, context, unitId, portrait)?.also {
                        SlotAdRegistry.put(slotKey, it)
                    }
                } else {
                    null
                }
        )
    }
    LaunchedEffect(slotKey, unitId, portrait, fetch) {
        if (!fetch || ad != null) return@LaunchedEffect
        val cached = SlotAdRegistry.get(slotKey)
        if (cached != null) {
            ad = cached
            return@LaunchedEffect
        }
        com.tpsk.tj.common.data.ads.NativeAdPool.warm(adManager, context, unitId, portrait, target = 3)
        repeat(8) {
            delay(300)
            val next = com.tpsk.tj.common.data.ads.NativeAdPool.take(adManager, context, unitId, portrait)
            if (next != null) {
                SlotAdRegistry.put(slotKey, next)
                ad = next
                return@LaunchedEffect
            }
        }
    }
    return ad
}

private val playbackState = java.util.WeakHashMap<NativeAdView, Boolean>()

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
