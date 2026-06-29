package com.example.firstapp.home.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.firstapp.home.ui.viewmodel.*
import com.example.firstapp.home.ui.components.*
import com.example.firstapp.chat.ui.viewmodel.*
import com.example.firstapp.chat.ui.components.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.data.*
import com.example.firstapp.media.ui.viewmodel.*
import com.example.firstapp.media.ui.components.*
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.screens.MediaScreen
import com.example.firstapp.media.ui.screens.AdvertViewer
import com.example.firstapp.media.ui.screens.StoryScreen
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.music.ui.screens.MusicScreen
import com.example.firstapp.music.ui.screens.NowPlayingScreen
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.model.Playlist
import com.example.firstapp.drive.ui.viewmodel.*
import com.example.firstapp.drive.ui.screens.DriveScreen
import com.example.firstapp.jobs.ui.viewmodel.*
import com.example.firstapp.jobs.ui.components.*
import com.example.firstapp.jobs.model.*
import com.example.firstapp.profile.ui.viewmodel.*
import com.example.firstapp.profile.ui.screens.*
import com.example.firstapp.common.model.ConversationType
import java.util.Locale
import com.example.firstapp.common.model.*
import com.example.firstapp.common.model.User
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.ui.viewmodel.FilesViewModel
import com.example.firstapp.common.ui.viewmodel.CalendarViewModel
import com.example.firstapp.common.util.*
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.search.model.SearchFilter
import androidx.compose.foundation.lazy.LazyRow
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.data.api.TrendingTopic
import androidx.paging.compose.collectAsLazyPagingItems

data class ShortcutItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val route: String? = null
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    mediaViewModel: MediaViewModel,
    shortsViewModel: ShortsViewModel,
    advertsViewModel: AdvertsViewModel,
    channelsViewModel: ChannelsViewModel,
    searchViewModel: SearchViewModel,
    jobViewModel: JobViewModel,
    userViewModel: UserViewModel,
    musicViewModel: MusicViewModel,
    storyViewModel: StoryViewModel,
    callViewModel: CallViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    navController: NavController,
    onLogout: () -> Unit,
    initialTab: String = "home"
) {
    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        MainScreenContent(
            chatViewModel,
            mediaViewModel,
            shortsViewModel,
            advertsViewModel,
            channelsViewModel,
            searchViewModel,
            jobViewModel,
            userViewModel,
            musicViewModel,
            storyViewModel,
            callViewModel,
            navController,
            onLogout,
            initialTab
        )
    }
}

@Composable
private fun MainScreenContent(
    chatViewModel: ChatViewModel,
    mediaViewModel: MediaViewModel,
    shortsViewModel: ShortsViewModel,
    advertsViewModel: AdvertsViewModel,
    channelsViewModel: ChannelsViewModel,
    searchViewModel: SearchViewModel,
    jobViewModel: JobViewModel,
    userViewModel: UserViewModel,
    musicViewModel: MusicViewModel,
    storyViewModel: StoryViewModel,
    callViewModel: CallViewModel,
    navController: NavController,
    onLogout: () -> Unit,
    initialTab: String
) {
    var selectedBottomItem by remember { mutableIntStateOf(
        when(initialTab) {
            "media" -> 1
            "shorts" -> 2
            "music" -> 3
            "drive" -> 4
            else -> 0
        }
    ) }
    var selectedTabItem by remember { mutableIntStateOf(
        when(initialTab) {
            "adverts", "rewards" -> 1
            "jobs" -> 2
            "channels" -> 3
            else -> 0
        }
    ) }
    var selectedMediaFeedTab by remember { mutableIntStateOf(0) }

    val incomingCall by GlobalCallState.incomingCall.collectAsState()
    val autoAcceptSessionId by GlobalCallState.autoAcceptSessionId.collectAsState()
    val activeSessionId by GlobalCallState.activeSessionId.collectAsState()
    val activeParticipantId by GlobalCallState.activeParticipantId.collectAsState()
    
    val chatsState by chatViewModel.chats.collectAsState()
    val sanitizedChats = remember(chatsState) {
        (chatsState.data ?: emptyList()).map { chat ->
            val cachedName = GlobalCallState.getContactName(chat.id)
            val sanitized = chat.senderName.takeIf { 
                it.isNotBlank() && 
                !it.startsWith("User ") && 
                it.lowercase() != "unknown" && 
                it.lowercase() != "null" && 
                it.lowercase() != "undefined" 
            } ?: cachedName ?: chat.id.toString()
            chat.copy(senderName = sanitized)
        }
    }

    LaunchedEffect(autoAcceptSessionId, incomingCall?.senderId) {
        val sessionId = autoAcceptSessionId ?: return@LaunchedEffect
        val call = incomingCall ?: return@LaunchedEffect
        if (call.sessionId == sessionId) {
            navController.navigate(Screen.Chat.createRoute(call.senderId))
        }
    }

    LaunchedEffect(activeSessionId, activeParticipantId) {
        if (activeSessionId != null && activeParticipantId != null) {
            val participantId = activeParticipantId ?: return@LaunchedEffect
            if (navController.currentDestination?.route?.startsWith(Screen.Main.route) == true) {
                 navController.navigate(Screen.Chat.createRoute(participantId))
            }
        }
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var createPostTrigger by remember { mutableIntStateOf(0) }
    var showAddChannelDialog by remember { mutableStateOf(false) }

    val jobsState by jobViewModel.jobs.collectAsState()
    val advertsState by advertsViewModel.adverts.collectAsState()
    val viewedAdvertsState by advertsViewModel.viewedAdverts.collectAsState()
    val channelsState by channelsViewModel.channels.collectAsState()
    val shortsState by shortsViewModel.shorts.collectAsState()
    val songsState by musicViewModel.songs.collectAsState()
    val storiesState by storyViewModel.stories.collectAsState()
    val archivedChatsState by chatViewModel.archivedChats.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val points by advertsViewModel.points.collectAsState()
    val toastMessage by userViewModel.toastMessage.collectAsState()
    val adToastMessage by advertsViewModel.toastMessage.collectAsState()
    val channelToastMessage by channelsViewModel.toastMessage.collectAsState()

    val mediaUiState by mediaViewModel.uiState.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(toastMessage, adToastMessage, channelToastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            userViewModel.clearToast()
        }
        adToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            advertsViewModel.clearToast()
        }
        channelToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            channelsViewModel.clearToast()
        }
    }

    val currentlyPlaying by musicViewModel.currentlyPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    var isNowPlayingVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        musicViewModel.initPlayer(context)
    }

    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableIntStateOf(-1) }
    var activeShortForMoreMenu by remember { mutableStateOf<ShortMedia?>(null) }

    val shortRepliesState by shortsViewModel.shortReplies.collectAsState()

    // Periodic refresh for real-time feel
    LaunchedEffect(Unit) {
        while(true) {
            chatViewModel.fetchChats()
            chatViewModel.fetchContacts()
            storyViewModel.fetchStories()
            delay(60000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainContent(
            chatViewModel = chatViewModel,
            mediaViewModel = mediaViewModel,
            shortsViewModel = shortsViewModel,
            advertsViewModel = advertsViewModel,
            channelsViewModel = channelsViewModel,
            jobViewModel = jobViewModel,
            storyViewModel = storyViewModel,
            searchViewModel = searchViewModel,
            navController = navController,
            selectedBottomItem = selectedBottomItem,
            onBottomItemSelect = { selectedBottomItem = it },
            selectedTabItem = selectedTabItem,
            onTabItemSelect = { selectedTabItem = it },
            selectedMediaFeedTab = selectedMediaFeedTab,
            onMediaFeedTabSelect = { selectedMediaFeedTab = it },
            storiesState = storiesState,
            jobsState = jobsState,
            advertsState = advertsState,
            viewedAdvertsState = viewedAdvertsState,
            channelsState = channelsState,
            shortsState = shortsState,
            songsState = songsState,
            trendingTopicsState = mediaUiState.trendingTopics,
            suggestedUsersState = mediaUiState.suggestedUsers,
            showMoreSheet = showMoreSheet,
            onShowMoreSheet = { showMoreSheet = it },
            onShowAddChannelDialog = { showAddChannelDialog = it },
            musicViewModel = musicViewModel,
            currentlyPlaying = currentlyPlaying,
            isPlaying = isPlaying,
            isNowPlayingVisible = isNowPlayingVisible,
            onSetNowPlayingVisible = { isNowPlayingVisible = it },
            createPostTrigger = createPostTrigger,
            onCreatePostTrigger = { createPostTrigger++ },
            onShowShortComments = { id ->
                activeShortIdForComments = id
                shortsViewModel.fetchShortReplies(id)
                showShortComments = true
            },
            onShowShortMoreMenu = { short ->
                activeShortForMoreMenu = short
                showShortMoreMenu = true
            },
            onAdvertClick = { ad ->
                val activity = context as? android.app.Activity
                if (activity != null) {
                    advertsViewModel.adManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            advertsViewModel.markAdvertAsViewed(ad.id)
                            Toast.makeText(context, "Reward Earned!", Toast.LENGTH_SHORT).show()
                        },
                        onAdClosed = {
                            // Pre-load next ad
                            advertsViewModel.adManager.loadRewardedAd()
                        }
                    )
                }
            },
            sanitizedChats = sanitizedChats,
            currentUser = currentUser,
            points = points,
            onLogout = onLogout,
            unreadChatsCount = chatsState.data?.sumOf { it.unreadCount } ?: 0,
            archivedChatsCount = archivedChatsState.data?.size ?: 0
        )

        if (showShortComments && activeShortIdForComments != -1) {
            ShortCommentsSheet(
                shortId = activeShortIdForComments,
                repliesState = shortRepliesState,
                onDismiss = {
                    showShortComments = false
                    activeShortIdForComments = -1
                    shortsViewModel.clearActiveShortReplies()
                },
                onSendReply = { content ->
                    shortsViewModel.replyToShort(activeShortIdForComments, content)
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
        
        if (showAddChannelDialog) {
            var channelName by remember { mutableStateOf("") }
            var channelDesc by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showAddChannelDialog = false },
                title = { Text("Create Channel", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = channelName,
                            onValueChange = { channelName = it },
                            placeholder = { Text("Channel Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = AppSecondary,
                                unfocusedContainerColor = AppSecondary
                            )
                        )
                        TextField(
                            value = channelDesc,
                            onValueChange = { channelDesc = it },
                            placeholder = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = AppSecondary,
                                unfocusedContainerColor = AppSecondary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (channelName.isNotBlank()) {
                                channelsViewModel.createChannel(channelName, channelDesc)
                                showAddChannelDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                    ) {
                        Text("Create", color = AppBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddChannelDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = AppSecondary
            )
        }

        val isCallActive by GlobalCallState.isCallActive.collectAsState()

        if (isNowPlayingVisible && currentlyPlaying != null) {
            Box(modifier = Modifier.fillMaxSize().zIndex(200f)) {
                val queue = songsState.data ?: emptyList()
                val isShuffleEnabled by musicViewModel.isShuffleEnabled.collectAsState()
                val repeatMode by musicViewModel.repeatMode.collectAsState()
                val progress by musicViewModel.progress.collectAsState()
                val likedSongs by musicViewModel.likedSongs.collectAsState()
                val sleepTimerRemaining by musicViewModel.sleepTimerRemaining.collectAsState()
                val playlistsState by musicViewModel.playlists.collectAsState()

                NowPlayingScreen(
                    song = currentlyPlaying!!,
                    queue = queue,
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    progress = progress,
                    isLiked = likedSongs.contains(currentlyPlaying!!.id),
                    onClose = { isNowPlayingVisible = false },
                    onTogglePlay = { musicViewModel.togglePlayPause() },
                    onToggleShuffle = { musicViewModel.toggleShuffle() },
                    onToggleRepeat = { musicViewModel.toggleRepeat() },
                    onPrevious = { musicViewModel.skipPrevious() },
                    onNext = { musicViewModel.skipNext() },
                    onSeek = { musicViewModel.seekTo(it) },
                    onToggleLike = { musicViewModel.toggleLike(currentlyPlaying!!.id) },
                    onPlayFromQueue = { musicViewModel.playSong(it, queue) },
                    onSeekForward = { musicViewModel.seekForward() },
                    onSeekBackward = { musicViewModel.seekBackward() },
                    onSetSleepTimer = { musicViewModel.setSleepTimer(it) },
                    sleepTimerRemaining = sleepTimerRemaining,
                    playlists = playlistsState.data ?: emptyList(),
                    onAddSongToPlaylist = { musicViewModel.addSongToPlaylist(it, currentlyPlaying!!.id) },
                    onArtistClick = { artistName ->
                        isNowPlayingVisible = false
                        musicViewModel.requestArtist(artistName)
                        selectedBottomItem = 3
                    }
                )
            }
        }

        incomingCall?.let { call ->
            if (!isCallActive) {
                val callerMsg = sanitizedChats.firstOrNull { it.id == call.senderId }
                val callerName = callerMsg?.senderName 
                    ?: GlobalCallState.getContactName(call.senderId)
                    ?: "User ${call.senderId}"
                val callLabel = if (call.callType == CallType.VIDEO) "Incoming video call" else "Incoming voice call"

                Box(
                    modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(alpha = 0.85f)).zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = AppAccent.copy(alpha = 0.2f),
                            border = BorderStroke(2.dp, AppAccent)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (call.callType == CallType.VIDEO) Icons.Default.VideoCall else Icons.Default.Call,
                                    contentDescription = null,
                                    tint = AppAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(callLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                            Text(callerName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = {
                                        CallWebSocketClient.reject(call.senderId, call.sessionId, call.callType)
                                        GlobalCallState.clear()
                                    },
                                    containerColor = Color(0xFFF44336),
                                    shape = CircleShape
                                ) { Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White) }
                                Spacer(Modifier.height(8.dp))
                                Text("Decline", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { 
                                        GlobalCallState.acceptCall(call, callViewModel.webRtcCallManager)
                                    },
                                    containerColor = Color(0xFF4CAF50),
                                    shape = CircleShape
                                ) { Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White) }
                                Spacer(Modifier.height(8.dp))
                                Text("Accept", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    chatViewModel: ChatViewModel,
    mediaViewModel: MediaViewModel,
    shortsViewModel: ShortsViewModel,
    advertsViewModel: AdvertsViewModel,
    channelsViewModel: ChannelsViewModel,
    jobViewModel: JobViewModel,
    storyViewModel: StoryViewModel,
    searchViewModel: SearchViewModel,
    navController: NavController,
    selectedBottomItem: Int,
    onBottomItemSelect: (Int) -> Unit,
    selectedTabItem: Int,
    onTabItemSelect: (Int) -> Unit,
    selectedMediaFeedTab: Int,
    onMediaFeedTabSelect: (Int) -> Unit,
    storiesState: Resource<List<UserStory>>,
    jobsState: Resource<List<Job>>,
    advertsState: Resource<List<Advert>>,
    viewedAdvertsState: Resource<List<Advert>>,
    channelsState: Resource<List<Channel>>,
    shortsState: Resource<List<ShortMedia>>,
    songsState: Resource<List<Song>>,
    trendingTopicsState: List<TrendingTopic>,
    suggestedUsersState: List<Contact>,
    showMoreSheet: Boolean,
    onShowMoreSheet: (Boolean) -> Unit,
    onShowAddChannelDialog: (Boolean) -> Unit,
    musicViewModel: MusicViewModel,
    currentlyPlaying: Song?,
    isPlaying: Boolean,
    isNowPlayingVisible: Boolean,
    onSetNowPlayingVisible: (Boolean) -> Unit,
    createPostTrigger: Int,
    onCreatePostTrigger: () -> Unit,
    onShowShortComments: (Int) -> Unit,
    onShowShortMoreMenu: (ShortMedia) -> Unit,
    onAdvertClick: (Advert) -> Unit,
    sanitizedChats: List<ChatMessage>,
    currentUser: User?,
    points: Double,
    onLogout: () -> Unit,
    unreadChatsCount: Int,
    archivedChatsCount: Int
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isShortsSearchActive by remember { mutableStateOf(false) }
    var isMediaProfileActive by remember { mutableStateOf(false) }
    var cachedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var isChatInitialScrollDone by remember { mutableStateOf(false) }
    
    LaunchedEffect(songsState) {
        val latestSongs = songsState.data.orEmpty()
        if (latestSongs.isNotEmpty()) cachedSongs = latestSongs
    }

    val mediaSongs = songsState.data?.takeIf { it.isNotEmpty() } ?: cachedSongs
    
    LaunchedEffect(selectedTabItem, isSearchActive) {
        if (isSearchActive && searchQuery.isNotEmpty() && selectedBottomItem == 0) {
            val searchType = when (selectedTabItem) {
                0 -> SearchType.CHATS
                2 -> SearchType.JOBS
                3 -> SearchType.CHANNELS
                else -> SearchType.GLOBAL
            }
            searchViewModel.search(searchQuery, searchType)
        }
    }

    val onBackAction = {
        if (isNowPlayingVisible) onSetNowPlayingVisible(false)
        else if (isShortsSearchActive) { isShortsSearchActive = false; shortsViewModel.fetchShorts() }
        else if (isSearchActive) { isSearchActive = false; searchQuery = ""; searchViewModel.search("") }
        else if (showMoreSheet) onShowMoreSheet(false)
        else if (selectedBottomItem != 0) {
            onBottomItemSelect(0)
            onTabItemSelect(0)
        }
        else if (selectedTabItem != 0) onTabItemSelect(0)
        else { navController.popBackStack() }
        Unit
    }

    androidx.activity.compose.BackHandler(enabled = isNowPlayingVisible || isShortsSearchActive || isSearchActive || showMoreSheet || selectedBottomItem != 0 || selectedTabItem != 0, onBack = onBackAction)

    val bottomItems = listOf("Chat", "Media", "Shorts", "Music", "Drive", "More")
    val bottomIcons = listOf(Icons.AutoMirrored.Filled.Chat, Icons.Filled.PermMedia, Icons.Filled.PlayArrow, Icons.Filled.MusicNote, Icons.Filled.DirectionsCar, Icons.Filled.MoreHoriz)
    val tabs = listOf("Chats", "Rewards", "Jobs", "Channels")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (selectedBottomItem in 0..4 && !(selectedBottomItem == 1 && isMediaProfileActive)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { 
                            topBarHeight = with(density) { it.size.height.toDp() }
                        }
                        .zIndex(1f),
                    color = if (selectedBottomItem == 2) Color.Transparent else AppBackground,
                    tonalElevation = if (selectedBottomItem == 2) 0.dp else 4.dp,
                    shadowElevation = if (selectedBottomItem == 2) 0.dp else 4.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isSearchActive && selectedBottomItem == 0) {
                            val searchType = when (selectedTabItem) {
                                0 -> SearchType.CHATS
                                2 -> SearchType.JOBS
                                3 -> SearchType.CHANNELS
                                else -> SearchType.GLOBAL
                            }
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    searchViewModel.search(it, searchType)
                                },
                                placeholder = { 
                                    val hint = when (selectedTabItem) {
                                        0 -> "Search chats, contacts, messages..."
                                        2 -> "Search jobs, companies..."
                                        3 -> "Search channels..."
                                        else -> "Search..."
                                    }
                                    Text(hint, color = Color.Gray) 
                                },
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
                                    IconButton(onClick = { isSearchActive = false; searchQuery = "" ; searchViewModel.search("") }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = ""; searchViewModel.search("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            if (selectedBottomItem == 0) {
                                AppLogo(size = 32.dp, modifier = Modifier.clickable { navController.navigate(Screen.Story.createRoute()) })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "FirstApp",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val titleText = when (selectedBottomItem) { 
                                    1 -> "Media"
                                    2 -> "Shorts"
                                    3 -> "Music"
                                    4 -> "Drive"
                                    else -> "Home" 
                                }
                                Text(text = titleText, color = if (selectedBottomItem == 2) Color.White else AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                            
                            if (selectedBottomItem == 0 || selectedBottomItem == 1) {
                                IconButton(onClick = { isSearchActive = true }) { 
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary) 
                                }
                                if (selectedBottomItem == 1) {
                                    IconButton(onClick = { onCreatePostTrigger() }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AppAccent)
                                    }
                                }
                            }
                            
                            Box {
                                var showMainMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMainMenu = true }) { 
                                    Icon(
                                        imageVector = Icons.Default.MoreVert, 
                                        contentDescription = "More", 
                                        tint = if (selectedBottomItem == 2) Color.White else AppTextPrimary
                                    ) 
                                }
                                DropdownMenu(
                                    expanded = showMainMenu,
                                    onDismissRequest = { showMainMenu = false }
                                ) {
                                    when (selectedBottomItem) {
                                        0 -> when (selectedTabItem) {
                                            0 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Contacts", color = Color.White) },
                                                    onClick = { showMainMenu = false; navController.navigate(Screen.ContactPicker.route) },
                                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = AppAccent) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("New Group", color = Color.White) },
                                                    onClick = { showMainMenu = false; navController.navigate(Screen.ContactPicker.createRoute(isGroupMode = true)) },
                                                    leadingIcon = { Icon(Icons.Default.GroupAdd, null, tint = AppAccent) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Starred Messages", color = Color.White) },
                                                    onClick = { showMainMenu = false; navController.navigate(Screen.StarredMessages.route) },
                                                    leadingIcon = { Icon(Icons.Default.Star, null, tint = Color.Yellow) }
                                                )
                                            }
                                            1 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Wallet", color = Color.White) },
                                                    onClick = { showMainMenu = false; navController.navigate(Screen.Wallet.route) },
                                                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.Green) }
                                                )
                                            }
                                            2 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Job Profile", color = Color.White) },
                                                    onClick = { showMainMenu = false; navController.navigate(Screen.JobProfile.route) },
                                                    leadingIcon = { Icon(Icons.Default.Work, null, tint = AppAccent) }
                                                )
                                            }
                                            3 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Add Channel", color = Color.White) },
                                                    onClick = { 
                                                        showMainMenu = false
                                                        onShowAddChannelDialog(true)
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Add, null, tint = AppAccent) }
                                                )
                                            }
                                        }
                                        else -> {}
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))
                                    
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = Color.White) },
                                        onClick = { showMainMenu = false; navController.navigate(Screen.Settings.route) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.Gray) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign Out", color = Color(0xFFE57373)) },
                                        onClick = { showMainMenu = false; onLogout() },
                                        leadingIcon = { Icon(Icons.Default.Logout, null, tint = Color(0xFFE57373)) }
                                    )
                                }
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
                        
                        val count = if (item == "Chat") unreadChatsCount else 0

                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (count > 0) {
                                            Badge(containerColor = AppAccent) {
                                                Text(text = if (count > 99) "99+" else count.toString(), color = AppBackground)
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
        containerColor = AppBackground,
        floatingActionButton = {
            if (selectedBottomItem == 0 && selectedTabItem == 0 && !isSearchActive) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ContactPicker.route) },
                    containerColor = AppAccent,
                    contentColor = AppBackground,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = if (currentlyPlaying != null) 70.dp else 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val contentPadding = if (selectedBottomItem == 0 && selectedTabItem == 0) {
                PaddingValues(bottom = innerPadding.calculateBottomPadding())
            } else if (selectedBottomItem >= 2) {
                PaddingValues(0.dp)
            } else {
                innerPadding
            }
            
            Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                when (selectedBottomItem) {
                    0 -> when (selectedTabItem) {
                        0 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            
                            if (isSearchActive && searchQuery.isNotEmpty()) {
                                val searchType = when (selectedTabItem) {
                                    0 -> SearchType.CHATS
                                    2 -> SearchType.JOBS
                                    3 -> SearchType.CHANNELS
                                    else -> SearchType.GLOBAL
                                }
                                SearchResultsView(
                                    results = searchResults,
                                    type = searchType,
                                    onChatClick = { chat: ChatMessage ->
                                        navController.navigate(Screen.Chat.createRoute(chat.id))
                                    },
                                    onMessageClick = { msg: Message ->
                                        navController.navigate(Screen.Chat.createRoute(msg.chatId))
                                    },
                                    onSongClick = { song, queue ->
                                        musicViewModel.playSong(song, queue)
                                        onBottomItemSelect(3)
                                    },
                                    onArtistClick = { artist ->
                                        musicViewModel.requestArtist(artist.displayName)
                                        onBottomItemSelect(3)
                                    },
                                    onContactClick = { contact ->
                                        navController.navigate(Screen.Chat.createRoute(contact.id))
                                    }
                                )
                            } else {
                                ChatList(
                                    chats = sanitizedChats,
                                    archivedChatsCount = archivedChatsCount,
                                    stories = storiesState.data ?: emptyList(),
                                    chatViewModel = chatViewModel,
                                    topBarHeight = topBarHeight,
                                    currentUserId = currentUser?.id,
                                    initialScrollDone = isChatInitialScrollDone,
                                    onInitialScrollDone = { isChatInitialScrollDone = it },
                                    onChatClick = { chat -> 
                                        val typeStr = when (chat.type) {
                                            ConversationType.CHANNEL -> "channel"
                                            ConversationType.GROUP -> "group"
                                            else -> "direct"
                                        }
                                        navController.navigate(Screen.Chat.createRoute(chat.id, type = typeStr)) 
                                    },
                                    onStoryClick = { story: UserStory -> navController.navigate(Screen.Story.createRoute(story.id)) },
                                    onMediaStoryClick = { navController.navigate(Screen.NewStory.route) },
                                    onTextStoryClick = { navController.navigate(Screen.Story.createRoute(showComposer = true)) },
                                    onArchivedClick = { navController.navigate(Screen.ArchivedChats.route) },
                                    onPostedStoriesClick = { navController.navigate(Screen.Story.createRoute()) }
                                )
                            }
                        }
                        1 -> {
                            val ads = advertsState.data ?: emptyList()
                            val viewedAds = viewedAdvertsState.data ?: emptyList()
                            
                            val isLoading = advertsState is Resource.Loading || viewedAdvertsState is Resource.Loading
                            val isError = advertsState is Resource.Error && viewedAdvertsState is Resource.Error
                            
                            if (isLoading && ads.isEmpty() && viewedAds.isEmpty()) {
                                SkeletonLoader()
                            } else if (isError && ads.isEmpty() && viewedAds.isEmpty()) {
                                ErrorStateView(advertsState.message ?: viewedAdvertsState.message)
                            } else {
                                RewardsScreen(
                                    adverts = ads,
                                    viewedAdverts = viewedAds,
                                    adsWatched = currentUser?.adsWatchedToday ?: 0,
                                    points = points,
                                    totalAdsToWatch = 20,
                                    onAdvertClick = { ad -> onAdvertClick(ad) },
                                    onRefresh = { advertsViewModel.fetchAdverts() },
                                    onResetAds = { advertsViewModel.resetAds() }
                                )
                            }
                        }
                        2 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            if (isSearchActive && searchQuery.isNotEmpty()) {
                                SearchResultsView(
                                    results = searchResults,
                                    type = SearchType.JOBS,
                                    onChatClick = {},
                                    onMessageClick = {},
                                    onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                    onContactClick = { contact ->
                                        navController.navigate(Screen.Chat.createRoute(contact.id))
                                    }
                                )
                            } else {
                                DataStateHandler(jobsState) { jobs -> JobList(jobs) { navController.navigate(Screen.JobDetail.createRoute(it.id)) } }
                            }
                        }
                        3 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            if (isSearchActive && searchQuery.isNotEmpty()) {
                                SearchResultsView(
                                    results = searchResults,
                                    type = SearchType.CHANNELS,
                                    onChatClick = {},
                                    onMessageClick = {},
                                    onChannelClick = { navController.navigate(Screen.Chat.createRoute(it.id, type = "channel")) },
                                    onContactClick = { contact ->
                                        navController.navigate(Screen.Chat.createRoute(contact.id))
                                    }
                                )
                            } else {
                                DataStateHandler(channelsState) { channels -> ChannelList(channels) { navController.navigate(Screen.Chat.createRoute(it.id, type = "channel")) } }
                            }
                        }
                    }
                    1 -> {
                        val uiState by mediaViewModel.uiState.collectAsState()
                        val pagingPosts = uiState.posts.collectAsLazyPagingItems()
                        val pagingFollowingPosts = uiState.followingPosts.collectAsLazyPagingItems()
                        
                        MediaScreen(
                            posts = pagingPosts, 
                            followingPosts = pagingFollowingPosts,
                            songs = mediaSongs, 
                            trendingTopics = uiState.trendingTopics,
                            suggestedUsers = uiState.suggestedUsers,
                            mediaFeedTab = selectedMediaFeedTab, 
                            selectedPost = null, 
                            onPostSelect = { it?.let { _ -> } }, 
                            onCreatePost = { c, m, t -> mediaViewModel.createPost(content = c, mediaUris = m, tags = t) },
                            onLikePost = { id, l, il -> mediaViewModel.likePost(id, l, il) }, 
                            onRepostPost = { mediaViewModel.repostPost(it) }, 
                            onReplySubmit = { p1, p2, c -> mediaViewModel.createPostReply(p1, c, p2) }, 
                            onReplyLike = { mediaViewModel.likePostReply(it) }, 
                            onReplyRepost = { mediaViewModel.repostPostReply(it) }, 
                            onBookmarkPost = { mediaViewModel.bookmarkPost(it) }, 
                            onBookmarkReply = { mediaViewModel.bookmarkPostReply(it) }, 
                            onViewPost = { mediaViewModel.viewPost(it) }, 
                            onProfileViewChange = { isMediaProfileActive = it }, 
                            onPlaySong = { s, q -> musicViewModel.playSong(s, q) }, 
                            onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                            onFollowUser = { mediaViewModel.followUser(it) },
                            onArtistClick = { artistName ->
                                musicViewModel.requestArtist(artistName)
                                onBottomItemSelect(3)
                            },
                            onCallClick = { userId, type -> chatViewModel.initiateCall(userId, type) },
                            onCreatePostClick = createPostTrigger,
                            onRefresh = { }
                        )
                    }
                    2 -> DataStateHandler(shortsState) { shorts -> ShortsList(shorts = shorts, shortsViewModel = shortsViewModel, bottomPadding = bottomBarHeight, onShowComments = onShowShortComments, onShowMoreMenu = onShowShortMoreMenu) }
                    3 -> DataStateHandler(songsState) { songs -> 
                        MusicScreen(
                            songs = songs, 
                            viewModel = musicViewModel, 
                            bottomPadding = bottomBarHeight, 
                            onShareSong = { song ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this song: ${song.title}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Listen to ${song.title} by ${song.artist} on FirstApp!")
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share Song"))
                            }, 
                            onShowSettings = { navController.navigate(Screen.Settings.route) }
                        ) 
                    }
                    4 -> DriveScreen(bottomPadding = innerPadding.calculateBottomPadding() + 80.dp, onShowSettings = { navController.navigate(Screen.Settings.route) })
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
                            ShortcutItem("Wallet", Icons.Default.AccountBalanceWallet, Color(0xFFFF9800), "wallet"),
                            ShortcutItem("Updates", Icons.Default.History, Color(0xFF4CAF50), Screen.Story.route),
                            ShortcutItem("Vocab", Icons.Default.Language, AppPrimary, Screen.Vocab.route),
                            ShortcutItem("Files", Icons.Default.Description, Color(0xFF2196F3), Screen.Files.route),
                            ShortcutItem("Calendar", Icons.Default.CalendarToday, Color(0xFFE91E63), Screen.Calendar.route),
                            ShortcutItem("Direct Messages", Icons.AutoMirrored.Filled.Chat, AppPrimary, Screen.DirectMessages.route),
                            ShortcutItem("Archived", Icons.Default.Archive, Color(0xFF7CB342), Screen.ArchivedChats.route),
                            ShortcutItem("Job Profile", Icons.Default.Work, AppAccent, Screen.JobProfile.route),
                            ShortcutItem("Applications", Icons.AutoMirrored.Filled.Assignment, AppPrimary, Screen.Applications.route),
                            ShortcutItem("Places", Icons.Default.Place, Color(0xFFF44336), "drive"),
                            ShortcutItem("Channels", Icons.Default.Groups, Color(0xFF9C27B0), "channels"),
                            ShortcutItem("Saved", Icons.Default.Bookmark, Color(0xFFFFC107), Screen.StarredMessages.route),
                            ShortcutItem("Settings", Icons.Default.Settings, Color.Gray, Screen.Settings.route)
                        )
                        
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3), 
                            verticalArrangement = Arrangement.spacedBy(24.dp), 
                            horizontalArrangement = Arrangement.spacedBy(16.dp), 
                            modifier = Modifier.height(420.dp)
                        ) {
                            items(moreItems.size) { idx ->
                                val item = moreItems[idx]
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, 
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { 
                                        onShowMoreSheet(false)
                                        when (item.route) {
                                            "drive" -> onBottomItemSelect(4)
                                            "wallet" -> navController.navigate(Screen.Wallet.route)
                                            "channels" -> { onBottomItemSelect(0); onTabItemSelect(3) }
                                            else -> if (item.route != null) navController.navigate(item.route)
                                        }
                                    }.padding(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(60.dp), 
                                        shape = RoundedCornerShape(18.dp), 
                                        color = item.color.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, item.color.copy(alpha = 0.2f))
                                    ) { 
                                        Box(contentAlignment = Alignment.Center) { 
                                            Icon(item.icon, contentDescription = item.label, tint = item.color, modifier = Modifier.size(28.dp)) 
                                        } 
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = item.label,
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
fun <T> DataStateHandler(
    resource: Resource<T>, 
    onLoading: @Composable () -> Unit = { SkeletonLoader() },
    onEmpty: @Composable () -> Unit = { EmptyStateView() },
    onError: @Composable (String?) -> Unit = { ErrorStateView(it) },
    onSuccess: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Loading -> {
            if (resource.data != null && (resource.data as? List<*>)?.isNotEmpty() == true) {
                onSuccess(resource.data)
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = AppAccent,
                    trackColor = Color.Transparent
                )
            } else {
                onLoading()
            }
        }
        is Resource.Error -> {
            if (resource.data != null && (resource.data as? List<*>)?.isNotEmpty() == true) {
                onSuccess(resource.data)
                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
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
                onError(resource.message)
            }
        }
        is Resource.Success -> {
            val data = resource.data
            if (data == null || (data is List<*> && data.isEmpty())) {
                onEmpty()
            } else {
                onSuccess(data)
            }
        }
    }
}

@Composable
fun SkeletonLoader() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(10) {
            SkeletonItem()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ErrorStateView(message: String?) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = message ?: "Connection Error", color = Color.White, textAlign = TextAlign.Center)
            Button(
                onClick = { },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Retry", color = AppBackground)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatList(
    chats: List<ChatMessage>,
    archivedChatsCount: Int,
    stories: List<UserStory>,
    chatViewModel: ChatViewModel,
    topBarHeight: Dp = 0.dp,
    currentUserId: Int? = null,
    initialScrollDone: Boolean = false,
    onInitialScrollDone: (Boolean) -> Unit = {},
    onChatClick: (ChatMessage) -> Unit,
    onStoryClick: (UserStory) -> Unit,
    onMediaStoryClick: () -> Unit,
    onTextStoryClick: () -> Unit,
    onArchivedClick: () -> Unit,
    onPostedStoriesClick: () -> Unit
) {
    var selectedChatForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    LaunchedEffect(chats, stories, archivedChatsCount, topBarHeight) {
        if (!initialScrollDone && (chats.isNotEmpty() || archivedChatsCount > 0) && topBarHeight > 0.dp) {
            snapshotFlow { lazyListState.layoutInfo.totalItemsCount }.first { it > 2 }
            delay(500)
            val topBarHeightPx = with(density) { topBarHeight.roundToPx() }
            repeat(3) {
                lazyListState.scrollToItem(2, -topBarHeightPx)
                delay(100)
            }
            onInitialScrollDone(true)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val firstItemOffset = if (lazyListState.firstVisibleItemIndex == 1) {
                            lazyListState.firstVisibleItemScrollOffset.toFloat()
                        } else if (lazyListState.firstVisibleItemIndex > 1) {
                            1000f
                        } else {
                            0f
                        }
                        alpha = (1f - (firstItemOffset / 300f)).coerceAtLeast(0f)
                        translationY = -firstItemOffset * 0.5f
                    }
            ) {
                StoriesRow(
                    stories = stories,
                    currentUserId = currentUserId,
                    onStoryClick = onStoryClick,
                    onMediaStoryClick = onMediaStoryClick,
                    onTextStoryClick = onTextStoryClick,
                    onPostedStoriesClick = onPostedStoriesClick
                )
            }
        }

        if (chats.isEmpty() && archivedChatsCount == 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No chats yet",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        if (archivedChatsCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArchivedClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = AppAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Archived", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(text = archivedChatsCount.toString(), color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
            }
        }

        items(chats, key = { it.id }) { chat ->
            val story = stories.find { it.id == chat.id }
            val hasStory = story != null
            val allStoriesViewed = story?.stories?.all { it.isViewed } ?: false
            
            Column(
                modifier = Modifier.combinedClickable(
                    onClick = { onChatClick(chat) },
                    onLongClick = { selectedChatForMenu = chat }
                )
            ) {
                ChatItem(
                    chat = chat,
                    hasStory = hasStory,
                    allStoriesViewed = allStoriesViewed,
                    onAvatarClick = if (hasStory && story != null) { { onStoryClick(story) } } else null
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
            }
        }
    }

    selectedChatForMenu?.let { selectedChat: ChatMessage ->
        AlertDialog(
            onDismissRequest = { selectedChatForMenu = null },
            title = { Text(selectedChat.senderName, color = Color.White) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (selectedChat.isPinned) "Unpin Chat" else "Pin Chat", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.PushPin, null, tint = AppAccent, modifier = Modifier.rotate(if (selectedChat.isPinned) 0f else 45f)) },
                        modifier = Modifier.clickable { 
                            chatViewModel.pinChat(selectedChat.id, !selectedChat.isPinned)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(if (selectedChat.isMuted) "Unmute Notifications" else "Mute Notifications", color = Color.White) },
                        leadingContent = { Icon(if (selectedChat.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null, tint = AppAccent) },
                        modifier = Modifier.clickable { 
                            chatViewModel.muteChat(selectedChat.id, !selectedChat.isMuted)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Archive Chat", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.Archive, null, tint = AppAccent) },
                        modifier = Modifier.clickable { 
                            chatViewModel.archiveChat(selectedChat.id, true)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Delete Chat", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        modifier = Modifier.clickable { 
                            chatViewModel.deleteChat(selectedChat.id)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            },
            confirmButton = {},
            containerColor = AppSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SearchResultsView(
    results: com.example.firstapp.search.model.SearchResult?,
    type: com.example.firstapp.search.model.SearchType = com.example.firstapp.search.model.SearchType.GLOBAL,
    onChatClick: (ChatMessage) -> Unit,
    onMessageClick: (Message) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onArtistClick: (Contact) -> Unit = {},
    onJobClick: (Job) -> Unit = {},
    onChannelClick: (Channel) -> Unit = {},
    onContactClick: (Contact) -> Unit = {}
) {
    if (results == null) return
    
    val allChats = results.chats
    val contacts = results.contacts.filter { contact -> !allChats.any { it.id == contact.id } }
    val messages = results.messages
    val songs = results.songs
    val artists = results.artists
    val jobs = results.jobs
    val channels = results.channels

    val showChats = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showContacts = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showMessages = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showJobs = type == SearchType.GLOBAL || type == SearchType.JOBS
    val showChannels = type == SearchType.GLOBAL || type == SearchType.CHANNELS
    val showMusic = type == SearchType.GLOBAL || type == SearchType.MUSIC

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showContacts && contacts.isNotEmpty()) {
            item { SearchSectionHeader("Contacts") }
            items(items = contacts, key = { "contact_${it.id}" }) { contactItem: Contact ->
                Column(modifier = Modifier.clickable { onContactClick(contactItem) }) {
                    ContactSearchItem(contactItem)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMusic && artists.isNotEmpty()) {
            item { SearchSectionHeader("Artists") }
            items(artists, key = { "artist_${it.id}" }) { artist ->
                Column(modifier = Modifier.clickable { onArtistClick(artist) }) {
                    ContactSearchItem(artist)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMusic && songs.isNotEmpty()) {
            item { SearchSectionHeader("Songs") }
            items(songs, key = { "song_${it.id}" }) { song ->
                Column(modifier = Modifier.clickable { onSongClick(song, songs) }) {
                    SongSearchItem(song)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showJobs && jobs.isNotEmpty()) {
            item { SearchSectionHeader("Jobs") }
            items(jobs, key = { "job_${it.id}" }) { job ->
                Column(modifier = Modifier.clickable { onJobClick(job) }) {
                    JobItem(job = job, onClick = { onJobClick(job) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showChannels && channels.isNotEmpty()) {
            item { SearchSectionHeader("Channels") }
            items(channels, key = { "channel_${it.id}" }) { channel ->
                Column(modifier = Modifier.clickable { onChannelClick(channel) }) {
                    ChannelItem(channel = channel, onClick = { onChannelClick(channel) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showChats && allChats.isNotEmpty()) {
            item { SearchSectionHeader("Chats") }
            items(allChats, key = { "chat_${it.id}" }) { chat ->
                Column(modifier = Modifier.clickable { onChatClick(chat) }) {
                    ChatItem(chat)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMessages && messages.isNotEmpty()) {
            item { SearchSectionHeader("Messages") }
            items(messages, key = { "message_${it.id}" }) { message ->
                Column(modifier = Modifier.clickable { onMessageClick(message) }) {
                    MessageSearchItem(message)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }
    }
}

@Composable
fun MessageSearchItem(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (message.type) {
            MessageType.IMAGE -> Icons.Default.Image
            MessageType.VIDEO -> Icons.Default.VideoLibrary
            MessageType.AUDIO_MESSAGE -> Icons.Default.Audiotrack
            MessageType.DOCUMENT -> Icons.Default.Description
            MessageType.LOCATION -> Icons.Default.LocationOn
            MessageType.CONTACT -> Icons.Default.Person
            else -> Icons.AutoMirrored.Filled.Message
        }
        
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(AppSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.senderName ?: "Unknown", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(formatWhatsAppTime(message.timestamp), color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = if (message.type == MessageType.TEXT) message.content else message.type.name,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongSearchItem(song: Song) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(song.albumArtColor),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUrl != null) {
                coil.compose.AsyncImage(model = song.albumArtUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, null, tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(song.artist, color = Color.Gray, fontSize = 14.sp)
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
fun JobList(jobs: List<Job>, onJobClick: (Job) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
        items(jobs, key = { it.id }) { job -> 
    JobItem(job = job, onClick = { onJobClick(job) })
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
}
    }
}

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (Channel) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
        items(channels, key = { it.id }) { ch -> 
    ChannelItem(channel = ch, onClick = { onChannelClick(ch) })
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
}
    }
}

@Composable
fun AdvertList(
    adverts: List<Advert>, 
    viewedAdverts: List<Advert> = emptyList(),
    header: @Composable () -> Unit = {}, 
    onAdvertClick: (Advert) -> Unit
) {
    if (adverts.isEmpty() && viewedAdverts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Celebration, null, tint = AppAccent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("You're all caught up!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
            item { header() }
            if (adverts.isNotEmpty()) {
                item {
                    Text(
                        "New Adverts",
                        color = AppAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(adverts, key = { "new_${it.id}" }) { ad -> 
                    Column(modifier = Modifier.clickable { onAdvertClick(ad) }) { 
                        AdvertItem(ad)
                        HorizontalDivider(modifier = Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
                    } 
                }
            }
            if (viewedAdverts.isNotEmpty()) {
                item {
                    Text(
                        "Viewed Adverts",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(viewedAdverts, key = { "viewed_${it.id}" }) { ad -> 
                    Column(modifier = Modifier.clickable { onAdvertClick(ad) }.alpha(0.6f)) { 
                        AdvertItem(ad)
                        HorizontalDivider(modifier = Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
                    } 
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(shorts: List<ShortMedia>, shortsViewModel: ShortsViewModel, bottomPadding: Dp, onShowComments: (Int) -> Unit, onShowMoreMenu: (ShortMedia) -> Unit) {
    if (shorts.isEmpty()) return
    val context = LocalContext.current
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { shorts.size }
    
    LaunchedEffect(pagerState.currentPage, shorts) {
        val nextIndices = (pagerState.currentPage + 1..pagerState.currentPage + 3)
            .filter { it in shorts.indices }
        val urls = nextIndices.map { shorts[it].videoUrl }
        ShortsVideoCache.preload(context, urls)
    }

    androidx.compose.foundation.pager.VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        ShortItem(short = shorts[page], isVisible = pagerState.currentPage == page, bottomInset = bottomPadding, onLikeClick = { shortsViewModel.likeShort(it) }, onSubscribeClick = { shortsViewModel.subscribeToShortCreator(it) }, onCommentClick = { onShowComments(it) }, onRepostClick = { shortsViewModel.repostShort(it) }, onBookmarkClick = { shortsViewModel.bookmarkShort(it) }, onMoreClick = { onShowMoreMenu(shorts[page]) })
    }
}

@Composable
fun SearchFilterChips(
    activeFilter: com.example.firstapp.search.model.SearchFilter,
    onFilterChange: (com.example.firstapp.search.model.SearchFilter) -> Unit
) {
    val filters = com.example.firstapp.search.model.SearchFilter.values()
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters) { filter ->
            val isSelected = activeFilter == filter
            val icon = when (filter) {
                com.example.firstapp.search.model.SearchFilter.PHOTOS -> Icons.Default.Image
                com.example.firstapp.search.model.SearchFilter.VIDEOS -> Icons.Default.VideoLibrary
                com.example.firstapp.search.model.SearchFilter.LINKS -> Icons.Default.Link
                com.example.firstapp.search.model.SearchFilter.GIFS -> Icons.Default.Animation
                com.example.firstapp.search.model.SearchFilter.AUDIO -> Icons.Default.Audiotrack
                com.example.firstapp.search.model.SearchFilter.DOCUMENTS -> Icons.Default.Description
                else -> null
            }
            val label = filter.name

            Surface(
                onClick = { onFilterChange(filter) },
                color = if (isSelected) AppAccent.copy(alpha = 0.2f) else AppSecondary,
                shape = RoundedCornerShape(20.dp),
                border = if (isSelected) BorderStroke(1.dp, AppAccent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSelected) AppAccent else Color.White)
                    }
                    Text(
                        text = label,
                        color = if (isSelected) AppAccent else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    }
}
