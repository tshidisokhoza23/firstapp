package com.tpsk.tj.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.music.ui.components.SongListItem
import com.tpsk.tj.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchScreen(viewModel: MusicViewModel) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.search(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isEmpty()) {
            Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
            // Show search history
        } else {
            when (val res = searchResults) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Success -> {
                    val songs = res.data?.songs ?: emptyList()
                    LazyColumn {
                        items(songs) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = song.id == currentlyPlaying?.id,
                                onClick = { viewModel.playSong(song, songs) }
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    Text("Error: ${res.message}", color = Color.Red)
                }
            }
        }
    }
}
