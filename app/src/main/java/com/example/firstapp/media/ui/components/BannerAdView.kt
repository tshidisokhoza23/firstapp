package com.example.firstapp.media.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.ads.MaxAdView

@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            val adView = MaxAdView(adUnitId, ctx)
            adView.setListener(object : MaxAdViewAdListener {
                override fun onAdLoaded(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {}
                override fun onAdDisplayFailed(ad: MaxAd, error: com.applovin.mediation.MaxError) {}
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdExpanded(ad: MaxAd) {}
                override fun onAdCollapsed(ad: MaxAd) {}
                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {}
            })
            adView.loadAd()
            adView
        },
        update = { view ->
            // Update if needed
        }
    )
}
