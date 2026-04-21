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
