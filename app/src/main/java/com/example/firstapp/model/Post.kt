package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

enum class PostType {
    TEXT_ONLY,
    IMAGE_WITH_TEXT,
    VIDEO_WITH_TEXT
}

data class Post(
    val id: Int,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarColor: Color,
    val content: String,
    val timestamp: String,
    val likes: String,
    val repliesCount: String,
    val shares: String,
    val views: String,
    val type: PostType = PostType.TEXT_ONLY,
    val mediaPlaceholderColor: Color? = null,
    val hasThread: Boolean = false,
    val threadCount: Int = 0,
    val replies: List<PostReply> = emptyList()
)

data class PostReply(
    val id: Int,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarColor: Color,
    val content: String,
    val timestamp: String,
    val likes: String,
    val nestedReplies: List<PostReply> = emptyList()
)
