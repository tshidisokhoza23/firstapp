package com.tpsk.tj.common.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmerUtil {

    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long
    ): Uri = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "trimmed_story_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        
        context.contentResolver.openFileDescriptor(inputUri, "r")?.use { pfd ->
            extractor.setDataSource(pfd.fileDescriptor)
        } ?: throw IllegalArgumentException("Cannot open video descriptor")

        val trackCount = extractor.trackCount
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val indexMap = HashMap<Int, Int>()

        var bufferSize = -1
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                val newIndex = muxer.addTrack(format)
                indexMap[i] = newIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (newSize > bufferSize) bufferSize = newSize
                }
            }
        }

        if (bufferSize < 0) bufferSize = 1024 * 1024
        muxer.start()

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L

        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)

            if (bufferInfo.size < 0) {
                bufferInfo.size = 0
                break
            }

            val presentationTimeUs = extractor.sampleTime
            if (presentationTimeUs < 0 || presentationTimeUs > endUs) {
                if (presentationTimeUs > endUs) break
            }

            val trackIndex = extractor.sampleTrackIndex
            if (indexMap.containsKey(trackIndex)) {
                bufferInfo.presentationTimeUs = (presentationTimeUs - startUs).coerceAtLeast(0L)
                val sampleFlags = extractor.sampleFlags
                var flags = 0
                if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                }
                bufferInfo.flags = flags
                muxer.writeSampleData(indexMap[trackIndex]!!, buffer, bufferInfo)
            }

            extractor.advance()
        }

        try {
            muxer.stop()
        } catch (_: Exception) {}
        muxer.release()
        extractor.release()

        Uri.fromFile(outputFile)
    }

    fun formatDurationMs(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
