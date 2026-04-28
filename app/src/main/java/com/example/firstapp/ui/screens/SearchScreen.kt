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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.AppSecondary

@Composable
fun SearchScreen(
    songs: List<Song>,
    onPlay: (Song, List<Song>) -> Unit,
    onShare: (Song) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isEmpty()) emptyList() else songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }

    val categories = listOf("Rock", "Pop", "Jazz", "Hip-Hop", "Afrobeat", "Electronic", "Classical", "R&B")

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search songs or artists", color = AppTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppTextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppSecondary,
                unfocusedContainerColor = AppSecondary,
                focusedBorderColor = AppAccent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        if (query.isEmpty()) {
            Text(
                "Explore Genres",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { cat ->
                    val color = remember(cat) { 
                        listOf(Color(0xFF673AB7), Color(0xFF3949AB), Color(0xFF00897B), Color(0xFFD81B60), 
                               Color(0xFF0288D1), Color(0xFF2E7D32), Color(0xFFF57C00), Color(0xFF5E35B1)).random()
                    }
                    Box(
                        modifier = Modifier
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.4f))
                                )
                            )
                            .clickable { query = cat },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
else {
            LazyColumn {
                items(filtered) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onPlay(song, filtered) },
                        onShare = { onShare(song) }
                    )
                }
            }
        }
    }
}
