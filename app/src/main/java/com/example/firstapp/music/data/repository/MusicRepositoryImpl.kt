package com.example.firstapp.music.data.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.music.data.api.*
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.music.model.*
import com.example.firstapp.music.data.local.dao.*
import com.example.firstapp.music.data.local.entity.*
import com.example.firstapp.search.model.SearchResult
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    private val musicApi: MusicApi,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) : MusicRepository {

    override fun getSongs(): Flow<Resource<List<Song>>> = flow {
        val cached = songDao.getSongs().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = musicApi.getSongs()
            if (response.isSuccessful && response.body() != null) {
                val apiSongs = response.body()?.songs ?: emptyList()
                val mappedSongs = apiSongs.map { it.toModel() }
                songDao.insertSongs(mappedSongs.map { it.toEntity() })
                emit(Resource.Success(mappedSongs))
            } else {
                emit(Resource.Error("Failed to load songs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun incrementSongPlay(songId: String): Flow<Resource<Boolean>> = flow {
        try {
            musicApi.incrementSongPlay(songId)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun likeSong(songId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = musicApi.likeSong(songId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to like song"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getPlaylists(): Flow<Resource<List<Playlist>>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.getPlaylists()
            if (response.isSuccessful && response.body() != null) {
                val playlists = response.body()!!.playlists.map { 
                    Playlist(it.id, it.name, "", it.cover_art_url ?: "", it.is_public, System.currentTimeMillis())
                }
                emit(Resource.Success(playlists))
            } else {
                emit(Resource.Error("Failed to load playlists"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun createPlaylist(name: String, coverArtUrl: String?, isPublic: Boolean): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.createPlaylist(CreatePlaylistRequest(name, coverArtUrl, isPublic))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.playlist_id))
            } else {
                emit(Resource.Error("Failed to create playlist"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getPlaylistSongs(playlistId: String): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.getPlaylistSongs(playlistId)
            if (response.isSuccessful && response.body() != null) {
                val songs = response.body()!!.songs.map { it.toModel() }
                emit(Resource.Success(songs))
            } else {
                emit(Resource.Error("Failed to load playlist songs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun addSongToPlaylist(playlistId: String, songId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = musicApi.addSongToPlaylist(playlistId, songId)
            emit(Resource.Success(response.isSuccessful))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun removeSongFromPlaylist(playlistId: String, songId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = musicApi.removeSongFromPlaylist(playlistId, songId)
            emit(Resource.Success(response.isSuccessful))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun deletePlaylist(playlistId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = musicApi.deletePlaylist(playlistId)
            emit(Resource.Success(response.isSuccessful))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getRecentlyPlayed(): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Success(emptyList()))
    }

    override suspend fun saveRecentlyPlayed(songId: String) {
        // Implementation
    }

    override fun search(query: String): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.search(query)
            if (response.isSuccessful && response.body() != null) {
                val songs = response.body()!!.songs.map { it.toModel() }
                emit(Resource.Success(SearchResult(songs = songs)))
            } else {
                emit(Resource.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }
}
