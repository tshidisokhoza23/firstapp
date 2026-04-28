package com.example.firstapp.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.firstapp.data.media.ShortsVideoCache
import com.example.firstapp.model.Short
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppPrimary
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortItem(
    short: Short,
    isVisible: Boolean,
    bottomInset: Dp,
    onLikeClick: (Int) -> Unit,
    onSubscribeClick: (Int) -> Unit,
    onCommentClick: (Int) -> Unit,
    onRepostClick: (Int) -> Unit,
    onBookmarkClick: (Int) -> Unit,
    onMoreClick: (Int) -> Unit,
    onVideoEnded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val videoUrl = short.videoUrl.takeIf { it.isNotBlank() }
    
    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(false) }
    var controlsTimer by remember { mutableStateOf(0L) }

    val exoPlayer = remember(videoUrl, isVisible) {
        if (isVisible && videoUrl != null) {
            ShortsVideoCache.acquirePlayer(context, videoUrl).apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (isMuted) 0f else 1f
                playWhenReady = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
            }
        } else {
            null
        }
    }

    var isVideoReady by remember(short.id, videoUrl, isVisible) {
        mutableStateOf(false)
    }
    var isBuffering by remember(short.id, videoUrl, isVisible) {
        mutableStateOf(isVisible && videoUrl != null)
    }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    LaunchedEffect(exoPlayer, isVisible) {
        if (isVisible && exoPlayer != null) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                delay(100)
            }
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isPlaying) {
        exoPlayer?.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer, videoUrl) {
        val player = exoPlayer
        if (player == null || videoUrl == null) {
            isVideoReady = false
            isBuffering = false
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isVideoReady = true
                            isBuffering = false
                            duration = player.duration
                        }
                        Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        Player.STATE_ENDED -> {
                            onVideoEnded?.invoke()
                        }
                        else -> {}
                    }
                }
                override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                    isPlaying = isPlayingParam
                }
            }

            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
    }

    val likeIconColor by animateColorAsState(
        targetValue = if (short.isLiked) Color.Red else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LikeAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isMuted = !isMuted
                showPlayerControls = true
                controlsTimer = System.currentTimeMillis()
            }
    ) {
        // Full screen video or poster
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred Thumbnail Background while loading
            if (!isVideoReady) {
                ShortPoster(
                    short = short,
                    modifier = Modifier.fillMaxSize().blur(20.dp),
                    showOverlay = false
                )
            }

            if (exoPlayer != null) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Loading Spinner
            if (isBuffering && isVisible) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f), strokeWidth = 2.dp)
                }
            }
        }

        // Overlay Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset)
        ) {
            // Mute/Unmute Indicator
            LaunchedEffect(showPlayerControls, controlsTimer) {
                if (showPlayerControls) {
                    delay(1500)
                    showPlayerControls = false
                }
            }
            
            AnimatedVisibility(
                visible = showPlayerControls,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Top Left "Live" tag
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right Actions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Avatar with Plus
                Box(contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(short.thumbnailColor)
                    ) {
                        AsyncImage(
                            model = short.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!short.isSubscribed) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(y = 10.dp)
                                .background(Color.White, CircleShape)
                                .clickable { onSubscribeClick(short.id) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                val haptic = LocalHapticFeedback.current
                ShortActionButton(
                    icon = if (short.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    text = short.likes,
                    iconColor = likeIconColor,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick(short.id) 
                    }
                )
                ShortActionButton(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    text = short.comments,
                    onClick = { onCommentClick(short.id) }
                )
                ShortActionButton(
                    icon = Icons.Filled.BookmarkBorder,
                    text = "Save",
                    onClick = { onBookmarkClick(short.id) }
                )
                ShortActionButton(
                    icon = Icons.Filled.Share,
                    text = "Share",
                    onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, short.videoUrl)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    }
                )
                ShortActionButton(
                    icon = Icons.Filled.MoreVert,
                    text = "",
                    onClick = { onMoreClick(short.id) }
                )
            }

            // Bottom Left Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 24.dp, end = 80.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${short.creatorName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (!short.isSubscribed) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            onClick = { onSubscribeClick(short.id) },
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, AppAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text("Subscribe", color = AppAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = short.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Original Audio - ${short.creatorName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
            
            // Bottom Progress Bar
            if (duration > 0) {
                val progress = currentPosition.toFloat() / duration.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AppAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortPoster(
    short: Short, 
    modifier: Modifier = Modifier,
    showOverlay: Boolean = true
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        if (!short.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(short.thumbnailColor.copy(alpha = 0.3f))
            )
        }

        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 300f
                        )
                    )
            )
        }
    }
}

@Composable
fun ShortActionButton(
    icon: ImageVector,
    text: String,
    iconColor: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
