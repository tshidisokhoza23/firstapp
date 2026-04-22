package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.components.*
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(songs: List<Song>, onPlay: (Song, List<Song>) -> Unit) {
    var selectedTab by remember { mutableStateOf("Songs") }
    val tabs = listOf("Songs", "Playlists", "Artists", "Albums")
    var sortBy by remember { mutableStateOf("Recently Played") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { selectedTab = tab },
                    color = if (isSelected) AppAccent else AppSecondary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = AppAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$sortBy • ${songs.size} songs",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        when (selectedTab) {
            "Songs" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(songs) { song ->
                        SongListItem(song) { onPlay(song, songs) }
                    }
                }
            }
            "Playlists" -> {
                val cats = remember(songs) { songs.map { it.category }.distinct() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cats) { cat ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppSecondary)
                                .clickable { /* Navigate to playlist */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = AppAccent)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    cat,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            "Artists" -> {
                val artists = remember(songs) { songs.map { it.artist }.distinct() }
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(artists) { artist ->
                        ArtistListItem(artist)
                    }
                }
            }
            "Albums" -> {
                val albums = remember(songs) { songs.groupBy { it.albumName } }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(albums.keys.toList()) { name ->
                        val albumSongs = albums[name]!!
                        AlbumCard(name ?: "Single", albumSongs.first().artist, albumSongs.first().albumArtColor)
                    }
                }
            }
        }
    }
}
