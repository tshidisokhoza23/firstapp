package com.example.firstapp.chat.model

import androidx.compose.ui.graphics.Color

data class Channel(
    val id: String,
    val name: String,
    val description: String = "",
    val followers: String,
    val avatarUrl: String? = null,
    val avatarColor: Color,
    val isVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val isMuted: Boolean = false,
    val isAdmin: Boolean = false,
    val category: String = "Community",
    val createdBy: String? = null,
    val lastPostText: String = "",
    val lastPostImageUrl: String? = null,
    val lastPostImageColor: Color = Color.Gray,
    val lastPostTime: String = "",
    val conversationId: String? = null
)
