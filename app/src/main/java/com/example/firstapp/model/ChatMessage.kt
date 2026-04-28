package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class ChatMessage(
    val id: Int,
    val senderName: String,
    val senderId: String = "",
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarColor: Color,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val wallpaperUrl: String? = null,
    val messages: List<Message> = emptyList()
)
