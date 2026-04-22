package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Short(
    val id: Int,
    val creatorName: String,
    val description: String,
    val likes: String,
    val comments: String,
    val shares: String = "0",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val thumbnailColor: Color = Color.Black,
    val profilePictureColor: Color = Color.Gray,
    val isLiked: Boolean = false,
    val isSubscribed: Boolean = false
)

data class ShortReply(
    val id: Int,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color,
    val isVerifiedAuthor: Boolean = false,
    val content: String,
    val timestamp: String,
    val likes: String,
    val isLiked: Boolean = false
)
