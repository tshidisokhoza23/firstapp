package com.example.firstapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerUserId: String,
    val coverArtUrl: String?,
    val isPublic: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val addedAt: Long
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Int = 0,
    val songId: String,
    val playedAt: Long
)
