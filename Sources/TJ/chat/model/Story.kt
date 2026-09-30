package com.tpsk.tj.chat.model

import androidx.compose.ui.graphics.Color

enum class StoryType { TEXT, IMAGE, VIDEO, AD }

data class UserStory(
    val id: String,
    val userId: String = "",
    val userName: String,
    val phoneNumber: String? = null,
    val userAvatarUrl: String? = null,
    val userAvatarColor: Color,
    val stories: List<StoryItem>,
    val lastUpdated: String,
    val isMuted: Boolean = false
) {
    val allViewed: Boolean get() = stories.isNotEmpty() && stories.all { it.isViewed }
}

data class StoryItem(
    val id: String,
    val content: String,
    val contentUrl: String? = null,
    val type: StoryType, 
    val timestamp: String,
    val createdAt: Long = 0,
    // Packed ARGB. Do not store Compose Color here — Room persists this list via
    // Gson, which cannot round-trip androidx.compose.ui.graphics.Color and was
    // dropping every story item on read (empty tray / empty viewer).
    val backgroundColorArgb: Long = 0xFF546E7A,
    val isViewed: Boolean = false,
    val durationMs: Int = 5000,
    val expiresAt: String = "",
    val viewsCount: Int = 0,
    val adObject: Any? = null
) {
    val backgroundColor: Color get() = Color(backgroundColorArgb.toInt())
}


