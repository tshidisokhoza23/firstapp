package com.example.firstapp.home.ui.screens

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
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.ui.components.SongListItem
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppTextSecondary

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
import com.example.firstapp.common.ui.theme.AppSecondary

import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Contact

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person

import androidx.compose.foundation.lazy.LazyRow

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    currentlyPlaying: Song? = null,
    isPlaying: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onPlay: (Song, List<Song>) -> Unit,
    onShare: (Song) -> Unit = {},
    onArtistClick: (com.example.firstapp.chat.model.Contact) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val categories = listOf("Rock", "Pop", "Jazz", "Hip-Hop", "Afrobeat", "Electronic", "Classical", "R&B")

    Column(modifier = Modifier.fillMaxSize()) {
        // ... (TextField)
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.search(it)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search songs or artists", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { 
                        query = ""
                        viewModel.search("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )

        if (query.isEmpty()) {
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text(
                        "Clear", 
                        color = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.clickable { viewModel.clearSearchHistory() },
                        fontSize = 14.sp
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHistory) { historyItem ->
                        Surface(
                            onClick = { 
                                query = historyItem
                                viewModel.search(historyItem)
                            },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f))
                        ) {
                            Text(
                                historyItem, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "Explore Genres",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 180.dp + bottomPadding, start = 16.dp, end = 16.dp, top = 8.dp),
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
                            .clickable { 
                                query = cat
                                viewModel.search(cat)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        } else {
            val data = searchResults.data
            val error = (searchResults as? Resource.Error)?.message

            if (error != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (searchResults is Resource.Loading && data == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (data != null) {
                LazyColumn(contentPadding = PaddingValues(bottom = 90.dp + bottomPadding)) {
                    if (data.artists.isNotEmpty()) {
                        item {
                            Text("Artists", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        }
                        items(data.artists) { artist ->
                            ArtistListItemWithAction(artist) { onArtistClick(artist) }
                        }
                    }
                    
                    if (data.songs.isNotEmpty()) {
                        item {
                            Text("Songs", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        }
                        items(data.songs) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                                onClick = { onPlay(song, data.songs) },
                                onShare = { onShare(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onArtistClick = { name ->
                                    val contact = data.artists.find { it.displayName == name }
                                    if (contact != null) {
                                        onArtistClick(contact)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistListItemWithAction(artist: com.example.firstapp.chat.model.Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.avatarUrl != null) {
                coil.compose.AsyncImage(model = artist.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(artist.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Artist", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
