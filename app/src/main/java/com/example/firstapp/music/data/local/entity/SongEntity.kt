package com.example.firstapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val albumName: String,
    val albumId: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val streamUrl: String,
    val localUri: String?,
    val sourceType: String,
    val isExplicit: Boolean,
    val isDownloaded: Boolean,
    val downloadQuality: String?,
    val lyricsSyncedUrl: String?,
    val duration: String,
    val category: String,
    val playCount: String,
    val isLiked: Boolean,
    val lyrics: String?,
    val releaseDate: String
)
