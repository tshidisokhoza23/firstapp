package com.tpsk.tj.common.ui.components

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.tpsk.tj.R
import java.util.ArrayDeque

/**
 * One native ad per feed slot.
 *
 * AdMob rules this follows:
 * - A NativeAd is owned by exactly one NativeAdView.
 * - Asset views are registered before setNativeAd.
 * - setNativeAd runs only after the view is laid out, so the SDK size check passes.
 * - AdChoices is placed by the SDK, not forced into a tiny custom view.
 * - The ad is destroyed when its slot leaves the screen, and nowhere else.
 */
@Composable
fun FeedNativeAd(
    adUnitId: String,
    slotKey: String,
    modifier: Modifier = Modifier,
    layoutResId: Int = R.layout.native_ad_social_item
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var nativeAd by remember(slotKey) { mutableStateOf<NativeAd?>(null) }
    var bound by remember(slotKey) { mutableStateOf(false) }
    var failed by remember(slotKey) { mutableStateOf(false) }
    val news = layoutResId == R.layout.native_ad_news
    val slotHeight = if (news) NewsSlotHeight else SocialSlotHeight
    val height by animateDpAsState(
        targetValue = if (failed) 0.dp else slotHeight,
        animationSpec = tween(durationMillis = 180),
        label = "adSlotHeight"
    )

    DisposableEffect(slotKey, adUnitId) {
        failed = false
        val owner = SlotOwner()
        val cancel = FeedNativeAdLoader.load(
            context.applicationContext,
            adUnitId,
            onLoaded = { ad ->
                if (owner.disposed) {
                    ad.destroy()
                } else {
                    owner.ad = ad
                    nativeAd = ad
                }
            },
            onFailed = { failed = true }
        )
        onDispose {
            owner.disposed = true
            cancel()
            owner.ad?.destroy()
            owner.ad = null
            nativeAd = null
            bound = false
        }
    }

    if (failed && height < 1.dp) return
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        if (!bound) {
            AdSlotSkeleton(news = news)
        }
        val ad = nativeAd ?: return@Box
        AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(slotHeight)
            .alpha(if (bound) 1f else 0f),
        factory = { viewContext ->
            val root = FrameLayout(viewContext)
            try {
                val adView = LayoutInflater.from(viewContext)
                    .inflate(layoutResId, root, false) as NativeAdView
                adView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                root.addView(adView)
                bindWhenLaidOut(
                    adView,
                    ad,
                    onBound = { bound = true },
                    onFailed = { failed = true }
                )
            } catch (e: Exception) {
                android.util.Log.w("FeedNativeAd", "Ad layout skipped", e)
                failed = true
            }
            root
        },
        update = { root ->
            val adView = root.getChildAt(0) as? NativeAdView ?: return@AndroidView
            if (adView.tag != ad) {
                bindWhenLaidOut(
                    adView,
                    ad,
                    onBound = { bound = true },
                    onFailed = { failed = true }
                )
            } else if (!bound) {
                bound = true
            }
        }
        )
    }
}

private val SocialSlotHeight = 398.dp
private val NewsSlotHeight = 364.dp

@Composable
private fun AdSlotSkeleton(news: Boolean) {
    if (news) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(NewsSlotHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            ShimmerEffect(Modifier.fillMaxWidth().height(200.dp))
            Column(Modifier.padding(16.dp)) {
                ShimmerEffect(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(12.dp))
                ShimmerEffect(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                ShimmerEffect(
                    Modifier
                        .fillMaxWidth(0.84f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(16.dp))
                ShimmerEffect(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .height(SocialSlotHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row {
                ShimmerEffect(Modifier.size(42.dp).clip(CircleShape))
                Spacer(Modifier.width(10.dp))
                ShimmerEffect(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.46f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            Spacer(Modifier.height(16.dp))
            ShimmerEffect(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(8.dp))
            ShimmerEffect(
                Modifier
                    .fillMaxWidth(0.78f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(12.dp))
            ShimmerEffect(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))
            ShimmerEffect(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

private class SlotOwner {
    var disposed: Boolean = false
    var ad: NativeAd? = null
}

private fun bindWhenLaidOut(
    adView: NativeAdView,
    ad: NativeAd,
    onBound: () -> Unit = {},
    onFailed: () -> Unit = {}
) {
    if (adView.tag == ad) {
        onBound()
        return
    }
    val commit = Runnable {
        if (!adView.isAttachedToWindow || adView.tag == ad) return@Runnable
        val minPx = (32 * adView.resources.displayMetrics.density).toInt()
        if (adView.width < minPx || adView.height < minPx) return@Runnable
        try {
            registerAndBind(adView, ad)
            onBound()
        } catch (e: RuntimeException) {
            android.util.Log.w("FeedNativeAd", "Native ad was not shown", e)
            adView.visibility = View.INVISIBLE
            onFailed()
        }
    }
    val minPx = (32 * adView.resources.displayMetrics.density).toInt()
    adView.minimumWidth = minPx
    adView.minimumHeight = minPx
    if (adView.isLaidOut && adView.width >= minPx && adView.height >= minPx) {
        commit.run()
        return
    }
    val observer = adView.viewTreeObserver
    val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (adView.width < minPx || adView.height < minPx) return
            if (adView.viewTreeObserver.isAlive) {
                adView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
            commit.run()
        }
    }
    observer.addOnGlobalLayoutListener(listener)
    adView.post(commit)
}

private fun registerAndBind(adView: NativeAdView, ad: NativeAd) {
    val headline = adView.findViewById<TextView>(R.id.ad_headline) ?: return
    headline.text = ad.headline.orEmpty()
    headline.visibility = View.VISIBLE
    adView.headlineView = headline

    val body = adView.findViewById<TextView>(R.id.ad_body)
    if (body != null && !ad.body.isNullOrBlank()) {
        body.text = ad.body
        body.visibility = View.VISIBLE
        adView.bodyView = body
    } else if (body != null) {
        body.text = ""
        body.visibility = View.INVISIBLE
    }

    val cta = adView.findViewById<Button>(R.id.ad_call_to_action)
    if (cta != null && !ad.callToAction.isNullOrBlank()) {
        cta.text = ad.callToAction
        cta.visibility = View.VISIBLE
        adView.callToActionView = cta
    } else if (cta != null) {
        cta.text = ""
        cta.visibility = View.INVISIBLE
    }

    val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val drawable = ad.icon?.drawable
    if (icon != null && drawable != null) {
        icon.setImageDrawable(drawable)
        icon.visibility = View.VISIBLE
        adView.iconView = icon
    } else if (icon != null) {
        icon.setImageDrawable(null)
        icon.visibility = View.INVISIBLE
    }

    val advertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
    if (advertiser != null && !ad.advertiser.isNullOrBlank()) {
        advertiser.text = ad.advertiser
        advertiser.visibility = View.VISIBLE
        adView.advertiserView = advertiser
    } else {
        advertiser?.visibility = View.GONE
    }

    val media = adView.findViewById<MediaView>(R.id.ad_media)
    if (media != null && ad.mediaContent != null) {
        media.visibility = View.VISIBLE
        adView.mediaView = media
        media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        media.mediaContent = ad.mediaContent
    } else if (media != null) {
        media.visibility = View.INVISIBLE
    }

    adView.setNativeAd(ad)
    adView.tag = ad
}

/**
 * At most two native requests are in flight. Extra slots wait instead of
 * flooding the ad unit, which is what was returning no fill.
 */
internal object FeedNativeAdLoader {
    private const val MAX_IN_FLIGHT = 2
    private val main = Handler(Looper.getMainLooper())
    private var inFlight = 0
    private val pending = ArrayDeque<Request>()

    private class Request(
        val context: android.content.Context,
        val unitId: String,
        val onLoaded: (NativeAd) -> Unit,
        val onFailed: (String) -> Unit
    ) {
        @Volatile var cancelled: Boolean = false
    }

    fun load(
        context: android.content.Context,
        unitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit
    ): () -> Unit {
        val request = Request(context, unitId, onLoaded, onFailed)
        main.post {
            pending.add(request)
            drain()
        }
        return { request.cancelled = true }
    }

    private fun drain() {
        while (inFlight < MAX_IN_FLIGHT && pending.isNotEmpty()) {
            val request = pending.removeFirst()
            if (request.cancelled) continue
            inFlight++
            val finished = booleanArrayOf(false)
            fun finish(success: NativeAd?, error: String?) {
                if (finished[0]) return
                finished[0] = true
                inFlight = (inFlight - 1).coerceAtLeast(0)
                if (success != null) request.onLoaded(success) else request.onFailed(error ?: "No ad")
                drain()
            }
            try {
                val options = NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                    .setRequestMultipleImages(false)
                    .setVideoOptions(
                        VideoOptions.Builder()
                            .setStartMuted(true)
                            .setCustomControlsRequested(false)
                            .build()
                    )
                    .build()
                val loader = AdLoader.Builder(request.context, request.unitId)
                    .forNativeAd { ad -> finish(ad, null) }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            finish(null, error.message)
                        }
                    })
                    .withNativeAdOptions(options)
                    .build()
                loader.loadAd(AdRequest.Builder().build())
            } catch (e: RuntimeException) {
                finish(null, e.message)
            }
        }
    }
}
