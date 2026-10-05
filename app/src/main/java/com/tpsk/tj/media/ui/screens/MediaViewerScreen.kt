package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.media.data.ShortsVideoCache
import com.tpsk.tj.media.ui.components.ActiveVideoPlayback

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaViewerScreen(
    url: String,
    type: String = "image",
    initialPosition: Long = 0L,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (type == "video") {
            val exoPlayer = remember(url) {
                androidx.media3.exoplayer.ExoPlayer.Builder(context)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                            ShortsVideoCache.dataSourceFactory(context)
                        )
                    )
                    .build()
            }
            val posterUrl = MediaUtils.posterUrl(url)
            val playerView = remember(url) { mutableStateOf<PlayerView?>(null) }
            var isBuffering by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DisposableEffect(lifecycleOwner, exoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                        exoPlayer.playWhenReady = false
                        exoPlayer.pause()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(url) {
                ActiveVideoPlayback.claim(exoPlayer)
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                if (initialPosition > 0) {
                    exoPlayer.seekTo(initialPosition)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        errorMessage = error.localizedMessage ?: "Failed to play video"
                        isBuffering = false
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                    val view = playerView.value
                    playerView.value = null
                    ActiveVideoPlayback.detachAndRelease(view, exoPlayer)
                }
            }

            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        playerView.value = this
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    playerView.value = view
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && errorMessage == null) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unable to play video",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange: Float, offsetChange: Offset, _: Float ->
                scale *= zoomChange
                offset += offsetChange
            }

            val finalScale = if (scale < 1f) 1f else if (scale > 5f) 5f else scale

            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = finalScale,
                        scaleY = finalScale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}
