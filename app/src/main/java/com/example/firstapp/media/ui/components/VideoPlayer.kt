package com.example.firstapp.media.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    useController: Boolean = true,
    isMuted: Boolean = false,
    initialPosition: Long = 0L,
    onPositionUpdate: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val playerInstance = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
            volume = if (isMuted) 0f else 1f
        }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            playerInstance.setMediaItem(mediaItem)
            if (initialPosition > 0) {
                playerInstance.seekTo(initialPosition)
            }
            playerInstance.prepare()
            playerInstance.playWhenReady = isPlaying
        }
    }

    LaunchedEffect(isPlaying) {
        playerInstance.playWhenReady = isPlaying
    }

    LaunchedEffect(isMuted) {
        android.util.Log.d("VideoPlayer", "Unmuting: isMuted=$isMuted. Setting volume to ${if (isMuted) 0f else 1f}")
        playerInstance.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                onPositionUpdate(playerInstance.currentPosition)
                delay(1000)
            }
        }
    }

    DisposableEffect(playerInstance) {
        onDispose {
            playerInstance.release()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerInstance
                    this.useController = useController
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            },
            update = { view ->
                view.useController = useController
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
