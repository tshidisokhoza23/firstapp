package com.example.firstapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.firstapp.model.Song
import com.example.firstapp.ui.components.SongListItem
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppTextSecondary

@Composable
fun SearchScreen(songs: List<Song>, onPlay: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isEmpty()) emptyList() else songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search songs or artists", color = AppTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppTextSecondary) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E2A30),
                unfocusedContainerColor = Color(0xFF1E2A30),
                focusedIndicatorColor = AppAccent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
        LazyColumn { items(filtered) { SongListItem(it) { onPlay(it) } } }
    }
}