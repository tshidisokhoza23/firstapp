package com.example.firstapp.music.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.music.model.Playlist
import retrofit2.Response
import retrofit2.http.*

interface MusicApi {
    @GET("songs")
    suspend fun getSongs(@Query("category") category: String? = null): Response<SongsResponse>

    @POST("songs/{songId}/like")
    suspend fun likeSong(@Path("songId") songId: Int): Response<StatusResponse>

    @POST("songs/{songId}/play")
    suspend fun incrementSongPlay(@Path("songId") songId: Int): Response<StatusResponse>

    @GET("playlists")
    suspend fun getPlaylists(): Response<PlaylistsResponse>

    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<PlaylistIdResponse>

    @GET("playlists/{playlistId}/songs")
    suspend fun getPlaylistSongs(@Path("playlistId") playlistId: Int): Response<SongsResponse>

    @POST("playlists/{playlistId}/songs/{songId}")
    suspend fun addSongToPlaylist(@Path("playlistId") playlistId: Int, @Path("songId") songId: Int): Response<StatusResponse>

    @DELETE("playlists/{playlistId}/songs/{songId}")
    suspend fun removeSongFromPlaylist(@Path("playlistId") playlistId: Int, @Path("songId") songId: Int): Response<StatusResponse>

    @DELETE("playlists/{playlistId}")
    suspend fun deletePlaylist(@Path("playlistId") playlistId: Int): Response<StatusResponse>

    @GET("songs/search")
    suspend fun search(@Query("query") query: String): Response<SongsResponse>
}

data class CreatePlaylistRequest(val name: String, val cover_art_url: String?, val is_public: Boolean)

data class ApiSong(
    val id: Int,
    val title: String,
    val artist: String,
    val artist_id: String?,
    val album_name: String?,
    val album_art_url: String?,
    val audio_url: String?,
    val duration: String?,
    val category: String?,
    val release_date: String?,
    val play_count: Long,
    val is_liked: Boolean,
    val is_explicit: Boolean,
    val lyrics: String? = null
)

data class SongsResponse(val songs: List<ApiSong>)
data class PlaylistsResponse(val playlists: List<Playlist>)
data class PlaylistIdResponse(val id: Int, val status: String)
