package com.tpsk.tj.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageExportEngine {

    suspend fun processAndCompressImage(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        cropRectNormalized: RectF = RectF(0f, 0f, 1f, 1f),
        rotationDegrees: Int = 0,
        maxTargetDim: Int = 2048,
        quality: Int = 85
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return@withContext false

            // 1. Decode bounds only (zero heap allocation)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val fullWidth = options.outWidth
            val fullHeight = options.outHeight

            if (fullWidth <= 0 || fullHeight <= 0) return@withContext false

            // 2. Map normalized crop coordinates (0.0 to 1.0) back to actual pixels
            val pixelCrop = Rect(
                (cropRectNormalized.left * fullWidth).toInt().coerceAtLeast(0),
                (cropRectNormalized.top * fullHeight).toInt().coerceAtLeast(0),
                (cropRectNormalized.right * fullWidth).toInt().coerceAtMost(fullWidth),
                (cropRectNormalized.bottom * fullHeight).toInt().coerceAtMost(fullHeight)
            )

            val cropW = pixelCrop.width().coerceAtLeast(1)
            val cropH = pixelCrop.height().coerceAtLeast(1)

            // 3. Subsampling calculation (inSampleSize) to prevent OOM
            var inSampleSize = 1
            while ((cropW / inSampleSize) > maxTargetDim || (cropH / inSampleSize) > maxTargetDim) {
                inSampleSize *= 2
            }

            // 4. Region Decoding with fallback
            var croppedBitmap: Bitmap? = null
            try {
                val decoderStream = context.contentResolver.openInputStream(inputUri)
                if (decoderStream != null) {
                    val regionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        BitmapRegionDecoder.newInstance(decoderStream)
                    } else {
                        @Suppress("DEPRECATION")
                        BitmapRegionDecoder.newInstance(decoderStream, false)
                    }
                    val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                    croppedBitmap = regionDecoder?.decodeRegion(pixelCrop, decodeOpts)
                    decoderStream.close()
                    regionDecoder?.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (croppedBitmap == null) {
                // Fallback: decode full bitmap and crop in memory
                val fallbackStream = context.contentResolver.openInputStream(inputUri) ?: return@withContext false
                val fullOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                val fullBitmap = BitmapFactory.decodeStream(fallbackStream, null, fullOpts)
                fallbackStream.close()
                if (fullBitmap != null) {
                    val cL = (cropRectNormalized.left * fullBitmap.width).toInt().coerceIn(0, fullBitmap.width - 1)
                    val cT = (cropRectNormalized.top * fullBitmap.height).toInt().coerceIn(0, fullBitmap.height - 1)
                    val cR = (cropRectNormalized.right * fullBitmap.width).toInt().coerceIn(cL + 1, fullBitmap.width)
                    val cB = (cropRectNormalized.bottom * fullBitmap.height).toInt().coerceIn(cT + 1, fullBitmap.height)
                    
                    croppedBitmap = Bitmap.createBitmap(
                        fullBitmap,
                        cL,
                        cT,
                        (cR - cL).coerceAtLeast(1),
                        (cB - cT).coerceAtLeast(1)
                    )
                    if (croppedBitmap != fullBitmap) {
                        fullBitmap.recycle()
                    }
                }
            }

            if (croppedBitmap == null) return@withContext false

            // 5. Apply matrix transformation (rotation/scaling)
            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }

            val finalBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height, matrix, true)
            if (finalBitmap != croppedBitmap) {
                croppedBitmap.recycle()
            }

            // 6. Write compressed WebP / JPEG to disk
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out)
                } else {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
            }
            finalBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
