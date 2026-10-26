package com.tpsk.tj.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object MediaUtils {
    fun compressImage(context: Context, uri: Uri, maxDimension: Int = 1920, quality: Int = 80): File? {
        return try {
            val bitmap = decodeScaledBitmap(context, uri, maxDimension) ?: return copyImageFallback(context, uri)
            val outFile = File(context.cacheDir, "mingle_${java.util.UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
                outStream.flush()
            }
            bitmap.recycle()
            if (outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            copyImageFallback(context, uri)
        }
    }

    /**
     * Decode through the platform image codec (hardware on HEIC/JPEG where the device provides it)
     * and scale during decode so the CPU does not inflate a full camera frame.
     */
    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return decodeWithPlatformCodec(context, uri, maxDimension) ?: decodeWithBitmapFactory(context, uri, maxDimension)
        }
        return decodeWithBitmapFactory(context, uri, maxDimension)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithPlatformCodec(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val width = info.size.width
                    val height = info.size.height
                    val largest = max(width, height)
                    if (largest > maxDimension && largest > 0) {
                        val scale = maxDimension.toFloat() / largest
                        decoder.setTargetSize(
                            (width * scale).toInt().coerceAtLeast(1),
                            (height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sampleSize = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        while (largest / sampleSize > maxDimension && sampleSize < 32) {
            sampleSize *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
            })
        } ?: return null
        val rotation = readExifRotation(context, uri)
        if (rotation == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun copyImageFallback(context: Context, uri: Uri): File? {
        return try {
            val fallbackFile = File(context.cacheDir, "fallback_${java.util.UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(fallbackFile).use { outputStream -> inputStream.copyTo(outputStream) }
            }
            if (fallbackFile.length() > 0) fallbackFile else null
        } catch (_: Exception) {
            null
        }
    }

    fun processImageForAvatar(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            // 1. Calculate dimensions for 512x512 with aspect ratio preserved (crop to square)
            val size = Math.min(originalBitmap.width, originalBitmap.height)
            val x = (originalBitmap.width - size) / 2
            val y = (originalBitmap.height - size) / 2
            
            val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
            val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)
            
            val outFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.webp")
            val outStream = FileOutputStream(outFile)
            
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            
            scaledBitmap.compress(format, 85, outStream)
            outStream.flush()
            outStream.close()
            
            if (originalBitmap != squareBitmap) originalBitmap.recycle()
            // Note: createScaledBitmap might return the same bitmap if dimensions match
            if (squareBitmap != scaledBitmap) squareBitmap.recycle()
            scaledBitmap.recycle()
            
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createImageFileUri(context: Context): Uri {
        val file = File(context.cacheDir, "CAMERA_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    /** Sibling WebP poster written next to optimized MP4 uploads. */
    fun posterUrl(videoUrl: String?): String? {
        if (videoUrl.isNullOrBlank()) return null
        val match = Regex("(?i)\\.(mp4|mov|m4v|webm|mkv)(\\?.*)?$").find(videoUrl) ?: return null
        val ext = match.groupValues[1]
        val query = match.groupValues.getOrElse(2) { "" }
        return videoUrl.dropLast(ext.length + 1 + query.length) + "webp" + query
    }

    fun formatUrl(url: String?): String? {
        if (url == null || url.isEmpty()) return null
        
        val base = com.tpsk.tj.common.data.network.NetworkConstants.BASE_URL
        val baseUri = try { android.net.Uri.parse(base) } catch (e: Exception) { null }
        val targetHost = baseUri?.host ?: "192.168.100.7"
        
        var cleanBase = base
        if (cleanBase.endsWith("/api/v1/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length - 8)
        } else if (cleanBase.endsWith("/api/v1")) {
            cleanBase = cleanBase.substring(0, cleanBase.length - 7)
        } else if (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length - 1)
        }
        
        // Handle absolute URLs
        if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("data:") || url.startsWith("file:") || url.startsWith("local:") || url.startsWith("content:")) {
            var formatted = url
            
            // Fix localhost/127.0.0.1 -> real base or targetHost
            if (formatted.contains("://localhost") || formatted.contains("://127.0.0.1")) {
                formatted = formatted
                    .replace("http://127.0.0.1:8000", cleanBase)
                    .replace("http://localhost:8000", cleanBase)
                    .replace("http://127.0.0.1", cleanBase)
                    .replace("http://localhost", cleanBase)
                    .replace("127.0.0.1", targetHost)
                    .replace("localhost", targetHost)
            }
            if (formatted.startsWith("http://") && cleanBase.startsWith("https://")) {
                formatted = formatted.replace("http://", "https://")
            }
            return formatted
        }
        
        var cleanUrl = if (url.startsWith("/")) url.substring(1) else url
        
        // Ensure relative paths start with uploads/ if they don't already
        if (!cleanUrl.startsWith("uploads/")) {
            cleanUrl = "uploads/$cleanUrl"
        }
        
        return "$cleanBase/$cleanUrl"
    }

    fun isVideo(url: String?): Boolean {
        val s = url?.lowercase() ?: return false
        return s.contains(".mp4") || s.contains(".mov") || s.contains(".avi") || 
               s.contains(".webm") || s.contains(".mkv") || s.contains(".3gp") ||
               s.contains("video") || s.contains("/shorts/") || s.contains(".tmp")
    }

    fun getVideoThumbnail(context: Context, uri: Uri): File? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            
            if (bitmap == null) return null
            
            val outFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            val outStream = FileOutputStream(outFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
            outStream.flush()
            outStream.close()
            bitmap.recycle()
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getVideoDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
