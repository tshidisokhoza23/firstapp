package com.tpsk.tj.stories.ui

import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Send
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.common.ui.theme.AppAccent
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
    onFinish: () -> Unit,
    onClose: () -> Unit,
    onNavigateToUser: (Int) -> Unit = {},
    onReplyStory: (storyId: String, text: String, storySummary: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStoryIndex by remember(userStory.id, initialStoryIndex) {
        mutableIntStateOf(initialStoryIndex.coerceIn(0, (userStory.stories.size - 1).coerceAtLeast(0))) 
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasFinished by remember(userStory.id) { mutableStateOf(false) }

    fun finishOnce() {
        if (!hasFinished) {
            hasFinished = true
            onFinish()
        }
    }

    val stories = userStory.stories
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { finishOnce() }
        return
    }
    
    val currentStory = stories[currentStoryIndex]
    
    val exoPlayer = remember(currentStory.id) {
        if (currentStory.type == StoryType.VIDEO && !currentStory.contentUrl.isNullOrBlank()) {
            com.tpsk.tj.media.data.ShortsVideoCache.acquirePlayer(context, currentStory.contentUrl!!).apply {
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
                    com.tpsk.tj.media.data.ShortsVideoCache.releasePlayer(it, url)
                } else {
                    it.release()
                }
            }
        }
    }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var videoDuration by remember { mutableLongStateOf(10000L) }
    var showReplySheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val emojis = remember { listOf("❤️", "🔥", "😂", "😮", "😢", "👏", "🙌", "💯", "😍", "🎉", "👍", "🙏") }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isPaused = true
                    exoPlayer?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    isPaused = false
                    exoPlayer?.playWhenReady = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    
    // Track horizontal story navigation separately from downward dismissal.
    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // Video duration listener
    LaunchedEffect(exoPlayer) {
        if (exoPlayer != null) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        videoDuration = exoPlayer.duration.coerceAtLeast(1000L)
                    }
                }
            }
            exoPlayer.addListener(listener)
            if (exoPlayer.duration > 0) {
                videoDuration = exoPlayer.duration
            }
        } else {
            videoDuration = 10000L // Default for images/text
        }
    }

    LaunchedEffect(currentStoryIndex, isPaused, videoDuration) {
        if (!isPaused) {
            // Count as viewed at the start of the story
            onStoryViewed(currentStoryIndex)

            // Ensure duration is based on content
            val durationMs = if (currentStory.type == StoryType.VIDEO) {
                videoDuration
            } else {
                currentStory.durationMs.toLong().coerceAtLeast(10000L)
            }
            
            progress.snapTo(0f)

            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs.toInt(),
                    easing = LinearEasing
                )
            )
            
            if (currentStoryIndex < stories.size - 1) {
                progress.snapTo(0f)
                currentStoryIndex++
            } else {
                finishOnce()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .background(if (currentStory.type == StoryType.TEXT) Color.DarkGray else Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) && offsetY == 0f) {
                            offsetX += dragAmount.x
                        } else if (dragAmount.y > 0 || offsetY > 0) {
                            offsetY += dragAmount.y
                        } else if (dragAmount.y < -15f && !isOwnStory && currentStory.type != StoryType.AD) {
                            showReplySheet = true
                            isPaused = true
                        }
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 240f) {
                            if (offsetX < 0 && currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                            } else if (offsetX < 0) {
                                finishOnce()
                            } else if (offsetX > 0 && currentStoryIndex > 0) {
                                currentStoryIndex--
                            } else if (offsetX > 0) {
                                onNavigateToUser(-1)
                            }
                            scope.launch { progress.snapTo(0f) }
                        } else if (offsetY > 300f) {
                            onClose()
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = { offsetX = 0f; offsetY = 0f }
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
                                onNavigateToUser(-1)
                            }
                        } else {
                            if (currentStoryIndex < stories.size - 1) {
                                scope.launch { progress.snapTo(0f) }
                                currentStoryIndex++
                            } else {
                                finishOnce()
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
                StoryType.AD -> {
                    if (currentStory.adObject != null) {
                        com.tpsk.tj.common.ui.components.NativeAdCard(
                            adObject = currentStory.adObject,
                            modifier = Modifier.fillMaxSize(),
                            layoutResId = com.tpsk.tj.R.layout.item_story_native_ad
                        )
                    }
                }
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
                StoryType.IMAGE -> {
                    if (!currentStory.contentUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentStory.contentUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Story Media Caption Overlay (For Image/Video)
        if (currentStory.type != StoryType.TEXT && currentStory.type != StoryType.AD && currentStory.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (!isOwnStory) 88.dp else 28.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentStory.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
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
                        if (currentStory.type == StoryType.AD) "Sponsored" else if (isOwnStory) "My story" else userStory.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (currentStory.type == StoryType.AD) "Promoted" else if (currentStory.createdAt > 0) com.tpsk.tj.chat.data.repository.formatTimeAgo(currentStory.createdAt) else currentStory.timestamp,
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

        // Bottom Reply trigger bar (when sheet is closed)
        if (!showReplySheet && !isOwnStory && currentStory.type != StoryType.AD) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        showReplySheet = true
                        isPaused = true
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Swipe up to reply",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send message...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Reply & Emoji Reactions Overlay Sheet
        if (showReplySheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        showReplySheet = false
                        isPaused = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures { } // Consume all tap gestures inside sheet so story viewer tap gestures are not triggered
                        }
                        .clickable(enabled = false) {}, // prevent click-through
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFF1E1E1E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "React or reply to ${userStory.userName}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                showReplySheet = false
                                isPaused = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Emoji Reactions (Horizontal Scrollable Row)
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(emojis.size) { index ->
                                val emoji = emojis[index]
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable {
                                            val storySummary = when {
                                                currentStory.content.isNotBlank() -> currentStory.content
                                                currentStory.type == StoryType.VIDEO -> "Video story"
                                                currentStory.type == StoryType.IMAGE -> "Photo story"
                                                else -> "Story"
                                            }
                                            onReplyStory(currentStory.id, emoji, storySummary)
                                            android.widget.Toast.makeText(context, "Reaction sent $emoji", android.widget.Toast.LENGTH_SHORT).show()
                                            showReplySheet = false
                                            isPaused = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reply Input & Send
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Send message...", color = Color.Gray, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppAccent,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF2C2C2C),
                                    unfocusedContainerColor = Color(0xFF2C2C2C)
                                ),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val body = replyText.trim()
                                    if (body.isNotEmpty()) {
                                        val storySummary = when {
                                            currentStory.content.isNotBlank() -> currentStory.content
                                            currentStory.type == StoryType.VIDEO -> "Video story"
                                            currentStory.type == StoryType.IMAGE -> "Photo story"
                                            else -> "Story"
                                        }
                                        onReplyStory(currentStory.id, body, storySummary)
                                        android.widget.Toast.makeText(context, "Reply sent", android.widget.Toast.LENGTH_SHORT).show()
                                        replyText = ""
                                        showReplySheet = false
                                        isPaused = false
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AppAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun InViewerStoryAd(
    adObject: Any?,
    onFinish: () -> Unit, 
    onClose: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(adObject) {
        // Ads typically show for 10-15 seconds if not interactive
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
        )
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (adObject != null) {
            com.tpsk.tj.common.ui.components.NativeAdCard(
                adObject = adObject,
                modifier = Modifier.fillMaxSize(),
                layoutResId = com.tpsk.tj.R.layout.item_story_native_ad
            )
        } else {
            // Fallback if ad disappeared
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
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
            "Sponsored",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}
