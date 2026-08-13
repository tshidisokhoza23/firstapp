package com.example.firstapp.chat.model

import androidx.compose.ui.graphics.Color

enum class StoryType { TEXT, IMAGE, VIDEO, AD }

data class UserStory(
    val id: String,
    val userId: String = "",
    val userName: String,
    val userAvatarUrl: String? = null,
    val userAvatarColor: Color,
    val stories: List<StoryItem>,
    val lastUpdated: String,
    val isMuted: Boolean = false
) {
    val allViewed: Boolean get() = stories.all { it.isViewed }
}

data class StoryItem(
    val id: String,
    val content: String,
    val contentUrl: String? = null,
    val type: StoryType, 
    val timestamp: String,
    val createdAt: Long = 0,
    val backgroundColor: Color = Color.DarkGray,
    val isViewed: Boolean = false,
    val durationMs: Int = 5000,
    val expiresAt: String = "",
    val adObject: Any? = null
)


