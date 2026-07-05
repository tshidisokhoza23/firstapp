package com.example.firstapp.music.data.local.dao

import androidx.room.*
import com.example.firstapp.music.data.local.entity.PlaylistEntity
import com.example.firstapp.music.data.local.entity.PlaylistSongCrossRef
import com.example.firstapp.music.data.local.entity.RecentlyPlayedEntity
import com.example.firstapp.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.addedAt ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(item: RecentlyPlayedEntity)

    @Query("""
        SELECT s.* FROM songs s
        JOIN recently_played rp ON s.id = rp.songId
        ORDER BY rp.playedAt DESC
        LIMIT 20
    """)
    fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()
}
