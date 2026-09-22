package com.tpsk.tj.media.model

import androidx.compose.ui.graphics.Color

data class MediaMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val mediaUrl: String? = null,
    val type: String = "text" // text, image, video
)

data class MediaConversation(
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatar: String?,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0
)
