package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.*

@Composable
fun MusicScreen(songs: List<Song>) {
    var currentlyPlaying by remember { mutableStateOf<Song?>(songs.firstOrNull()) }
    
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Discover",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Horizontal categories
                item {
                    val categories = listOf("Relax", "Workout", "Party", "Focus", "Energize")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(category, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF233138)
                                ),
                                border = null
                            )
                        }
                    }
                }

                // Section: Trending Now
                item {
                    Text(
                        "Trending Now",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(songs.take(5)) { song ->
                            SongCard(song) { currentlyPlaying = song }
                        }
                    }
                }

                // Section: Recommended for You
                item {
                    Text(
                        "Recommended for You",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(songs.drop(5)) { song ->
                    SongListItem(song) { currentlyPlaying = song }
                }
            }
        }

        // Floating Mini Player (Better than Spotify)
        currentlyPlaying?.let { song ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E2A30),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(song.albumArtColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = AppAccent)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                // Progress bar at the very bottom of the mini player
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight()
                            .background(AppAccent)
                    )
                }
            }
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(song.albumArtColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun SongListItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(song.albumArtColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(song.artist, color = AppTextSecondary, fontSize = 12.sp)
        }
        Text(song.duration, color = AppTextSecondary, fontSize = 12.sp)
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = AppTextSecondary)
        }
    }
}
