package com.tpsk.tj.common.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tpsk.tj.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun StaticAdCard(
    nativeAd: Any,
    modifier: Modifier = Modifier,
    layoutResId: Int = R.layout.item_media_native_ad
) {
    if (nativeAd !is NativeAd) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(layoutResId, null) as NativeAdView
            
            view.headlineView = view.findViewById(R.id.ad_headline)
            view.bodyView = view.findViewById(R.id.ad_body)
            view.callToActionView = view.findViewById(R.id.ad_call_to_action)
            view.iconView = view.findViewById(R.id.ad_app_icon)
            view.advertiserView = view.findViewById(R.id.ad_advertiser)
            view.mediaView = view.findViewById(R.id.ad_media)
            
            view
        },
        update = { view ->
            val ad = nativeAd as NativeAd
            
            val headline = ad.headline
            if (headline != null) (view.headlineView as? TextView)?.text = headline
            
            val body = ad.body
            if (body != null) (view.bodyView as? TextView)?.text = body
            
            val cta = ad.callToAction
            if (cta != null) (view.callToActionView as? Button)?.text = cta
            
            val icon = ad.icon
            if (icon != null) {
                val iconView = view.iconView as? android.widget.ImageView
                iconView?.visibility = android.view.View.VISIBLE
                iconView?.setImageDrawable(icon.drawable)
            } else {
                view.iconView?.visibility = android.view.View.GONE
            }
            
            val advertiser = ad.advertiser
            if (advertiser != null) {
                val advView = view.advertiserView as? TextView
                advView?.visibility = android.view.View.VISIBLE
                advView?.text = advertiser
            } else {
                view.advertiserView?.visibility = android.view.View.GONE
            }
            
            view.setNativeAd(ad)
        }
    )
}
