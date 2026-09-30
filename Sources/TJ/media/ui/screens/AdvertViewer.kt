package com.tpsk.tj.media.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.tpsk.tj.chat.model.Advert
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvertViewer(
    adverts: List<Advert>,
    initialIndex: Int,
    onAdvertViewed: (String) -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { adverts.size })
    val currentIndex = pagerState.settledPage
    val currentAd = remember(currentIndex) { adverts.getOrNull(currentIndex) }
    
    if (currentAd == null && currentIndex >= 0) {
        SideEffect { onFinish() }
        return
    }

    val progress = remember(currentIndex) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        if (currentIndex < 0 || currentIndex >= adverts.size) return@LaunchedEffect
        
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 10000, // 10 seconds per ad
                easing = LinearEasing
            )
        )
        
        // Loop logic: mark as viewed and go to next
        val adToMark = adverts[currentIndex]
        onAdvertViewed(adToMark.id)

        // Delay slightly after progress finishes to show the ad fully
        kotlinx.coroutines.delay(500)

        if (pagerState.settledPage == currentIndex) {
            if (currentIndex < adverts.size - 1) {
                pagerState.animateScrollToPage(currentIndex + 1)
            } else {
                onFinish()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { pageIndex ->
            val ad = adverts[pageIndex]
            
            // Real Google Ad Integration
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (offset.x < size.width / 3) {
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    }
                                } else {
                                    if (pagerState.currentPage < adverts.size - 1) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    } else {
                                        onFinish()
                                    }
                                }
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Real Google Ad Placement",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.MEDIUM_RECTANGLE)
                            // This is a Google Test Ad Unit ID
                            adUnitId = com.tpsk.tj.common.data.ads.AdUnitConfig.BANNER_MAIN_ID
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    update = { /* No-op to avoid redundant loads */ }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = ad.advertiserName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Ad ${pageIndex + 1} of ${adverts.size}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                
                if (ad.contentText.isNotEmpty()) {
                    Text(
                        text = ad.contentText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }

        // Top bars (Progress bars)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                adverts.forEachIndexed { index, _ ->
                    val barProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            // Ad Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(currentAd?.avatarColor ?: com.tpsk.tj.common.ui.theme.BoyishPurple.toArgb())),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentAd?.advertiserName ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Sponsored",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
