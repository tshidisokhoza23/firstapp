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
    STORY(30_000L, 15L * 1024 * 1024, 720, 1_800_000),
    SHORT(60_000L, 30L * 1024 * 1024, 720, 2_000_000),
    GENERAL(180_000L, 40L * 1024 * 1024, 720, 2_000_000)
}

class VideoTooLargeException(message: String) : Exception(message)

@UnstableApi
object VideoTransformerManager {

    /**
     * 720p H.264 + AAC. Rejects clips over the profile duration or compressed size.
     */
    suspend fun exportForUpload(
        context: Context,
        inputUri: Uri,
        profile: VideoUploadProfile
    ): File {
        val durationMs = readDurationMs(context, inputUri)
        if (durationMs > profile.maxDurationMs + 750) {
            throw VideoTooLargeException(
                "Video is ${durationMs / 1000}s. Max is ${profile.maxDurationMs / 1000}s."
            )
        }
        val endMs = if (durationMs > 0L) durationMs else profile.maxDurationMs
        val exported = trimAndExportVideo(context, inputUri, 0L, endMs, profile = profile)
            ?: throw VideoTooLargeException("Could not compress this video. Try a shorter clip.")
        if (exported.length() > profile.maxBytes) {
            exported.delete()
            val mb = profile.maxBytes / (1024 * 1024)
            throw VideoTooLargeException("Compressed video is still over ${mb} MB.")
        }
        return exported
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
                        Log.e("VideoTransformer", "Export failed", exportException)
                        outputFile.delete()
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
                .build()

            continuation.invokeOnCancellation { transformer.cancel() }
            try {
                transformer.start(edited, outputFile.absolutePath)
            } catch (e: Exception) {
                Log.e("VideoTransformer", "Failed to start export", e)
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
