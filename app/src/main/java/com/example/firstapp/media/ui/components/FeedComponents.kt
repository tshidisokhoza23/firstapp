package com.example.firstapp.media.ui.components

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
import coil.compose.AsyncImage
import com.example.firstapp.media.model.PostPoll
import kotlin.text.*

@Composable
fun InteractionIcon(
    icon: ImageVector,
    count: Long,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = count.toString(), color = color, fontSize = 12.sp)
    }
}

@Composable
fun MediaGrid(urls: List<String>, onMediaClick: (String) -> Unit = {}) {
    val shape = RoundedCornerShape(12.dp)
    val modifier = if (urls.size == 1) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 500.dp)
            .clip(shape)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), shape)
            .background(Color.Black)
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.77f)
            .clip(shape)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), shape)
            .background(Color.Black.copy(alpha = 0.2f))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            urls.isEmpty() -> {}
            urls.size == 1 -> {
                val rawUrl = urls[0]
                val urlString = com.example.firstapp.common.util.MediaUtils.formatUrl(rawUrl) ?: rawUrl
                val isVideo = com.example.firstapp.common.util.MediaUtils.isVideo(rawUrl)
                if (isVideo) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        VideoPlayer(
                            videoUrl = urlString,
                            isPlaying = true, // Auto-play single videos in grid
                            modifier = Modifier.fillMaxWidth(),
                            useController = false
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).align(Alignment.Center),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        // Overlay click area
                        Box(modifier = Modifier.matchParentSize().clickable { onMediaClick(urlString) })
                    }
                } else {
                    AsyncImage(
                        model = urlString,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMediaClick(urlString) },
                        contentScale = ContentScale.Fit
                    )
                }
            }
            urls.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[0]),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onMediaClick(urls[0]) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(1.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[1]),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onMediaClick(urls[1]) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            urls.size == 3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[0]),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onMediaClick(urls[0]) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(1.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[1]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[1]) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[2]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[2]) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            else -> { // 4 or more
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[0]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[0]) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(1.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[1]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[1]) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[2]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[2]) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(1.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = com.example.firstapp.common.util.MediaUtils.formatUrl(urls[3]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onMediaClick(urls[3]) },
                                contentScale = ContentScale.Crop
                            )
                            if (urls.size > 4) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+${urls.size - 4}",
                                        color = Color.White,
                                        fontSize = 20.sp,
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
