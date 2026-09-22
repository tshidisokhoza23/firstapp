package com.tpsk.tj.common.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailGenerator {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun extractThumbnails(
        context: Context,
        videoUri: Uri,
        count: Int = 8,
        thumbWidth: Int = 100,
        thumbHeight: Int = 100
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val cacheKeyBase = "${videoUri}_${count}_${thumbWidth}x${thumbHeight}"
        val cachedList = mutableListOf<Bitmap>()
        var allCached = true

        for (i in 0 until count) {
            val key = "${cacheKeyBase}_$i"
            val cachedBmp = memoryCache.get(key)
            if (cachedBmp != null) {
                cachedList.add(cachedBmp)
            } else {
                allCached = false
                break
            }
        }

        if (allCached && cachedList.size == count) {
            return@withContext cachedList
        }

        val retriever = MediaMetadataRetriever()
        val bitmaps = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationUs = (durationStr?.toLongOrNull() ?: 0L) * 1000L
            if (durationUs <= 0) return@withContext emptyList()

            val stepUs = durationUs / count.coerceAtLeast(1)

            for (i in 0 until count) {
                val key = "${cacheKeyBase}_$i"
                val existing = memoryCache.get(key)
                if (existing != null) {
                    bitmaps.add(existing)
                    continue
                }

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

                frame?.let {
                    memoryCache.put(key, it)
                    bitmaps.add(it)
                }
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
     * Calculates estimated file size in MB: ((endSec - startSec) * totalBitrateBps) / (8 * 1024 * 1024)
     */
    fun calculateEstimatedSizeBytes(startMs: Long, endMs: Long, totalBitrateBps: Long): Float {
        val durationSec = (endMs - startMs).coerceAtLeast(0) / 1000f
        val bytes = (durationSec * totalBitrateBps) / 8f
        return bytes / (1024f * 1024f)
    }
}
