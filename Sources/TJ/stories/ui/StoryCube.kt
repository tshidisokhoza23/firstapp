package com.tpsk.tj.stories.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * WhatsApp / Instagram cube-out. [pageOffset] matches ViewPager2 position:
 * 0 is centered, negative is leaving to the left, positive is entering from the right.
 */
fun Modifier.storyCubePage(pageOffset: Float): Modifier = graphicsLayer {
    cameraDistance = 16f * density
    clip = false
    when {
        pageOffset < -1f || pageOffset > 1f -> alpha = 0f
        pageOffset <= 0f -> {
            alpha = 1f
            transformOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
            rotationY = 90f * pageOffset
        }
        else -> {
            alpha = 1f
            transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
            rotationY = 90f * pageOffset
        }
    }
}

fun storyPagerOffset(page: Int, currentPage: Int, currentPageOffsetFraction: Float): Float {
    return (page - currentPage) + currentPageOffsetFraction
}
