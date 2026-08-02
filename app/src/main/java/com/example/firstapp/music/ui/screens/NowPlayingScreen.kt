@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.firstapp.music.ui.screens

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
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.music.ui.components.*

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
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        currentProgress = progress
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, 
                                null, 
                                tint = MaterialTheme.colorScheme.onBackground, 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSleepTimer = true }) {
                            Icon(
                                Icons.Default.Timer, 
                                null, 
                                tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd, 
                                null, 
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.MoreVert, 
                                null, 
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(song.albumArtColor)
                ) {
                    if (!song.albumArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = song.albumArtUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(100.dp).align(Alignment.Center)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            modifier = Modifier.clickable { onArtistClick(song.artist) }
                        )
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = currentProgress,
                    onValueChange = { currentProgress = it },
                    onValueChangeFinished = { onSeek(currentProgress) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0:00", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(song.duration, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle, 
                            null, 
                            tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevious) {
                            Icon(
                                Icons.Filled.SkipPrevious, 
                                null, 
                                tint = MaterialTheme.colorScheme.onBackground, 
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        IconButton(onClick = onNext) {
                            Icon(
                                Icons.Filled.SkipNext, 
                                null, 
                                tint = MaterialTheme.colorScheme.onBackground, 
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = when (repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                androidx.media3.common.Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = null,
                            tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                title = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    if (playlists.isEmpty()) {
                        Text("No playlists created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn {
                            items(playlists) { playlist ->
                                TextButton(
                                    onClick = {
                                        onAddSongToPlaylist(playlist.id)
                                        showPlaylistDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(playlist.name, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistDialog = false }) { 
                        Text("Cancel", color = MaterialTheme.colorScheme.primary) 
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
