package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Short(
    val id: Int,
    val creatorName: String,
    val description: String,
    val likes: String,
    val comments: String,
    val thumbnailColor: Color,
    val profilePictureColor: Color = Color.Gray
)
