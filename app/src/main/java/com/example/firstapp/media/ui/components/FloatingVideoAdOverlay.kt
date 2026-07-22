package com.example.firstapp.media.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay

@Composable
fun FloatingVideoAdOverlay(
    adUnitId: String,
    isVisible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<MaxAd?>(null) }
    var adLoader by remember { mutableStateOf<MaxNativeAdLoader?>(null) }
    var showAfterDelay by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000) // Show after 3 seconds
            showAfterDelay = true
        } else {
            showAfterDelay = false
        }
    }

    DisposableEffect(adUnitId) {
        val sdk = AppLovinSdk.getInstance(context)
        val loader = MaxNativeAdLoader(adUnitId, sdk, context)
        loader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                nativeAd = ad
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

    AnimatedVisibility(
        visible = isVisible && showAfterDelay && nativeAd != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                factory = { ctx ->
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

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Ad",
                    tint = Color.White
                )
            }
            
            // Mandatory "Ad" badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Ad",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
