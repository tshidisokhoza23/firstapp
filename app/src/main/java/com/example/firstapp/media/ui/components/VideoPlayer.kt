package com.example.firstapp.media.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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
        val p = ExoPlayer.Builder(context).build()
        p.repeatMode = Player.REPEAT_MODE_ONE
        p
    }

    var internalMuted by remember { mutableStateOf(isMuted) }
    
    LaunchedEffect(videoUrl) {
        if (videoUrl.length > 0) {
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

    LaunchedEffect(internalMuted) {
        playerInstance.volume = if (internalMuted) 0f else 1f
    }

    // Emit position periodically if playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                onPositionUpdate(playerInstance.currentPosition)
                kotlinx.coroutines.delay(1000)
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

        if (!useController) {
            IconButton(
                onClick = { internalMuted = !internalMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (internalMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
