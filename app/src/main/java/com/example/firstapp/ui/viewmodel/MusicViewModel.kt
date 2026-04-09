package com.example.firstapp.ui.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.firstapp.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
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

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
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

    fun playSong(song: Song) {
        if (_currentlyPlaying.value == song) {
            togglePlayPause()
            return
        }
        
        _currentlyPlaying.value = song
        exoPlayer?.let {
            it.stop()
            it.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.audioUrl.toUri())
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
            _isPlaying.value = true
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
        val current = _likedSongs.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _likedSongs.value = current
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
