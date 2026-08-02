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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import com.example.firstapp.common.ui.theme.AppAccent

import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

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
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp + bottomPadding)
    ) {
        // ... (rest of the sections)
        // ... (Featured, Quick Picks, Artists, Trending, New Releases, Charts, Moods)
        // 1. Featured Card
        item {
            FeaturedPlaylistCard(
                name = "Weekly Mix",
                description = "Based on your recent listening",
                imageUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 2. Quick Picks (Grid-like)
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
                        // Fill empty space if row is not full
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

        // 3. Recommended Artists (Circles)
        item { MusicSectionHeader("Recommended Artists") }
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

        // 4. Trending Now (New Large Horizontal List)
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

        // 5. New Releases (Horizontal List)
        item { MusicSectionHeader("New Releases") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(songs.reversed().take(8)) { song ->
                    SongCard(
                        song = song,
                        isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                        onClick = { onPlay(song, songs) },
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        // 6. Top Charts (Vertical-ish Horizontal List)
        item { MusicSectionHeader("Top Charts") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val charts = songs.chunked(3).take(3)
                items(charts.size) { index ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        charts[index].forEachIndexed { i, song ->
                            ChartItemRow(
                                rank = (index * 3) + i + 1,
                                song = song,
                                isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                                onClick = { onPlay(song, songs) }
                            )
                        }
                    }
                }
            }
        }

        // 7. Moods & Genres (Chips)
        item { MusicSectionHeader("Moods & Genres") }
        item {
            val moods = listOf("Chill", "Workout", "Party", "Focus", "Romance", "Sleep", "Energize", "Sad")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moods) { mood ->
                    GenreChip(mood)
                }
            }
        }

        // 8. Recently Played (Horizontal List)
        item {
            val history = recentlyPlayedState.data ?: emptyList()
            if (history.isNotEmpty()) {
                MusicSectionHeader("Recently Played")
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

        item {
            val history = recentlyPlayedState.data ?: emptyList()
            if (history.isNotEmpty()) {
                MusicSectionHeader("Add to Playlist Test")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(songs.take(5)) { song ->
                        TextButton(onClick = { onAddToPlaylist(song) }) {
                            Text("+ ${song.title}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
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
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
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
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChartItemRow(
    rank: Int,
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(song.albumArtColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = song.title,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = song.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
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
                .size(100.dp) // Fixed size for better grid appearance
                .clip(RoundedCornerShape(12.dp))
                .background(song.albumArtColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GenreChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { /* Filter by genre */ }
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
