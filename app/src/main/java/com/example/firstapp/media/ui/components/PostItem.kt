package com.example.firstapp.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatTwitterTime
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.Song
import kotlin.collections.*

@Composable
fun PostItem(
    adManager: AdManager,
    post: Post,
    isOwnPost: Boolean = false,
    showThreadLine: Boolean = false,
    isLastInThread: Boolean = false,
    onClick: () -> Unit,
    onLike: (String, Long, Boolean) -> Unit,
    onRepostClick: (Post) -> Unit,
    onBookmark: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReplyClick: (Post, Post?) -> Unit,
    onProfileClick: (User) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    if (post.type == PostType.EXTERNAL_AD_PLACEHOLDER || post.type == PostType.AD_UNIT) {
        // Native Ad Component using shared modular parts
        NativeAdCard(
            adManager = adManager,
            adUnitId = "ca-app-pub-3940256099942544/2247696110",
            post = post
        )
    } else {
        val displayPost = if (post.repostOfId != null && post.originalPost != null) post.originalPost!! else post
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp)
            ) {
                // Vertical Thread Line Column
                PostSideBar(
                    avatarUrl = post.authorAvatarUrl,
                    avatarColor = post.authorAvatarColor,
                    showThreadLine = showThreadLine,
                    isLastInThread = isLastInThread,
                    onProfileClick = { onProfileClick(post.authorToUser()) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Shared Header Logic (Repost/Promoted)
                    PostHeaderExtras(post, isOwnPost)

                    // Main Header (Name, Handle, Time)
                    PostMainHeader(
                        displayPost = displayPost,
                        isOwnPost = isOwnPost,
                        onProfileClick = onProfileClick,
                        onDelete = { onDelete(post.id) }
                    )

                    // Content
                    Text(
                        text = displayPost.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 20.sp
                    )

                    // Media
                    if (displayPost.mediaUrls.size > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isVideo = displayPost.mediaUrls.any { it.lowercase().run { endsWith(".mp4") || endsWith(".mov") } }
                        if (isVideo && displayPost.mediaUrls.size == 1) {
                            Box(modifier = Modifier.clickable { 
                                val url = displayPost.mediaUrls[0]
                                onMediaClick(url, "video")
                            }) {
                                VideoPlayer(
                                    videoUrl = displayPost.mediaUrls[0],
                                    isPlaying = false,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(displayPost.aspectRatio?.toFloat() ?: 1.77f).clip(RoundedCornerShape(12.dp)),
                                    useController = false
                                )
                            }
                        } else {
                            MediaGrid(displayPost.mediaUrls) { url ->
                                val type = if (url.lowercase(java.util.Locale.ROOT).endsWith(".mp4") || url.lowercase(java.util.Locale.ROOT).endsWith(".mov")) "video" else "image"
                                onMediaClick(url, type)
                            }
                        }
                    }

                    // Polls
                    if (displayPost.poll != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PollView(displayPost.poll) { optionId ->
                            onVotePoll(displayPost.id, optionId)
                        }
                    }

                    // Action Buttons
                    PostActions(
                        post = post,
                        onLike = onLike,
                        onReplyClick = { onReplyClick(post, null) },
                        onRepostClick = { onRepostClick(post) },
                        onBookmark = { onBookmark(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PostSideBar(
    avatarUrl: String?,
    avatarColor: Color,
    showThreadLine: Boolean,
    isLastInThread: Boolean,
    onProfileClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .clickable { onProfileClick() }
        ) {
            AsyncImage(
                model = com.example.firstapp.common.util.MediaUtils.formatUrl(avatarUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(avatarColor),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(avatarColor)
            )
        }

        if (showThreadLine && !isLastInThread) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .padding(top = 4.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )
        }
    }
}

@Composable
fun PostHeaderExtras(post: Post, isOwnPost: Boolean) {
    if (post.type == PostType.POST && post.repostOfId != null && post.originalPost != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(Icons.Default.Repeat, null, tint = SecondaryText, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOwnPost) "You reposted" else "${post.authorName} reposted",
                color = SecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (post.type == PostType.INTERNAL_AD || post.type == PostType.NATIVE_AD) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Icon(Icons.Default.TrendingUp, null, tint = AppAccent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Promoted", color = AppAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun PostMainHeader(
    displayPost: Post,
    isOwnPost: Boolean,
    onProfileClick: (User) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = displayPost.authorName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable { onProfileClick(displayPost.authorToUser()) }
        )
        if (displayPost.isVerifiedAuthor) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Verified, null, tint = TwitterBlue, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "@${displayPost.authorHandle} · ${formatTwitterTime(displayPost.timestamp)}",
            color = SecondaryText,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onProfileClick(displayPost.authorToUser()) }
        )

        if (isOwnPost) {
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = AppSecondary
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Post", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PostActions(
    post: Post,
    onLike: (String, Long, Boolean) -> Unit,
    onReplyClick: () -> Unit,
    onRepostClick: () -> Unit,
    onBookmark: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InteractionIcon(
            icon = Icons.Default.ChatBubbleOutline,
            count = post.repliesCount,
            color = SecondaryText,
            onClick = onReplyClick
        )
        InteractionIcon(
            icon = Icons.Default.Repeat,
            count = post.shares,
            color = if (post.isReposted) Color.Green else SecondaryText,
            onClick = onRepostClick
        )
        InteractionIcon(
            icon = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            count = post.likes,
            color = if (post.isLiked) Color.Red else SecondaryText,
            onClick = { onLike(post.id, post.likes, post.isLiked) }
        )
        InteractionIcon(
            icon = if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            count = 0,
            color = if (post.isBookmarked) TwitterBlue else SecondaryText,
            onClick = onBookmark
        )
        IconButton(onClick = { /* Share */ }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Share, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun NativeAdCard(
    adManager: AdManager,
    adUnitId: String,
    post: Post
) {
    // This uses a custom component that bridges AdMob Native with Compose
    // but visually matches the standard post layout.
    Box(modifier = Modifier.fillMaxWidth()) {
        com.example.firstapp.common.ui.components.DynamicAdCard(
            adManager = adManager,
            adUnitId = adUnitId
        )
        // Mandatory Ad Badge in top-right corner
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "Ad",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// Extension functions for mapping
fun Post.authorToUser(): User = User(
    id = authorId,
    username = authorHandle,
    displayName = authorName,
    avatarUrl = authorAvatarUrl
)
