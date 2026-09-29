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
                            .heightIn(min = 280.dp, max = 520.dp)
                            .aspectRatio(4f / 3f)
                            .clip(shape)
                            .background(Color.DarkGray.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onMediaClick(urlString) },
                            contentScale = ContentScale.Crop,
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
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    urls.take(2).forEach { rawUrl ->
                        val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
                        AsyncImage(
                            model = formattedUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable { onMediaClick(formattedUrl) },
                            contentScale = ContentScale.Crop,
                            placeholder = placeholderPainter,
                            error = errorPainter
                        )
                    }
                }
            }
            3 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    val url0 = com.tpsk.tj.common.util.MediaUtils.formatUrl(urls[0]) ?: urls[0]
                    AsyncImage(
                        model = url0,
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray.copy(alpha = 0.2f))
                            .clickable { onMediaClick(url0) },
                        contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter,
                        error = errorPainter
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        urls.subList(1, 3).forEach { rawUrl ->
                            val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
                            AsyncImage(
                                model = formattedUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.2f))
                                    .clickable { onMediaClick(formattedUrl) },
                                contentScale = ContentScale.Crop,
                                placeholder = placeholderPainter,
                                error = errorPainter
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
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 0..1) {
                            val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(urls[i]) ?: urls[i]
                            AsyncImage(
                                model = formattedUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.2f))
                                    .clickable { onMediaClick(formattedUrl) },
                                contentScale = ContentScale.Crop,
                                placeholder = placeholderPainter,
                                error = errorPainter
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        val url2 = com.tpsk.tj.common.util.MediaUtils.formatUrl(urls[2]) ?: urls[2]
                        AsyncImage(
                            model = url2,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable { onMediaClick(url2) },
                            contentScale = ContentScale.Crop,
                            placeholder = placeholderPainter,
                            error = errorPainter
                        )
                        val url3 = com.tpsk.tj.common.util.MediaUtils.formatUrl(urls[3]) ?: urls[3]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                                .clickable { onMediaClick(url3) },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = url3,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = placeholderPainter,
                                error = errorPainter
                            )
                            if (extraCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$extraCount",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
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
    onPostClick: (Post) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPostClick(post) }
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = com.tpsk.tj.common.util.MediaUtils.formatUrl(post.authorAvatarUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(post.authorAvatarColor),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " @${post.authorHandle} · ${formatTwitterTime(post.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (post.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
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
