package com.tpsk.tj.common.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

enum class VideoUploadProfile(
    val maxDurationMs: Long,
    val maxBytes: Long,
    val height: Int,
    val videoBitrate: Int
) {
    STORY(60_000L, 50L * 1024 * 1024, 720, 1_800_000),
    SHORT(Long.MAX_VALUE, 250L * 1024 * 1024, 1080, 3_500_000),
    GENERAL(Long.MAX_VALUE, 250L * 1024 * 1024, 1080, 3_500_000)
}

class VideoTooLargeException(message: String) : Exception(message)

@UnstableApi
object VideoTransformerManager {

    /**
     * Hardware-accelerated 1080p/720p compression via Android MediaCodec.
     * Max size is 250MB with no duration restrictions for Shorts.
     */
    suspend fun exportForUpload(
        context: Context,
        inputUri: Uri,
        profile: VideoUploadProfile
    ): File = withContext(Dispatchers.IO) {
        // 1. File size validation up front (Max 250MB for SHORT)
        val pfd = runCatching { context.contentResolver.openFileDescriptor(inputUri, "r") }.getOrNull()
        val inputSize = pfd?.statSize ?: 0L
        pfd?.close()

        if (inputSize > profile.maxBytes) {
            val maxMb = profile.maxBytes / (1024 * 1024)
            throw VideoTooLargeException("Video exceeds $maxMb MB size limit")
        }

        // 2. Only validate duration if the profile explicitly specifies a finite limit (e.g. STORY)
        if (profile.maxDurationMs < Long.MAX_VALUE) {
            val durationMs = readDurationMs(context, inputUri)
            if (durationMs > profile.maxDurationMs + 750) {
                throw VideoTooLargeException(
                    "Video is ${durationMs / 1000}s. Max allowed is ${profile.maxDurationMs / 1000}s."
                )
            }
        }

        // 3. Attempt hardware-accelerated MediaCodec compression
        val durationMs = readDurationMs(context, inputUri)
        val endMs = if (durationMs > 0L) durationMs else 300_000L

        val exported = trimAndExportVideo(context, inputUri, 0L, endMs, profile = profile)

        if (exported != null && exported.exists() && exported.length() > 0 && exported.length() <= profile.maxBytes) {
            return@withContext exported
        }

        // 4. Fallback: Copy original file directly if under 250MB
        val tempDir = File(context.cacheDir, "media_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        val fallbackFile = File(tempDir, "short_direct_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            fallbackFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (fallbackFile.length() > profile.maxBytes) {
            fallbackFile.delete()
            throw VideoTooLargeException("Video file exceeds 250 MB limit")
        }

        fallbackFile
    }

    /**
     * Re-encode a voice note or imported audio clip with the device AAC encoder (MediaCodec).
     * Returns null when the hardware path fails so the caller can upload the original.
     */
    suspend fun exportAudioForUpload(context: Context, inputUri: Uri): File? = withContext(Dispatchers.Main) {
        val tempDir = File(context.cacheDir, "media_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        val outputFile = File(tempDir, "compressed_audio_${System.currentTimeMillis()}.m4a")
        val encoder = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .build()
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoder)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(outputFile.takeIf { it.exists() && it.length() > 0 })
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e("VideoTransformer", "Hardware audio export warning: ${exportException.message}")
                        outputFile.delete()
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
                .build()
            continuation.invokeOnCancellation { transformer.cancel() }
            try {
                transformer.start(MediaItem.fromUri(inputUri), outputFile.absolutePath)
            } catch (e: Exception) {
                Log.e("VideoTransformer", "Failed to start hardware audio export", e)
                outputFile.delete()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    suspend fun trimAndExportVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {},
        profile: VideoUploadProfile = VideoUploadProfile.GENERAL
    ): File? = withContext(Dispatchers.Main) {
        onProgress(0f)
        val tempDir = File(context.cacheDir, "media_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        val outputFile = File(tempDir, "compressed_${System.currentTimeMillis()}.mp4")
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0L))
            .setEndPositionMs(endMs)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(inputUri)
            .setClippingConfiguration(clipping)
            .build()
        val edited = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(profile.height))))
            .build()

        // Use Android hardware-accelerated encoders (Qualcomm, MediaTek, Exynos, Tensor)
        val encoder = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(profile.videoBitrate).build()
            )
            .setEnableFallback(true)
            .build()

        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoder)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        onProgress(1f)
                        if (continuation.isActive) continuation.resume(outputFile)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e("VideoTransformer", "Hardware export warning: ${exportException.message}")
                        outputFile.delete()
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
                .build()

            continuation.invokeOnCancellation { transformer.cancel() }
            try {
                transformer.start(edited, outputFile.absolutePath)
            } catch (e: Exception) {
                Log.e("VideoTransformer", "Failed to start hardware export", e)
                outputFile.delete()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private suspend fun readDurationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
