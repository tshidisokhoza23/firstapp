package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val albumName: String = "",
    val albumArtUrl: String? = null,
    val albumArtColor: Color,
    val audioUrl: String = "",
    val duration: String,
    val lyrics: String? = null,
    val category: String, // e.g., Pop, Rock, Synth-pop
    val releaseDate: String = "",
    val playCount: String = "0",
    val isLiked: Boolean = false,
    val isExplicit: Boolean = false
)
