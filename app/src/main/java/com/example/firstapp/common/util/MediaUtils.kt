package com.example.firstapp.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object MediaUtils {
    fun compressImage(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        val outFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        val outStream = FileOutputStream(outFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream)
        outStream.flush()
        outStream.close()
        
        return outFile
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

    fun getFileFromUri(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it) }
        }
        val contentResolver = context.contentResolver
        val fileName = "TEMP_" + System.currentTimeMillis()
        val extension = getExtension(context, uri) ?: ""
        val file = File(context.cacheDir, if (extension.length > 0) "$fileName.$extension" else fileName)
        
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.let { mime ->
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
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
        if (url == null) return null
        if (url.length == 0) return null
        if (url.startsWith("http") || url.startsWith("data:")) return url
        
        val base = com.example.firstapp.BuildConfig.API_BASE_URL
        var cleanBase = base
        if (base.endsWith("/api/v1/")) {
            cleanBase = base.substring(0, base.length - 8)
        } else if (base.endsWith("/api/v1")) {
            cleanBase = base.substring(0, base.length - 7)
        } else if (base.endsWith("/")) {
            cleanBase = base.substring(0, base.length - 1)
        }
        
        val cleanUrl = if (url.startsWith("/")) url.substring(1) else url
        return "$cleanBase/$cleanUrl"
    }

    fun isVideo(url: String?): Boolean {
        val s = url?.lowercase() ?: return false
        return s.contains(".mp4") || s.contains(".mov") || s.contains(".avi") || 
               s.contains(".webm") || s.contains(".mkv") || s.contains(".3gp") ||
               s.contains("video") || s.contains("/shorts/") || s.contains(".tmp")
    }

    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val mimeType = context.contentResolver.getType(uri)
            val extension = if (mimeType != null) {
                android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "tmp"
            } else {
                "tmp"
            }
            
            val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
            tempFile
        } catch (e: Exception) { null }
    }
}
