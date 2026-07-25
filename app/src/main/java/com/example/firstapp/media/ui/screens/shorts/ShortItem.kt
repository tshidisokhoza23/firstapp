package com.example.firstapp.media.ui.screens.shorts

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.model.Post
import com.example.firstapp.media.model.PostType
import com.example.firstapp.media.ui.components.FloatingVideoAdOverlay
import com.example.firstapp.common.ui.theme.AppAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortItem(
    adManager: com.example.firstapp.common.data.ads.AdManager,
    short: com.example.firstapp.media.model.Post,
    currentUserAvatarUrl: String? = null,
    isVisible: Boolean,
    bottomInset: Dp,
    onLikeClick: (String) -> Unit,
    onSubscribeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onMoreClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit = {},
    onEngagement: (String, String, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onVideoEnded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val videoUrl = short.mediaUrls.firstOrNull()?.takeIf { it.isNotBlank() }
    
    var startTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            startTime = System.currentTimeMillis()
        } else if (startTime > 0) {
            val watchTime = (System.currentTimeMillis() - startTime).toInt()
            onEngagement(short.id, "dwell", watchTime, false)
            startTime = 0
        }
    }
    
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(false) }
    var controlsTimer by remember { mutableLongStateOf(0L) }
    
    var showHeartAnimation by remember { mutableStateOf(false) }
    var heartOffset by remember { mutableStateOf(Offset.Zero) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInBackground by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isAppInBackground = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInBackground = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exoPlayer: androidx.media3.exoplayer.ExoPlayer? = remember(videoUrl, isVisible) {
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
    var playerErrorMessage by remember(short.id) { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer, isVisible) {
        if (isVisible && exoPlayer != null) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                delay(100)
            }
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isPlaying, isAppInBackground) {
        exoPlayer?.playWhenReady = isPlaying && !isAppInBackground
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
                            val watchTime = (System.currentTimeMillis() - startTime).toInt()
                            onEngagement(short.id, "dwell", watchTime, true)
                            onVideoEnded?.invoke()
                        }
                        else -> {}
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val cause = error.cause?.message ?: error.message
                    isBuffering = false
                    playerErrorMessage = "Video playback failed: $cause"
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
                ShortsVideoCache.releasePlayer(player, videoUrl)
            }
        }
    }

    val likeIconColor by animateColorAsState(
        targetValue = if (short.isLiked) Color.Red else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LikeAnimation"
    )

    val likeIcon = if (short.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        heartOffset = offset
                        showHeartAnimation = true
                        if (!short.isLiked) onLikeClick(short.id)
                    },
                    onTap = {
                        isPlaying = !isPlaying
                        showPlayerControls = true
                        controlsTimer = System.currentTimeMillis()
                    }
                )
            }
        ) {
            if (!isVideoReady) {
                ShortPoster(
                    short = short,
                    modifier = Modifier.fillMaxSize().blur(20.dp),
                    showOverlay = false
                )
            }

            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (isBuffering && isVisible) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f), strokeWidth = 2.dp)
                }
            }

            if (playerErrorMessage != null && isVisible) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(playerErrorMessage!!, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                playerErrorMessage = null
                                isBuffering = true
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
        }

        if (showHeartAnimation) {
            TikTokHeartAnimation(
                offset = heartOffset,
                onAnimationEnd = { showHeartAnimation = false }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset)
        ) {
            LaunchedEffect(showPlayerControls, controlsTimer) {
                if (showPlayerControls) {
                    delay(1000)
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
                            imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A4B57))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { 
                            // Go to my own profile
                            short.authorId.takeIf { it == short.authorId }?.let { 
                                // Actually we want the current user's profile
                                // but we need the current user's ID
                                // For now, if we don't have it explicitly passed as a param,
                                // we can use a callback.
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val formattedUrl = com.example.firstapp.common.util.MediaUtils.formatUrl(currentUserAvatarUrl)
                    if (formattedUrl != null) {
                        AsyncImage(
                            model = formattedUrl,
                            contentDescription = "Me",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))

                val haptic = LocalHapticFeedback.current
                ShortActionButton(
                    icon = likeIcon,
                    text = short.likes.toString(),
                    iconColor = likeIconColor,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick(short.id) 
                    }
                )
                ShortActionButton(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    text = short.repliesCount.toString(),
                    onClick = { onCommentClick(short.id) }
                )
                ShortActionButton(
                    icon = if (short.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    text = "Save",
                    onClick = { onBookmarkClick(short.id) }
                )
                ShortActionButton(
                    icon = Icons.Filled.Share,
                    text = short.shares.toString(),
                    onClick = {
                        onShareClick(short.id)
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, videoUrl ?: "")
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 24.dp, end = 80.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(short.authorAvatarColor)
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { onCreatorClick(short.authorId) }
                    ) {
                        val formattedUrl = com.example.firstapp.common.util.MediaUtils.formatUrl(short.authorAvatarUrl)
                        AsyncImage(
                            model = formattedUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.clickable { onCreatorClick(short.authorId) }) {
                        Text(
                            text = short.authorName,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "@${short.authorHandle}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = short.content,
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
                        "Original Audio - ${short.authorName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
            
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

            var showAdOverlay by remember { mutableStateOf(true) }
            if (short.type == com.example.firstapp.media.model.PostType.INTERNAL_AD) {
                FloatingVideoAdOverlay(
                    adManager = adManager,
                    adUnitId = "ca-app-pub-3940256099942544/2247696110", // AdMob Native Test ID
                    isVisible = isVisible && showAdOverlay,
                    onClose = { showAdOverlay = false },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun TikTokHeartAnimation(
    offset: Offset,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(Unit) {
        rotation.snapTo((-15..15).random().toFloat())
        launch {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            scale.animateTo(1f)
        }
        launch {
            delay(500)
            alpha.animateTo(0f, animationSpec = tween(300))
            onAnimationEnd()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (offset.x - 100).toInt(),
                        (offset.y - 100).toInt()
                    )
                }
                .size(100.dp)
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    alpha = alpha.value,
                    rotationZ = rotation.value
                )
        )
    }
}

@Composable
private fun ShortPoster(
    short: com.example.firstapp.media.model.Post, 
    modifier: Modifier = Modifier,
    showOverlay: Boolean = true
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        val model = if (!short.thumbnailUrl.isNullOrBlank()) {
            short.thumbnailUrl
        } else if (short.mediaUrls.isNotEmpty()) {
            short.mediaUrls[0]
        } else {
            null
        }

        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background((short.mediaPlaceholderColor ?: Color.Gray).copy(alpha = 0.3f))
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
                .shadow(elevation = if (isPressed) 0.dp else 2.dp, shape = CircleShape)
        )
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}
