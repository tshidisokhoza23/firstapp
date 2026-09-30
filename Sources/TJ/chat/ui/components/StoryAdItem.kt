package com.tpsk.tj.chat.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import com.tpsk.tj.R
import com.tpsk.tj.chat.model.StoryItemWrapper
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.MediaContent

/**
 * STEP 4: Data Binding / View Presentation Component
 * Compose-native wrapper for the Story Ad Layout.
 */
@Composable
fun StoryAdItem(
    adContent: StoryItemWrapper.AdContent,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.item_story_ad, null) as NativeAdView
            populateNativeAdView(adContent.nativeAd, view)
            view
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            populateNativeAdView(adContent.nativeAd, view)
        }
    )

    // STEP 5: State Machine & Safety Handling Mechanics
    DisposableEffect(adContent.id) {
        onDispose {
            // CONSTRAINT 1 & 5: Garbage Collection Optimization. 
            // Destroy immediately when detached to prevent OOM and overheating.
            adContent.nativeAd.destroy()
        }
    }
}

/**
 * TRADITIONAL VIEW PRESENTATION:
 * Native Ad ViewHolder for RecyclerView-based story implementations.
 */
class StoryAdViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.item_story_ad, parent, false)
) {
    private val adView: NativeAdView = itemView as NativeAdView

    fun bind(nativeAd: NativeAd) {
        populateNativeAdView(nativeAd, adView)
    }
}

/**
 * PRODUCTION-READY BINDING ROUTINE:
 * Maps AdMob assets to XML fields and registers the view for clicks.
 */
fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // 1. Map ID fields
    adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)

    // 2. Set Basic Content
    (adView.headlineView as? TextView)?.text = nativeAd.headline
    nativeAd.mediaContent?.let { content: MediaContent ->
        adView.mediaView?.setMediaContent(content)
    }

    // 3. Handle Optional Properties (Failsafe Hiding)
    if (nativeAd.body == null) {
        adView.bodyView?.visibility = View.INVISIBLE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = View.INVISIBLE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        adView.iconView?.visibility = View.GONE
    } else {
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView?.visibility = View.VISIBLE
    }

    // 4. FINAL NOTIFICATION: Register NativeAd object
    adView.setNativeAd(nativeAd)
}