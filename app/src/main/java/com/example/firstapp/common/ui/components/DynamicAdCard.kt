package com.example.firstapp.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.common.data.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Button
import com.example.firstapp.R

import com.google.android.gms.ads.AdView

@Composable
fun DynamicAdCard(
    adManager: AdManager,
    adUnitId: String,
    modifier: Modifier = Modifier,
    isBanner: Boolean = false
) {
    val context = LocalContext.current
    var adObject by remember { mutableStateOf<Any?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    DisposableEffect(adUnitId) {
        if (isBanner) {
            adManager.loadBannerAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { adObject = it },
                onAdFailed = { loadFailed = true }
            )
        } else {
            adManager.loadNativeAd(
                context = context,
                adUnitId = adUnitId,
                onAdLoaded = { adObject = it },
                onAdFailed = { loadFailed = true }
            )
        }
        onDispose {
            adManager.destroyAd(adObject)
        }
    }

    if (loadFailed || adObject == null) {
        // REQUIREMENT: Collapse cleanly on failure
        return
    }

    if (adObject is NativeAd) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(min = 0.dp),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.native_ad_layout, null) as NativeAdView
                val ad = adObject as NativeAd
                
                // Map SDK payload to UI programmatically
                ad.headline?.let { headline ->
                    view.headlineView = view.findViewById<TextView>(R.id.ad_title_text)?.apply { text = headline }
                }
                ad.body?.let { body ->
                    view.bodyView = view.findViewById<TextView>(R.id.ad_body_text)?.apply { text = body }
                }
                ad.callToAction?.let { cta ->
                    view.callToActionView = view.findViewById<Button>(R.id.ad_cta_button)?.apply { text = cta }
                }
                ad.icon?.let { icon ->
                    view.iconView = view.findViewById<android.widget.ImageView>(R.id.ad_icon_image)?.apply { setImageDrawable(icon.drawable) }
                }
                ad.advertiser?.let { advertiser ->
                    view.advertiserView = view.findViewById<TextView>(R.id.ad_advertiser_text)?.apply { text = advertiser }
                }
                
                // Bind the native ad object to the view
                view.setNativeAd(ad)
                view
            }
        )
    } else if (adObject is AdView) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { _ ->
                val adView = adObject as AdView
                (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                adView
            }
        )
    }
}
