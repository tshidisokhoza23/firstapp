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
    val albumArtUrl: String?,
    val audioUrl: String,
    val duration: String,
    val category: String,
    val playCount: String,
    val isLiked: Boolean,
    val isExplicit: Boolean
)
