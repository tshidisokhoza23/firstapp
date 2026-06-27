package com.example.firstapp.media.model

import androidx.compose.ui.graphics.Color

data class Channel(
    val id: Int,
    val name: String,
    val description: String = "",
    val followers: String,
    val avatarUrl: String? = null,
    val avatarColor: Color,
    val isVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val isMuted: Boolean = false,
    val category: String = "Community",
    val lastPostText: String = "",
    val lastPostImageUrl: String? = null,
    val lastPostImageColor: Color = Color.Gray,
    val lastPostTime: String = ""
)
