package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        if (urls.size == 1) {
            val rawUrl = urls[0]
            val urlString = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
            val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(rawUrl)

            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 500.dp)
                        .clip(shape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayer(
                        videoUrl = urlString,
                        isPlaying = true,
                        modifier = Modifier.fillMaxWidth(),
                        useController = false,
                        autoplayEnabled = autoplayEnabled,
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { onMediaClick(urlString) })
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(urlString)
                        .crossfade(true)
                        .apply {
                            val isGif = urlString.lowercase().endsWith(".gif")
                            if (isGif && !autoplayGifsEnabled) {
                                setParameter("play_animated", false)
                            }
                        }
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 500.dp)
                        .clip(shape)
                        .background(Color.DarkGray.copy(alpha = 0.2f))
                        .clickable { onMediaClick(urlString) },
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray.copy(alpha = 0.2f)),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray.copy(alpha = 0.4f))
                )
            }
        } else {
            // Show ALL images in rows of 2 without any +N indicator or truncation
            val chunked = urls.chunked(2)
            chunked.forEachIndexed { rowIndex, rowUrls ->
                if (rowIndex > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    rowUrls.forEachIndexed { colIndex, rawUrl ->
                        if (colIndex > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val formattedUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.2f))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(formattedUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(formattedUrl) },
                                contentScale = ContentScale.Crop,
                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray.copy(alpha = 0.2f)),
                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray.copy(alpha = 0.4f))
                            )
                        }
                    }
                    if (rowUrls.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            if (post.mediaUrls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val url = post.mediaUrls[0]
                    val isVideo = com.tpsk.tj.common.util.MediaUtils.isVideo(url)
                    val displayUrl = if (isVideo && post.thumbnailUrl != null) post.thumbnailUrl else url
                    
                    AsyncImage(
                        model = com.tpsk.tj.common.util.MediaUtils.formatUrl(displayUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isVideo) {
                        Icon(
                            Icons.Default.PlayCircle,
                            null,
                            modifier = Modifier.align(Alignment.Center).size(32.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
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
