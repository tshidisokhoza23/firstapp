package com.example.firstapp.chat.model

import androidx.compose.ui.graphics.Color

data class ChatMessage(
    val id: Int,
    val senderName: String,
    val senderId: String = "",
    val lastMessage: String,
    val timestamp: Long, // Changed from time: String - Rule 20
    val unreadCount: Int = 0,
    val lastSeen: Long = 0L,
    val avatarColor: Color,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val isArchived: Boolean = false,
    val wallpaperUrl: String? = null,
    val customNotificationTone: String? = null,
    val mediaVisibility: Boolean = true,
    val disappearingMessagesDuration: Int = 0,
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val publicKey: String? = null,
    val messages: List<Message> = emptyList()
)
