package com.tpsk.tj.music.model

data class Playlist(
    val id: String,
    val name: String,
    val ownerUserId: String,
    val coverArtUrl: String?,
    val isPublic: Boolean,
    val createdAt: Long
)
