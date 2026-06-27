package com.example.firstapp.music.domain.repository

import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.common.util.Resource
import com.example.firstapp.search.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getSongs(): Flow<Resource<List<Song>>>
    fun incrementSongPlay(songId: Int): Flow<Resource<Boolean>>
    fun likeSong(songId: Int): Flow<Resource<Boolean>>
    
    // Playlists
    fun getPlaylists(): Flow<Resource<List<Playlist>>>
    fun createPlaylist(name: String, coverArtUrl: String?, isPublic: Boolean): Flow<Resource<Int>>
    fun getPlaylistSongs(playlistId: Int): Flow<Resource<List<Song>>>
    fun addSongToPlaylist(playlistId: Int, songId: Int): Flow<Resource<Boolean>>
    fun removeSongFromPlaylist(playlistId: Int, songId: Int): Flow<Resource<Boolean>>
    fun deletePlaylist(playlistId: Int): Flow<Resource<Boolean>>
    
    // History
    fun getRecentlyPlayed(): Flow<Resource<List<Song>>>
    suspend fun saveRecentlyPlayed(songId: Int)

    // Search
    fun search(query: String): Flow<Resource<SearchResult>>
}
