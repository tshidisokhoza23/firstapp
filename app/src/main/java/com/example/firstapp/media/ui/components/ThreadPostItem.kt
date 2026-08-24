package com.example.firstapp.media.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.media.model.Post
import com.example.firstapp.media.model.FlattenedThreadItem
import com.example.firstapp.common.ui.theme.TwitterBlue
import com.example.firstapp.common.util.formatTwitterTime
import java.util.Locale

private val RootAvatarSize = 42.dp
private val NestedAvatarSize = 32.dp
private val AvatarColumnWidth = 56.dp
private val IndentPerLevel = 24.dp
private const val MAX_VISIBLE_DEPTH = 4

@Composable
fun ThreadPostItem(
    item: FlattenedThreadItem,
    isRoot: Boolean,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val post = item.post
    val depth = item.depth.coerceAtMost(MAX_VISIBLE_DEPTH)
    val avatarSize = if (depth == 0) RootAvatarSize else NestedAvatarSize
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp + (IndentPerLevel * depth),
                end = 12.dp,
                top = if (isRoot) 12.dp else 4.dp,
                bottom = 4.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${post.authorName}: ${post.content}. ${post.likes} likes."
                }
        ) {
            // Left Column: Avatar + Exact-Pixel Line Alignment
            Box(
                modifier = Modifier
                    .width(AvatarColumnWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val strokeWidthPx = with(density) { 2.dp.toPx() }
                    val avatarRadiusPx = with(density) { avatarSize.toPx() / 2f }
                    val avatarTopMarginPx = 0f
                    val avatarCenterY = avatarTopMarginPx + avatarRadiusPx

                    // Line segment above avatar (connects from parent)
                    if (item.hasVerticalLineAbove) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, avatarTopMarginPx - with(density) { 2.dp.toPx() }),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    }

                    // Line segment below avatar (runs full height of item)
                    if (item.hasVerticalLineBelow) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, avatarCenterY + avatarRadiusPx + with(density) { 4.dp.toPx() }),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Square
                        )
                    }
                }

                AsyncImage(
                    model = com.example.firstapp.common.util.MediaUtils.formatUrl(post.authorAvatarUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(post.authorAvatarColor)
                        .clickable { onProfileClick(post.authorId) },
                    contentScale = ContentScale.Crop,
                    fallback = androidx.compose.ui.graphics.painter.ColorPainter(post.authorAvatarColor),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(post.authorAvatarColor)
                )
            }

            // Right Column: Post Body and Action Bar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, bottom = 8.dp)
            ) {
                // User details
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.isVerifiedAuthor) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = TwitterBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "· ${formatTwitterTime(post.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.authorHandle.isNotBlank()) {
                        Text(
                            text = "@${post.authorHandle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (post.mediaUrls.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    MediaGrid(urls = post.mediaUrls) { url ->
                        val type = if (com.example.firstapp.common.util.MediaUtils.isVideo(url)) "video" else "image"
                        onMediaClick(url, type, 0L)
                    }
                }

                Spacer(Modifier.height(8.dp))

                ThreadActionBar(
                    likeCount = post.likes,
                    replyCount = post.repliesCount,
                    isLiked = post.isLiked,
                    onLikeClick = onLikeClick,
                    onReplyClick = onReplyClick,
                    onRepostClick = onRepostClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

@Composable
private fun ThreadActionBar(
    likeCount: Long,
    replyCount: Long,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onRepostClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            count = likeCount,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) Color(0xFFF91880) else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onLikeClick
        )
        ActionButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = replyCount,
            contentDescription = "Reply",
            onClick = onReplyClick
        )
        ActionButton(
            icon = Icons.Outlined.Repeat,
            contentDescription = "Repost",
            onClick = onRepostClick
        )
        ActionButton(
            icon = Icons.Outlined.Share,
            contentDescription = "Share",
            onClick = onShareClick
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Long = 0,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000 -> "%.1fK".format(count / 1_000f)
    else -> count.toString()
}
