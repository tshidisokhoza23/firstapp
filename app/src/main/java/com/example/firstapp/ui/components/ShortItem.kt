package com.example.firstapp.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.firstapp.data.media.ShortsVideoCache
import com.example.firstapp.model.Short

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortItem(
    short: Short,
    isVisible: Boolean,
    bottomInset: Dp,
    onLikeClick: (Int) -> Unit,
    onSubscribeClick: (Int) -> Unit,
    onVideoEnded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bottomOverlayPadding = if (bottomInset > 0.dp) bottomInset + 8.dp else 20.dp
    val videoUrl = short.videoUrl.takeIf { it.isNotBlank() }
    val exoPlayer = remember(videoUrl, isVisible) {
        if (isVisible && videoUrl != null) {
            ShortsVideoCache.buildPlayer(context)
        } else {
            null
        }
    }

    var isVideoReady by remember(short.id, videoUrl, isVisible) {
        mutableStateOf(videoUrl == null || !isVisible)
    }
    var isBuffering by remember(short.id, videoUrl, isVisible) {
        mutableStateOf(videoUrl != null && isVisible)
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
                        }
                        Player.STATE_BUFFERING,
                        Player.STATE_IDLE -> {
                            isBuffering = true
                        }
                        Player.STATE_ENDED -> {
                            isBuffering = false
                            onVideoEnded?.invoke()
                        }
                    }
                }
            }

            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            player.playWhenReady = isVisible

            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
    }

    LaunchedEffect(exoPlayer, isVisible) {
        exoPlayer?.playWhenReady = isVisible
    }

    val likeIconColor by animateColorAsState(
        targetValue = if (short.isLiked) Color.Red else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LikeAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
    ) {
        ShortPoster(
            short = short,
            modifier = Modifier.fillMaxSize()
        )

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
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (videoUrl != null && isVisible && isBuffering) {
                    Surface(
                        color = Color(0xB310161A),
                        shape = CircleShape
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFB84D),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Buffering video",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.84f)
                        ),
                        startY = 120f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 24.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomOverlayPadding, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like
            ShortActionButton(
                icon = if (short.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                text = short.likes,
                iconColor = likeIconColor,
                onClick = { onLikeClick(short.id) }
            )
            // Replies (comments)
            ShortActionButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                text = short.comments,
                onClick = {
                    android.widget.Toast.makeText(
                        LocalContext.current,
                        "Replies coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
            // Repost
            ShortActionButton(
                icon = Icons.Filled.Repeat,
                text = "Repost",
                onClick = {
                    android.widget.Toast.makeText(
                        LocalContext.current,
                        "Repost coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
            // Share
            ShortActionButton(
                icon = Icons.Filled.Share,
                text = "Share",
                onClick = {
                    val context = LocalContext.current
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, short.videoUrl)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )
            // Bookmark
            ShortActionButton(
                icon = Icons.Filled.BookmarkBorder,
                text = "Save",
                onClick = {
                    android.widget.Toast.makeText(
                        LocalContext.current,
                        "Bookmark coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
            // 3-dots more menu
            ShortActionButton(
                icon = Icons.Filled.MoreVert,
                text = "More",
                onClick = {
                    android.widget.Toast.makeText(
                        LocalContext.current,
                        "More options coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = bottomOverlayPadding, end = 80.dp)
        ) {
            Surface(
                color = Color(0x96202A31),
                shape = CircleShape,
                tonalElevation = 0.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFFB84D), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live in Shorts",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${short.creatorName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onSubscribeClick(short.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (short.isSubscribed) Color.White.copy(alpha = 0.14f) else Color(0xFFFFB84D),
                        contentColor = if (short.isSubscribed) Color.White else Color(0xFF14181B)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (short.isSubscribed) "Subscribed" else "Subscribe",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = short.description,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0x7020282E),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Original Audio - ${short.creatorName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortPoster(short: Short, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF081017),
                    short.thumbnailColor.copy(alpha = 0.92f),
                    Color(0xFF1B2A35)
                )
            )
        )
    ) {
        if (!short.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )

        Surface(
            color = Color(0xAA11181D),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFFB84D),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "@${short.creatorName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = short.description,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        modifier = Modifier.width(60.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xA5172026),
            tonalElevation = 0.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
