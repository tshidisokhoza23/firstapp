package com.example.firstapp.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MusicViewModel

import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.filled.MusicNote

@Composable
fun MusicScreen(
    songs: List<Song>,
    viewModel: MusicViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("Recommendations") }
    val tabs = listOf("Recommendations", "Library", "Search")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AppBackground)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Music",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row {
                        IconButton(onClick = { /* Search */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
                
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(currentScreen),
                    containerColor = Color.Transparent,
                    contentColor = AppAccent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = currentScreen == tab,
                            onClick = { currentScreen = tab },
                            text = {
                                Text(
                                    tab,
                                    fontSize = 16.sp,
                                    fontWeight = if (currentScreen == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No songs found", color = Color.Gray, fontSize = 18.sp)
                    }
                }
            } else {
                when (currentScreen) {
                    "Recommendations" -> RecommendationsScreen(songs) { song, queue -> viewModel.playSong(song, queue) }
                    "Library" -> LibraryScreen(songs) { song, queue -> viewModel.playSong(song, queue) }
                    "Search" -> SearchScreen(songs) { song, queue -> viewModel.playSong(song, queue) }
                }
            }
        }
    }
}
