package com.example.firstapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.music.model.Song
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.home.ui.screens.SearchScreen
import com.example.firstapp.music.ui.viewmodel.MusicViewModel

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MusicNote

import com.example.firstapp.music.ui.components.SongListItem
import com.example.firstapp.music.ui.components.MusicSectionHeader
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person

@Composable
fun ArtistSongsScreen(
    artistName: String,
    artistId: String? = null,
    avatarUrl: String? = null,
    bio: String? = null,
    allSongs: List<Song>,
    currentlyPlaying: Song?,
    isPlaying: Boolean,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onShare: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit = {}
) {
    val artistSongs = remember(artistName, artistId, allSongs) {
        allSongs.filter { it.artist == artistName || (artistId != null && it.artistId == artistId) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(artistName, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp + bottomPadding)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(80.dp))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Text("ARTIST", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
                }
            }

            if (!bio.isNullOrBlank()) {
                item {
                    Text(
                        text = bio,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        lineHeight = 20.sp
                    )
                }
            }

            item { MusicSectionHeader("Songs") }

            if (artistSongs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No songs found for this artist", color = Color.Gray)
                    }
                }
            } else {
                items(artistSongs) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = song.id == currentlyPlaying?.id && isPlaying,
                        onClick = { onPlay(song, artistSongs) },
                        onShare = { onShare(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onArtistClick = { name ->
                            // Already on this artist's screen, but could be a featured artist
                            if (name != artistName) {
                                // Navigate to other artist
                                // This could be tricky recursively, but let's just trigger a re-selection
                                // actually, let's just use the name
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MusicScreen(
    songs: List<Song>,
    viewModel: MusicViewModel,
    bottomPadding: Dp = 0.dp,
    onShareSong: (Song) -> Unit = {},
    onShowSettings: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("Recommendations") }
    val tabs = listOf("Recommendations", "Library", "Search")
    
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playlistsState by viewModel.playlists.collectAsState()
    val requestedArtist by viewModel.requestedArtist.collectAsState()

    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    var selectedArtistAvatar by remember { mutableStateOf<String?>(null) }
    var selectedArtistBio by remember { mutableStateOf<String?>(null) }
    
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    androidx.activity.compose.BackHandler(enabled = selectedArtistName != null || currentScreen != "Recommendations") {
        if (selectedArtistName != null) {
            selectedArtistName = null
            selectedArtistId = null
            selectedArtistAvatar = null
            selectedArtistBio = null
        } else {
            currentScreen = "Recommendations"
        }
    }

    LaunchedEffect(requestedArtist) {
        if (requestedArtist != null) {
            selectedArtistName = requestedArtist
            // Find additional info if possible
            val artistSong = songs.find { it.artist == requestedArtist }
            selectedArtistId = artistSong?.artistId
            // Reset request so it doesn't trigger again on configuration change
            viewModel.requestArtist(null)
        }
    }

    if (songForPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songForPlaylist = null },
            title = { Text("Add to Playlist") },
            text = {
                val playlists = playlistsState.data ?: emptyList()
                if (playlists.isEmpty()) {
                    Text("No playlists created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            TextButton(
                                onClick = {
                                    viewModel.addSongToPlaylist(playlist.id, songForPlaylist!!.id)
                                    songForPlaylist = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(playlist.name, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songForPlaylist = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (selectedArtistName != null) {
        ArtistSongsScreen(
            artistName = selectedArtistName!!,
            artistId = selectedArtistId,
            avatarUrl = selectedArtistAvatar,
            bio = selectedArtistBio,
            allSongs = songs,
            currentlyPlaying = currentlyPlaying,
            isPlaying = isPlaying,
            bottomPadding = bottomPadding,
            onBack = { 
                selectedArtistName = null
                selectedArtistId = null
                selectedArtistAvatar = null
                selectedArtistBio = null
            },
            onPlay = { s, q -> viewModel.playSong(s, q) },
            onShare = onShareSong,
            onAddToPlaylist = { songForPlaylist = it }
        )
        return
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Music",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row {
                        IconButton(onClick = { currentScreen = "Search" }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { onShowSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                
                TabRow(
                    selectedTabIndex = tabs.indexOf(currentScreen),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(currentScreen)]).padding(horizontal = 24.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = currentScreen == tab,
                            onClick = { currentScreen = tab },
                            text = {
                                Text(
                                    text = tab,
                                    color = if (currentScreen == tab) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = if (currentScreen == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    "Recommendations" -> RecommendationsScreen(
                        songs = songs, 
                        viewModel = viewModel,
                        currentlyPlaying = currentlyPlaying,
                        isPlaying = isPlaying,
                        bottomPadding = bottomPadding,
                        onPlay = { song, queue -> viewModel.playSong(song, queue) },
                        onShare = onShareSong,
                        onAddToPlaylist = { songForPlaylist = it },
                        onArtistClick = { name ->
                            selectedArtistName = name
                            // Try to find more info from songs if available
                            val artistSong = songs.find { it.artist == name }
                            selectedArtistId = artistSong?.artistId
                        }
                    )
                    "Library" -> LibraryScreen(
                        songs = songs, 
                        viewModel = viewModel,
                        currentlyPlaying = currentlyPlaying,
                        isPlaying = isPlaying,
                        bottomPadding = bottomPadding,
                        onPlay = { song, queue -> viewModel.playSong(song, queue) },
                        onShare = onShareSong,
                        onAddToPlaylist = { songForPlaylist = it },
                        onArtistClick = { name ->
                            selectedArtistName = name
                            val artistSong = songs.find { it.artist == name }
                            selectedArtistId = artistSong?.artistId
                        }
                    )
                    "Search" -> SearchScreen(
                        viewModel = viewModel,
                        currentlyPlaying = currentlyPlaying,
                        isPlaying = isPlaying,
                        bottomPadding = bottomPadding,
                        onPlay = { song, queue -> viewModel.playSong(song, queue) },
                        onShare = onShareSong,
                        onArtistClick = { contact ->
                            selectedArtistName = contact.displayName
                            selectedArtistId = contact.id.toString()
                            selectedArtistAvatar = contact.avatarUrl
                            selectedArtistBio = contact.bio
                        },
                        onAddToPlaylist = { songForPlaylist = it }
                    )
                }
            }
        }
    }
}
