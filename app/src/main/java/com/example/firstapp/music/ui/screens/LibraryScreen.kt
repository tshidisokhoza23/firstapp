package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.music.ui.components.*
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.home.ui.screens.DataStateHandler

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    var selectedTab by remember { mutableStateOf("Songs") }
    val tabs = listOf("Songs", "Playlists", "Artists", "Albums")
    var sortBy by remember { mutableStateOf("Recently Played") }
    
    val playlistsRes by viewModel.playlists.collectAsState()
    val playlistSongs by viewModel.playlistSongs.collectAsState()

    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlaylist) {
        selectedPlaylist?.let {
            viewModel.fetchPlaylistSongs(it.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedPlaylist != null) selectedPlaylist = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(bottom = bottomPadding)) {
            if (selectedPlaylist == null) {
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab),
                    containerColor = AppBackground,
                    contentColor = AppAccent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                when (selectedTab) {
                    "Songs" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(songs) { song ->
                                SongListItem(
                                    song = song,
                                    isPlaying = song.id == currentlyPlaying?.id,
                                    onClick = { onPlay(song, songs) },
                                    onShare = { onShare(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                    "Playlists" -> {
                        val playlists = playlistsRes.data ?: emptyList()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlists) { playlist ->
                                PlaylistListItem(
                                    playlist = playlist,
                                    onClick = { selectedPlaylist = playlist }
                                )
                            }
                        }
                    }
                    "Artists" -> {
                        val artists = songs.map { it.artist }.distinct()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(artists) { artist ->
                                ArtistListItem(name = artist)
                            }
                        }
                    }
                    "Albums" -> {
                        val albums = songs.distinctBy { it.albumName }.filter { it.albumName.isNotBlank() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums) { album ->
                                AlbumCard(name = album.albumName, artist = album.artist, color = album.albumArtColor)
                            }
                        }
                    }
                }
            } else {
                // Playlist detail view
                val playlist = selectedPlaylist!!
                val songsRes = playlistSongs[playlist.id] ?: Resource.Loading()
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(AppSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!playlist.coverArtUrl.isNullOrBlank()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(playlist.coverArtUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = AppAccent, modifier = Modifier.size(80.dp))
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        ) {
                            Text(playlist.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Playlist", color = AppTextSecondary, fontSize = 14.sp)
                        }
                    }

                    DataStateHandler(
                        resource = songsRes
                    ) { songsList ->
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(songsList) { song ->
                                SongListItem(
                                    song = song,
                                    isPlaying = song.id == currentlyPlaying?.id,
                                    onClick = { onPlay(song, songsList) },
                                    onShare = { onShare(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
