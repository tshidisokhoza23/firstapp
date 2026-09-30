package com.tpsk.tj.media.ui.components

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
import com.tpsk.tj.R
import com.tpsk.tj.common.data.ads.AdManager
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
                    com.tpsk.tj.common.ui.components.prepareNativeAdView(view, fillParent = false)
                    view
                },
                update = { view ->
                    val ad = nativeAd
                    if (ad != null) com.tpsk.tj.common.ui.components.bindNativeAd(view, ad)
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
