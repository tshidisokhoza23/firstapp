package com.tpsk.tj.common.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.AdChoicesView
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
    view.headlineView = view.findViewById(R.id.ad_headline)
    view.bodyView = view.findViewById(R.id.ad_body)
    view.callToActionView = view.findViewById(R.id.ad_call_to_action)
    view.iconView = view.findViewById(R.id.ad_app_icon)
    view.advertiserView = view.findViewById(R.id.ad_advertiser)
    view.mediaView = view.findViewById(R.id.ad_media)
    if (!fillParent) {
        view.mediaView?.minimumHeight = (200 * view.resources.displayMetrics.density).toInt()
    }
    val options = view.findViewById<ViewGroup>(R.id.ad_options_view_container)
    if (options != null && view.adChoicesView == null) {
        val choices = AdChoicesView(view.context)
        options.removeAllViews()
        options.addView(
            choices,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        view.adChoicesView = choices
    }
}

internal fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as? TextView)?.text = ad.headline.orEmpty()
    val bodyView = view.bodyView
    if (bodyView is TextView) {
        val body = ad.body
        bodyView.text = body.orEmpty()
        bodyView.visibility = if (body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    val ctaView = view.callToActionView
    if (ctaView is Button) {
        val cta = ad.callToAction
        ctaView.text = cta.orEmpty()
        ctaView.visibility = if (cta.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    val iconView = view.iconView as? ImageView
    val icon = ad.icon
    if (iconView != null) {
        if (icon != null) {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(icon.drawable)
        } else {
            iconView.visibility = View.GONE
        }
    }
    val advertiserView = view.advertiserView as? TextView
    val advertiser = ad.advertiser
    if (advertiserView != null) {
        if (!advertiser.isNullOrBlank()) {
            advertiserView.visibility = View.VISIBLE
            advertiserView.text = advertiser
        } else {
            advertiserView.visibility = View.GONE
        }
    }
    view.mediaView?.mediaContent = ad.mediaContent
    view.mediaView?.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    if (view.tag != ad) {
        view.tag = ad
        view.setNativeAd(ad)
    }
}
