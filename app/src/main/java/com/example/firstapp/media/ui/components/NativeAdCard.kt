package com.example.firstapp.media.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.applovin.sdk.AppLovinSdk
import com.example.firstapp.R

@Composable
fun NativeAdCard(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<MaxAd?>(null) }
    var adLoader by remember { mutableStateOf<MaxNativeAdLoader?>(null) }

    DisposableEffect(adUnitId) {
        val sdk = AppLovinSdk.getInstance(context)
        val loader = MaxNativeAdLoader(adUnitId, sdk, context)
        loader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                // Clean up any previous ad
                val oldAd = nativeAd
                if (oldAd != null) loader.destroy(oldAd)
                nativeAd = ad
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {
                android.util.Log.e("NativeAdCard", "Ad Load Failed: ${error.message}")
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                // Track click if needed
            }
        })
        loader.loadAd()
        adLoader = loader

        onDispose {
            val currentAd = nativeAd
            if (currentAd != null) loader.destroy(currentAd)
            loader.destroy()
        }
    }

    if (nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                factory = { ctx ->
                    // Create the native ad view from layout
                    // Note: You need a layout file for this to work perfectly with the binder
                    // Or you can create it programmatically.
                    val binder = MaxNativeAdViewBinder.Builder(R.layout.native_ad_layout)
                        .setTitleTextViewId(R.id.ad_title_text)
                        .setBodyTextViewId(R.id.ad_body_text)
                        .setAdvertiserTextViewId(R.id.ad_advertiser_text)
                        .setIconImageViewId(R.id.ad_icon_image)
                        .setMediaContentViewGroupId(R.id.ad_media_view_container)
                        .setOptionsContentViewGroupId(R.id.ad_options_view_container)
                        .setCallToActionButtonId(R.id.ad_cta_button)
                        .build()
                    
                    MaxNativeAdView(binder, ctx)
                },
                update = { view ->
                    adLoader?.render(view, nativeAd)
                }
            )
        }
    }
}
