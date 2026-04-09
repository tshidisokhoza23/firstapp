package com.example.firstapp.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.firstapp.model.Short

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ShortItem(short: Short, isVisible: Boolean, onLikeClick: (Int) -> Unit, onSubscribeClick: (Int) -> Unit) {
    val context = LocalContext.current
    var isLiked by remember(short.id) { mutableStateOf(short.isLiked) }
    var isSubscribed by remember(short.id) { mutableStateOf(short.isSubscribed) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    LaunchedEffect(short.videoUrl) {
        if (short.videoUrl.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(short.videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val likeIconColor by animateColorAsState(
        targetValue = if (isLiked) Color.Red else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LikeAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Real Video Player
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gradient for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 500f
                    )
                )
        )

        // Right side action bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(short.profilePictureColor, CircleShape)
                        .padding(2.dp)
                        .background(Color.White, CircleShape)
                )
                if (!isSubscribed) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = 8.dp)
                            .background(Color.White, CircleShape)
                            .clickable { 
                                isSubscribed = true
                                onSubscribeClick(short.id) 
                            }
                    )
                }
            }

            ShortActionButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                text = short.likes,
                iconColor = likeIconColor,
                onClick = { 
                    isLiked = !isLiked
                    onLikeClick(short.id)
                }
            )

            ShortActionButton(
                icon = Icons.Default.Comment,
                text = short.comments,
                onClick = { /* Open Comments */ }
            )

            ShortActionButton(
                icon = Icons.Default.Share,
                text = short.shares,
                onClick = { /* Share */ }
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF2C2C2E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 32.dp, end = 80.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${short.creatorName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = { 
                        isSubscribed = !isSubscribed
                        onSubscribeClick(short.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        contentColor = if (isSubscribed) Color.White else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
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
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Original Audio - ${short.creatorName}",
                    color = Color.White,
                    fontSize = 13.sp
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
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
