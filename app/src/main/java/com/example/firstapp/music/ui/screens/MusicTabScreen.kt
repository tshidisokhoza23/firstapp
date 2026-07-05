package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.firstapp.music.ui.viewmodel.MusicViewModel

import androidx.compose.ui.unit.dp

@Composable
fun MusicTabScreen(
    navController: NavController,
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val songsState by musicViewModel.songs.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        MusicScreen(
            songs = songsState.data ?: java.util.Collections.emptyList(),
            viewModel = musicViewModel,
            bottomPadding = 0.dp,
            onShareSong = { /* Handle share */ },
            onShowSettings = { /* Handle settings */ }
        )
    }
}
