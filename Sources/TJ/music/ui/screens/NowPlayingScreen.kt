@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.music.model.Playlist
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.music.ui.components.*

@Composable
fun NowPlayingScreen(
    song: Song,
    queue: List<Song>,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    progress: Float,
    isLiked: Boolean,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    onPlayFromQueue: (Song) -> Unit,
    onSeekForward: () -> Unit = {},
    onSeekBackward: () -> Unit = {},
    onSetSleepTimer: (Int) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    sleepTimerRemaining: Long? = null,
    playlists: List<Playlist> = emptyList(),
    onAddSongToPlaylist: (String) -> Unit = {}
) {
    var currentProgress by remember { mutableStateOf(progress) }
    var showQueue by remember { mutableStateOf(false) }
    var isLyricsVisible by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        currentProgress = progress
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            song.albumArtColor.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "PLAYING FROM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                song.albumName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(32.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* More actions */ }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                // Artwork / Lyrics Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { isLyricsVisible = !isLyricsVisible }
                ) {
                    if (isLyricsVisible && song.lyrics != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(24.dp)
                        ) {
                            Text(
                                song.lyrics ?: "No lyrics available",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Info & Like
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onArtistClick(song.artist) }
                        )
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Progress
                Column {
                    Slider(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        onValueChangeFinished = { onSeek(currentProgress) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSeconds = (currentProgress * (song.durationMs / 1000)).toInt()
                        Text(
                            formatTime(currentSeconds.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            song.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle,
                            null,
                            tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(48.dp))
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(48.dp))
                        }
                    }

                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = when (repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = null,
                            tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null)
                    }
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(Icons.Default.Timer, null, tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { showPlaylistDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null)
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            }
        }

        // Bottom Sheets for Queue, Sleep Timer, etc. would go here
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
