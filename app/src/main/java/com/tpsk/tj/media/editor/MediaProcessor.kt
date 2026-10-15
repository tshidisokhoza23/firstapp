package com.tpsk.tj.media.editor

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.tpsk.tj.common.util.ImageExportEngine
import com.tpsk.tj.common.util.MediaCacheManager
import com.tpsk.tj.common.util.VideoTransformerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaProcessor {
    suspend fun trimAndCompressVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        destination: MediaDestination,
        caption: String?,
        onProgress: (Float) -> Unit = {}
    ): Result<MediaEditorResult>

    suspend fun processImage(
        context: Context,
        inputUri: Uri,
        destination: MediaDestination,
        caption: String?
    ): Result<MediaEditorResult>
}

object DefaultMediaProcessor : MediaProcessor {

    @OptIn(UnstableApi::class)
    override suspend fun trimAndCompressVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        destination: MediaDestination,
        caption: String?,
        onProgress: (Float) -> Unit
    ): Result<MediaEditorResult> = withContext(Dispatchers.IO) {
        runCatching {
            val limits = MediaLimitsFactory.getLimitsFor(destination)

            // Check original input file size (250 MB cap)
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")
            val inputSize = pfd?.statSize ?: 0L
            pfd?.close()

            if (inputSize > limits.maxSizeBytes) {
                throw IllegalArgumentException("File exceeds maximum limit of 250 MB")
            }

            // Enforce max duration constraint if applicable (120s for Stories)
            val effectiveEndMs = if (limits.maxDurationMs != null) {
                val clamped = startMs + limits.maxDurationMs
                if (endMs > clamped) clamped else endMs
            } else {
                endMs
            }

            val exportedFile = VideoTransformerManager.trimAndExportVideo(
                context = context,
                inputUri = inputUri,
                startMs = startMs,
                endMs = effectiveEndMs,
                onProgress = onProgress
            ) ?: throw IllegalStateException("Could not compress video")

            if (exportedFile.length() > limits.maxSizeBytes) {
                exportedFile.delete()
                throw IllegalArgumentException("Compressed file exceeds maximum limit of 250 MB")
            }

            val outputUri = Uri.fromFile(exportedFile)
            val duration = (effectiveEndMs - startMs).coerceAtLeast(0L)

            MediaEditorResult(
                originalUri = inputUri,
                processedUri = outputUri,
                mediaType = MediaType.VIDEO,
                durationMs = duration,
                sizeBytes = exportedFile.length(),
                caption = caption
            )
        }
    }

    override suspend fun processImage(
        context: Context,
        inputUri: Uri,
        destination: MediaDestination,
        caption: String?
    ): Result<MediaEditorResult> = withContext(Dispatchers.IO) {
        runCatching {
            val limits = MediaLimitsFactory.getLimitsFor(destination)

            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")
            val inputSize = pfd?.statSize ?: 0L
            pfd?.close()

            if (inputSize > limits.maxSizeBytes) {
                throw IllegalArgumentException("File exceeds maximum limit of 250 MB")
            }

            val outputFile = MediaCacheManager.createTempFile(context, "EDIT_", ".webp")
            val success = ImageExportEngine.processAndCompressImage(
                context = context,
                inputUri = inputUri,
                outputFile = outputFile
            )

            if (!success || !outputFile.exists()) {
                throw IllegalStateException("Image processing failed")
            }

            if (outputFile.length() > limits.maxSizeBytes) {
                outputFile.delete()
                throw IllegalArgumentException("Processed image exceeds maximum limit of 250 MB")
            }

            MediaEditorResult(
                originalUri = inputUri,
                processedUri = Uri.fromFile(outputFile),
                mediaType = MediaType.IMAGE,
                durationMs = null,
                sizeBytes = outputFile.length(),
                caption = caption
            )
        }
    }
}
