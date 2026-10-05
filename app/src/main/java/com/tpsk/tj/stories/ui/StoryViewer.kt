@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.stories.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.isActive
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
    onNavigateToUser: (direction: Int, storyIndex: Int) -> Unit = { _, _ -> },
    onStoryIndexChanged: (Int) -> Unit = {},
    isForeground: Boolean = true,
    onGetStoryViewers: (String) -> Flow<Resource<List<ApiViewer>>> = { kotlinx.coroutines.flow.flowOf() },
    onReplyStory: (storyId: String, text: String, storySummary: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStoryIndex by remember(userStory.id) {
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
        LaunchedEffect(userStory.id) {
            kotlinx.coroutines.delay(6_000)
            if (isActive) finishOnce()
        }
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Story unavailable", color = Color.White)
        }
        return
    }

    LaunchedEffect(currentStoryIndex, isForeground) {
        if (isForeground) onStoryIndexChanged(currentStoryIndex)
    }

    val currentStory = stories[currentStoryIndex]
    val formattedMediaUrl = remember(currentStory.contentUrl) { formatMediaUrl(currentStory.contentUrl) }
    val visualType = remember(currentStory.id, currentStory.type, formattedMediaUrl, currentStory.content) {
        storyVisualType(currentStory, formattedMediaUrl)
    }

    val exoPlayer = remember(currentStory.id, formattedMediaUrl, visualType) {
        if (visualType == StoryType.VIDEO && !formattedMediaUrl.isNullOrBlank()) {
            com.tpsk.tj.media.data.ShortsVideoCache.acquirePlayer(context, formattedMediaUrl, loop = false).apply {
                setPlaybackSpeed(1f)
                seekTo(0)
                playWhenReady = false
            }
        } else {
            null
        }
    }

    DisposableEffect(formattedMediaUrl, exoPlayer) {
        onDispose {
            if (!formattedMediaUrl.isNullOrBlank()) {
                com.tpsk.tj.media.data.ShortsVideoCache.releasePlayer(formattedMediaUrl)
            }
        }
    }

    LaunchedEffect(currentStoryIndex, stories) {
        val upcoming = stories.drop(currentStoryIndex).take(3).mapNotNull { story ->
            val url = formatMediaUrl(story.contentUrl)
            if (storyVisualType(story, url) == StoryType.VIDEO) url else null
        }
        if (upcoming.isNotEmpty()) {
            com.tpsk.tj.media.data.ShortsVideoCache.preload(context, upcoming)
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
    val viewConfiguration = LocalViewConfiguration.current

    var isVideoBuffering by remember(currentStory.id) { mutableStateOf(visualType == StoryType.VIDEO && exoPlayer != null) }
    var isImageLoading by remember(currentStory.id) { mutableStateOf(visualType == StoryType.IMAGE && !formattedMediaUrl.isNullOrBlank()) }
    var imageFailed by remember(currentStory.id) { mutableStateOf(false) }

    val isMediaLoading = isVideoBuffering || isImageLoading
    val effectivePaused = !isForeground || isPaused || isMediaLoading || showViewersSheet || showReplySheet
    val pausedState = rememberUpdatedState(effectivePaused)
    val holdDuration = rememberUpdatedState(
        storyHoldDurationMs(visualType, currentStory.durationMs, videoDuration, videoReady = !isVideoBuffering)
    )

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: run {
            isVideoBuffering = false
            return@LaunchedEffect
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isVideoBuffering = true
                    Player.STATE_READY -> {
                        val reported = player.duration
                        if (reported in 1..3_600_000) {
                            videoDuration = reported
                            isVideoBuffering = false
                        }
                    }
                    Player.STATE_ENDED -> player.playWhenReady = false
                }
            }

            override fun onRenderedFirstFrame() {
                isVideoBuffering = false
            }
        }
        player.addListener(listener)
        val reported = player.duration
        if (player.playbackState == Player.STATE_READY && reported in 1..3_600_000) {
            videoDuration = reported
            isVideoBuffering = false
        }
    }

    LaunchedEffect(effectivePaused, exoPlayer) {
        exoPlayer?.playWhenReady = !effectivePaused && exoPlayer.playbackState != Player.STATE_ENDED
    }

    LaunchedEffect(currentStory.id, isVideoBuffering) {
        if (!isVideoBuffering) return@LaunchedEffect
        kotlinx.coroutines.delay(8_000)
        if (isVideoBuffering) {
            isVideoBuffering = false
            if (videoDuration !in 1..3_600_000) videoDuration = 6_000
        }
    }

    LaunchedEffect(currentStory.id) {
        progress.snapTo(0f)
        if (isForeground) onStoryViewed(currentStoryIndex)
        var elapsed = 0L
        var lastFrame = 0L
        while (isActive) {
            val frame = withFrameNanos { it }
            val duration = holdDuration.value
            val holding = pausedState.value || duration <= 0L
            if (!holding && lastFrame != 0L) {
                elapsed += ((frame - lastFrame) / 1_000_000L).coerceAtLeast(0L)
            }
            lastFrame = frame
            if (duration > 0L) {
                progress.snapTo((elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f))
                if (elapsed >= duration) break
            }
        }
        if (!isActive || !isForeground) return@LaunchedEffect
        if (currentStoryIndex < stories.lastIndex) {
            currentStoryIndex++
        } else {
            onNavigateToUser(1, currentStoryIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .background(if (currentStory.type == StoryType.TEXT) currentStory.backgroundColor else Color.Black)
            .pointerInput(currentStoryIndex, isOwnStory) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var verticalDismiss = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (verticalDismiss && offsetY > 300f) onClose()
                            offsetY = 0f
                            break
                        }
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (!verticalDismiss && (abs(totalX) > slop || abs(totalY) > slop)) {
                            if (abs(totalX) > abs(totalY)) {
                                break
                            }
                            if (totalY < 0f && isOwnStory) {
                                showViewersSheet = true
                                isPaused = true
                                change.consume()
                                break
                            }
                            verticalDismiss = totalY > 0f
                        }
                        if (verticalDismiss) {
                            change.consume()
                            offsetY = (offsetY + delta.y).coerceAtLeast(0f)
                        }
                    }
                }
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
                        if (offset.x < size.width * 0.3f) {
                            if (currentStoryIndex > 0) {
                                scope.launch { progress.snapTo(0f) }
                                currentStoryIndex--
                            } else {
                                onNavigateToUser(-1, currentStoryIndex)
                            }
                        } else {
                            if (currentStoryIndex < stories.size - 1) {
                                scope.launch { progress.snapTo(0f) }
                                currentStoryIndex++
                            } else {
                                onNavigateToUser(1, currentStoryIndex)
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
            when (visualType) {
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
                        val poster = com.tpsk.tj.common.util.MediaUtils.posterUrl(formattedMediaUrl)
                            ?: userStory.userAvatarUrl?.takeIf { it.isNotBlank() }?.let { formatMediaUrl(it) ?: it }
                        if (!poster.isNullOrBlank()) {
                            AsyncImage(
                                model = poster,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AndroidView(
                            factory = { viewContext ->
                                (android.view.LayoutInflater.from(viewContext)
                                    .inflate(com.tpsk.tj.R.layout.story_player_view, null) as androidx.media3.ui.PlayerView)
                            },
                            update = { view ->
                                view.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                if (view.player !== exoPlayer) view.player = exoPlayer
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isVideoBuffering) 0f else 1f)
                        )
                    } else {
                        StoryUnavailableText(currentStory.content)
                    }
                }
                StoryType.IMAGE -> {
                    if (!formattedMediaUrl.isNullOrBlank() && !imageFailed) {
                        AsyncImage(
                            model = formattedMediaUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onLoading = { isImageLoading = true },
                            onSuccess = { isImageLoading = false },
                            onError = {
                                isImageLoading = false
                                imageFailed = true
                            }
                        )
                    } else {
                        StoryUnavailableText(currentStory.content)
                    }
                }
            }
        }

        // Story Caption Overlay
        if (visualType != StoryType.TEXT && visualType != StoryType.AD && currentStory.content.isNotBlank() && !imageFailed) {
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


private fun storyVisualType(story: StoryItem, mediaUrl: String?): StoryType {
    if (story.type == StoryType.AD) return StoryType.AD
    val path = mediaUrl?.substringBefore('?')?.lowercase().orEmpty()
    val video = path.endsWith(".mp4") || path.endsWith(".mov") || path.endsWith(".webm") || path.endsWith(".m3u8")
    val image = path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".gif")
    return when {
        story.type == StoryType.VIDEO || video -> if (mediaUrl.isNullOrBlank()) StoryType.TEXT else StoryType.VIDEO
        story.type == StoryType.IMAGE || image -> if (mediaUrl.isNullOrBlank()) StoryType.TEXT else StoryType.IMAGE
        !mediaUrl.isNullOrBlank() && story.content.isBlank() -> StoryType.IMAGE
        else -> StoryType.TEXT
    }
}

private fun storyHoldDurationMs(type: StoryType, storyDurationMs: Int, videoDurationMs: Long, videoReady: Boolean): Long {
    return when (type) {
        StoryType.VIDEO -> if (!videoReady || videoDurationMs !in 1..3_600_000) 0L else videoDurationMs.coerceIn(3_000L, 120_000L)
        else -> storyDurationMs.toLong().coerceIn(6_000L, 15_000L)
    }
}

@Composable
private fun StoryUnavailableText(content: String) {
    Text(
        text = content.ifBlank { "Couldn't load this story" },
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(40.dp)
    )
}
