package com.example.firstapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.firstapp.model.*
import com.example.firstapp.ui.components.*
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.viewmodel.MusicViewModel
import com.example.firstapp.util.Resource
import com.example.firstapp.ui.navigation.Screen

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    navController: NavController,
    initialChatId: Int? = null
) {
    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        MainScreenContent(onLogout, viewModel, musicViewModel, navController, initialChatId)
    }
}

@Composable
private fun MainScreenContent(
    onLogout: () -> Unit,
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    navController: NavController,
    initialChatId: Int? = null
) {
    var selectedBottomItem by remember { mutableIntStateOf(0) }
    var selectedTabItem by remember { mutableIntStateOf(0) }
    var selectedMediaFeedTab by remember { mutableIntStateOf(0) }

    val incomingCall by com.example.firstapp.data.realtime.GlobalCallState.incomingCall.collectAsState()
    val autoAcceptSessionId by com.example.firstapp.data.realtime.GlobalCallState.autoAcceptSessionId.collectAsState()
    val activeSessionId by com.example.firstapp.data.realtime.GlobalCallState.activeSessionId.collectAsState()
    val activeParticipantId by com.example.firstapp.data.realtime.GlobalCallState.activeParticipantId.collectAsState()
    
    val chatsState by viewModel.chats.collectAsState()
    val chatsForOverlay = (chatsState as? Resource.Success)?.data ?: emptyList()

    LaunchedEffect(autoAcceptSessionId, incomingCall?.senderId) {
        val sessionId = autoAcceptSessionId ?: return@LaunchedEffect
        val call = incomingCall ?: return@LaunchedEffect
        if (call.sessionId == sessionId) {
            navController.navigate(Screen.Chat.createRoute(call.senderId))
        }
    }

    // Handle returning to an active call from notification
    LaunchedEffect(activeSessionId, activeParticipantId) {
        if (activeSessionId != null && activeParticipantId != null) {
            // Check if we are currently at the top of the stack and NOT in ChatScreen
            // This is a bit tricky with NavController, but we can at least try to navigate
            // if we are on the Main route.
            if (navController.currentDestination?.route == Screen.Main.route) {
                 navController.navigate(Screen.Chat.createRoute(activeParticipantId!!))
            }
        }
    }

    var showAdvertViewer by remember { mutableStateOf(false) }
    var adsToView by remember { mutableStateOf<List<Advert>>(emptyList()) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }
    
    var showMoreSheet by remember { mutableStateOf(false) }
    var createPostTrigger by remember { mutableIntStateOf(0) }

    val jobsState by viewModel.jobs.collectAsState()
    val advertsState by viewModel.adverts.collectAsState()
    val channelsState by viewModel.channels.collectAsState()
    val storiesState by viewModel.stories.collectAsState()
    val shortsState by viewModel.shorts.collectAsState()
    val songsState by viewModel.songs.collectAsState()
    val postsState by viewModel.posts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val currentlyPlaying by musicViewModel.currentlyPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.progress.collectAsState()
    var isNowPlayingVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        musicViewModel.initPlayer(context)
    }

    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableIntStateOf(-1) }
    var activeShortForMoreMenu by remember { mutableStateOf<com.example.firstapp.model.Short?>(null) }

    val shortRepliesState by viewModel.shortReplies.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MainContent(
            viewModel = viewModel,
            navController = navController,
            selectedBottomItem = selectedBottomItem,
            onBottomItemSelect = { selectedBottomItem = it },
            selectedTabItem = selectedTabItem,
            onTabItemSelect = { selectedTabItem = it },
            selectedMediaFeedTab = selectedMediaFeedTab,
            onMediaFeedTabSelect = { selectedMediaFeedTab = it },
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
            storiesState = storiesState,
            shortsState = shortsState,
            songsState = songsState,
            postsState = postsState,
            onLogout = onLogout,
            currentUser = currentUser,
            showMoreSheet = showMoreSheet,
            onShowMoreSheet = { showMoreSheet = it },
            musicViewModel = musicViewModel,
            currentlyPlaying = currentlyPlaying,
            isPlaying = isPlaying,
            progress = progress,
            isNowPlayingVisible = isNowPlayingVisible,
            onSetNowPlayingVisible = { isNowPlayingVisible = it },
            createPostTrigger = createPostTrigger,
            onCreatePostTrigger = { createPostTrigger++ },
            onShowShortComments = { id ->
                activeShortIdForComments = id
                viewModel.fetchShortReplies(id)
                showShortComments = true
            },
            onShowShortMoreMenu = { short ->
                activeShortForMoreMenu = short
                showShortMoreMenu = true
            },
            onSearchShorts = { query ->
                viewModel.searchShorts(query)
            }
        )

        if (showShortComments && activeShortIdForComments != -1) {
            ShortCommentsSheet(
                shortId = activeShortIdForComments,
                repliesState = shortRepliesState,
                onDismiss = {
                    showShortComments = false
                    activeShortIdForComments = -1
                    viewModel.clearActiveShortReplies()
                },
                onSendReply = { content ->
                    viewModel.replyToShort(activeShortIdForComments, content)
                }
            )
        }

        activeShortForMoreMenu?.let { short ->
            if (showShortMoreMenu) {
                ShortMoreMenuSheet(
                    short = short,
                    onDismiss = {
                        showShortMoreMenu = false
                        activeShortForMoreMenu = null
                    },
                    onAction = { action ->
                        Toast.makeText(context, "Action: $action", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showAdvertViewer) {
            AdvertViewer(
                adverts = adsToView,
                initialIndex = viewerInitialIndex,
                onAdvertViewed = { viewModel.markAdvertAsViewed(it) },
                onFinish = { showAdvertViewer = false },
                onClose = { showAdvertViewer = false }
            )
        }

        val isCallActive by com.example.firstapp.data.realtime.GlobalCallState.isCallActive.collectAsState()

        incomingCall?.let { call ->
            if (!isCallActive) {
                val callerMsg = chatsForOverlay.firstOrNull { it.id == call.senderId }
                val callerName = callerMsg?.senderName ?: "User ${call.senderId}"
                val callLabel = if (call.callType == com.example.firstapp.data.realtime.CallType.VIDEO) "Incoming video call" else "Incoming voice call"

                Box(
                    modifier = Modifier.fillMaxSize().background(color = Color(0xDD000000)).zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(callLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(callerName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = {
                                        com.example.firstapp.data.realtime.CallWebSocketClient.reject(call.senderId, call.sessionId, call.callType)
                                        com.example.firstapp.data.realtime.GlobalCallState.clear()
                                    },
                                    containerColor = Color.Red
                                ) { Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White) }
                                Spacer(Modifier.height(8.dp))
                                Text("Decline", color = Color.White, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { navController.navigate(Screen.Chat.createRoute(call.senderId)) },
                                    containerColor = AppAccent
                                ) { Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White) }
                                Spacer(Modifier.height(8.dp))
                                Text("Accept", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: MainViewModel,
    navController: NavController,
    selectedBottomItem: Int,
    onBottomItemSelect: (Int) -> Unit,
    selectedTabItem: Int,
    onTabItemSelect: (Int) -> Unit,
    selectedMediaFeedTab: Int,
    onMediaFeedTabSelect: (Int) -> Unit,
    showAdvertViewer: Boolean,
    onShowAdvertViewer: (Boolean) -> Unit,
    adsToView: List<Advert>,
    onSetAdsToView: (List<Advert>) -> Unit,
    viewerInitialIndex: Int,
    onSetViewerInitialIndex: (Int) -> Unit,
    onAdvertViewed: (Int) -> Unit,
    chatsState: Resource<List<ChatMessage>>,
    jobsState: Resource<List<Job>>,
    advertsState: Resource<List<Advert>>,
    channelsState: Resource<List<Channel>>,
    storiesState: Resource<List<UserStory>>,
    shortsState: Resource<List<com.example.firstapp.model.Short>>,
    songsState: Resource<List<Song>>,
    postsState: Resource<List<Post>>,
    onLogout: () -> Unit,
    currentUser: User?,
    showMoreSheet: Boolean,
    onShowMoreSheet: (Boolean) -> Unit,
    musicViewModel: MusicViewModel,
    currentlyPlaying: Song?,
    isPlaying: Boolean,
    progress: Float,
    isNowPlayingVisible: Boolean,
    onSetNowPlayingVisible: (Boolean) -> Unit,
    createPostTrigger: Int,
    onCreatePostTrigger: () -> Unit,
    onShowShortComments: (Int) -> Unit,
    onShowShortMoreMenu: (com.example.firstapp.model.Short) -> Unit,
    onSearchShorts: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isShortsSearchActive by remember { mutableStateOf(false) }
    var shortsSearchQuery by remember { mutableStateOf("") }
    var isMediaProfileActive by remember { mutableStateOf(false) }
    var cachedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(songsState) {
        val latestSongs = (songsState as? Resource.Success<List<Song>>)?.data.orEmpty()
        if (latestSongs.isNotEmpty()) cachedSongs = latestSongs
    }

    val mediaSongs = (songsState as? Resource.Success<List<Song>>)?.data?.takeIf { it.isNotEmpty() } ?: cachedSongs
    
    val onBackAction = {
        if (isNowPlayingVisible) onSetNowPlayingVisible(false)
        else if (isShortsSearchActive) { isShortsSearchActive = false; shortsSearchQuery = ""; viewModel.fetchShorts() }
        else if (isSearchActive) { isSearchActive = false; searchQuery = ""; viewModel.search("") }
        else if (showMoreSheet) onShowMoreSheet(false)
        else if (selectedBottomItem != 0) onBottomItemSelect(0)
        else { navController.popBackStack() }
        Unit
    }

    androidx.activity.compose.BackHandler(enabled = isNowPlayingVisible || isShortsSearchActive || isSearchActive || showMoreSheet || selectedBottomItem != 0, onBack = onBackAction)

    val bottomItems = listOf("Home", "Media", "Shorts", "Music", "Drive", "More")
    val bottomIcons = listOf(Icons.Filled.Home, Icons.Filled.PermMedia, Icons.Filled.PlayArrow, Icons.Filled.MusicNote, Icons.Filled.DirectionsCar, Icons.Filled.MoreHoriz)
    val tabs = listOf("Home", "Jobs", "Adverts", "Channels")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (selectedBottomItem == 0 || (selectedBottomItem == 1 && !isMediaProfileActive) || selectedBottomItem == 2) {
                Column(modifier = Modifier.background(if (selectedBottomItem == 2) Color.Transparent else AppBackground).statusBarsPadding()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isSearchActive && selectedBottomItem == 0) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Search chats, contacts, messages...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = AppAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                leadingIcon = {
                                    IconButton(onClick = { isSearchActive = false; searchQuery = ""; viewModel.search("") }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            if (selectedBottomItem == 0) {
                                // Profile Picture
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppSecondary)
                                        .clickable { navController.navigate(Screen.Story.route) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val profileImage = currentUser?.avatar_url ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400"
                                    coil.compose.AsyncImage(
                                        model = profileImage,
                                        contentDescription = "Profile",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val titleText = when (selectedBottomItem) { 1 -> "Media"; 2 -> "Shorts"; 3 -> "Music"; 4 -> "Drive"; else -> "Home" }
                                Text(text = titleText, color = if (selectedBottomItem == 2) Color.White else AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                            
                            if (selectedBottomItem == 0 || selectedBottomItem == 1) {
                                IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary) }
                                if (selectedBottomItem == 0) {
                                    Box {
                                        var showMainMenu by remember { mutableStateOf(false) }
                                        IconButton(onClick = { showMainMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextPrimary) }
                                        DropdownMenu(
                                            expanded = showMainMenu,
                                            onDismissRequest = { showMainMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Contacts", color = Color.White) },
                                                onClick = { 
                                                    showMainMenu = false
                                                    navController.navigate(Screen.ContactPicker.route)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppAccent) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("New Group", color = Color.White) },
                                                onClick = { 
                                                    showMainMenu = false
                                                    navController.navigate(Screen.CreateGroup.route)
                                                },
                                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = AppAccent) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Starred Messages", color = Color.White) },
                                                onClick = { showMainMenu = false },
                                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Settings", color = Color.White) },
                                                onClick = { showMainMenu = false; navController.navigate(Screen.Settings.route) },
                                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray) }
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(onClick = { onCreatePostTrigger() }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AppAccent)
                                    }
                                }
                            } else {
                                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppTextPrimary) }
                            }
                        }
                    }
                    if (selectedBottomItem == 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTabItem == index
                                Surface(
                                    onClick = { onTabItemSelect(index) },
                                    color = if (isSelected) AppAccent else AppSecondary,
                                    shape = RoundedCornerShape(24.dp),
                                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Text(
                                        text = title,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        color = if (isSelected) AppBackground else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (selectedBottomItem == 1 && !isMediaProfileActive) {
                        val mediaTabs = listOf("My Feed", "Following", "Trending")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mediaTabs.forEachIndexed { index, title ->
                                val isSelected = selectedMediaFeedTab == index
                                Surface(
                                    onClick = { onMediaFeedTabSelect(index) },
                                    color = if (isSelected) AppAccent else AppSecondary,
                                    shape = RoundedCornerShape(24.dp),
                                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Text(
                                        text = title,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        color = if (isSelected) AppBackground else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(AppSecondary)
                    .navigationBarsPadding()
                    .onGloballyPositioned { bottomBarHeight = with(density) { it.size.height.toDp() } }
            ) {
                currentlyPlaying?.let { song ->
                    if (!isNowPlayingVisible) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSetNowPlayingVisible(true) }, 
                            color = AppSecondary, 
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp).fillMaxSize(), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(song.albumArtColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) { 
                                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                                    Text(song.artist, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1) 
                                }
                                IconButton(onClick = { musicViewModel.togglePlayPause() }) { 
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                        contentDescription = null, 
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    ) 
                                }
                            }
                        }
                    }
                }
                NavigationBar(
                    containerColor = AppSecondary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    bottomItems.forEachIndexed { index, item ->
                        val isMore = item == "More"
                        val isSelected = if (isMore) showMoreSheet else (selectedBottomItem == index && !showMoreSheet)
                        
                        val unreadCount = if (item == "Home") {
                            chatsState.data?.sumOf { it.unreadCount } ?: 0
                        } else 0

                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(containerColor = AppAccent) {
                                                Text(
                                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                    color = AppBackground
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = bottomIcons[index], 
                                        contentDescription = item,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = { 
                                Text(
                                    text = item, 
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ) 
                            },
                            selected = isSelected,
                            onClick = { if (isMore) onShowMoreSheet(!showMoreSheet) else { onBottomItemSelect(index); onShowMoreSheet(false) } },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppAccent,
                                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                                selectedTextColor = AppAccent,
                                unselectedTextColor = Color.White.copy(alpha = 0.4f),
                                indicatorColor = AppAccent.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(if (selectedBottomItem >= 2) PaddingValues(0.dp) else innerPadding).fillMaxSize()) {
                when (selectedBottomItem) {
                    0 -> when (selectedTabItem) {
                        0 -> {
                            val searchResults by viewModel.searchResults.collectAsState()
                            val allChatsRes = chatsState
                            val allChats = allChatsRes.data ?: emptyList()
                            
                            if (isSearchActive && searchQuery.isNotEmpty()) {
                                SearchResultsView(
                                    results = searchResults,
                                    onChatClick = { chat: ChatMessage ->
                                        if (!allChats.any { it.id == chat.id }) {
                                            viewModel.createChat(chat.id)
                                        }
                                        navController.navigate(Screen.Chat.createRoute(chat.id))
                                    },
                                    onMessageClick = { msg: Message ->
                                        navController.navigate(Screen.Chat.createRoute(msg.senderId ?: 0))
                                    }
                                )
                            } else {
                                if (allChatsRes is Resource.Loading && allChats.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppAccent) }
                                } else if (allChatsRes is Resource.Error && allChats.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { Text(text = allChatsRes.message ?: "Failed to load chats", color = Color.Red, textAlign = TextAlign.Center) }
                                } else {
                                    ChatList(
                                        allChats,
                                        onStatusClick = { navController.navigate(Screen.Story.route) }
                                    ) { chat ->
                                        navController.navigate(Screen.Chat.createRoute(chat.id))
                                    }
                                }
                            }
                        }
                        1 -> DataStateHandler(jobsState) { jobs -> JobList(jobs) { navController.navigate(Screen.JobDetail.createRoute(it.id)) } }
                        2 -> DataStateHandler(advertsState) { ads -> AdvertList(ads, { navController.navigate(Screen.AdvertDetail.createRoute(it.id)) }, { /* handle header */ }) }
                        3 -> DataStateHandler(channelsState) { channels -> ChannelList(channels) { navController.navigate(Screen.ChannelDetail.createRoute(it.id)) } }
                    }
                    1 -> DataStateHandler(postsState) { posts -> MediaScreen(posts = posts, songs = mediaSongs, mediaFeedTab = selectedMediaFeedTab, selectedPost = null, onPostSelect = { it?.let { p -> /* handle select if needed */ } }, onCreatePost = { c, m -> viewModel.createPost(c, m) }, onLikePost = { viewModel.likePost(it) }, onRepostPost = { viewModel.repostPost(it) }, onReplySubmit = { p1, p2, c -> viewModel.createPostReply(p1, c, p2) }, onReplyLike = { viewModel.likePostReply(it) }, onReplyRepost = { viewModel.repostPostReply(it) }, onBookmarkPost = { viewModel.bookmarkPost(it) }, onBookmarkReply = { viewModel.bookmarkPostReply(it) }, onViewPost = { viewModel.viewPost(it) }, onProfileViewChange = { isMediaProfileActive = it }, onPlaySong = { s, q -> musicViewModel.playSong(s, q) }, onCreatePostClick = createPostTrigger) }
                    2 -> DataStateHandler(shortsState) { shorts -> ShortsList(shorts = shorts, viewModel = viewModel, onRetry = { viewModel.fetchShorts() }, bottomPadding = bottomBarHeight, onShowComments = onShowShortComments, onShowMoreMenu = onShowShortMoreMenu) }
                    3 -> DataStateHandler(songsState) { songs -> MusicScreen(songs = songs, viewModel = musicViewModel, onShareSong = { /* share */ }, onShowSettings = { navController.navigate(Screen.Settings.route) }) }
                    4 -> DriveScreen(bottomPadding = innerPadding.calculateBottomPadding(), onShowSettings = { navController.navigate(Screen.Settings.route) })
                }
            }
            if (showMoreSheet) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onShowMoreSheet(false) }.zIndex(4f))
            AnimatedVisibility(
                visible = showMoreSheet, 
                enter = slideInVertically { it } + fadeIn(), 
                exit = fadeOut() + slideOutVertically { it }, 
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = innerPadding.calculateBottomPadding() + 8.dp).zIndex(5f)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), 
                    shape = RoundedCornerShape(28.dp), 
                    color = AppSecondary, 
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shortcuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            IconButton(onClick = { onShowMoreSheet(false) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        val moreItems = listOf(
                            Triple("Vocab", Icons.Default.Language, AppPrimary),
                            Triple("Files", Icons.Default.Description, Color(0xFF2196F3)),
                            Triple("Calendar", Icons.Default.CalendarToday, Color(0xFFE91E63)),
                            Triple("Direct Messages", Icons.AutoMirrored.Filled.Chat, AppPrimary),
                            Triple("Job Profile", Icons.Default.Work, AppAccent),
                            Triple("Applications", Icons.Default.Assignment, AppPrimary),
                            Triple("Places", Icons.Default.Place, Color(0xFFF44336)),
                            Triple("Saved", Icons.Default.Bookmark, Color(0xFFFFC107)),
                            Triple("Settings", Icons.Default.Settings, Color.Gray)
                        )
                        
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3), 
                            verticalArrangement = Arrangement.spacedBy(24.dp), 
                            horizontalArrangement = Arrangement.spacedBy(16.dp), 
                            modifier = Modifier.height(380.dp)
                        ) {
                            items(moreItems.size) { idx ->
                                val (label, icon, color) = moreItems[idx]
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, 
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        onShowMoreSheet(false)
                                        when (label) {
                                            "Vocab" -> navController.navigate(Screen.Vocab.route)
                                            "Files" -> navController.navigate(Screen.Files.route)
                                            "Calendar" -> navController.navigate(Screen.Calendar.route)
                                            "Direct Messages" -> navController.navigate(Screen.DirectMessages.route)
                                            "Job Profile" -> navController.navigate(Screen.JobProfile.route)
                                            "Applications" -> navController.navigate(Screen.Applications.route)
                                            "Settings" -> navController.navigate(Screen.Settings.route)
                                            else -> Toast.makeText(context, "Clicked $label", Toast.LENGTH_SHORT).show()
                                        }
                                    }.padding(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(60.dp), 
                                        shape = RoundedCornerShape(18.dp), 
                                        color = color.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                                    ) { 
                                        Box(contentAlignment = Alignment.Center) { 
                                            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp)) 
                                        } 
                                    }
                                    Spacer(modifier = Modifier.height(10.dp)); 
                                    Text(
                                        text = label, 
                                        fontSize = 12.sp, 
                                        fontWeight = FontWeight.SemiBold, 
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> DataStateHandler(resource: Resource<T>, onSuccess: @Composable (T) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (resource) {
            is Resource.Loading -> {
                if (resource.data != null && (resource.data as? List<*>)?.isNotEmpty() == true) {
                    onSuccess(resource.data)
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                        color = AppAccent,
                        trackColor = Color.Transparent
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        repeat(10) {
                            SkeletonItem()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            is Resource.Error -> {
                if (resource.data != null && (resource.data as? List<*>)?.isNotEmpty() == true) {
                    onSuccess(resource.data)
                    // Show a subtle error banner at the top
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = "Offline Mode: ${resource.message}",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(text = resource.message ?: "Connection Error", color = Color.White, textAlign = TextAlign.Center)
                            Button(onClick = { /* Could trigger refresh */ }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            is Resource.Success -> {
                if (resource.data != null && (resource.data as? List<*>)?.isNotEmpty() == true) {
                    onSuccess(resource.data)
                } else if (resource.data != null && resource.data !is List<*>) {
                    onSuccess(resource.data)
                } else {
                    EmptyStateView()
                }
            }
        }
    }
}

@Composable
fun SkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Row(modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudQueue, 
                contentDescription = null, 
                tint = Color.Gray.copy(alpha = 0.5f), 
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Everything is quiet here",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Check back later for updates",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ChatList(chats: List<ChatMessage>, onStatusClick: () -> Unit, onChatClick: (ChatMessage) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            // Status/Stories Row at the top of the chat list
            StatusQuickRow(onStatusClick = onStatusClick)
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF233138))
        }
        items(chats) { chat -> Column(modifier = Modifier.clickable { onChatClick(chat) }) { ChatItem(chat); HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138)) } }
    }
}

@Composable
fun StatusQuickRow(onStatusClick: () -> Unit) {
    // This could be populated with real status data later
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStatusClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(54.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = AppSecondary
            ) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(12.dp))
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .background(AppAccent, CircleShape)
                    .border(2.dp, AppBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = AppBackground, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("My status", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Tap to add status update", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SearchResultsView(
    results: com.example.firstapp.data.repository.SearchResult?,
    onChatClick: (ChatMessage) -> Unit,
    onMessageClick: (Message) -> Unit
) {
    if (results == null) return
    
    val allChats = results.chats
    val contacts = results.contacts.filter { contact -> !allChats.any { it.id == contact.id } }
    val messages = results.messages

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (allChats.isNotEmpty()) {
            item { SearchSectionHeader("Chats") }
            items(allChats) { chat ->
                Column(modifier = Modifier.clickable { onChatClick(chat) }) {
                    ChatItem(chat)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }
        
        if (contacts.isNotEmpty()) {
            item { SearchSectionHeader("Contacts") }
            items(contacts) { contact ->
                Column(modifier = Modifier.clickable { 
                    onChatClick(ChatMessage(id = contact.id, senderName = contact.displayName, lastMessage = "Contact: ${contact.username}", time = "", avatarColor = Color.Gray, avatarUrl = contact.avatarUrl))
                }) {
                    ContactSearchItem(contact)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (messages.isNotEmpty()) {
            item { SearchSectionHeader("Messages") }
            items(messages) { message ->
                Column(modifier = Modifier.clickable { onMessageClick(message) }) {
                    MessageSearchItem(message)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }
        
        if (allChats.isEmpty() && contacts.isEmpty() && messages.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        color = AppAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ContactSearchItem(contact: Contact) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (contact.avatarUrl != null) {
                coil.compose.AsyncImage(model = contact.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(contact.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(contact.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("@${contact.username}", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun MessageSearchItem(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(AppPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Message, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(message.senderName ?: "Unknown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(message.text, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(com.example.firstapp.util.formatWhatsAppTime(message.time), color = Color.Gray.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
fun JobList(jobs: List<Job>, onJobClick: (Job) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(jobs) { job -> Column(modifier = Modifier.clickable { onJobClick(job) }) { JobItem(job); HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138)) } }
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (Channel) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels) { ch -> Column(modifier = Modifier.clickable { onChannelClick(ch) }) { ChannelItem(ch); HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138)) } }
    }
}

@Composable
fun AdvertList(adverts: List<Advert>, onAdvertClick: (Advert) -> Unit, onHeaderClick: (Advert) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(adverts) { ad -> Column(modifier = Modifier.clickable { onAdvertClick(ad) }) { AdvertItem(ad); HorizontalDivider(modifier = Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Color(0xFF233138)) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(shorts: List<com.example.firstapp.model.Short>, viewModel: MainViewModel, onRetry: () -> Unit, bottomPadding: Dp, onShowComments: (Int) -> Unit, onShowMoreMenu: (com.example.firstapp.model.Short) -> Unit) {
    if (shorts.isEmpty()) return
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { shorts.size }
    androidx.compose.foundation.pager.VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        ShortItem(short = shorts[page], isVisible = pagerState.currentPage == page, bottomInset = bottomPadding, onLikeClick = { viewModel.likeShort(it) }, onSubscribeClick = { viewModel.subscribeToShortCreator(it) }, onCommentClick = { onShowComments(it) }, onRepostClick = { viewModel.repostShort(it) }, onBookmarkClick = { viewModel.bookmarkShort(it) }, onMoreClick = { onShowMoreMenu(shorts[page]) })
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FirstAppTheme {
        // MainScreen preview would need a mock navController
    }
}
