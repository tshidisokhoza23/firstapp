package com.example.firstapp.music.data.api

import com.example.firstapp.common.data.api.StatusResponse
import retrofit2.Response
import retrofit2.http.*

interface MusicApi {
    @GET("songs")
    suspend fun getSongs(@Query("category") category: String? = null): Response<SongsResponse>

    @POST("songs/{songId}/like")
    suspend fun likeSong(@Path("songId") songId: String): Response<StatusResponse>

    @POST("songs/{songId}/play")
    suspend fun incrementSongPlay(@Path("songId") songId: String): Response<StatusResponse>

    @GET("playlists")
    suspend fun getPlaylists(): Response<PlaylistsResponse>

    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<PlaylistIdResponse>

    @GET("playlists/{playlistId}/songs")
    suspend fun getPlaylistSongs(@Path("playlistId") playlistId: String): Response<SongsResponse>

    @POST("playlists/{playlistId}/songs/{songId}")
    suspend fun addSongToPlaylist(@Path("playlistId") playlistId: String, @Path("songId") songId: String): Response<StatusResponse>

    @DELETE("playlists/{playlistId}/songs/{songId}")
    suspend fun removeSongFromPlaylist(@Path("playlistId") playlistId: String, @Path("songId") songId: String): Response<StatusResponse>

    @DELETE("playlists/{playlistId}")
    suspend fun deletePlaylist(@Path("playlistId") playlistId: String): Response<StatusResponse>

    @GET("songs/search")
    suspend fun search(@Query("query") query: String): Response<SongsResponse>
}

data class SongsResponse(val songs: List<ApiSong>)

data class ApiSong(
    val id: String,
    val title: String,
    val artist: String,
    val artist_id: String? = null,
    val album_name: String? = null,
    val album_id: String? = null,
    val duration_ms: Long = 0,
    val artwork_url: String? = null,
    val stream_url: String? = null,
    val local_uri: String? = null,
    val source_type: String = "STREAM",
    val is_explicit: Boolean = false,
    val is_downloaded: Boolean = false,
    val download_quality: String? = null,
    val lyrics_synced_url: String? = null,
    val audio_url: String? = null,
    val duration: String? = null,
    val category: String? = null,
    val lyrics: String? = null,
    val release_date: String? = null,
    val play_count: Int = 0,
    val is_liked: Boolean = false
)

data class PlaylistIdResponse(val playlist_id: String)
data class PlaylistsResponse(val playlists: List<ApiPlaylist>)
data class CreatePlaylistRequest(val name: String, val cover_art_url: String?, val is_public: Boolean)

data class ApiPlaylist(
    val id: String,
    val name: String,
    val cover_art_url: String?,
    val is_public: Boolean,
    val song_count: Int
)
