package com.example.firstapp.common.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.util.ThumbnailGenerator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerScrubber(
    context: Context,
    videoUri: Uri,
    maxDurationMs: Long = 60_000L, // Max 60s clamp
    onTrimRangeChanged: (startMs: Long, endMs: Long) -> Unit
) {
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var totalBitrateBps by remember { mutableLongStateOf(2_500_000L) }
    var sliderRange by remember { mutableStateOf(0f..1f) }

    LaunchedEffect(videoUri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            totalDurationMs = durStr?.toLongOrNull() ?: 0L
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bRate = bitrateStr?.toLongOrNull()
            if (bRate != null && bRate > 0) {
                totalBitrateBps = bRate
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        thumbnails = ThumbnailGenerator.extractThumbnails(context, videoUri, count = 8, thumbWidth = 100, thumbHeight = 100)
    }

    // Enforce 60-second max duration clamp
    val rawStartMs = (sliderRange.start * totalDurationMs).toLong()
    var rawEndMs = (sliderRange.endInclusive * totalDurationMs).toLong()

    val currentSelectedDurMs = rawEndMs - rawStartMs
    if (currentSelectedDurMs > maxDurationMs && totalDurationMs > 0) {
        rawEndMs = (rawStartMs + maxDurationMs).coerceAtMost(totalDurationMs)
    }

    val selectedStartMs = rawStartMs
    val selectedEndMs = rawEndMs

    val estimatedSizeMb = ThumbnailGenerator.calculateEstimatedSizeBytes(
        selectedStartMs, selectedEndMs, totalBitrateBps
    )

    LaunchedEffect(selectedStartMs, selectedEndMs) {
        onTrimRangeChanged(selectedStartMs, selectedEndMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail Scrubber Strip
        if (thumbnails.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                thumbnails.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Draggable Handles
        RangeSlider(
            value = sliderRange,
            onValueChange = { range ->
                val rangeDurMs = ((range.endInclusive - range.start) * totalDurationMs).toLong()
                if (rangeDurMs <= maxDurationMs || totalDurationMs <= 0) {
                    sliderRange = range
                } else {
                    // Clamp to max 60s
                    val maxRatio = maxDurationMs.toFloat() / totalDurationMs.toFloat()
                    sliderRange = range.start..(range.start + maxRatio).coerceAtMost(1f)
                }
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = AppAccent,
                activeTrackColor = AppAccent,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Dynamic Badge: 0:14 • 2.8 MB
        val durationSec = ((selectedEndMs - selectedStartMs) / 1000).coerceAtLeast(0)
        val durationFormatted = String.format(Locale.US, "%d:%02d", durationSec / 60, durationSec % 60)
        val sizeFormatted = String.format(Locale.US, "%.1f MB", estimatedSizeMb)

        Text(
            text = "$durationFormatted • $sizeFormatted",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
