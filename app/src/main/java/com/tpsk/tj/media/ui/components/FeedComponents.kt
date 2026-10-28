package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Verified
import com.tpsk.tj.common.ui.theme.TwitterBlue
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.model.PostPoll
import com.tpsk.tj.common.util.formatTwitterTime
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.request.ImageRequest
import kotlin.text.*

@Composable
fun InteractionIcon(
    icon: ImageVector,
    count: Long,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        if (showCount) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = count.toString(), color = color, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FeedMediaTile(
    rawUrl: String,
    modifier: Modifier,
    placeholderPainter: androidx.compose.ui.graphics.painter.Painter,
    errorPainter: androidx.compose.ui.graphics.painter.Painter,
    onMediaClick: (String) -> Unit,
    badge: String? = null
) {
    val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
    val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(rawUrl)
    val lower = formattedUrl.lowercase()
    val isAudio = !isVideo && (lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".ogg"))
    Box(
        modifier = modifier.clickable { onMediaClick(formattedUrl) },
        contentAlignment = Alignment.Center
    ) {
        if (isAudio) {
            Icon(Icons.Default.PlayCircle, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(36.dp))
        } else {
            AsyncImage(
                model = formattedUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = errorPainter
            )
            if (isVideo) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaGrid(
    urls: List<String>, 
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    onMediaClick: (String) -> Unit = {}
) {
    if (urls.isEmpty()) return

    val shape = RoundedCornerShape(12.dp)
    val context = LocalContext.current
    val spacing = 4.dp

    val placeholderPainter = remember { androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray.copy(alpha = 0.2f)) }
    val errorPainter = remember { androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray.copy(alpha = 0.4f)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        when (urls.size) {
            1 -> {
                val rawUrl = urls[0]
                val urlString = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
                val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(rawUrl)

                if (isVideo) {
                    val imageRequest = remember(urlString) {
                        ImageRequest.Builder(context)
                            .data(urlString)
                            .crossfade(true)
                            .build()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 540.dp)
                            .clip(shape)
                            .background(Color.Black)
                            .clickable { onMediaClick(urlString) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = placeholderPainter,
                            error = errorPainter
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).align(Alignment.Center),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    val isGif = urlString.lowercase().endsWith(".gif")
                    val imageRequest = remember(urlString, autoplayGifsEnabled) {
                        ImageRequest.Builder(context)
                            .data(urlString)
                            .crossfade(true)
                            .apply {
                                if (isGif && !autoplayGifsEnabled) {
                                    setParameter("play_animated", false)
                                }
                            }
                            .build()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 520.dp)
                            .clip(shape)
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 520.dp)
                                .clickable { onMediaClick(urlString) },
                            contentScale = ContentScale.Fit,
                            placeholder = placeholderPainter,
                            error = errorPainter
                        )
                    }
                }
            }
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    urls.take(2).forEach { rawUrl ->
                        FeedMediaTile(
                            rawUrl = rawUrl,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f)),
                            placeholderPainter = placeholderPainter,
                            errorPainter = errorPainter,
                            onMediaClick = onMediaClick
                        )
                    }
                }
            }
            3 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    FeedMediaTile(
                        rawUrl = urls[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray.copy(alpha = 0.2f)),
                        placeholderPainter = placeholderPainter,
                        errorPainter = errorPainter,
                        onMediaClick = onMediaClick
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        urls.subList(1, 3).forEach { rawUrl ->
                            FeedMediaTile(
                                rawUrl = rawUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.2f)),
                                placeholderPainter = placeholderPainter,
                                errorPainter = errorPainter,
                                onMediaClick = onMediaClick
                            )
                        }
                    }
                }
            }
            else -> {
                // 4 or more images: 2x2 grid preview layout (maximum 4 images displayed) with +N badge for >4 images
                val extraCount = urls.size - 4
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 0..1) {
                            FeedMediaTile(
                                rawUrl = urls[i],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.2f)),
                                placeholderPainter = placeholderPainter,
                                errorPainter = errorPainter,
                                onMediaClick = onMediaClick
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        FeedMediaTile(
                            rawUrl = urls[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f)),
                            placeholderPainter = placeholderPainter,
                            errorPainter = errorPainter,
                            onMediaClick = onMediaClick
                        )
                        FeedMediaTile(
                            rawUrl = urls[3],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f)),
                            placeholderPainter = placeholderPainter,
                            errorPainter = errorPainter,
                            onMediaClick = onMediaClick,
                            badge = if (extraCount > 0) "+$extraCount" else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuotedPostCard(
    post: Post,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    onPostClick: (Post) -> Unit = {},
    onHashtagClick: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPostClick(post) }
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val avatarUrl = remember(post.authorAvatarUrl) {
                    com.tpsk.tj.common.util.MediaUtils.formatUrl(post.authorAvatarUrl)
                }
                val avatarPainter = remember(post.authorAvatarColor) {
                    androidx.compose.ui.graphics.painter.ColorPainter(post.authorAvatarColor)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(post.authorAvatarColor)
                        .clickable { onProfileClick(post.authorId.ifBlank { post.authorHandle }) }
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = avatarPainter
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = post.authorName.ifBlank { "User" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.isVerifiedAuthor) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = TwitterBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = " @${post.authorHandle.ifBlank { "user" }} · ${formatTwitterTime(post.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                HashtagText(
                    text = post.content,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 6,
                    onHashtagClick = onHashtagClick
                )
            }

            if (post.mediaUrls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                MediaGrid(
                    urls = post.mediaUrls,
                    autoplayEnabled = false,
                    autoplayGifsEnabled = false,
                    onMediaClick = { mediaUrl ->
                        val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(mediaUrl)
                        onMediaClick(mediaUrl, if (isVideo) "video" else "image", 0L)
                    }
                )
            }

            if (post.poll != null) {
                Spacer(Modifier.height(8.dp))
                PollView(poll = post.poll, onVote = { _ -> })
            }
        }
    }
}
@Composable
fun PollView(poll: PostPoll, onVote: (String) -> Unit) {
    var totalVotes = 0L
    poll.options.forEach { totalVotes += it.voteCount }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        poll.options.forEach { option ->
            val percentage = if (totalVotes > 0) (option.voteCount.toFloat() / totalVotes) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .clickable { onVote(option.id) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percentage)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("${(percentage * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }
}
