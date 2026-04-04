package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.model.*
import com.example.firstapp.ui.components.*
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.util.Resource

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selectedBottomItem by remember { mutableIntStateOf(0) }
    var selectedTabItem by remember { mutableIntStateOf(0) }
    var selectedChat by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedAdvert by remember { mutableStateOf<Advert?>(null) }

    // Collect states from ViewModel
    val chatsState by viewModel.chats.collectAsState()
    val jobsState by viewModel.jobs.collectAsState()
    val advertsState by viewModel.adverts.collectAsState()
    val channelsState by viewModel.channels.collectAsState()
    val shortsState by viewModel.shorts.collectAsState()
    val songsState by viewModel.songs.collectAsState()
    val postsState by viewModel.posts.collectAsState()

    if (selectedChat != null) {
        ChatScreen(chat = selectedChat!!, onBack = { selectedChat = null })
    } else if (selectedJob != null) {
        JobDetailScreen(job = selectedJob!!, onBack = { selectedJob = null })
    } else if (selectedChannel != null) {
        ChannelDetailScreen(channel = selectedChannel!!, onBack = { selectedChannel = null })
    } else if (selectedAdvert != null) {
        AdvertDetailScreen(advert = selectedAdvert!!, onBack = { selectedAdvert = null })
    } else {
        val bottomItems = listOf("Chats", "Media", "Music", "Drive", "Shorts")
        val bottomIcons = listOf(
            Icons.Filled.Chat,
            Icons.Filled.PermMedia,
            Icons.Filled.MusicNote,
            Icons.Filled.Cloud,
            Icons.Filled.PlayArrow
        )
        val tabs = listOf("Chats", "Jobs", "Adverts", "Channels")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.background(AppBackground)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedBottomItem == 0 && selectedTabItem != 0) tabs[selectedTabItem] else "FirstApp",
                            color = AppTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextPrimary)
                        }
                    }

                    if (selectedBottomItem == 0) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabItem,
                            containerColor = AppBackground,
                            contentColor = AppAccent,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabItem == index,
                                    onClick = { selectedTabItem = index },
                                    text = {
                                        Text(
                                            text = title,
                                            color = if (selectedTabItem == index) AppAccent else AppTextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = AppSecondary) {
                    bottomItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(bottomIcons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedBottomItem == index,
                            onClick = { selectedBottomItem = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppTextPrimary,
                                unselectedIconColor = AppTextSecondary,
                                indicatorColor = Color(0xFF374045)
                            )
                        )
                    }
                }
            },
            containerColor = AppBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (selectedBottomItem) {
                    0 -> when (selectedTabItem) {
                        0 -> DataStateHandler(chatsState) { chats ->
                            ChatList(chats) { selectedChat = it }
                        }
                        1 -> DataStateHandler(jobsState) { jobs ->
                            JobList(jobs) { selectedJob = it }
                        }
                        2 -> DataStateHandler(advertsState) { adverts ->
                            AdvertList(adverts) { selectedAdvert = it }
                        }
                        3 -> DataStateHandler(channelsState) { channels ->
                            ChannelList(channels) { selectedChannel = it }
                        }
                    }
                    1 -> DataStateHandler(postsState) { posts ->
                        MediaScreen(posts)
                    }
                    2 -> DataStateHandler(songsState) { songs ->
                        MusicScreen(songs)
                    }
                    3 -> DriveScreen()
                    4 -> DataStateHandler(shortsState) { shorts ->
                        ShortsList(shorts)
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsList(shorts: List<com.example.firstapp.model.Short>) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { shorts.size })
    androidx.compose.foundation.pager.VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ShortItem(shorts[page])
    }
}

@Composable
fun <T> DataStateHandler(
    resource: Resource<T>,
    onSuccess: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppAccent)
            }
        }
        is Resource.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = resource.message ?: "An error occurred", color = Color.Red)
            }
        }
        is Resource.Success -> {
            resource.data?.let { onSuccess(it) }
        }
    }
}

@Composable
fun ChatList(chats: List<ChatMessage>, onChatClick: (ChatMessage) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(chats) { chat ->
            Column(modifier = Modifier.clickable { onChatClick(chat) }) {
                ChatItem(chat)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF233138)
                )
            }
        }
    }
}

@Composable
fun JobList(jobs: List<Job>, onJobClick: (Job) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(jobs) { job ->
            Column(modifier = Modifier.clickable { onJobClick(job) }) {
                JobItem(job)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF233138)
                )
            }
        }
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (Channel) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels) { channel ->
            Column(modifier = Modifier.clickable { onChannelClick(channel) }) {
                ChannelItem(channel)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF233138)
                )
            }
        }
    }
}

@Composable
fun AdvertList(adverts: List<Advert>, onAdvertClick: (Advert) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(adverts) { advert ->
            Column(modifier = Modifier.clickable { onAdvertClick(advert) }) {
                AdvertItem(advert)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 88.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF233138)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FirstAppTheme {
        MainScreen()
    }
}
