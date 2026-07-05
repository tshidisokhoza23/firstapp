package com.example.firstapp.music.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.firstapp.music.data.PlaybackService
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.common.util.Resource
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.*
import kotlin.text.*
import kotlin.*

import com.example.firstapp.music.domain.repository.MusicRepository

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _currentlyPlaying = MutableStateFlow<Song?>(null)
    val currentlyPlaying: StateFlow<Song?> = _currentlyPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _likedSongs = MutableStateFlow<Set<String>>(emptySet())
    val likedSongs: StateFlow<Set<String>> = _likedSongs.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private val _songs = MutableStateFlow<Resource<List<Song>>>(Resource.Loading())
    val songs: StateFlow<Resource<List<Song>>> = _songs.asStateFlow()

    private val _playlists = MutableStateFlow<Resource<List<Playlist>>>(Resource.Loading())
    val playlists: StateFlow<Resource<List<Playlist>>> = _playlists.asStateFlow()

    private val _playlistSongs = MutableStateFlow<Map<String, Resource<List<Song>>>>(emptyMap())
    val playlistSongs: StateFlow<Map<String, Resource<List<Song>>>> = _playlistSongs.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<Resource<List<Song>>>(Resource.Loading())
    val recentlyPlayed: StateFlow<Resource<List<Song>>> = _recentlyPlayed.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<SearchResult>>(Resource.Success(SearchResult()))
    val searchResults: StateFlow<Resource<SearchResult>> = _searchResults.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    private val _requestedArtist = MutableStateFlow<String?>(null)
    val requestedArtist: StateFlow<String?> = _requestedArtist.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        fetchSongs()
        fetchPlaylists()
        fetchRecentlyPlayed()
    }

    fun fetchSongs() {
        viewModelScope.launch {
            repository.getSongs().collect { _songs.value = it }
        }
    }

    fun fetchPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().collect { _playlists.value = it }
        }
    }

    fun fetchRecentlyPlayed() {
        viewModelScope.launch {
            repository.getRecentlyPlayed().collect { _recentlyPlayed.value = it }
        }
    }

    fun fetchPlaylistSongs(playlistId: String) {
        viewModelScope.launch {
            repository.getPlaylistSongs(playlistId).collect { resource ->
                _playlistSongs.value = _playlistSongs.value + (playlistId to resource)
            }
        }
    }

    fun createPlaylist(name: String, coverArtUrl: String? = null, isPublic: Boolean = true) {
        viewModelScope.launch {
            repository.createPlaylist(name, coverArtUrl, isPublic).collect { result ->
                if (result is Resource.Success) fetchPlaylists()
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId).collect { result ->
                if (result is Resource.Success) fetchPlaylistSongs(playlistId)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId).collect { result ->
                if (result is Resource.Success) fetchPlaylistSongs(playlistId)
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId).collect { result ->
                if (result is Resource.Success) fetchPlaylists()
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(SearchResult())
            return
        }
        
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        _searchHistory.value = current.take(10)

        viewModelScope.launch {
            repository.search(query).collect { _searchResults.value = it }
        }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun initPlayer(context: Context) {
        if (controllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                val controller = controller ?: return@addListener
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentlyPlaying(mediaItem)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                            stopProgressUpdate()
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _isShuffleEnabled.value = shuffleModeEnabled
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        super.onPlayerError(error)
                        _isPlaying.value = false
                        stopProgressUpdate()
                    }
                })
                
                _isPlaying.value = controller.isPlaying
                _isShuffleEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode
                updateCurrentlyPlaying(controller.currentMediaItem)
                if (controller.isPlaying) startProgressUpdate()
                
            }, MoreExecutors.directExecutor())
        }
    }

    private fun updateCurrentlyPlaying(mediaItem: MediaItem?) {
        val songId = mediaItem?.mediaId ?: return
        val song = _currentQueue.value.find { it.id == songId } ?: _songs.value.data?.find { it.id == songId }
        _currentlyPlaying.value = song
        
        if (song != null) {
            viewModelScope.launch {
                repository.incrementSongPlay(song.id).collect { }
                repository.saveRecentlyPlayed(song.id)
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                controller?.let {
                    if (it.isPlaying) {
                        val current = it.currentPosition.toFloat()
                        val duration = it.duration.coerceAtLeast(1L).toFloat()
                        _progress.value = (current / duration).coerceIn(0f, 1f)
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val currentController = controller ?: return
        
        val newQueue = if (queue.isNotEmpty()) queue else _songs.value.data ?: listOf(song)
        
        if (newQueue != _currentQueue.value) {
            _currentQueue.value = newQueue
            currentController.setMediaItems(newQueue.map { it.toMediaItem() })
        }
        
        val index = newQueue.indexOfFirst { it.id == song.id }
        
        if (index != -1) {
            if (currentController.currentMediaItemIndex == index && currentController.playbackState != Player.STATE_IDLE) {
                if (!currentController.isPlaying) currentController.play()
            } else {
                currentController.seekTo(index, 0L)
                currentController.prepare()
                currentController.play()
            }
        } else {
            currentController.setMediaItems(listOf(song.toMediaItem()), true)
            currentController.prepare()
            currentController.play()
        }
        _isPlaying.value = true
        _currentlyPlaying.value = song
    }

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(audioUrl.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(albumName)
                    .setArtworkUri(albumArtUrl?.toUri())
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
            _isPlaying.value = it.isPlaying
        }
    }

    fun seekTo(pos: Float) {
        controller?.let {
            it.seekTo((pos * it.duration).toLong())
        }
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekForward() {
        controller?.let {
            it.seekTo(it.currentPosition + 10000)
        }
    }

    fun seekBackward() {
        controller?.let {
            it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
        }
    }

    fun toggleShuffle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
            _isShuffleEnabled.value = it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        controller?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun toggleLike(songId: String) {
        viewModelScope.launch {
            repository.likeSong(songId).collect { result ->
                if (result is Resource.Success) {
                    val current = _likedSongs.value.toMutableSet()
                    if (current.contains(songId)) {
                        current.remove(songId)
                    } else {
                        current.add(songId)
                    }
                    _likedSongs.value = current
                }
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerRemaining.value = null
            return
        }
        
        var remainingMillis = minutes * 60 * 1000L
        _sleepTimerRemaining.value = remainingMillis
        
        sleepTimerJob = viewModelScope.launch {
            while (remainingMillis > 0) {
                delay(1000)
                remainingMillis -= 1000
                _sleepTimerRemaining.value = remainingMillis
            }
            controller?.pause()
            _sleepTimerRemaining.value = null
        }
    }

    fun requestArtist(name: String?) {
        _requestedArtist.value = name
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
