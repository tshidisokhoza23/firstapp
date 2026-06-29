package com.example.firstapp.music.data.repository

import com.example.firstapp.music.data.api.MusicApi
import com.example.firstapp.music.data.api.CreatePlaylistRequest
import com.example.firstapp.music.data.local.dao.SongDao
import com.example.firstapp.music.data.local.dao.PlaylistDao
import com.example.firstapp.music.data.local.entity.PlaylistSongCrossRef
import com.example.firstapp.music.data.local.entity.RecentlyPlayedEntity
import com.example.firstapp.common.util.Resource
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.common.data.repository.formatUrl
import com.example.firstapp.common.data.repository.mapApiContact
import kotlinx.coroutines.flow.*
import android.util.Log

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
                val mappedSongs = apiSongs.map { apiSong -> 
                    Song(
                        id = apiSong.id, 
                        title = apiSong.title, 
                        artist = apiSong.artist, 
                        artistId = apiSong.artist_id ?: "", 
                        albumName = apiSong.album_name ?: "", 
                        albumArtUrl = apiSong.album_art_url ?: "", 
                        albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A), 
                        audioUrl = apiSong.audio_url ?: "",
                        duration = apiSong.duration ?: "", 
                        category = apiSong.category ?: "", 
                        lyrics = apiSong.lyrics ?: "", 
                        releaseDate = apiSong.release_date ?: "", 
                        playCount = apiSong.play_count.toString(), 
                        isLiked = apiSong.is_liked, 
                        isExplicit = apiSong.is_explicit
                    ) 
                }
                songDao.clearSongs()
                songDao.insertSongs(mappedSongs.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "getSongs failed", e)
        }
        
        songDao.getSongs().collect { entities ->
            val models = entities.map { it.toModel() }
            if (models.isEmpty()) {
                emit(Resource.Success(emptyList()))
            } else {
                emit(Resource.Success(models))
            }
        }
    }

    override fun incrementSongPlay(songId: Int): Flow<Resource<Boolean>> = flow {
        try {
            musicApi.incrementSongPlay(songId)
            emit(Resource.Success(true))
        } catch (_: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun likeSong(songId: Int): Flow<Resource<Boolean>> = flow {
        try {
            musicApi.likeSong(songId)
            emit(Resource.Success(true))
        } catch (_: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun getPlaylists(): Flow<Resource<List<Playlist>>> = flow {
        val cached = playlistDao.getPlaylists().first().map { it.toPlaylist() }
        emit(Resource.Loading(cached))
        try {
            val response = musicApi.getPlaylists()
            if (response.isSuccessful && response.body() != null) {
                val playlistList = response.body()?.playlists ?: emptyList()
                playlistDao.insertPlaylists(playlistList.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        playlistDao.getPlaylists().collect { entities ->
             emit(Resource.Success(entities.map { it.toPlaylist() }))
        }
    }

    override fun createPlaylist(name: String, coverArtUrl: String?, isPublic: Boolean): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.createPlaylist(CreatePlaylistRequest(name, coverArtUrl, isPublic))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.id))
            } else {
                emit(Resource.Error("Failed to create"))
            }
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun getPlaylistSongs(playlistId: Int): Flow<Resource<List<Song>>> = flow {
        val cached = playlistDao.getSongsForPlaylist(playlistId).first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = musicApi.getPlaylistSongs(playlistId)
            if (response.isSuccessful && response.body() != null) {
                val songs = response.body()!!.songs.map { apiSong ->
                    Song(
                        id = apiSong.id,
                        title = apiSong.title,
                        artist = apiSong.artist,
                        artistId = apiSong.artist_id ?: "",
                        albumName = apiSong.album_name ?: "",
                        albumArtUrl = apiSong.album_art_url ?: "",
                        albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A),
                        audioUrl = apiSong.audio_url ?: "",
                        duration = apiSong.duration ?: "",
                        category = apiSong.category ?: "",
                        lyrics = apiSong.lyrics ?: "",
                        releaseDate = apiSong.release_date ?: "",
                        playCount = apiSong.play_count.toString(),
                        isLiked = apiSong.is_liked,
                        isExplicit = apiSong.is_explicit
                    )
                }
                songs.forEach { playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, it.id, System.currentTimeMillis())) }
            }
        } catch (_: Exception) {}
        playlistDao.getSongsForPlaylist(playlistId).collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun addSongToPlaylist(playlistId: Int, songId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (musicApi.addSongToPlaylist(playlistId, songId).isSuccessful) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, System.currentTimeMillis()))
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun removeSongFromPlaylist(playlistId: Int, songId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (musicApi.removeSongFromPlaylist(playlistId, songId).isSuccessful) {
                playlistDao.removeSongFromPlaylist(playlistId, songId)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun deletePlaylist(playlistId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (musicApi.deletePlaylist(playlistId).isSuccessful) {
                playlistDao.deletePlaylist(playlistId)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun getRecentlyPlayed(): Flow<Resource<List<Song>>> = flow {
        playlistDao.getRecentlyPlayedSongs().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override suspend fun saveRecentlyPlayed(songId: Int) {
        playlistDao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = songId, playedAt = System.currentTimeMillis()))
    }

    override fun search(query: String): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        try {
            val response = musicApi.search(query)
            if (response.isSuccessful && response.body() != null) {
                val apiSongs = response.body()?.songs ?: emptyList()
                val mappedSongs = apiSongs.map { apiSong ->
                    Song(
                        id = apiSong.id,
                        title = apiSong.title,
                        artist = apiSong.artist,
                        artistId = apiSong.artist_id ?: "",
                        albumName = apiSong.album_name ?: "",
                        albumArtUrl = formatUrl(apiSong.album_art_url),
                        albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A),
                        audioUrl = formatUrl(apiSong.audio_url),
                        duration = apiSong.duration ?: "",
                        category = apiSong.category ?: "",
                        lyrics = apiSong.lyrics ?: "",
                        releaseDate = apiSong.release_date ?: "",
                        playCount = apiSong.play_count.toString(),
                        isLiked = apiSong.is_liked,
                        isExplicit = apiSong.is_explicit
                    )
                }
                emit(Resource.Success(SearchResult(
                    chats = emptyList(),
                    contacts = emptyList(),
                    messages = emptyList(),
                    songs = mappedSongs,
                    artists = emptyList() // The new endpoint only returns songs for now
                )))
            } else {
                emit(Resource.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }
}
