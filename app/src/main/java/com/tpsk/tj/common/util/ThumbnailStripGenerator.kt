package com.tpsk.tj.common.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailStripGenerator {

    suspend fun extractThumbnails(
        context: Context,
        videoUri: Uri,
        count: Int = 8,
        thumbWidth: Int = 120,
        thumbHeight: Int = 120
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val bitmaps = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationUs = (durationStr?.toLongOrNull() ?: 0L) * 1000L
            if (durationUs <= 0) return@withContext emptyList()

            val stepUs = durationUs / count.coerceAtLeast(1)

            for (i in 0 until count) {
                val timeUs = i * stepUs
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    try {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            thumbWidth,
                            thumbHeight
                        )
                    } catch (_: Exception) {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                frame?.let { bitmaps.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        bitmaps
    }

    /**
     * Estimates dynamic file size (in MB) for trimmed video range.
     */
    fun estimateTrimmedSizeMb(startMs: Long, endMs: Long, totalDurationMs: Long, totalFileSizeBytes: Long): Float {
        if (totalDurationMs <= 0 || totalFileSizeBytes <= 0) return 0f
        val selectedDurationMs = (endMs - startMs).coerceAtLeast(0)
        val ratio = selectedDurationMs.toFloat() / totalDurationMs.toFloat()
        val estimatedBytes = totalFileSizeBytes * ratio
        return estimatedBytes / (1024f * 1024f)
    }
}
