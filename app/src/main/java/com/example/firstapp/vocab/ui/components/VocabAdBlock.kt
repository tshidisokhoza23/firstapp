package com.example.firstapp.vocab.ui.components

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun VocabAdBlock(
    modifier: Modifier = Modifier,
    isPremium: Boolean = false
) {
    if (isPremium) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { loadedAd ->
                nativeAd?.destroy()
                nativeAd = loadedAd
            }
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    LaunchedEffect(Unit) {
        MobileAds.initialize(context)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        AndroidView(
            factory = { ctx -> createVocabNativeAdView(ctx) },
            update = { view ->
                nativeAd?.let { ad -> bindVocabNativeAd(view, ad) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

private fun createVocabNativeAdView(context: Context): NativeAdView {
    val root = NativeAdView(context)
    root.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val adLabel = TextView(context).apply {
        tag = "ad_label"
        text = "SPONSORED"
        textSize = 10f
        setTextColor(android.graphics.Color.GRAY)
    }

    val headline = TextView(context).apply {
        tag = "ad_headline"
        textSize = 16f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(android.graphics.Color.BLACK)
    }

    val body = TextView(context).apply {
        tag = "ad_body"
        textSize = 14f
        setTextColor(android.graphics.Color.DKGRAY)
    }

    val mediaView = MediaView(context).apply {
        tag = "ad_media"
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            150
        )
    }

    val cta = Button(context).apply {
        tag = "ad_cta"
    }

    container.addView(adLabel)
    container.addView(headline)
    container.addView(body)
    container.addView(mediaView)
    container.addView(cta)
    root.addView(container)

    root.headlineView = headline
    root.bodyView = body
    root.mediaView = mediaView
    root.callToActionView = cta

    return root
}

private fun bindVocabNativeAd(adView: NativeAdView, ad: NativeAd) {
    val headline = adView.findViewWithTag<TextView>("ad_headline")
    val body = adView.findViewWithTag<TextView>("ad_body")
    val cta = adView.findViewWithTag<Button>("ad_cta")

    headline.text = ad.headline
    body.text = ad.body ?: ""
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.text = ad.callToAction ?: "Learn More"
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    adView.setNativeAd(ad)
}
