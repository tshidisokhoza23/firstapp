package com.tpsk.tj.common.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000,
) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = (widthOfShadowBrush + 500).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation.value - widthOfShadowBrush, y = 0.0f),
        end = Offset(x = translateAnimation.value, y = angleOfAxisY),
    )

    Box(
        modifier = modifier
            .background(brush)
    )
}

@Composable
fun ChatItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerEffect(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun PostItemShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ShimmerEffect(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun MediaAdShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        MediaAdShimmerHeader()
    }
}

@Composable
private fun MediaAdShimmerHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        ShimmerEffect(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        MediaAdShimmerBody(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MediaAdShimmerBody(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        MediaAdShimmerTitle()
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBar(widthFraction = 0.9f)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBar(widthFraction = 0.6f)
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        MediaAdShimmerActions()
    }
}

@Composable
private fun MediaAdShimmerTitle() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ShimmerBar(widthFraction = 0.3f)
        ShimmerEffect(
            modifier = Modifier
                .width(30.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun MediaAdShimmerActions() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ShimmerEffect(modifier = Modifier.width(80.dp).height(32.dp).clip(RoundedCornerShape(16.dp)))
        Row {
            repeat(4) { index ->
                ShimmerEffect(modifier = Modifier.size(18.dp).clip(CircleShape))
                if (index < 3) Spacer(modifier = Modifier.width(20.dp))
            }
        }
    }
}

@Composable
private fun ShimmerBar(widthFraction: Float) {
    ShimmerEffect(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
fun ShortsAdShimmer() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerEffect(modifier = Modifier.size(40.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    ShimmerEffect(modifier = Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerEffect(modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerEffect(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerEffect(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(20.dp))
            ShimmerEffect(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)))
        }
    }
}
