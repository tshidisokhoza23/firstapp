package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    var showStories by remember { mutableStateOf(false) }
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

    MainContent(
        selectedBottomItem = selectedBottomItem,
        onBottomItemSelect = { selectedBottomItem = it },
        selectedTabItem = selectedTabItem,
        onTabItemSelect = { selectedTabItem = it },
        selectedChat = selectedChat,
        onChatSelect = { selectedChat = it },
        showStories = showStories,
        onShowStories = { showStories = it },
        selectedJob = selectedJob,
        onJobSelect = { selectedJob = it },
        selectedChannel = selectedChannel,
        onChannelSelect = { selectedChannel = it },
        selectedAdvert = selectedAdvert,
        onAdvertSelect = { selectedAdvert = it },
        chatsState = chatsState,
        jobsState = jobsState,
        advertsState = advertsState,
        channelsState = channelsState,
        shortsState = shortsState,
        songsState = songsState,
        postsState = postsState
    )
}

@Composable
fun MainContent(
    selectedBottomItem: Int,
    onBottomItemSelect: (Int) -> Unit,
    selectedTabItem: Int,
    onTabItemSelect: (Int) -> Unit,
    selectedChat: ChatMessage?,
    onChatSelect: (ChatMessage?) -> Unit,
    selectedJob: Job?,
    onJobSelect: (Job?) -> Unit,
    selectedChannel: Channel?,
    onChannelSelect: (Channel?) -> Unit,
    selectedAdvert: Advert?,
    onAdvertSelect: (Advert?) -> Unit,
    showStories: Boolean,
    onShowStories: (Boolean) -> Unit,
    chatsState: Resource<List<ChatMessage>>,
    jobsState: Resource<List<Job>>,
    advertsState: Resource<List<Advert>>,
    channelsState: Resource<List<Channel>>,
    shortsState: Resource<List<com.example.firstapp.model.Short>>,
    songsState: Resource<List<Song>>,
    postsState: Resource<List<Post>>
) {
    if (selectedChat != null) {
        ChatScreen(chat = selectedChat, onBack = { onChatSelect(null) })
    } else if (showStories) {
        StoryScreen(onBack = { onShowStories(false) })
    } else if (selectedJob != null) {
        JobDetailScreen(job = selectedJob, onBack = { onJobSelect(null) })
    } else if (selectedChannel != null) {
        ChannelDetailScreen(channel = selectedChannel, onBack = { onChannelSelect(null) })
    } else if (selectedAdvert != null) {
        AdvertDetailScreen(advert = selectedAdvert, onBack = { onAdvertSelect(null) })
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
            floatingActionButton = {
                if (selectedBottomItem == 0 && selectedTabItem == 0) {
                    FloatingActionButton(
                        onClick = { onShowStories(true) },
                        containerColor = AppAccent,
                        contentColor = AppBackground,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Stories")
                    }
                }
            },
            topBar = {
                Column(
                    modifier = Modifier
                        .background(AppBackground)
                        .statusBarsPadding() // Ensures it doesn't overlap with the notch/status bar
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedBottomItem == 0 && selectedTabItem != 0) tabs[selectedTabItem] else "FirstApp",
                            color = AppTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppTextPrimary)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextPrimary)
                        }
                    }

                    if (selectedBottomItem == 0) {
                        TabRow( // Changed from ScrollableTabRow to TabRow for better distribution on most screens
                            selectedTabIndex = selectedTabItem,
                            containerColor = AppBackground,
                            contentColor = AppAccent,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabItem]),
                                    color = AppAccent
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabItem == index,
                                    onClick = { onTabItemSelect(index) },
                                    text = {
                                        Text(
                                            text = title,
                                            color = if (selectedTabItem == index) AppAccent else AppTextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = AppSecondary,
                    tonalElevation = 8.dp
                ) {
                    bottomItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(bottomIcons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedBottomItem == index,
                            onClick = { onBottomItemSelect(index) },
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
                            ChatList(chats) { onChatSelect(it) }
                        }
                        1 -> DataStateHandler(jobsState) { jobs ->
                            JobList(jobs) { onJobSelect(it) }
                        }
                        2 -> DataStateHandler(advertsState) { adverts ->
                            AdvertList(adverts) { onAdvertSelect(it) }
                        }
                        3 -> DataStateHandler(channelsState) { channels ->
                            ChannelList(channels) { onChannelSelect(it) }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp) // Adds padding at the very end of the list
    ) {
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
        MainContent(
            selectedBottomItem = 0,
            onBottomItemSelect = {},
            selectedTabItem = 0,
            onTabItemSelect = {},
            selectedChat = null,
            onChatSelect = {},
            selectedJob = null,
            onJobSelect = {},
            selectedChannel = null,
            onChannelSelect = {},
            selectedAdvert = null,
            onAdvertSelect = {},
            showStories = false,
            onShowStories = {},
            chatsState = Resource.Success(com.example.firstapp.data.ChatService.dummyChats),
            jobsState = Resource.Success(emptyList()),
            advertsState = Resource.Success(emptyList()),
            channelsState = Resource.Success(emptyList()),
            shortsState = Resource.Success(emptyList()),
            songsState = Resource.Success(emptyList()),
            postsState = Resource.Success(emptyList())
        )
    }
}
