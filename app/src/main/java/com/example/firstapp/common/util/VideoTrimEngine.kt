package com.example.firstapp.common.util

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

object VideoTrimEngine {

    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                outputFile.parentFile?.mkdirs()
                if (outputFile.exists()) outputFile.delete()

                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(endMs)
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
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            exportException.printStackTrace()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    })
                    .build()

                transformer.start(editedMediaItem, outputFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
}
