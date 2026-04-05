package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class UserStory(
    val id: Int,
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
    val id: Int,
    val content: String,
    val contentUrl: String? = null,
    val type: StoryType,
    val timestamp: String,
    val backgroundColor: Color = Color.Gray,
    val isViewed: Boolean = false,
    val durationMs: Int = 5000 // Default 5 seconds per story
)

enum class StoryType {
    TEXT, VIDEO, IMAGE
}
