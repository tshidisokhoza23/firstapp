package com.tpsk.tj.common.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
    ) {
        val fillParent = layoutResId == R.layout.item_short_native_ad
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillParent) Modifier.fillMaxSize() else Modifier.wrapContentHeight()),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(layoutResId, null) as NativeAdView
                prepareNativeAdView(view, fillParent)
                bindNativeAd(view, nativeAd)
                view
            },
            update = { view -> bindNativeAd(view, nativeAd) }
        )
    }
}
