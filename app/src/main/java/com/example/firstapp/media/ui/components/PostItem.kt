package com.example.firstapp.media.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import com.example.firstapp.media.ui.components.VideoPlayer
import com.example.firstapp.media.ui.components.MediaGrid
import com.example.firstapp.media.ui.components.PollView
import java.util.Locale
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
    onBookmark: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onReplyClick: (Post, Post?) -> Unit,
    onProfileClick: (User) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onMediaClick: (String, String) -> Unit = { _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onFollowClick: (String) -> Unit = {}
) {
    if (post.type == PostType.EXTERNAL_AD_PLACEHOLDER || post.type == PostType.AD_UNIT) {
        NativeAdCard(
            adManager = adManager,
            adUnitId = "ca-app-pub-3940256099942544/2247696110",
            post = post
        )
    } else {
        VerticalFeedPostCard(
            post = post,
            isOwnPost = isOwnPost,
            onLikeClick = { onLike(post.id, post.likes, post.isLiked) },
            onCommentClick = { onReplyClick(post, null) },
            onRepostClick = { onRepostClick(post) },
            onShareClick = { onShareClick(post) },
            onFollowClick = { onFollowClick(post.authorId) },
            onMoreClick = { /* Handle more menu */ },
            onProfileClick = { onProfileClick(post.authorToUser()) },
            onMediaClick = onMediaClick,
            onVotePoll = onVotePoll,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
fun VerticalFeedPostCard(
    post: Post,
    isOwnPost: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMoreClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMediaClick: (String, String) -> Unit,
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppBackground)
            .padding(vertical = 4.dp)
    ) {
        // Repost Header (Optional)
        if (post.repostOfId != null && post.originalPost != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Repeat, null, tint = AppTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${post.authorName} reposted",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        val displayPost = if (post.repostOfId != null && post.originalPost != null) post.originalPost!! else post

        // 1. TOP ROW: PROFILE HEADER & FOLLOW BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(displayPost.authorAvatarColor)
                    .clickable { onProfileClick() }
            ) {
                AsyncImage(
                    model = com.example.firstapp.common.util.MediaUtils.formatUrl(displayPost.authorAvatarUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.painter.ColorPainter(displayPost.authorAvatarColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayPost.authorName,
                    color = AppTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${displayPost.authorHandle}",
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Follow Button
            if (!isOwnPost) {
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (post.isAuthorFollowed) Color.Transparent else AppTextPrimary,
                        contentColor = if (post.isAuthorFollowed) AppTextPrimary else AppBackground
                    ),
                    border = if (post.isAuthorFollowed) BorderStroke(1.dp, AppTextSecondary) else null,
                    shape = RoundedCornerShape(16.dp),
                    elevation = null
                ) {
                    Text(
                        text = if (post.isAuthorFollowed) "Following" else "Follow",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }
        }

        // 2. MIDDLE ROW: EXPANDABLE CAPTION TEXT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = displayPost.content,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!isExpanded && displayPost.content.length > 200) {
                Text(
                    text = "Show More",
                    color = AppAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp).clickable { isExpanded = true }
                )
            }
        }

        // 3. MAIN MEDIA AREA
        if (displayPost.mediaUrls.size > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val mType = displayPost.mediaType ?: ""
            var hasVideoInUrls = false
            for (u in displayPost.mediaUrls) {
                if (com.example.firstapp.common.util.MediaUtils.isVideo(u)) {
                    hasVideoInUrls = true
                    break
                }
            }
            val isVideo = (mType == "video") || (mType == "VIDEO") || (mType == "short") || (mType == "SHORT") || hasVideoInUrls
            
            if (isVideo && displayPost.mediaUrls.size == 1) {
                val videoUrl = com.example.firstapp.common.util.MediaUtils.formatUrl(displayPost.mediaUrls[0]) ?: ""
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(displayPost.aspectRatio?.toFloat() ?: 0.8f)
                        .background(Color.Black.copy(alpha = 0.1f))
                        .clickable { onMediaClick(videoUrl, "video") }
                ) {
                    VideoPlayer(
                        videoUrl = videoUrl,
                        isPlaying = true, // Auto-play single videos in feed
                        modifier = Modifier.fillMaxSize(),
                        useController = false
                    )
                    Icon(
                        Icons.Default.PlayCircle,
                        null,
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            } else {
                MediaGrid(urls = displayPost.mediaUrls) { url ->
                    val formattedUrl = com.example.firstapp.common.util.MediaUtils.formatUrl(url) ?: url
                    val type = if (com.example.firstapp.common.util.MediaUtils.isVideo(url)) "video" else "image"
                    onMediaClick(formattedUrl, type)
                }
            }
        }

        // Poll Support
        if (displayPost.poll != null) {
            Spacer(modifier = Modifier.height(8.dp))
            PollView(displayPost.poll) { optionId ->
                onVotePoll(displayPost.id, optionId)
            }
        }

        // 4. BOTTOM ROW: INTERACTION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InteractionIcon(
                icon = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = post.likes,
                color = if (post.isLiked) Color.Red else Color.Gray,
                onClick = onLikeClick
            )
            
            InteractionIcon(
                icon = Icons.Default.Repeat,
                count = post.shares,
                color = if (post.isReposted) Color.Green else Color.Gray,
                onClick = onRepostClick
            )
            
            InteractionIcon(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = displayPost.repliesCount,
                color = Color.Gray,
                onClick = onCommentClick
            )
            
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
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
    onBookmark: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InteractionIcon(
            icon = Icons.Outlined.ChatBubbleOutline,
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
        IconButton(onClick = onShareClick, modifier = Modifier.size(20.dp)) {
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
    Box(modifier = Modifier.fillMaxWidth()) {
        com.example.firstapp.common.ui.components.DynamicAdCard(
            adManager = adManager,
            adUnitId = adUnitId
        )
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

fun Post.authorToUser(): User = User(
    id = authorId,
    username = authorHandle,
    displayName = authorName,
    avatarUrl = authorAvatarUrl
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
fun VerticalFeedPostCardPreview() {
    val samplePost = Post(
        id = "1",
        authorName = "LatteArtPro",
        authorHandle = "barista_bex",
        authorAvatarColor = Color.Magenta,
        content = "Just perfected my classic rosette! It was easier than I thought, so satisfying! #coffee #art #rosette #barista",
        mediaUrls = listOf("https://images.unsplash.com/photo-1541167760496-1628856ab772"),
        likes = 1250,
        repliesCount = 45,
        shares = 88,
        isLiked = true,
        isAuthorFollowed = false,
        timestamp = "2023-10-27T10:00:00Z"
    )
    
    VerticalFeedPostCard(
        post = samplePost,
        onLikeClick = {},
        onCommentClick = {},
        onRepostClick = {},
        onShareClick = {},
        onFollowClick = {},
        onMoreClick = {},
        onProfileClick = {},
        onMediaClick = { _, _ -> }
    )
}
