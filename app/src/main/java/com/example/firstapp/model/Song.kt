package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val albumArtColor: Color,
    val duration: String,
    val category: String
)
