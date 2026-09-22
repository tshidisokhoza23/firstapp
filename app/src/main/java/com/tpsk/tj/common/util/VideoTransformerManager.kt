package com.tpsk.tj.common.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object VideoTransformerManager {

    @androidx.media3.common.util.UnstableApi
    suspend fun trimAndExportVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.Main) {
        val tempDir = File(context.cacheDir, "media_temp")
        if (!tempDir.exists()) tempDir.mkdirs()

        val outputFile = File(tempDir, "TRIM_${System.currentTimeMillis()}.mp4")

        suspendCancellableCoroutine { continuation ->
            try {
                if (outputFile.exists()) outputFile.delete()

                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(endMs)
                    .setStartsAtKeyFrame(false)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(inputUri)
                    .setClippingConfiguration(clippingConfig)
                    .build()

                val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(outputFile)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            exportException.printStackTrace()
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                    .build()

                transformer.start(editedMediaItem, outputFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
