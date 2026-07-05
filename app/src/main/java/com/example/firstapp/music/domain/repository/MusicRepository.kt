package com.example.firstapp.music.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.music.model.*
import com.example.firstapp.search.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getSongs(): Flow<Resource<List<Song>>>
    fun incrementSongPlay(songId: String): Flow<Resource<Boolean>>
    fun likeSong(songId: String): Flow<Resource<Boolean>>
    
    // Playlists
    fun getPlaylists(): Flow<Resource<List<Playlist>>>
    fun createPlaylist(name: String, coverArtUrl: String?, isPublic: Boolean): Flow<Resource<String>>
    fun getPlaylistSongs(playlistId: String): Flow<Resource<List<Song>>>
    fun addSongToPlaylist(playlistId: String, songId: String): Flow<Resource<Boolean>>
    fun removeSongFromPlaylist(playlistId: String, songId: String): Flow<Resource<Boolean>>
    fun deletePlaylist(playlistId: String): Flow<Resource<Boolean>>
    
    // History
    fun getRecentlyPlayed(): Flow<Resource<List<Song>>>
    suspend fun saveRecentlyPlayed(songId: String)

    // Search
    fun search(query: String): Flow<Resource<SearchResult>>
}
