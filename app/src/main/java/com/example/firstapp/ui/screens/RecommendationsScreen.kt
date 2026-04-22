package com.example.firstapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.components.MusicSectionHeader
import com.example.firstapp.ui.components.SongCard

import androidx.compose.ui.graphics.Color
import com.example.firstapp.ui.components.ArtistCard
import com.example.firstapp.ui.components.FeaturedPlaylistCard

@Composable
fun RecommendationsScreen(songs: List<Song>, onPlay: (Song, List<Song>) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            FeaturedPlaylistCard(
                name = "Weekly Mix",
                description = "Based on your recent listening",
                imageUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800"
            )
        }

        item { MusicSectionHeader("Quick Picks") }
        item {
            val quickPicks = remember(songs) { songs.shuffled().take(5) }
            Column {
                quickPicks.forEach { song ->
                    com.example.firstapp.ui.components.SongListItem(song) { onPlay(song, quickPicks) }
                }
            }
        }

        item { MusicSectionHeader("Jump Back In") }
        item {
            val recs = remember(songs) { songs.shuffled().take(10) }
            LazyRow(
                contentPadding = PaddingValues(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp) // Spacing handled inside SongCard padding
            ) {
                items(recs) { song -> SongCard(song) { onPlay(song, recs) } }
            }
        }

        item { MusicSectionHeader("Trending Artists") }
        item {
            val artists = remember(songs) { songs.map { it.artist }.distinct().shuffled().take(10) }
            LazyRow(
                contentPadding = PaddingValues(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(artists) { artist ->
                    ArtistCard(
                        name = artist,
                        imageUrl = null,
                        color = Color(0xFF37474F)
                    )
                }
            }
        }

        item { MusicSectionHeader("New Releases") }
        item {
            val newR = remember(songs) { songs.shuffled().take(10) }
            LazyRow(
                contentPadding = PaddingValues(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(newR) { song -> SongCard(song) { onPlay(song, newR) } }
            }
        }
    }
}
