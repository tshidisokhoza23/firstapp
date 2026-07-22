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
    isBanner: Boolean = false,
    isShortsAd: Boolean = false
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
        } else if (isShortsAd) {
            adManager.loadShortsNativeAd(
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
        if (isShortsAd) {
            ShortsAdShimmer()
        }
        return
    }

    if (adObject is NativeAd) {
        val ad = adObject as NativeAd
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isShortsAd) Modifier.fillMaxHeight() else Modifier.heightIn(min = 0.dp)),
            factory = { ctx ->
                val layoutRes = if (isShortsAd) R.layout.shorts_native_ad_layout else R.layout.native_ad_layout
                val view = LayoutInflater.from(ctx).inflate(layoutRes, null) as NativeAdView
                
                view.headlineView = view.findViewById(R.id.ad_headline)
                view.bodyView = view.findViewById(R.id.ad_body)
                view.callToActionView = view.findViewById(R.id.ad_call_to_action)
                view.iconView = view.findViewById(R.id.ad_app_icon)
                view.advertiserView = view.findViewById(R.id.ad_advertiser)
                view.mediaView = view.findViewById(R.id.ad_media)

                // Map SDK payload to UI programmatically
                ad.headline?.let { headline ->
                    (view.headlineView as? TextView)?.text = headline
                }
                ad.body?.let { body ->
                    (view.bodyView as? TextView)?.text = body
                }
                ad.callToAction?.let { cta ->
                    (view.callToActionView as? Button)?.text = cta
                }
                ad.icon?.let { icon ->
                    (view.iconView as? android.widget.ImageView)?.setImageDrawable(icon.drawable)
                } ?: run {
                    view.iconView?.visibility = android.view.View.GONE
                }
                ad.advertiser?.let { advertiser ->
                    (view.advertiserView as? TextView)?.text = advertiser
                } ?: run {
                    view.advertiserView?.visibility = android.view.View.GONE
                }
                
                ad.body?.let { body ->
                    (view.bodyView as? TextView)?.text = body
                } ?: run {
                    view.bodyView?.visibility = android.view.View.GONE
                }
                
                // Bind the native ad object to the view
                view.setNativeAd(ad)
                view
            },
            update = { view ->
                // Refresh if needed
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
