package com.example.firstapp.music.model

data class Playlist(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val coverArtUrl: String?,
    val isPublic: Boolean,
    val createdAt: Long
)
