package com.example.firstapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.components.*
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppTextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(songs: List<Song>, onPlay: (Song) -> Unit) {
    var selectedTab by remember { mutableStateOf("Songs") }
    val tabs = listOf("Songs", "Albums", "Artists", "Playlists")
    var sortBy by remember { mutableStateOf("Alphabetical") }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor = Color.Transparent,
            contentColor = AppAccent,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEach { tab ->
                Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab) })
            }
        }

        if (selectedTab == "Songs") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by: ", color = AppTextSecondary, fontSize = 12.sp)
                Text(
                    sortBy,
                    color = AppAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        sortBy = if (sortBy == "Alphabetical") "Duration" else "Alphabetical"
                    }
                )
            }
        }

        when (selectedTab) {
            "Songs" -> {
                val sortedSongs = remember(sortBy, songs) {
                    if (sortBy == "Alphabetical") songs.sortedBy { it.title }
                    else songs.sortedByDescending { it.duration }
                }
                LazyColumn { items(sortedSongs) { SongListItem(it) { onPlay(it) } } }
            }
            "Albums" -> {
                val albums = remember(songs) { songs.groupBy { it.albumName } }
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(albums.keys.toList()) { name ->
                        val albumSongs = albums[name]!!
                        AlbumCard(name, albumSongs.first().artist, albumSongs.first().albumArtColor)
                    }
                }
            }
            "Artists" -> {
                val artists = remember(songs) { songs.groupBy { it.artist }.keys.toList() }
                LazyColumn { items(artists) { ArtistListItem(it) } }
            }
            "Playlists" -> {
                val cats = remember(songs) { songs.map { it.category }.distinct() }
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(cats) { PlaylistCard(it) }
                }
            }
        }
    }
}
