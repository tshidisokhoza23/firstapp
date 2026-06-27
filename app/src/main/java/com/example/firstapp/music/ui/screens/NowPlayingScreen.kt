package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.music.ui.components.SongListItem
import com.example.firstapp.common.ui.theme.AppAccent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    onAddSongToPlaylist: (Int) -> Unit = {}
) {
    var currentProgress by remember { mutableStateOf(progress) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        currentProgress = progress
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background Artwork Blur Effect
        Image(
            painter = rememberAsyncImagePainter(song.albumArtUrl),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )
        
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ${song.category.uppercase()}",
                        color = Color.White.copy(0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(song.albumName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showPlaylistPickerDialog = true }) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = Color.White)
                }
                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }

            if (showPlaylistPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showPlaylistPickerDialog = false },
                    title = { Text("Add to Playlist") },
                    text = {
                        LazyColumn {
                            itemsIndexed(playlists) { _, playlist ->
                                TextButton(
                                    onClick = {
                                        onAddSongToPlaylist(playlist.id)
                                        showPlaylistPickerDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(playlist.name, color = Color.White)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlaylistPickerDialog = false }) { Text("Cancel") }
                    },
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            }

            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = { Text("Sleep Timer") },
                    text = {
                        Column {
                            val times = listOf(0, 15, 30, 45, 60)
                            times.forEach { mins ->
                                TextButton(
                                    onClick = {
                                        onSetSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (mins == 0) "Off" else "$mins minutes", color = Color.White)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            }

            if (sleepTimerRemaining != null) {
                Text(
                    text = "Sleep Timer: ${formatRemainingTime(sleepTimerRemaining)}",
                    color = AppAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (showQueue) {
                // Queue View
                Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Up Next", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showQueue = false }) {
                            Text("Show Player", color = AppAccent)
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(queue) { _, queueSong ->
                            SongListItem(
                                song = queueSong,
                                isPlaying = queueSong.id == song.id,
                                onClick = { onPlayFromQueue(queueSong) },
                                onArtistClick = { artistName -> 
                                    showQueue = false
                                    onClose()
                                    onArtistClick(artistName)
                                }
                            )
                        }
                    }
                }
            } else if (showLyrics) {
                // Lyrics View
                Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lyrics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showLyrics = false }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(
                            text = song.lyrics ?: "Lyrics not available for this track.",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(bottom = 48.dp)
                        )
                    }
                }
            } else {
                // Main Player View
                Spacer(modifier = Modifier.weight(1f))
                
                Surface(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    color = song.albumArtColor,
                    shadowElevation = 20.dp
                ) {
                    if (!song.albumArtUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(song.albumArtUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(100.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(0.8f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            song.artist, 
                            color = Color.White.copy(0.7f), 
                            fontSize = 20.sp, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onArtistClick(song.artist) }
                        )
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    Slider(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        onValueChangeFinished = { onSeek(currentProgress) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(currentProgress, song.duration), color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Text(song.duration, color = Color.White.copy(0.6f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(Icons.Default.Shuffle, null, tint = if (isShuffleEnabled) AppAccent else Color.White)
                    }
                    IconButton(onClick = onSeekBackward) {
                        Icon(Icons.Default.Replay10, null, tint = Color.White)
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Surface(
                        onClick = onTogglePlay,
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = onSeekForward) {
                        Icon(Icons.Default.Forward10, null, tint = Color.White)
                    }
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (repeatMode != 0) AppAccent else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { showLyrics = true }) {
                        Icon(Icons.Default.Mic, null, tint = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = { /* Bluetooth/Cast */ }) {
                        Icon(Icons.Default.Cast, null, tint = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White.copy(0.7f))
                    }
                }
            }
        }
    }
}

private fun formatDuration(progress: Float, totalDuration: String): String {
    try {
        val parts = totalDuration.split(":")
        val totalSeconds = if (parts.size == 2) {
            parts[0].toInt() * 60 + parts[1].toInt()
        } else 0
        
        val currentSeconds = (progress * totalSeconds).toInt()
        return String.format(Locale.getDefault(), "%d:%02d", currentSeconds / 60, currentSeconds % 60)
    } catch (_: Exception) {
        return "0:00"
    }
}

private fun formatRemainingTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
