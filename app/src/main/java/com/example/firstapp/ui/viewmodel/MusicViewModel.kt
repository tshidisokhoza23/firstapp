package com.example.firstapp.ui.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.firstapp.AppContainer
import com.example.firstapp.data.repository.AppRepository
import com.example.firstapp.model.Song
import com.example.firstapp.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val repository: AppRepository = AppContainer.repository
    private var exoPlayer: ExoPlayer? = null

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

    private val _likedSongs = MutableStateFlow<Set<Int>>(emptySet())
    val likedSongs: StateFlow<Set<Int>> = _likedSongs.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val index = currentMediaItemIndex
                        if (index != -1 && index < _currentQueue.value.size) {
                            _currentlyPlaying.value = _currentQueue.value[index]
                            _currentlyPlaying.value?.let { song ->
                                viewModelScope.launch {
                                    repository.incrementSongPlay(song.id).collect { }
                                }
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }
                })
            }
            startProgressUpdate()
        }
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        val current = it.currentPosition.toFloat()
                        val duration = it.duration.coerceAtLeast(1L).toFloat()
                        _progress.value = current / duration
                    }
                }
                delay(1000)
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        if (queue.isNotEmpty()) {
            _currentQueue.value = queue
        }
        
        if (_currentlyPlaying.value?.id == song.id) {
            togglePlayPause()
            return
        }
        
        _currentlyPlaying.value = song
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            if (_currentQueue.value.isNotEmpty()) {
                val mediaItems = _currentQueue.value.map { s -> MediaItem.fromUri(s.audioUrl.toUri()) }
                player.addMediaItems(mediaItems)
                val index = _currentQueue.value.indexOfFirst { s -> s.id == song.id }
                if (index != -1) {
                    player.seekTo(index, 0L)
                }
            } else {
                player.setMediaItem(MediaItem.fromUri(song.audioUrl.toUri()))
            }
            
            player.prepare()
            player.play()
            _isPlaying.value = true
            
            viewModelScope.launch {
                repository.incrementSongPlay(song.id).collect { }
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
            _isPlaying.value = it.isPlaying
        }
    }

    fun seekTo(pos: Float) {
        exoPlayer?.let {
            it.seekTo((pos * it.duration).toLong())
        }
    }

    fun skipNext() {
        exoPlayer?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        exoPlayer?.seekToPreviousMediaItem()
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        exoPlayer?.shuffleModeEnabled = _isShuffleEnabled.value
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = nextMode
    }

    fun toggleLike(songId: Int) {
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

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
