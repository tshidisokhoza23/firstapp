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
import com.example.firstapp.R
import com.example.firstapp.common.data.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import kotlinx.coroutines.delay

@Composable
fun FloatingVideoAdOverlay(
    adManager: AdManager,
    adUnitId: String,
    isVisible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var showAfterDelay by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2000) // Show after 2 seconds
            showAfterDelay = true
        } else {
            showAfterDelay = false
        }
    }

    DisposableEffect(adUnitId) {
        adManager.loadShortsNativeAd(
            context = context,
            adUnitId = adUnitId,
            onAdLoaded = { ad ->
                if (ad is NativeAd) {
                    nativeAd = ad
                }
            },
            onAdFailed = {
                loadFailed = true
            }
        )
        onDispose {
            nativeAd?.destroy()
        }
    }

    AnimatedVisibility(
        visible = isVisible && showAfterDelay && nativeAd != null && !loadFailed,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.item_short_native_ad, null) as NativeAdView
                    
                    view.headlineView = view.findViewById(R.id.ad_headline)
                    view.bodyView = view.findViewById(R.id.ad_body)
                    view.callToActionView = view.findViewById(R.id.ad_call_to_action)
                    view.iconView = view.findViewById(R.id.ad_app_icon)
                    view.advertiserView = view.findViewById(R.id.ad_advertiser)
                    view.mediaView = view.findViewById(R.id.ad_media)
                    
                    view
                },
                update = { view ->
                    val ad = nativeAd
                    if (ad != null) {
                        val headline = ad.headline
                        if (headline != null) (view.headlineView as? TextView)?.text = headline
                        
                        val body = ad.body
                        if (body != null) (view.bodyView as? TextView)?.text = body
                        
                        val cta = ad.callToAction
                        if (cta != null) (view.callToActionView as? Button)?.text = cta
                        
                        val icon = ad.icon
                        if (icon != null) (view.iconView as? ImageView)?.setImageDrawable(icon.drawable)
                        
                        val advertiser = ad.advertiser
                        if (advertiser != null) (view.advertiserView as? TextView)?.text = advertiser
                        
                        view.setNativeAd(ad)
                    }
                }
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Ad",
                    tint = Color.White
                )
            }
        }
    }
}
