package com.example.firstapp.music.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.firstapp.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTabScreen(
    navController: NavController,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Library", "Search", "Explore")
    
    val songsState by musicViewModel.songs.collectAsState()
    val currentlyPlaying by musicViewModel.currentlyPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                IconButton(onClick = { selectedTabIndex = 2 }) { // Search tab
                    Icon(Icons.Default.Search, null)
                }
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Default.Settings, null)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]).padding(horizontal = 24.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Content
        Crossfade(targetState = selectedTabIndex, modifier = Modifier.weight(1f).padding(bottom = bottomPadding)) { target ->
            when (target) {
                0 -> RecommendationsScreen(
                    songs = songsState.data ?: emptyList(),
                    viewModel = musicViewModel,
                    currentlyPlaying = currentlyPlaying,
                    isPlaying = isPlaying,
                    onPlay = { s, q -> musicViewModel.playSong(s, q) }
                )
                1 -> LibraryScreen(
                    songs = songsState.data ?: emptyList(),
                    viewModel = musicViewModel,
                    currentlyPlaying = currentlyPlaying,
                    isPlaying = isPlaying,
                    onPlay = { s, q -> musicViewModel.playSong(s, q) }
                )
                2 -> {
                    MusicSearchScreen(musicViewModel)
                }
                3 -> {
                    ExploreScreen()
                }
            }
        }
    }
}
