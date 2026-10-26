package com.tpsk.tj.common.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.tpsk.tj.R

/**
 * Binds a NativeAd to a NativeAdView exactly once.
 * Calling setNativeAd on every Compose recomposition detaches the media view
 * and leaves a blank card, which is what posts and shorts were showing.
 */
internal fun prepareNativeAdView(view: NativeAdView, fillParent: Boolean) {
    view.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        if (fillParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
    )
    val media = view.findViewById<View>(R.id.ad_media)
    if (!fillParent && media != null) {
        media.minimumHeight = (200 * view.resources.displayMetrics.density).toInt()
    }
}

internal fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    if (view.tag == ad) return
    try {
        val headline = view.findViewById<TextView>(R.id.ad_headline) ?: return
        headline.text = ad.headline.orEmpty()
        headline.visibility = View.VISIBLE
        view.headlineView = headline

        val bodyView = view.findViewById<TextView>(R.id.ad_body)
        val body = ad.body
        if (bodyView != null && !body.isNullOrBlank()) {
            bodyView.text = body
            bodyView.visibility = View.VISIBLE
            view.bodyView = bodyView
        } else {
            bodyView?.visibility = View.GONE
        }

        val ctaView = view.findViewById<Button>(R.id.ad_call_to_action)
        val cta = ad.callToAction
        if (ctaView != null && !cta.isNullOrBlank()) {
            ctaView.text = cta
            ctaView.visibility = View.VISIBLE
            view.callToActionView = ctaView
        } else {
            ctaView?.visibility = View.GONE
        }

        val iconView = view.findViewById<ImageView>(R.id.ad_app_icon)
        val icon = ad.icon?.drawable
        if (iconView != null && icon != null) {
            iconView.setImageDrawable(icon)
            iconView.visibility = View.VISIBLE
            view.iconView = iconView
        } else {
            iconView?.visibility = View.GONE
        }

        val advertiserView = view.findViewById<TextView>(R.id.ad_advertiser)
        val advertiser = ad.advertiser
        if (advertiserView != null && !advertiser.isNullOrBlank()) {
            advertiserView.text = advertiser
            advertiserView.visibility = View.VISIBLE
            view.advertiserView = advertiserView
        } else {
            advertiserView?.visibility = View.GONE
        }

        val media = view.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
        if (media != null && ad.mediaContent != null) {
            media.visibility = View.VISIBLE
            view.mediaView = media
            media.mediaContent = ad.mediaContent
            media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        }

        view.tag = ad
        view.setNativeAd(ad)
    } catch (e: RuntimeException) {
        android.util.Log.w("NativeAdBinder", "Skipping native ad that could not be bound", e)
        view.visibility = View.GONE
    }
}
