package com.tpsk.tj.music.data.repository

import com.tpsk.tj.music.data.local.entity.*
import com.tpsk.tj.music.model.*
import com.tpsk.tj.music.data.api.ApiSong
import androidx.compose.ui.graphics.Color

fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    albumName = albumName,
    albumId = albumId,
    durationMs = durationMs,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl,
    localUri = localUri,
    sourceType = sourceType,
    isExplicit = isExplicit,
    isDownloaded = isDownloaded,
    downloadQuality = downloadQuality,
    lyricsSyncedUrl = lyricsSyncedUrl,
    duration = duration,
    category = category,
    playCount = playCount,
    isLiked = isLiked,
    lyrics = lyrics,
    releaseDate = releaseDate
)

fun SongEntity.toModel() = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    albumName = albumName,
    albumId = albumId,
    durationMs = durationMs,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl,
    localUri = localUri,
    sourceType = sourceType,
    isExplicit = isExplicit,
    isDownloaded = isDownloaded,
    downloadQuality = downloadQuality,
    lyricsSyncedUrl = lyricsSyncedUrl,
    albumArtColor = Color(0xFF6A1B9A),
    duration = duration,
    category = category,
    isLiked = isLiked,
    playCount = playCount,
    lyrics = lyrics,
    releaseDate = releaseDate
)

fun ApiSong.toModel() = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artist_id ?: "",
    albumName = album_name ?: "",
    albumId = album_id ?: "",
    durationMs = duration_ms,
    artworkUrl = com.tpsk.tj.chat.data.repository.formatUrl(artwork_url),
    streamUrl = com.tpsk.tj.chat.data.repository.formatUrl(stream_url ?: audio_url),
    localUri = local_uri,
    sourceType = source_type,
    isExplicit = is_explicit,
    isDownloaded = is_downloaded,
    downloadQuality = download_quality,
    lyricsSyncedUrl = lyrics_synced_url,
    albumArtColor = Color(0xFF6A1B9A),
    duration = duration ?: "",
    category = category ?: "",
    lyrics = lyrics,
    releaseDate = release_date ?: "",
    playCount = play_count.toString(),
    isLiked = is_liked
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
