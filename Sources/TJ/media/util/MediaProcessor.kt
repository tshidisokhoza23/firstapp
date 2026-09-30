package com.tpsk.tj.media.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object MediaProcessor {
    /**
     * Downsamples image, compensates for camera hardware orientation,
     * strips EXIF metadata (GPS/Location, serial number, camera details),
     * and saves a compressed JPEG file to cacheDir.
     */
    suspend fun processAndStripExif(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = 2048,
        quality: Int = 82
    ): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open URI: $sourceUri")

        // 1. Decode bounds first to prevent Out-Of-Memory (OOM) crashes
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        // 2. Decode downsampled Bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val reOpenedStream = context.contentResolver.openInputStream(sourceUri)
        val bitmap = BitmapFactory.decodeStream(reOpenedStream, null, decodeOptions)
            ?: throw IllegalStateException("Bitmap decode failed for URI: $sourceUri")
        reOpenedStream?.close()

        // 3. Compensate for hardware camera rotation
        val orientation = getCameraOrientation(context, sourceUri)
        val matrix = Matrix().apply { if (orientation != 0) postRotate(orientation.toFloat()) }
        val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 4. Save to temporary cache without copying EXIF metadata (strips GPS/Device ID)
        val cleanFile = File(context.cacheDir, "post_img_${UUID.randomUUID()}.jpg")
        cleanFile.outputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        // Release native bitmap memory immediately
        if (finalBitmap != bitmap) bitmap.recycle()
        bitmap.recycle()

        cleanFile
    }

    private fun getCameraOrientation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }
}
