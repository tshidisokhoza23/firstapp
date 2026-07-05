package com.example.firstapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.chat.ui.viewmodel.CallViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.chat.ui.components.CallOverlay
import com.example.firstapp.music.ui.screens.NowPlayingScreen
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.common.util.Resource

@Composable
fun GlobalOverlayContainer(
    musicViewModel: MusicViewModel,
    callViewModel: CallViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val currentlyPlaying by musicViewModel.currentlyPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.progress.collectAsState()
    val isShuffleEnabled by musicViewModel.isShuffleEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()
    val currentQueue by musicViewModel.currentQueue.collectAsState()
    val likedSongs by musicViewModel.likedSongs.collectAsState()
    val playlistsResource by musicViewModel.playlists.collectAsState()
    val sleepTimerRemaining by musicViewModel.sleepTimerRemaining.collectAsState()
    
    var isNowPlayingVisible by remember { mutableStateOf(false) }
    
    val incomingCall by GlobalCallState.incomingCall.collectAsState()
    val activeSessionId by GlobalCallState.activeSessionId.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        if (incomingCall != null && activeSessionId == null) {
            CallOverlay(callViewModel)
        }
        
        if (currentlyPlaying != null && isNowPlayingVisible) {
            val song = currentlyPlaying!!
            val playlists = (playlistsResource as? Resource.Success)?.data ?: emptyList()
            
            NowPlayingScreen(
                song = song,
                queue = currentQueue,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                progress = progress,
                isLiked = likedSongs.contains(song.id),
                onClose = { isNowPlayingVisible = false },
                onTogglePlay = { musicViewModel.togglePlayPause() },
                onToggleShuffle = { musicViewModel.toggleShuffle() },
                onToggleRepeat = { musicViewModel.toggleRepeat() },
                onPrevious = { musicViewModel.skipPrevious() },
                onNext = { musicViewModel.skipNext() },
                onSeek = { musicViewModel.seekTo(it) },
                onToggleLike = { musicViewModel.toggleLike(song.id) },
                onPlayFromQueue = { musicViewModel.playSong(it, currentQueue) },
                onSeekForward = { musicViewModel.seekForward() },
                onSeekBackward = { musicViewModel.seekBackward() },
                onSetSleepTimer = { musicViewModel.setSleepTimer(it) },
                onArtistClick = { musicViewModel.requestArtist(it) },
                sleepTimerRemaining = sleepTimerRemaining,
                playlists = playlists,
                onAddSongToPlaylist = { musicViewModel.addSongToPlaylist(it, song.id) }
            )
        }
    }
}
