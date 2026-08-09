package com.example.firstapp.media.ui.screens

import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.media.model.UserStory
import com.example.firstapp.media.model.StoryType
import com.example.firstapp.common.ui.theme.AppAccent
import kotlinx.coroutines.*
import kotlin.math.roundToInt

@androidx.media3.common.util.UnstableApi
@Composable
fun StoryViewer(
    userStory: UserStory,
    isOwnStory: Boolean = false,
    initialStoryIndex: Int = 0,
    onStoryViewed: (Int) -> Unit,
    onDeleteStory: (String) -> Unit = {},
    onAdTriggered: () -> Unit = {},
    onReply: (String) -> Unit = {},
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentStoryIndex by remember(userStory.id, initialStoryIndex) { 
        mutableIntStateOf(initialStoryIndex.coerceIn(0, (userStory.stories.size - 1).coerceAtLeast(0))) 
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val stories = userStory.stories
    if (stories.isEmpty()) {
        onFinish()
        return
    }
    
    val currentStory = stories[currentStoryIndex]
    
    val exoPlayer = remember(currentStory.id) {
        if (currentStory.type == StoryType.VIDEO && !currentStory.contentUrl.isNullOrBlank()) {
            com.example.firstapp.media.data.ShortsVideoCache.acquirePlayer(context, currentStory.contentUrl!!).apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer, currentStory.contentUrl) {
        onDispose {
            exoPlayer?.let { 
                val url = currentStory.contentUrl
                if (url != null) {
                    com.example.firstapp.media.data.ShortsVideoCache.releasePlayer(it, url)
                } else {
                    it.release()
                }
            }
        }
    }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // For swipe to dismiss
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentStoryIndex, isPaused) {
        if (!isPaused) {
            // Count as viewed at the start of the story
            onStoryViewed(currentStoryIndex)

            val durationMs = currentStory.durationMs.toLong().coerceAtLeast(1000L)
            
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ((1f - progress.value) * durationMs).toInt(),
                    easing = LinearEasing
                )
            )
            
            if (currentStoryIndex < stories.size - 1) {
                progress.snapTo(0f)
                currentStoryIndex++
            } else {
                onFinish()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .background(if (currentStory.type == StoryType.TEXT) Color.DarkGray else Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 0 || offsetY > 0) {
                            offsetY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        if (offsetY > 300f) {
                            onClose()
                        } else {
                            offsetY = 0f
                        }
                    },
                    onDragCancel = { offsetY = 0f }
                )
            }
            .pointerInput(currentStoryIndex) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        try {
                            awaitRelease()
                        } finally {
                            isPaused = false
                        }
                    },
                    onTap = { offset ->
                        if (offset.x < size.width / 3) {
                            if (currentStoryIndex > 0) {
                                scope.launch { progress.snapTo(0f) }
                                currentStoryIndex--
                            } else {
                                onClose()
                            }
                        } else {
                            if (currentStoryIndex < stories.size - 1) {
                                scope.launch { progress.snapTo(0f) }
                                currentStoryIndex++
                            } else {
                                onFinish()
                            }
                        }
                    }
                )
            }
    ) {
        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentStory.type) {
                StoryType.TEXT -> {
                    Text(
                        text = currentStory.content,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(40.dp)
                    )
                }
                StoryType.VIDEO -> {
                    if (exoPlayer != null) {
                        AndroidView(
                            factory = {
                                androidx.media3.ui.PlayerView(it).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    if (!currentStory.contentUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentStory.contentUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (!currentStory.content.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))))
                                .padding(bottom = 100.dp, top = 40.dp, start = 24.dp, end = 24.dp)
                        ) {
                            Text(
                                text = currentStory.content,
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // Progress indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val barProgress = when {
                        index < currentStoryIndex -> 1f
                        index == currentStoryIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userStory.userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userStory.userAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            userStory.userName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isOwnStory) "My story" else userStory.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        currentStory.timestamp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                if (isOwnStory) {
                    IconButton(onClick = { isPaused = true; showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false; isPaused = false },
                containerColor = Color.DarkGray,
                title = { Text("Delete story update?", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteStory(currentStory.id)
                        showDeleteConfirm = false
                        isPaused = false
                        if (stories.size > 1) {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                            } else {
                                currentStoryIndex--
                            }
                        } else {
                            onClose()
                        }
                    }) {
                        Text("DELETE", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false; isPaused = false }) {
                        Text("CANCEL", color = Color.White)
                    }
                }
            )
        }

        // Reply Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var replyText by remember { mutableStateOf("") }
            
            Surface(
                modifier = Modifier.weight(1f).height(48.dp),
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = {
                            if (replyText.isNotBlank()) {
                                onReply(replyText)
                                replyText = ""
                            }
                        }
                    )
                )
            }
            
            // Reaction Emojis
            Text(
                "😍", 
                fontSize = 28.sp, 
                modifier = Modifier.clickable { 
                    Toast.makeText(context, "Sent 😍", Toast.LENGTH_SHORT).show()
                }
            )
            Text(
                "😂", 
                fontSize = 28.sp, 
                modifier = Modifier.clickable { 
                    Toast.makeText(context, "Sent 😂", Toast.LENGTH_SHORT).show()
                }
            )
            Text(
                "😮", 
                fontSize = 28.sp, 
                modifier = Modifier.clickable { 
                    Toast.makeText(context, "Sent 😮", Toast.LENGTH_SHORT).show()
                }
            )
            
            IconButton(onClick = { 
                Toast.makeText(context, "Story reshared", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Repeat, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            var isLiked by remember { mutableStateOf(false) }
            IconButton(onClick = { 
                isLiked = !isLiked
                Toast.makeText(context, if (isLiked) "Story liked" else "Like removed", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    null, 
                    tint = if (isLiked) Color.Red else Color.White, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun InViewerStoryAd(onFinish: () -> Unit, onClose: () -> Unit) {
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        com.example.firstapp.media.ui.components.NativeAdCard(
            adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
            modifier = Modifier.align(Alignment.Center).padding(16.dp)
        )
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 20.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
        
        Text(
            "Advertisement",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}
