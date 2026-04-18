package com.example.firstapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.data.repository.SearchResult
import com.example.firstapp.model.*
import com.example.firstapp.ui.components.*
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.viewmodel.MusicViewModel
import com.example.firstapp.util.Resource


@Composable
fun MainScreen(onLogout: () -> Unit, viewModel: MainViewModel = viewModel(), musicViewModel: MusicViewModel = viewModel()) {
    var selectedBottomItem by remember { mutableIntStateOf(0) }
    var selectedTabItem by remember { mutableIntStateOf(0) }
    var selectedMediaFeedTab by remember { mutableIntStateOf(0) }
    var selectedChat by remember { mutableStateOf<ChatMessage?>(null) }
    var showStories by remember { mutableStateOf(false) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedAdvert by remember { mutableStateOf<Advert?>(null) }
    var showAdvertViewer by remember { mutableStateOf(false) }
    var adsToView by remember { mutableStateOf<List<Advert>>(emptyList()) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showJobProfile by remember { mutableStateOf(false) }
    var showApplications by remember { mutableStateOf(false) }
    var showDirectMessages by remember { mutableStateOf(false) }

    // Collect states from ViewModel
    val chatsState by viewModel.chats.collectAsState()
    val jobsState by viewModel.jobs.collectAsState()
    val advertsState by viewModel.adverts.collectAsState()
    val channelsState by viewModel.channels.collectAsState()
    val shortsState by viewModel.shorts.collectAsState()
    val songsState by viewModel.songs.collectAsState()
    val postsState by viewModel.posts.collectAsState()
    val storiesState by viewModel.stories.collectAsState()
    val applications by viewModel.applications.collectAsState()

    // Music State
    val currentlyPlaying by musicViewModel.currentlyPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.progress.collectAsState()
    val isShuffleEnabled by musicViewModel.isShuffleEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()
    val likedSongs by musicViewModel.likedSongs.collectAsState()
    var isNowPlayingVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        musicViewModel.initPlayer(context)
    }

    MainContent(
        viewModel = viewModel,
        selectedBottomItem = selectedBottomItem,
        onBottomItemSelect = { selectedBottomItem = it },
        selectedTabItem = selectedTabItem,
        onTabItemSelect = { selectedTabItem = it },
        selectedMediaFeedTab = selectedMediaFeedTab,
        onMediaFeedTabSelect = { selectedMediaFeedTab = it },
        selectedChat = selectedChat,
        onChatSelect = { selectedChat = it },
        showStories = showStories,
        onShowStories = { showStories = it },
        selectedJob = selectedJob,
        onJobSelect = { selectedJob = it },
        selectedPost = selectedPost,
        onPostSelect = { selectedPost = it },
        selectedChannel = selectedChannel,
        onChannelSelect = { selectedChannel = it },
        selectedAdvert = selectedAdvert,
        onAdvertSelect = { selectedAdvert = it },
        showAdvertViewer = showAdvertViewer,
        onShowAdvertViewer = { showAdvertViewer = it },
        adsToView = adsToView,
        onSetAdsToView = { adsToView = it },
        viewerInitialIndex = viewerInitialIndex,
        onSetViewerInitialIndex = { viewerInitialIndex = it },
        onAdvertViewed = { viewModel.markAdvertAsViewed(it) },
        chatsState = chatsState,
        jobsState = jobsState,
        advertsState = advertsState,
        channelsState = channelsState,
        shortsState = shortsState,
        songsState = songsState,
        postsState = postsState,
        storiesState = storiesState,
        onLogout = onLogout,
        showContactPicker = showContactPicker,
        onShowContactPicker = { showContactPicker = it },
        showJobProfile = showJobProfile,
        onShowJobProfile = { showJobProfile = it },
        showApplications = showApplications,
        onShowApplications = { showApplications = it },
        applications = applications,
        showDirectMessages = showDirectMessages,
        onShowDirectMessages = { showDirectMessages = it },
        musicViewModel = musicViewModel,
        currentlyPlaying = currentlyPlaying,
        isPlaying = isPlaying,
        progress = progress,
        isShuffleEnabled = isShuffleEnabled,
        repeatMode = repeatMode,
        likedSongs = likedSongs,
        isNowPlayingVisible = isNowPlayingVisible,
        onSetNowPlayingVisible = { isNowPlayingVisible = it }
    )
}

@Composable
fun MainContent(
    viewModel: MainViewModel,
    selectedBottomItem: Int,
    onBottomItemSelect: (Int) -> Unit,
    selectedTabItem: Int,
    onTabItemSelect: (Int) -> Unit,
    selectedMediaFeedTab: Int,
    onMediaFeedTabSelect: (Int) -> Unit,
    selectedChat: ChatMessage?,
    onChatSelect: (ChatMessage?) -> Unit,
    selectedJob: Job?,
    onJobSelect: (Job?) -> Unit,
    selectedPost: Post?,
    onPostSelect: (Post?) -> Unit,
    selectedChannel: Channel?,
    onChannelSelect: (Channel?) -> Unit,
    selectedAdvert: Advert?,
    onAdvertSelect: (Advert?) -> Unit,
    showAdvertViewer: Boolean,
    onShowAdvertViewer: (Boolean) -> Unit,
    adsToView: List<Advert>,
    onSetAdsToView: (List<Advert>) -> Unit,
    viewerInitialIndex: Int,
    onSetViewerInitialIndex: (Int) -> Unit,
    onAdvertViewed: (Int) -> Unit,
    showStories: Boolean,
    onShowStories: (Boolean) -> Unit,
    chatsState: Resource<List<ChatMessage>>,
    jobsState: Resource<List<Job>>,
    advertsState: Resource<List<Advert>>,
    channelsState: Resource<List<Channel>>,
    shortsState: Resource<List<com.example.firstapp.model.Short>>,
    songsState: Resource<List<Song>>,
    postsState: Resource<List<Post>>,
    storiesState: Resource<List<UserStory>>,
    onLogout: () -> Unit,
    showContactPicker: Boolean,
    onShowContactPicker: (Boolean) -> Unit,
    showJobProfile: Boolean,
    onShowJobProfile: (Boolean) -> Unit,
    showApplications: Boolean,
    onShowApplications: (Boolean) -> Unit,
    applications: List<JobApplication>,
    showDirectMessages: Boolean,
    onShowDirectMessages: (Boolean) -> Unit,
    musicViewModel: MusicViewModel,
    currentlyPlaying: Song?,
    isPlaying: Boolean,
    progress: Float,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    likedSongs: Set<Int>,
    isNowPlayingVisible: Boolean,
    onSetNowPlayingVisible: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.clearSearch()
        } else {
            viewModel.search(searchQuery)
        }
    }

    val onBackAction = {
        if (isNowPlayingVisible) {
            onSetNowPlayingVisible(false)
        }
        else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
        else if (selectedChat != null) onChatSelect(null)
        else if (showStories) onShowStories(false)
        else if (showContactPicker) onShowContactPicker(false)
        else if (showJobProfile) onShowJobProfile(false)
        else if (showApplications) onShowApplications(false)
        else if (showDirectMessages) onShowDirectMessages(false)
        else if (selectedPost != null) onPostSelect(null)
        else if (selectedJob != null) onJobSelect(null)
        else if (selectedChannel != null) onChannelSelect(null)
        else if (selectedAdvert != null) onAdvertSelect(null)
        else if (showAdvertViewer) {
            onShowAdvertViewer(false)
            onSetAdsToView(emptyList())
        }
        else if (selectedBottomItem != 0) onBottomItemSelect(0)
    }

    androidx.activity.compose.BackHandler(
        enabled = isNowPlayingVisible || isSearchActive || selectedChat != null || showStories || showContactPicker || showJobProfile || showApplications || showDirectMessages || selectedJob != null || selectedChannel != null || selectedAdvert != null || showAdvertViewer || selectedBottomItem != 0,
        onBack = onBackAction
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedChat != null) {
            ChatScreen(
                chat = selectedChat,
                onBack = {
                    onChatSelect(null)
                }
            )
        } else if (showDirectMessages) {
            val chats = (chatsState as? Resource.Success)?.data ?: emptyList()
            DirectMessagesScreen(
                chats = chats,
                onBack = { onShowDirectMessages(false) },
                onChatSelect = { chat ->
                    onChatSelect(chat)
                },
                onNewDM = {
                    onShowDirectMessages(false)
                    onShowContactPicker(true)
                }
            )
        } else if (showStories) {
            val stories = (storiesState as? Resource.Success)?.data ?: emptyList()
            StoryScreen(stories = stories, onBack = { onShowStories(false) })
        } else if (showContactPicker) {
            ContactPickerScreen(
                viewModel = viewModel,
                onBack = { onShowContactPicker(false) },
                onContactSelected = { contact ->
                    val newChat = ChatMessage(
                        id = contact.id,
                        senderName = contact.displayName,
                        lastMessage = "",
                        time = "",
                        avatarColor = Color.Gray
                    )
                    onChatSelect(newChat)
                    onShowContactPicker(false)
                },
                onInviteClicked = { phoneNumber ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("sms:$phoneNumber")
                        putExtra("sms_body", "Hey! Download FirstApp to chat with me: https://firstapp.com/download")
                    }
                    context.startActivity(intent)
                }
            )
        } else if (showJobProfile) {
            val profileData by viewModel.jobProfile.collectAsState()
            JobProfileScreen(
                initialData = profileData,
                onClose = { onShowJobProfile(false) },
                onSubmit = { profile ->
                    viewModel.updateJobProfile(profile)
                    onShowJobProfile(false)
                }
            )
        } else if (showApplications) {
            JobApplicationsScreen(
                applications = applications,
                onBack = { onShowApplications(false) },
                onViewJobDetails = { jobId ->
                    val jobs = (jobsState as? Resource.Success)?.data
                    val job = jobs?.find { it.id == jobId }
                    if (job != null) {
                        onJobSelect(job)
                        onShowApplications(false)
                    } else {
                        onJobSelect(Job(id = jobId, title = "Job Details", companyName = "Unknown", location = "Remote", logoColor = Color.Gray))
                        onShowApplications(false)
                    }
                },
                onWithdrawApplication = { viewModel.withdrawApplication(it) },
                onContactRecruiter = { application ->
                    val contactChat = ChatMessage(
                        id = application.id + 5000,
                        senderName = "${application.companyName} Recruiter",
                        lastMessage = "Regarding your application for ${application.jobTitle}",
                        time = "now",
                        avatarColor = application.logoColor
                    )
                    onChatSelect(contactChat)
                    onShowApplications(false)
                },
                onSetReminder = { application ->
                    android.widget.Toast.makeText(context, "Reminder set for ${application.jobTitle}", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else if (selectedPost != null) {
            PostDetailContent(
                post = selectedPost,
                onBack = { onPostSelect(null) },
                onReplySubmit = { parentReplyId, content ->
                    viewModel.createPostReply(selectedPost.id, content, parentReplyId)
                },
                onReplyLike = { replyId ->
                    viewModel.likePostReply(replyId)
                },
                onMediaClick = { _, _ -> }
            )
        } else if (selectedJob != null) {
            JobDetailScreen(
                job = selectedJob,
                onBack = { onJobSelect(null) },
                onApply = {
                    viewModel.applyForJob(selectedJob.id)
                    android.widget.Toast.makeText(context, "Application submitted", android.widget.Toast.LENGTH_SHORT).show()
                    onJobSelect(null)
                    onShowApplications(true)
                }
            )
        } else if (selectedChannel != null) {
            ChannelDetailScreen(channel = selectedChannel, onBack = { onChannelSelect(null) })
        } else if (selectedAdvert != null) {
            AdvertDetailScreen(advert = selectedAdvert, onBack = { onAdvertSelect(null) })
        } else if (showAdvertViewer && adsToView.isNotEmpty()) {
            AdvertViewer(
                adverts = adsToView,
                initialIndex = viewerInitialIndex,
                onAdvertViewed = onAdvertViewed,
                onFinish = { 
                    onShowAdvertViewer(false)
                    onSetAdsToView(emptyList())
                },
                onClose = { 
                    onShowAdvertViewer(false)
                    onSetAdsToView(emptyList())
                }
            )
        } else {
            val bottomItems = listOf("Chats", "Media", "Music", "Drive", "Shorts")
            val bottomIcons = listOf(
                Icons.Filled.Chat,
                Icons.Filled.PermMedia,
                Icons.Filled.MusicNote,
                Icons.Filled.DirectionsCar,
                Icons.Filled.PlayArrow
            )
            val tabs = listOf("Chats", "Jobs", "Adverts", "Channels")

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = {
                    if (selectedBottomItem == 0 && selectedTabItem == 0) {
                        Column {
                            SmallFloatingActionButton(
                                onClick = { onShowStories(true) },
                                containerColor = AppSecondary,
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Stories")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            FloatingActionButton(
                                onClick = { onShowContactPicker(true) },
                                containerColor = AppAccent,
                                contentColor = AppBackground,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
                            }
                        }
                    }
                },
                topBar = {
                    if (selectedBottomItem == 0 || selectedBottomItem == 1) {
                        Column(
                            modifier = Modifier
                                .background(AppBackground)
                                .statusBarsPadding()
                        ) {
                            if (isSearchActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { 
                                        isSearchActive = false
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
                                    }
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Search chats, people...", color = AppTextSecondary) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = AppAccent,
                                            focusedTextColor = AppTextPrimary,
                                            unfocusedTextColor = AppTextPrimary
                                        ),
                                        singleLine = true
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AppTextPrimary)
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedPost != null) {
                                        IconButton(onClick = { onPostSelect(null) }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
                                        }
                                        Text(
                                            text = "Post",
                                            color = AppTextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        if (selectedBottomItem == 1) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF3A4B57)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = if (selectedBottomItem == 0 && selectedTabItem != 0) tabs[selectedTabItem] else "FirstApp",
                                                color = AppTextPrimary,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    
                                    if (selectedPost == null) {
                                        IconButton(onClick = { 
                                            if (selectedBottomItem == 0) {
                                                isSearchActive = true
                                            }
                                        }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary)
                                        }
                                        // Direct Messages icon
                                        if (selectedBottomItem == 1) {
                                            Box {
                                                IconButton(onClick = { onShowDirectMessages(true) }) {
                                                    Icon(Icons.Default.MailOutline, contentDescription = "Direct Messages", tint = AppTextPrimary)
                                                }
                                                // Unread DM badge
                                                val totalUnread = (chatsState as? Resource.Success)?.data?.sumOf { it.unreadCount } ?: 0
                                                if (totalUnread > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFE53935))
                                                            .align(Alignment.TopEnd)
                                                            .padding(end = 2.dp, top = 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (totalUnread > 99) "99" else totalUnread.toString(),
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        IconButton(onClick = { }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppTextPrimary)
                                        }
                                        Box {
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextPrimary)
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                modifier = Modifier.background(AppSecondary)
                                            ) {
                                                if (selectedBottomItem == 0 && selectedTabItem == 1) {
                                                    DropdownMenuItem(
                                                        text = { Text("Job Profile", color = Color.White) },
                                                        onClick = {
                                                            showMenu = false
                                                            onShowJobProfile(true)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Application History", color = Color.White) },
                                                        onClick = {
                                                            showMenu = false
                                                            onShowApplications(true)
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text("Logout", color = Color.White) },
                                                    onClick = {
                                                        showMenu = false
                                                        onLogout()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedBottomItem == 0) {
                                TabRow(
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
                                                    color = if (selectedTabItem == index) Color.White else Color.White.copy(alpha = 0.6f),
                                                    fontWeight = if (selectedTabItem == index) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        )
                                    }
                                }
                            } else if (selectedBottomItem == 1) {
                                val mediaTabs = listOf("You", "Following", "Trending")
                                TabRow(
                                    selectedTabIndex = selectedMediaFeedTab,
                                    containerColor = AppBackground,
                                    contentColor = AppAccent,
                                    divider = {},
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[selectedMediaFeedTab]),
                                            color = AppAccent
                                        )
                                    }
                                ) {
                                    mediaTabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedMediaFeedTab == index,
                                            onClick = { onMediaFeedTabSelect(index) },
                                            text = {
                                                Text(
                                                    text = title,
                                                    color = if (selectedMediaFeedTab == index) Color.White else Color.White.copy(alpha = 0.6f),
                                                    fontWeight = if (selectedMediaFeedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Column {
                        // Global Mini Player
                        currentlyPlaying?.let { song ->
                            if (!isNowPlayingVisible) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSetNowPlayingVisible(true) },
                                    color = Color(0xFF1E2A30),
                                    tonalElevation = 8.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp).fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(song.albumArtColor)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                                                Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
                                            }
                                            IconButton(onClick = { musicViewModel.togglePlayPause() }) {
                                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        // Mini Progress
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth(progress)
                                                .height(2.dp)
                                                .background(AppAccent)
                                        )
                                    }
                                }
                            }
                        }

                        NavigationBar(
                            containerColor = AppSecondary,
                            tonalElevation = 8.dp
                        ) {
                            bottomItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = { 
                                        Icon(
                                            imageVector = bottomIcons[index], 
                                            contentDescription = item
                                        ) 
                                    },
                                    label = { 
                                        Text(
                                            text = item,
                                            fontWeight = if (selectedBottomItem == index) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    selected = selectedBottomItem == index,
                                    onClick = { onBottomItemSelect(index) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                        selectedTextColor = Color.White,
                                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                        indicatorColor = Color(0xFF374045)
                                    )
                                )
                            }
                        }
                    }
                },
                containerColor = AppBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(if (selectedBottomItem == 2 || selectedBottomItem == 3 || selectedBottomItem == 4) PaddingValues(0.dp) else innerPadding)
                        .fillMaxSize()
                ) {
                    when (selectedBottomItem) {
                        0 -> when (selectedTabItem) {
                            0 -> DataStateHandler(chatsState) { chats ->
                                if (isSearchActive && searchQuery.isNotBlank() && searchResults != null) {
                                    SearchResultsList(
                                        searchResults = searchResults!!,
                                        onChatClick = { onChatSelect(it) },
                                        onContactClick = { contact ->
                                            val chat = ChatMessage(
                                                id = contact.id,
                                                senderName = contact.displayName,
                                                lastMessage = "",
                                                time = "",
                                                avatarColor = Color.Gray
                                            )
                                            onChatSelect(chat)
                                        }
                                    )
                                } else {
                                    ChatList(chats) { onChatSelect(it) }
                                }
                            }
                            1 -> DataStateHandler(jobsState) { jobs ->
                                val filteredJobs = if (searchQuery.isEmpty()) {
                                    jobs
                                } else {
                                    jobs.filter { job ->
                                        job.title.contains(searchQuery, ignoreCase = true) ||
                                        job.companyName.contains(searchQuery, ignoreCase = true) ||
                                        job.location.contains(searchQuery, ignoreCase = true)
                                    }
                                }
                                JobList(filteredJobs) { onJobSelect(it) }
                            }
                            2 -> DataStateHandler(advertsState) { adverts ->
                                AdvertList(
                                    adverts = adverts,
                                    onAdvertClick = { onAdvertSelect(it) },
                                    onHeaderClick = { clickedAd ->
                                        val unviewed = adverts.filter { !it.isViewed }
                                        val startIndex = unviewed.indexOf(clickedAd).coerceAtLeast(0)
                                        onSetAdsToView(unviewed)
                                        onSetViewerInitialIndex(startIndex)
                                        onShowAdvertViewer(true)
                                    }
                                )
                            }
                            3 -> DataStateHandler(channelsState) { channels ->
                                ChannelList(channels) { onChannelSelect(it) }
                            }
                        }
                        1 -> DataStateHandler(postsState) { posts ->
                            MediaScreen(
                                posts = posts,
                                mediaFeedTab = selectedMediaFeedTab,
                                selectedPost = selectedPost,
                                onPostSelect = onPostSelect,
                                onCreatePost = { content, mediaUrls ->
                                    viewModel.createPost(content, mediaUrls)
                                },
                                onLikePost = { postId ->
                                    viewModel.likePost(postId)
                                },
                                onReplySubmit = { postId, parentReplyId, content ->
                                    viewModel.createPostReply(postId, content, parentReplyId)
                                },
                                onReplyLike = { replyId ->
                                    viewModel.likePostReply(replyId)
                                }
                            )
                        }
                        2 -> DataStateHandler(songsState) { songs ->
                            MusicScreen(songs, musicViewModel)
                        }
                        3 -> DriveScreen(bottomPadding = innerPadding.calculateBottomPadding())
                        4 -> DataStateHandler(shortsState) { shorts ->
                            ShortsList(shorts, viewModel)
                        }
                    }
                }
            }
        }

        // Persistent Full Player Overlay
        AnimatedVisibility(
            visible = isNowPlayingVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentlyPlaying?.let { song ->
                NowPlayingScreen(
                    song = song,
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    progress = progress,
                    isLiked = likedSongs.contains(song.id),
                    onClose = { onSetNowPlayingVisible(false) },
                    onTogglePlay = { musicViewModel.togglePlayPause() },
                    onToggleShuffle = { musicViewModel.toggleShuffle() },
                    onToggleRepeat = { musicViewModel.toggleRepeat() },
                    onPrevious = { musicViewModel.skipPrevious() },
                    onNext = { musicViewModel.skipNext() },
                    onSeek = { musicViewModel.seekTo(it) },
                    onToggleLike = { musicViewModel.toggleLike(song.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsList(shorts: List<com.example.firstapp.model.Short>, viewModel: MainViewModel) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { shorts.size })
    androidx.compose.foundation.pager.VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ShortItem(
            short = shorts[page],
            isVisible = pagerState.currentPage == page,
            onLikeClick = { viewModel.likeShort(it) },
            onSubscribeClick = { viewModel.subscribeToShortCreator(it) }
        )
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
fun SearchResultsList(
    searchResults: SearchResult,
    onChatClick: (ChatMessage) -> Unit,
    onContactClick: (com.example.firstapp.model.Contact) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (searchResults.chats.isNotEmpty()) {
            item {
                Text(
                    "Chats",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = AppAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            items(searchResults.chats) { chat ->
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
        if (searchResults.contacts.isNotEmpty()) {
            item {
                Text(
                    "People",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = AppAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            items(searchResults.contacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (contact.isVerified) AppAccent else Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(contact.displayName, color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                        if (contact.phoneNumber.isNotBlank()) {
                            Text(contact.phoneNumber, color = AppTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF233138)
                )
            }
        }
        if (searchResults.chats.isEmpty() && searchResults.contacts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No results found", color = AppTextSecondary)
                }
            }
        }
    }
}

@Composable
fun ChatList(chats: List<ChatMessage>, onChatClick: (ChatMessage) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
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
fun AdvertList(adverts: List<Advert>, onAdvertClick: (Advert) -> Unit, onHeaderClick: (Advert) -> Unit) {
    val unviewedAds = adverts.filter { !it.isViewed }
    val viewedAds = adverts.filter { it.isViewed }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (unviewedAds.isNotEmpty()) {
            item {
                Text(
                    "Recent ads",
                    modifier = Modifier.padding(16.dp),
                    color = AppAccent,
                    fontWeight = FontWeight.Bold
                )
            }
            items(unviewedAds) { advert ->
                Column(modifier = Modifier.clickable { onHeaderClick(advert) }) {
                    AdvertItem(advert)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 88.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFF233138)
                    )
                }
            }
        }

        if (viewedAds.isNotEmpty()) {
            item {
                Text(
                    "Viewed ads",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(viewedAds) { advert ->
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
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FirstAppTheme {
        MainContent(
            viewModel = viewModel(),
            selectedBottomItem = 0,
            onBottomItemSelect = {},
            selectedTabItem = 0,
            onTabItemSelect = {},
            selectedMediaFeedTab = 0,
            onMediaFeedTabSelect = {},
            selectedChat = null,
            onChatSelect = {},
            selectedJob = null,
            onJobSelect = {},
            selectedPost = null,
            onPostSelect = {},
            selectedChannel = null,
            onChannelSelect = {},
            selectedAdvert = null,
            onAdvertSelect = {},
            showAdvertViewer = false,
            onShowAdvertViewer = {},
            adsToView = emptyList(),
            onSetAdsToView = {},
            viewerInitialIndex = 0,
            onSetViewerInitialIndex = {},
            onAdvertViewed = {},
            showStories = false,
            onShowStories = {},
            chatsState = Resource.Success(com.example.firstapp.data.ChatService.dummyChats),
            jobsState = Resource.Success(emptyList()),
            advertsState = Resource.Success(emptyList()),
            channelsState = Resource.Success(emptyList()),
            shortsState = Resource.Success(emptyList()),
            songsState = Resource.Success(emptyList()),
            postsState = Resource.Success(emptyList()),
            storiesState = Resource.Success(emptyList()),
            onLogout = {},
            showContactPicker = false,
            onShowContactPicker = {},
            showJobProfile = false,
            onShowJobProfile = {},
            showApplications = false,
            onShowApplications = {},
            applications = emptyList(),
            showDirectMessages = false,
            onShowDirectMessages = {},
            musicViewModel = viewModel(),
            currentlyPlaying = null,
            isPlaying = false,
            progress = 0f,
            isShuffleEnabled = false,
            repeatMode = 0,
            likedSongs = emptySet(),
            isNowPlayingVisible = false,
            onSetNowPlayingVisible = {}
        )
    }
}
