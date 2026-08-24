package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.ui.components.*
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import androidx.compose.runtime.collectAsState
import com.example.firstapp.LocalCurrentUser
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun RecommendationsScreen(
    songs: List<Song>,
    viewModel: MusicViewModel,
    currentlyPlaying: Song? = null,
    isPlaying: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onPlay: (Song, List<Song>) -> Unit,
    onShare: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {},
    onArtistClick: (String) -> Unit = {}
) {
    val recentlyPlayedState by viewModel.recentlyPlayed.collectAsState()
    val currentUser = LocalCurrentUser.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp + bottomPadding)
    ) {
        // Greeting
        item {
            val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 0..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
            }
            Text(
                text = "$greeting, ${currentUser.displayName.split(" ").firstOrNull() ?: ""}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Recently Played / Jump Back In
        val history = recentlyPlayedState.data ?: emptyList()
        if (history.isNotEmpty()) {
            item { MusicSectionHeader("Jump Back In") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history) { song ->
                        SongCard(
                            song = song,
                            isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                            onClick = { onPlay(song, history) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        // Made For You (Daily Mixes)
        item { MusicSectionHeader("Made For You") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val mixes = listOf("Daily Mix 1", "Daily Mix 2", "Daily Mix 3", "Discovery Weekly")
                items(mixes) { mixName ->
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mixName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Picks
        item { MusicSectionHeader("Quick Picks") }
        item {
            val quickPicks = remember(songs) { songs.shuffled().take(6) }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                quickPicks.chunked(3).forEach { rowSongs ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowSongs.forEach { song ->
                            CompactSongCard(
                                song = song,
                                isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                                onClick = { onPlay(song, quickPicks) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowSongs.size < 3) {
                            repeat(3 - rowSongs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Trending Now
        item { MusicSectionHeader("Trending Now") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val trendingSongs = songs.shuffled().take(5)
                items(trendingSongs) { song ->
                    TrendingSongCard(
                        song = song,
                        isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                        onClick = { onPlay(song, trendingSongs) }
                    )
                }
            }
        }

        // Top Artists
        item { MusicSectionHeader("Your Top Artists") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val artists = songs.map { it.artist }.distinct().take(10)
                items(artists) { artist ->
                    ArtistCard(name = artist, imageUrl = null, color = MaterialTheme.colorScheme.surfaceVariant, onClick = { onArtistClick(artist) })
                }
            }
        }

        // Moods & Activity
        item { MusicSectionHeader("Moods & Activity") }
        item {
            val moods = listOf("Chill", "Workout", "Party", "Focus", "Romance", "Sleep", "Energize", "Cooking")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moods) { mood ->
                    GenreChip(mood)
                }
            }
        }
    }
}

@Composable
fun TrendingSongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(song.albumArtColor)
            .clickable { onClick() }
    ) {
        if (!song.artworkUrl.isNullOrBlank()) {
             coil.compose.AsyncImage(
                model = song.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CompactSongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(song.albumArtColor)
        ) {
            if (!song.artworkUrl.isNullOrBlank()) {
                 coil.compose.AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GenreChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { /* Filter by mood */ }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
