package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Channel(
    val id: Int,
    val name: String,
    val followers: String,
    val avatarColor: Color,
    val lastPostImageColor: Color = Color.Gray,
    val lastPostText: String = ""
)
