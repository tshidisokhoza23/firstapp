package com.tpsk.tj.media.editor

import android.net.Uri

enum class MediaDestination {
    CHAT,
    STORY,
    POST,
    SHORT
}

enum class MediaType {
    IMAGE,
    VIDEO
}

data class MediaLimits(
    val maxSizeBytes: Long = 250L * 1024 * 1024, // 250 MB strict universal limit
    val maxDurationMs: Long? = null              // null = no time limit; 120_000L for Stories
)

object MediaLimitsFactory {
    fun getLimitsFor(destination: MediaDestination): MediaLimits = when (destination) {
        MediaDestination.STORY -> MediaLimits(maxDurationMs = 120_000L) // 2 minutes (120 seconds)
        MediaDestination.SHORT -> MediaLimits(maxDurationMs = null)    // No time limit, 250 MB cap
        MediaDestination.POST  -> MediaLimits(maxDurationMs = null)    // No time limit, 250 MB cap
        MediaDestination.CHAT  -> MediaLimits(maxDurationMs = null)    // No time limit, 250 MB cap
    }
}

data class MediaEditorResult(
    val originalUri: Uri,
    val processedUri: Uri,
    val mediaType: MediaType,
    val durationMs: Long?,
    val sizeBytes: Long,
    val caption: String?
)
