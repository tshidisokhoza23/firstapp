package com.tpsk.tj.stories.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.tpsk.tj.common.ui.theme.AppAccent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.StoryItem
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.chat.ui.components.formatMediaUrl
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.ApiViewer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    onGetStoryViewers: (String) -> Flow<Resource<List<ApiViewer>>> = { kotlinx.coroutines.flow.flowOf() },
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
    val formattedMediaUrl = remember(currentStory.contentUrl) { formatMediaUrl(currentStory.contentUrl) }

    val exoPlayer = remember(currentStory.id) {
        if (currentStory.type == StoryType.VIDEO && !formattedMediaUrl.isNullOrBlank()) {
            com.tpsk.tj.media.data.ShortsVideoCache.acquirePlayer(context, formattedMediaUrl).apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer, formattedMediaUrl) {
        onDispose {
            exoPlayer?.let { 
                if (formattedMediaUrl != null) {
                    com.tpsk.tj.media.data.ShortsVideoCache.releasePlayer(it, formattedMediaUrl)
                } else {
                    it.release()
                }
            }
        }
    }

    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var videoDuration by remember { mutableLongStateOf(10000L) }
    var showViewersSheet by remember { mutableStateOf(false) }
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
            videoDuration = 10000L
        }
    }

    LaunchedEffect(currentStoryIndex, isPaused, videoDuration, showViewersSheet, showReplySheet) {
        if (!isPaused && !showViewersSheet && !showReplySheet) {
            onStoryViewed(currentStoryIndex)

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
            .background(if (currentStory.type == StoryType.TEXT) currentStory.backgroundColor else Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (abs(dragAmount.x) > abs(dragAmount.y) && offsetY == 0f) {
                            offsetX += dragAmount.x
                        } else if (dragAmount.y > 0 || offsetY > 0) {
                            offsetY += dragAmount.y
                        } else if (dragAmount.y < -15f && isOwnStory) {
                            // Swipe up ONLY on own story triggers Viewers sheet
                            showViewersSheet = true
                            isPaused = true
                        }
                    },
                    onDragEnd = {
                        if (abs(offsetX) > 240f) {
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
                            if (!showViewersSheet && !showReplySheet) {
                                isPaused = false
                            }
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
                    if (!formattedMediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = formattedMediaUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Story Caption Overlay
        if (currentStory.type != StoryType.TEXT && currentStory.type != StoryType.AD && currentStory.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (!isOwnStory) 88.dp else 48.dp, start = 20.dp, end = 20.dp)
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

        // Top Header & Progress Indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
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
                            .height(2.5.dp)
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
                            model = formatMediaUrl(userStory.userAvatarUrl),
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

        // Bottom Footer (Own Story: Viewers Count & Pill vs Contact Story: Passive Reply Container)
        if (isOwnStory) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clickable {
                        isPaused = true
                        showViewersSheet = true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Views",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${currentStory.viewsCount}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (!showReplySheet && currentStory.type != StoryType.AD) {
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
                Text(
                    text = "Reply to ${userStory.userName}...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "❤️  😂  😮  🔥",
                    fontSize = 15.sp
                )
            }
        }

        // Viewers Sheet for Author
        if (showViewersSheet && isOwnStory) {
            StoryViewersSheet(
                storyItem = currentStory,
                onDismiss = {
                    showViewersSheet = false
                    isPaused = false
                },
                getViewersFlow = onGetStoryViewers
            )
        }

        // Quick Reply Sheet for Contacts
        if (showReplySheet && !isOwnStory) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF1E2428))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Quick Reactions",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.take(6).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.clickable {
                                    onReplyStory(currentStory.id, emoji, currentStory.content)
                                    showReplySheet = false
                                    isPaused = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Send reply...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReplyStory(currentStory.id, replyText, currentStory.content)
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
