package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class ChatMessage(
    val id: Int,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarColor: Color
)
