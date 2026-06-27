package com.example.firstapp.music.data.repository

import com.example.firstapp.music.data.local.entity.*
import com.example.firstapp.music.model.*
import androidx.compose.ui.graphics.Color

fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    albumName = albumName,
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    duration = duration,
    category = category,
    playCount = playCount,
    isLiked = isLiked,
    isExplicit = isExplicit
)

fun SongEntity.toModel() = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    albumName = albumName,
    albumArtUrl = albumArtUrl,
    albumArtColor = Color(0xFF6A1B9A),
    audioUrl = audioUrl,
    duration = duration,
    category = category,
    isLiked = isLiked,
    isExplicit = isExplicit,
    playCount = playCount
)

fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    ownerUserId = ownerUserId,
    coverArtUrl = coverArtUrl,
    isPublic = isPublic,
    createdAt = createdAt
)

fun PlaylistEntity.toModel() = Playlist(
    id = id,
    name = name,
    ownerUserId = ownerUserId,
    coverArtUrl = coverArtUrl,
    isPublic = isPublic,
    createdAt = createdAt
)

fun PlaylistEntity.toPlaylist() = toModel()
