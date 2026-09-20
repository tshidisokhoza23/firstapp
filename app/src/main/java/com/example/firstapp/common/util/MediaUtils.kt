package com.example.firstapp.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object MediaUtils {
    fun compressImage(context: Context, uri: Uri): File? {
        return try {
            val resolver = context.contentResolver
            
            // Calculate inSampleSize to prevent OOM on high-resolution camera photos
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            var sampleSize = 1
            val maxDim = Math.max(options.outWidth, options.outHeight)
            if (maxDim > 1920) {
                sampleSize = maxDim / 1080
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = Math.max(1, sampleSize)
            }
            
            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
            
            val outFile = File(context.cacheDir, "mingle_${java.util.UUID.randomUUID()}.jpg")
            
            if (bitmap != null) {
                FileOutputStream(outFile).use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                    outStream.flush()
                }
                bitmap.recycle()
                outFile
            } else {
                // Fallback: Copy direct bytes from Uri if bitmap decoding fails
                resolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (outFile.length() > 0) outFile else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallbackFile = File(context.cacheDir, "fallback_${java.util.UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(fallbackFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (fallbackFile.length() > 0) fallbackFile else null
            } catch (_: Exception) {
                null
            }
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

    fun formatUrl(url: String?): String? {
        if (url == null || url.isEmpty()) return null
        
        val base = com.example.firstapp.common.data.network.NetworkConstants.BASE_URL
        val baseUri = try { android.net.Uri.parse(base) } catch (e: Exception) { null }
        val targetHost = baseUri?.host ?: "192.168.100.7"
        
        // Handle absolute URLs
        if (url.startsWith("http") || url.startsWith("data:") || url.startsWith("file:") || url.startsWith("local:") || url.startsWith("content:")) {
            var formatted = url
            
            // Fix localhost/127.0.0.1 -> real IP
            if (formatted.contains("://localhost") || formatted.contains("://127.0.0.1")) {
                formatted = formatted.replace("localhost", targetHost).replace("127.0.0.1", targetHost)
            }
            return formatted
        }
        
        var cleanBase = base
        if (base.endsWith("/api/v1/")) {
            cleanBase = base.substring(0, base.length - 8)
        } else if (base.endsWith("/api/v1")) {
            cleanBase = base.substring(0, base.length - 7)
        } else if (base.endsWith("/")) {
            cleanBase = base.substring(0, base.length - 1)
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
