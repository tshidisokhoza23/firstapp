package com.tpsk.tj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.music.model.Playlist
import com.tpsk.tj.music.ui.components.*
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.ui.components.DataStateHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    viewModel: MusicViewModel,
    currentlyPlaying: Song? = null,
    isPlaying: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onPlay: (Song, List<Song>) -> Unit,
    onShare: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {},
    onArtistClick: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("Songs") }
    val filters = listOf("Playlists", "Artists", "Albums", "Songs", "Downloaded")
    
    val playlistsRes by viewModel.playlists.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()

    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(bottom = bottomPadding)) {
        if (selectedPlaylist == null) {
            // Sticky Filter Chips
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Library Content
            LazyColumn(modifier = Modifier.weight(1f)) {
                when (selectedFilter) {
                    "Songs" -> {
                        // Liked Songs Shortcut
                        item {
                            LibraryActionItem(
                                title = "Liked Songs",
                                subtitle = "${likedSongs.size} songs",
                                icon = Icons.Default.Favorite,
                                iconColor = Color.Red,
                                onClick = { /* Navigate to Liked Songs */ }
                            )
                        }
                        items(songs) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = song.id == currentlyPlaying?.id,
                                onClick = { onPlay(song, songs) },
                                onArtistClick = onArtistClick
                            )
                        }
                    }
                    "Playlists" -> {
                        item {
                            LibraryActionItem(
                                title = "Create playlist",
                                icon = Icons.Default.Add,
                                onClick = { /* Show create dialog */ }
                            )
                        }
                        val playlists = (playlistsRes as? Resource.Success)?.data ?: emptyList()
                        items(playlists) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onClick = { selectedPlaylist = playlist }
                            )
                        }
                    }
                    "Artists" -> {
                        val artists = songs.map { it.artist }.distinct()
                        items(artists) { artist ->
                            ArtistListItem(name = artist)
                        }
                    }
                }
            }
        } else {
            // Playlist Detail (Existing logic refined)
            PlaylistDetailView(
                playlist = selectedPlaylist!!,
                onBack = { selectedPlaylist = null },
                viewModel = viewModel,
                currentlyPlaying = currentlyPlaying,
                onPlay = onPlay,
                onArtistClick = onArtistClick
            )
        }
    }
}

@Composable
fun LibraryActionItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    onBack: () -> Unit,
    viewModel: MusicViewModel,
    currentlyPlaying: Song?,
    onPlay: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    val songsRes = playlistSongs[playlist.id] ?: Resource.Loading()

    LaunchedEffect(playlist.id) {
        viewModel.fetchPlaylistSongs(playlist.id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(8.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(playlist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        DataStateHandler(resource = songsRes) { songs ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(songs) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = song.id == currentlyPlaying?.id,
                        onClick = { onPlay(song, songs) },
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}
