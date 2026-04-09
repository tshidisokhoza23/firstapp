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

@Composable
fun RecommendationsScreen(songs: List<Song>, onPlay: (Song) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { MusicSectionHeader("For You") }
        item {
            val recs = remember(songs) { songs.shuffled().take(10) }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(recs) { song -> SongCard(song) { onPlay(song) } }
            }
        }

        item { MusicSectionHeader("Trending") }
        item {
            val trending = remember(songs) { songs.shuffled().take(10) }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(trending) { song -> SongCard(song) { onPlay(song) } }
            }
        }

        item { MusicSectionHeader("New Releases") }
        item {
            val newR = remember(songs) { songs.shuffled().take(10) }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(newR) { song -> SongCard(song) { onPlay(song) } }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}