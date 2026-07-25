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

    fun getFileFromUri(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it) }
        }
        val contentResolver = context.contentResolver
        val fileName = "TEMP_" + System.currentTimeMillis()
        val extension = getExtension(context, uri) ?: ""
        val file = File(context.cacheDir, if (extension.isNotEmpty()) "$fileName.$extension" else fileName)
        
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
        if (url == null || url.length == 0) return null
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) return url
        
        var baseUrl = com.example.firstapp.BuildConfig.API_BASE_URL
        if (baseUrl.endsWith("/api/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 7)
        } else if (baseUrl.endsWith("/api/v1/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 8)
        } else if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }
        
        val cleanUrl = if (url.startsWith("/")) url.substring(1) else url
        val result = "$baseUrl/$cleanUrl"
        return result
    }
}
