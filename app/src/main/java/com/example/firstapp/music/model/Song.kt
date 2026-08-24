package com.example.firstapp.music.model

import androidx.compose.ui.graphics.Color

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val albumName: String = "",
    val albumId: String = "",
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val streamUrl: String = "",
    val localUri: String? = null,
    val sourceType: String = "STREAM",
    val isExplicit: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadQuality: String? = null,
    val lyricsSyncedUrl: String? = null,
    val albumArtColor: Color = Color.Unspecified,
    val duration: String = "",
    val lyrics: String? = null,
    val category: String = "",
    val releaseDate: String = "",
    val playCount: String = "0",
    val isLiked: Boolean = false
)
