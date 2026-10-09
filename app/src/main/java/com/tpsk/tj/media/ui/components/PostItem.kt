package com.tpsk.tj.media.ui.components

import com.tpsk.tj.LocalCurrentUser

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import coil.request.videoFrameMillis
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.util.formatTwitterTime
import com.tpsk.tj.media.model.*
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.media.ui.components.VideoPlayer
import com.tpsk.tj.media.ui.components.MediaGrid
import com.tpsk.tj.media.ui.components.PollView
import java.util.Locale
import kotlin.collections.*

import com.tpsk.tj.common.data.ads.AdUnitConfig

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.tpsk.tj.media.model.PostType
import com.tpsk.tj.media.model.PostPoll
import com.tpsk.tj.media.model.PollOption

enum class PostDisplayMode {
    FEED, GRID, IMMERSIVE
}

@Composable
fun PostItem(
    adManager: AdManager,
    post: Post,
    isOwnPost: Boolean = false,
    showThreadLine: Boolean = false,
    showLineAbove: Boolean = true,
    showLineBelow: Boolean = true,
    isLastInThread: Boolean = false,
    depth: Int = 0,
    activePostId: String? = null,
    isVideoPlaying: Boolean = (activePostId != null && activePostId == post.id),
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    displayMode: PostDisplayMode = PostDisplayMode.FEED,
    onClick: () -> Unit,
    onQuotedPostClick: (Post) -> Unit = {},
    onLike: (String, Long, Boolean) -> Unit = { _, _, _ -> },
    onRepostClick: (Post) -> Unit = {},
    onBookmark: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onReplyClick: (Post, Post?) -> Unit = { _, _ -> },
    onProfileClick: (String) -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onMediaClick: (String, String, Long) -> Unit = { _, _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onFollowClick: (String, Boolean) -> Unit = { _, _ -> },
    onMoreClick: (Post) -> Unit = {}
) {
    if (post.type == PostType.EXTERNAL_AD_PLACEHOLDER || post.type == PostType.AD_UNIT || post.type == PostType.NATIVE_AD || post.type == PostType.INTERNAL_AD) {
        if (displayMode == PostDisplayMode.FEED) {
            FeedNativeAdCard(
                adManager = adManager,
                adUnitId = AdUnitConfig.NATIVE_POSTS_ID,
                post = post
            )
        }
    } else {
        when (displayMode) {
            PostDisplayMode.GRID -> {
                ShortGridItem(post = post, onClick = onClick)
            }
            PostDisplayMode.FEED -> {
                val currentUser = LocalCurrentUser.current
                val isSimpleRepost = post.repostOfId != null && post.originalPost != null && post.content.isBlank()
                val displayPost = if (isSimpleRepost) post.originalPost!! else post
                val myUserId = currentUser?.id.orEmpty()
                val myUsername = currentUser?.username.orEmpty()
                val isPostMine = isOwnPost || (myUserId.isNotEmpty() && displayPost.authorId == myUserId) || (myUsername.isNotEmpty() && displayPost.authorHandle.equals(myUsername, ignoreCase = true))
                
                VerticalFeedPostCard(
                    post = post,
                    isOwnPost = isPostMine,
                    activePostId = activePostId,
                    isVideoPlaying = isVideoPlaying,
                    showThreadLine = showThreadLine,
                    showLineAbove = showLineAbove,
                    showLineBelow = showLineBelow,
                    isLastInThread = isLastInThread,
                    depth = depth,
                    autoplayEnabled = autoplayEnabled,
                    autoplayGifsEnabled = autoplayGifsEnabled,
                    onLikeClick = { onLike(post.id, post.likes, post.isLiked) },
                    onCommentClick = { onReplyClick(post, null) },
                    onRepostClick = { onRepostClick(post) },
                    onShareClick = { onShareClick(post) },
                    onBookmarkClick = { onBookmark(post.id) },
                    onFollowClick = { onFollowClick(displayPost.authorId, displayPost.isAuthorFollowed) },
                    onMoreClick = { onMoreClick(post) },
                    onProfileClick = { onProfileClick(displayPost.authorId) },
                    onMediaClick = { url, type, pos -> onMediaClick(url, type, pos) },
                    onQuotedPostClick = onQuotedPostClick,
                    onVotePoll = onVotePoll,
                    modifier = Modifier.clickable(onClick = onClick)
                )
            }
            PostDisplayMode.IMMERSIVE -> {
                // This would be the logic from ShortItem.kt
            }
        }
    }
}

@Composable
fun ShortGridItem(post: Post, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rawModel = if (!post.thumbnailUrl.isNullOrBlank()) post.thumbnailUrl else post.mediaUrls.firstOrNull()
    val model = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawModel)

    val imageRequest = remember(model) {
        if (model != null) {
            coil.request.ImageRequest.Builder(context)
                .data(model)
                .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                .videoFrameMillis(1000)
                .crossfade(true)
                .build()
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2C3E50), Color(0xFF000000))
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.content.take(40),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Views",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = formatShortGridViews(post.views),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatShortGridViews(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VerticalFeedPostCard(
    post: Post,
    isOwnPost: Boolean = false,
    activePostId: String? = null,
    isVideoPlaying: Boolean = (activePostId != null && activePostId == post.id),
    showThreadLine: Boolean = false,
    showLineAbove: Boolean = true,
    showLineBelow: Boolean = true,
    isLastInThread: Boolean = false,
    depth: Int = 0,
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMoreClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    onQuotedPostClick: (Post) -> Unit = {},
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(true) }

    val isSimpleRepost = post.repostOfId != null && post.originalPost != null && post.content.isBlank()
    val displayPost = if (isSimpleRepost) post.originalPost!! else post
    val quotedPost = displayPost.originalPost

    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(
                start = if (showThreadLine) (20 * depth.coerceAtMost(4)).dp else 0.dp,
                top = 4.dp,
                bottom = 4.dp
            )
    ) {
        if (showThreadLine) {
            PostSideBar(
                avatarUrl = displayPost.authorAvatarUrl,
                avatarColor = displayPost.authorAvatarColor,
                depth = depth,
                showLineAbove = showLineAbove,
                showLineBelow = showLineBelow,
                onProfileClick = { onProfileClick(displayPost.authorId) }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp) // Restore standard end padding
        ) {
            val contentPadding = if (showThreadLine) 0.dp else 12.dp

            // Repost Header (Only shown for simple reposts without custom caption)
            if (post.repostOfId != null && post.originalPost != null && post.content.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = contentPadding, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Repeat, 
                        null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${post.authorName} reposted",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 1. TOP ROW: PROFILE HEADER & FOLLOW BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, start = contentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!showThreadLine) {
                    val avatarUrl = remember(displayPost.authorAvatarUrl) {
                        com.tpsk.tj.common.util.MediaUtils.formatUrl(displayPost.authorAvatarUrl)
                    }
                    val avatarPainter = remember(displayPost.authorAvatarColor) {
                        androidx.compose.ui.graphics.painter.ColorPainter(displayPost.authorAvatarColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(displayPost.authorAvatarColor)
                            .clickable { onProfileClick(displayPost.authorId) }
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = avatarPainter
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                val ts = remember(displayPost.timestamp) { formatTwitterTime(displayPost.timestamp) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick(displayPost.authorId) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayPost.authorName.ifBlank { "User" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (displayPost.isVerifiedAuthor) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, "Verified", tint = TwitterBlue, modifier = Modifier.size(14.dp))
                        }
                        if (ts.isNotBlank()) {
                            Text(" · $ts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                    Text("@${displayPost.authorHandle.ifBlank { "user" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (!isOwnPost && !displayPost.isAuthorFollowed) {
                    Button(
                        onClick = onFollowClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Follow", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (displayPost.mood.isNotBlank() || displayPost.aiAssisted || displayPost.collaborators.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = contentPadding, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (displayPost.mood.isNotBlank()) {
                        Text(displayPost.mood, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (displayPost.aiAssisted) {
                        Text("Improved with AI", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    if (displayPost.collaborators.isNotEmpty()) {
                        Text("${displayPost.collaborators.size} collaborator${if (displayPost.collaborators.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            // 2. MIDDLE ROW: CAPTION
            if (displayPost.content.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = contentPadding, top = 4.dp, bottom = 4.dp)
                ) {
                    HashtagText(
                        text = displayPost.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 5
                    )
                    if (!isExpanded && displayPost.content.length > 200) {
                        Text("Show More", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).clickable { isExpanded = true } )
                    }
                }
            }

            // 3. MAIN MEDIA AREA
            if (displayPost.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(displayPost.mediaUrls[0]) && displayPost.mediaUrls.size == 1
                
                if (isVideo) {
                    val videoUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(displayPost.mediaUrls[0]) ?: ""
                    val thumbUrl = if (!displayPost.thumbnailUrl.isNullOrBlank()) {
                        com.tpsk.tj.common.util.MediaUtils.formatUrl(displayPost.thumbnailUrl)
                    } else videoUrl

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = contentPadding)
                            .heightIn(min = 280.dp, max = 540.dp)
                            .aspectRatio(displayPost.aspectRatio?.toFloat() ?: (4f / 3f))
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .clickable { onMediaClick(videoUrl, "video", currentPlaybackPosition) }
                    ) {
                        if (isVideoPlaying && autoplayEnabled) {
                            VideoPlayer(
                                videoUrl = videoUrl,
                                isPlaying = true,
                                modifier = Modifier.fillMaxSize(),
                                useController = false,
                                isMuted = isMuted,
                                autoplayEnabled = autoplayEnabled,
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                onPositionUpdate = { currentPlaybackPosition = it }
                            )
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "Mute", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val thumbRequest = remember(thumbUrl) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(thumbUrl)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = thumbRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                Icons.Default.PlayCircle,
                                null,
                                modifier = Modifier.size(48.dp).align(Alignment.Center),
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.padding(start = contentPadding)) {
                        MediaGrid(
                            urls = displayPost.mediaUrls,
                            autoplayEnabled = autoplayEnabled,
                            autoplayGifsEnabled = autoplayGifsEnabled
                        ) { url ->
                            val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(url) ?: url
                            val type = if (com.tpsk.tj.common.util.MediaUtils.isVideo(url)) "video" else "image"
                            onMediaClick(formattedUrl, type, 0L)
                        }
                    }
                }
            }

            // Polls & Quoted Posts (Padded)
            if (displayPost.poll != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(start = contentPadding)) {
                    PollView(displayPost.poll) { optionId -> onVotePoll(displayPost.id, optionId) }
                }
            }
            if (quotedPost != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(start = contentPadding)) {
                    QuotedPostCard(
                        post = quotedPost,
                        onProfileClick = onProfileClick,
                        onMediaClick = onMediaClick,
                        onPostClick = onQuotedPostClick
                    )
                }
            }

            // 4. BOTTOM ROW: INTERACTIONS (ADAPTIVE TEXT PILLS)
            val isDark = isSystemInDarkTheme()
            val barBgColor = if (isDark) Color(0xFF0B101B) else MaterialTheme.colorScheme.surface
            val pillBgColor = if (isDark) Color(0xFF1A2232) else Color(0xFFF3F4F6)
            val pillTextColor = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)

            val replyText = remember(displayPost.repliesCount) { "Reply ${formatCount(displayPost.repliesCount)}" }
            val repostText = remember(post.repostCount) { "Repost ${formatCount(post.repostCount)}" }
            val likeText = remember(post.likes) { "Like ${formatCount(post.likes)}" }
            val viewsText = remember(post.views) { "Views ${formatCount(post.views)}" }
            val saveText = remember(post.isBookmarked, post.quoteCount) {
                if (post.isBookmarked) "Saved ${formatCount(post.quoteCount)}" else "Save ${formatCount(post.quoteCount)}"
            }
            val shareText = remember(post.shares) { "Share ${formatCount(post.shares)}" }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = barBgColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = contentPadding)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PostPill(
                        text = replyText, 
                        onClick = onCommentClick,
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    PostPill(
                        text = repostText, 
                        onClick = onRepostClick,
                        isSelected = post.isReposted,
                        selectedColor = Color(0xFF00BA7C),
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    PostPill(
                        text = likeText, 
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick()
                        },
                        isSelected = post.isLiked,
                        selectedColor = Color(0xFFF91880),
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    PostPill(
                        text = viewsText, 
                        onClick = {},
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    PostPill(
                        text = saveText, 
                        onClick = onBookmarkClick,
                        isSelected = post.isBookmarked,
                        selectedColor = Color(0xFF1D9BF0),
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    PostPill(
                        text = shareText, 
                        onClick = onShareClick,
                        backgroundColor = pillBgColor,
                        textColor = pillTextColor
                    )
                    if (!isOwnPost) {
                        var showTip by remember(post.id) { mutableStateOf(false) }
                        Surface(
                            onClick = { showTip = true },
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            modifier = Modifier.background(
                                Brush.horizontalGradient(listOf(Color(0xFF8A2BE2).copy(alpha = 0.3f), Color(0xFF00E5FF).copy(alpha = 0.3f))),
                                RoundedCornerShape(50)
                            )
                        ) {
                            Text(
                                text = "Tip 💸",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                        if (showTip) {
                            com.tpsk.tj.wallet.ui.TipPostDialog(postId = post.id, onDismiss = { showTip = false })
                        }
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Long): String {
    val nonNegative = count.coerceAtLeast(0)
    return when {
        nonNegative >= 1_000_000 -> String.format(Locale.US, "%.1fM", nonNegative / 1_000_000f)
        nonNegative >= 1_000 -> String.format(Locale.US, "%.1fK", nonNegative / 1_000f)
        else -> nonNegative.toString()
    }
}

@Composable
private fun PostPill(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    selectedColor: Color = Color.Unspecified,
    backgroundColor: Color = Color(0xFF1A2232),
    textColor: Color = Color(0xFFD1D5DB)
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected && selectedColor != Color.Unspecified) selectedColor else textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
fun PostSideBar(
    avatarUrl: String?,
    avatarColor: Color,
    depth: Int = 0,
    showLineAbove: Boolean = false,
    showLineBelow: Boolean = false,
    onProfileClick: () -> Unit
) {
    val avatarSize = if (depth == 0) 42.dp else 32.dp
    val columnWidth = if (depth == 0) 64.dp else 48.dp
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .width(columnWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val strokeWidth = 2.dp.toPx()
            val avatarRadiusPx = avatarSize.toPx() / 2f
            val avatarCenterY = avatarRadiusPx

            if (showLineAbove) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, avatarCenterY - avatarRadiusPx - 2.dp.toPx()),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            if (showLineBelow) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, avatarCenterY + avatarRadiusPx + 4.dp.toPx()),
                    end = Offset(centerX, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(avatarColor)
                .clickable { onProfileClick() }
        ) {
            AsyncImage(
                model = com.tpsk.tj.common.util.MediaUtils.formatUrl(avatarUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(avatarColor),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(avatarColor)
            )
        }
    }
}

@Composable
fun FeedNativeAdCard(adManager: AdManager, adUnitId: String, post: Post) {
    Box(modifier = Modifier.fillMaxWidth()) {
        com.tpsk.tj.common.ui.components.DynamicAdCard(
            adManager = adManager,
            adUnitId = adUnitId.ifBlank { com.tpsk.tj.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID },
            slotKey = "post_ad_${post.id}",
            reserveSlot = true
        )
        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) {
            Text("Ad", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
