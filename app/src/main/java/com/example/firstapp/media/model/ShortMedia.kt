package com.example.firstapp.media.model

import androidx.compose.ui.graphics.Color

enum class ShortType { REGULAR, AD, NATIVE_AD, AD_UNIT }

data class ShortMedia(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val creatorUsername: String = "",
    val creatorAvatarUrl: String? = null,
    val description: String,
    val likes: String,
    val comments: String,
    val shares: String = "0",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val thumbnailColor: Color = Color.Black,
    val profilePictureColor: Color = Color.Gray,
    val isLiked: Boolean = false,
    val isSubscribed: Boolean = false,
    val isBookmarked: Boolean = false,
    val type: ShortType = ShortType.REGULAR
)

data class ShortReply(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color,
    val isVerifiedAuthor: Boolean = false,
    val content: String,
    val timestamp: String,
    val likes: String,
    val isLiked: Boolean = false,
    val nestedReplies: List<ShortReply> = emptyList()
)
