package com.example.firstapp.chat.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.ui.components.*
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.chat.ui.viewmodel.AdvertsViewModel
import com.example.firstapp.chat.ui.viewmodel.ChannelsViewModel
import com.example.firstapp.chat.ui.viewmodel.StoryViewModel
import com.example.firstapp.common.model.*
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.ui.viewmodel.*
import com.example.firstapp.common.util.*
import com.example.firstapp.home.ui.components.AppLogo
import com.example.firstapp.home.ui.viewmodel.*
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.chat.jobs.ui.components.JobItem
import com.example.firstapp.chat.jobs.ui.viewmodel.JobViewModel
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.ui.components.*
import java.util.*
import kotlin.collections.*
import kotlin.text.*
import com.example.firstapp.chat.ui.components.StoriesRow
import com.example.firstapp.chat.ui.channels.ChannelList
import com.example.firstapp.chat.ui.channels.ChannelItem
import com.example.firstapp.chat.ui.rewards.RewardsScreen
import com.example.firstapp.chat.jobs.ui.screens.JobList
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.ui.components.DynamicAdCard
import com.example.firstapp.common.ui.components.BannerAdView
import com.example.firstapp.chat.data.CallType
import com.example.firstapp.chat.data.GlobalCallState
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

import com.example.firstapp.common.data.ads.AdUnitConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    navController: NavController,
    onLogout: () -> Unit,
    adManager: AdManager,
    bottomPadding: Dp = 0.dp,
    onShowMore: () -> Unit = {}, 
    initialTab: String? = null,
    chatViewModel: ChatViewModel = hiltViewModel(),
    advertsViewModel: AdvertsViewModel = hiltViewModel(),
    rewardViewModel: com.example.firstapp.chat.ui.viewmodel.RewardViewModel = hiltViewModel(),
    channelsViewModel: ChannelsViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    jobViewModel: JobViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val initialTabIndex = when(initialTab) {
        "chats" -> 0
        "channels" -> 1
        "jobs" -> 2
        "rewards" -> 3
        else -> 3 // Default active tab on launch is Rewards
    }
    var selectedTabItem by rememberSaveable { mutableIntStateOf(initialTabIndex) }
    
    LaunchedEffect(initialTab) {
        initialTab?.let {
            val idx = when(it) {
                "chats" -> 0
                "channels" -> 1
                "jobs" -> 2
                "rewards" -> 3
                else -> 3
            }
            selectedTabItem = idx
        }
    }
    val chatsState by chatViewModel.chats.collectAsState()
    val context = LocalContext.current
    
    // FIX: Optimized scroll behavior for vendor ROMs
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    val unreadChatsCount = remember(chatsState) {
        val data = chatsState.data
        var sum = 0
        if (data != null) {
            for (c in data) sum += c.unreadCount
        }
        sum
    }
    
    val sanitizedChats = remember(chatsState) {
        android.util.Log.d("ChatTabScreen", "Recomposing sanitizedChats: state=${chatsState::class.simpleName}, count=${chatsState.data?.size ?: 0}")
        val data = chatsState.data ?: emptyList()
        data.forEach { chat ->
            if (chat.otherUserId != null && chat.senderName.isNotEmpty()) {
                GlobalCallState.updateContactInfo(chat.otherUserId, chat.senderName, chat.phoneNumber)
            }
        }
        data
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var isChatInitialScrollDone by remember { mutableStateOf(false) }
    var showTopMoreSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive || showTopMoreSheet) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            showTopMoreSheet = false
        }
    }

    val storiesState by storyViewModel.stories.collectAsState()
    val archivedChatsState by chatViewModel.archivedChats.collectAsState()
    val currentUser = com.example.firstapp.LocalCurrentUser.current
    val points by advertsViewModel.points.collectAsState()
    val jobsState by jobViewModel.jobs.collectAsState()
    val advertsState by advertsViewModel.adverts.collectAsState()
    val viewedAdvertsState by advertsViewModel.viewedAdverts.collectAsState()
    val channelsState by channelsViewModel.channels.collectAsState()
    val hasRewardedAds by advertsViewModel.hasRewardedAdsAvailable.collectAsState()
    val adCooldownSeconds by advertsViewModel.cooldownRemainingSeconds.collectAsState()
    val animationsEnabled by chatViewModel.animationsEnabled.collectAsState()

    // Network sync is now handled by ChatViewModel init
    // LaunchedEffect(Unit) {
    //    chatViewModel.syncChats()
    // }

    LaunchedEffect(selectedTabItem) {
        if (selectedTabItem == 3) {
            advertsViewModel.initializePool(context, AdUnitConfig.REWARDED_POINTS_ID)
        }
    }

    val tabs = listOf("Chats", "Channels", "Jobs", "Rewards", "DM")

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        if (isSearchActive) {
                            val searchType = when (selectedTabItem) {
                                0 -> SearchType.CHATS
                                1 -> SearchType.CHANNELS
                                2 -> SearchType.JOBS
                                4 -> SearchType.GLOBAL
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
                                        1 -> "Search channels..."
                                        2 -> "Search jobs, companies..."
                                        4 -> "Search people by name or username..."
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
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                leadingIcon = {
                                    IconButton(onClick = { isSearchActive = false; searchQuery = "" ; searchViewModel.search("", searchType) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = ""; searchViewModel.search("", searchType) }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            IconButton(onClick = { showTopMoreSheet = true }) { 
                                Icon(Icons.Default.Menu, contentDescription = "Menu") 
                            }
                            
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                AppLogo(size = 36.dp)
                            }
                            
                            IconButton(onClick = { isSearchActive = true }) { 
                                Icon(Icons.Default.Search, contentDescription = "Search") 
                            }

                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabItem == index
                            val animatedColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                label = "tabColor"
                            )
                            val animatedContentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "tabContentColor"
                            )
                            
                            Surface(
                                onClick = { selectedTabItem = index },
                                color = animatedColor,
                                shape = RoundedCornerShape(24.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = animatedContentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabItem == 0 && !isSearchActive) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ContactPicker.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
                }
            }
        },
        bottomBar = {
            if (selectedTabItem != 0) {
                val adUnitId = when (selectedTabItem) {
                    1 -> AdUnitConfig.BANNER_CHANNELS_ID
                    2 -> AdUnitConfig.BANNER_JOBS_ID
                    3 -> AdUnitConfig.BANNER_REWARDS_ID
                    else -> AdUnitConfig.BANNER_MAIN_ID
                }
                Column {
                    BannerAdView(adUnitId = adUnitId)
                    Spacer(Modifier.height(bottomPadding)) // FIX: Pushes FAB above the outer bottom navigation
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (showTopMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTopMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    when (selectedTabItem) {
                        0 -> {
                            ListItem(
                                headlineContent = { Text("Contacts", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.ContactPicker.route) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text("New Group", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.onSurface) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.ContactPicker.createRoute(isGroupMode = true)) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            ListItem(
                                headlineContent = { Text("Starred Messages", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.Star, null, tint = Color.Yellow) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.StarredMessages.route) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                        1 -> {
                            ListItem(
                                headlineContent = { Text("Create Channel", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.CreateChannel.route)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                        2 -> {
                            ListItem(
                                headlineContent = { Text("Job Profile", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.Work, null, tint = MaterialTheme.colorScheme.onSurface) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.JobProfile.route) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                        3 -> {
                            ListItem(
                                headlineContent = { Text("Wallet", color = MaterialTheme.colorScheme.onSurface) },
                                leadingContent = { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.Green) },
                                modifier = Modifier.clickable { 
                                    showTopMoreSheet = false
                                    navController.navigate(Screen.Wallet.route) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    ListItem(
                        headlineContent = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable { 
                            showTopMoreSheet = false
                            navController.navigate(Screen.Settings.route) 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { 
                            showTopMoreSheet = false
                            onLogout() 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AnimatedContent(
                targetState = selectedTabItem,
                transitionSpec = {
                    if (animationsEnabled) {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    } else {
                        fadeIn().togetherWith(fadeOut())
                    }
                },
                label = "ChatTabContent",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (targetTab) {
                        0 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            val autoplayEnabled by mainViewModel.autoplayVideos.collectAsState()
                            val autoplayGifsEnabled by mainViewModel.autoplayGifs.collectAsState()
                            if (isSearchActive && searchQuery.length > 0) {
                                SearchResultsView(
                                    results = searchResults,
                                    type = SearchType.CHATS,
                                    autoplayEnabled = autoplayEnabled,
                                    autoplayGifsEnabled = autoplayGifsEnabled,
                                    onChatClick = { chat -> navController.navigate(Screen.Chat.createRoute(chat.id)) },
                                    onMessageClick = { msg -> navController.navigate(Screen.Chat.createRoute(msg.conversationId)) },
                                    onPostClick = { GlobalNavigation.navigateTo(Screen.PostDetail.createRoute(it.id)) },
                                    onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                                )
                            } else {
                                when {
                                    chatsState is Resource.Loading && sanitizedChats.isEmpty() -> {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(10) { ChatItemShimmer() }
                                        }
                                    }
                                    chatsState is Resource.Error && sanitizedChats.isEmpty() -> {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(24.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                                            Spacer(Modifier.height(16.dp))
                                            Text("Could not load chats", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                            Text(chatsState.message ?: "Unknown error", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                                            Spacer(Modifier.height(24.dp))
                                            Button(onClick = { chatViewModel.fetchChats() }) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                    else -> {
                                        ChatList(
                                            chats = sanitizedChats,
                                            archivedChatsCount = archivedChatsState.data?.size ?: 0,
                                            stories = storiesState.data ?: emptyList<UserStory>(),
                                            chatViewModel = chatViewModel,
                                            navController = navController,
                                            topBarHeight = 0.dp, // No spacer inside list
                                            bottomPadding = bottomPadding,
                                            currentUserId = currentUser?.id,
                                            initialScrollDone = isChatInitialScrollDone,
                                            onInitialScrollDone = { isChatInitialScrollDone = it },
                                            onChatClick = { chat -> 
                                                val typeStr = when (chat.type) {
                                                    ConversationType.CHANNEL -> "channel"
                                                    ConversationType.GROUP -> "group"
                                                    ConversationType.JOB_CHAT -> "job_chat"
                                                    else -> "direct"
                                                }
                                                navController.navigate(Screen.Chat.createRoute(chat.id, type = typeStr)) 
                                            },
                                            onStoryClick = { story -> navController.navigate(Screen.Story.createRoute(userId = story.userId.ifBlank { story.id })) },
                                            onMediaStoryClick = { navController.navigate(Screen.NewStory.route) },
                                            onTextStoryClick = { navController.navigate(Screen.Story.createRoute(showComposer = true)) },
                                            onArchivedClick = { navController.navigate(Screen.ArchivedChats.route) },
                                            onPostedStoriesClick = { navController.navigate(Screen.Story.createRoute(userId = currentUser?.id, play = false)) },
                                            onAvatarClick = { userId -> 
                                                val chat = sanitizedChats.find { it.id == userId }
                                                if (chat?.avatarUrl != null) {
                                                    navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                                                } else {
                                                    navController.navigate(Screen.ChatProfile.createRoute(userId, chat?.isGroup ?: false))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            val autoplayEnabled by mainViewModel.autoplayVideos.collectAsState()
                            val autoplayGifsEnabled by mainViewModel.autoplayGifs.collectAsState()
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (isSearchActive && searchQuery.length > 0) {
                                        SearchResultsView(
                                            results = searchResults,
                                            type = SearchType.CHANNELS,
                                            autoplayEnabled = autoplayEnabled,
                                            autoplayGifsEnabled = autoplayGifsEnabled,
                                            onChatClick = {},
                                            onMessageClick = {},
                                            onPostClick = { GlobalNavigation.navigateTo(Screen.PostDetail.createRoute(it.id)) },
                                            onChannelClick = { navController.navigate(Screen.Chat.createRoute(it.conversationId ?: it.id, type = "channel")) },
                                            onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                                        )
                                    } else {
                                        DataStateHandler(channelsState) { channels -> 
                                        ChannelList(
                                            channels = channels, 
                                            topBarHeight = 0.dp,
                                            onChannelClick = {
                                                navController.navigate(Screen.Chat.createRoute(it.conversationId ?: it.id, type = "channel"))
                                            },
                                            onSubscribe = { id -> channelsViewModel.subscribeToChannel(id) }
                                        ) 
                                    }
                                    }
                                }
                            }
                        }
                        2 -> {
                            val searchResults by searchViewModel.searchResults.collectAsState()
                            val autoplayEnabled by mainViewModel.autoplayVideos.collectAsState()
                            val autoplayGifsEnabled by mainViewModel.autoplayGifs.collectAsState()
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (isSearchActive && searchQuery.length > 0) {
                                        SearchResultsView(
                                            results = searchResults,
                                            type = SearchType.JOBS,
                                            autoplayEnabled = autoplayEnabled,
                                            autoplayGifsEnabled = autoplayGifsEnabled,
                                            onChatClick = {},
                                            onMessageClick = {},
                                            onPostClick = { GlobalNavigation.navigateTo(Screen.PostDetail.createRoute(it.id)) },
                                            onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                            onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                                        )
                                    } else {
                                        DataStateHandler(jobsState) { jobs -> 
                                            JobList(jobs, topBarHeight = 0.dp) { 
                                                navController.navigate(Screen.JobDetail.createRoute(it.id)) 
                                            } 
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            RewardsScreen(
                                viewModel = rewardViewModel,
                                topBarHeight = 0.dp
                            )
                        }
                        4 -> {
                            // DM Tab: Shows all direct messages from social profiles
                            DataStateHandler(chatsState) { chats ->
                                val dmChats = chats.filter { it.type == ConversationType.DIRECT }
                                if (dmChats.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No direct messages yet",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 80.dp)
                                    ) {
                                        items(dmChats, key = { it.id }) { chat ->
                                            Box(modifier = Modifier.clickable {
                                                navController.navigate(
                                                    Screen.Chat.createRoute(
                                                        chat.id,
                                                        type = "direct"
                                                    )
                                                )
                                            }) {
                                                ChatItem(chat = chat)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

@Composable
fun ChatList(
    chats: List<ChatMessage>,
    archivedChatsCount: Int,
    stories: List<UserStory>,
    chatViewModel: ChatViewModel,
    navController: NavController,
    topBarHeight: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    currentUserId: String? = null,
    initialScrollDone: Boolean = false,
    onInitialScrollDone: (Boolean) -> Unit = {},
    onChatClick: (ChatMessage) -> Unit,
    onAvatarClick: (String) -> Unit = {},
    onStoryClick: (UserStory) -> Unit,
    onMediaStoryClick: () -> Unit,
    onTextStoryClick: () -> Unit,
    onArchivedClick: () -> Unit,
    onPostedStoriesClick: () -> Unit
) {
    var selectedChatForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    
    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(chats, stories, archivedChatsCount, topBarHeight) {
        if (!initialScrollDone && (chats.size > 0 || archivedChatsCount > 0) && topBarHeight.value > 0f) {
            snapshotFlow { lazyListState.layoutInfo.totalItemsCount }.first { it > 2 }
            delay(500)
            val h = topBarHeight.value * density.density
            val hInt = h.toInt()
            var i = 0
            while (i < 3) {
                lazyListState.scrollToItem(2, -hInt)
                delay(100)
                i++
            }
            onInitialScrollDone(true)
        }
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = uiState.isChatsFetching,
        onRefresh = { chatViewModel.fetchChats() }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
        ) {
        item(key = "stories_row_item") {
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
                        val a = (1f - (firstItemOffset / 300f))
                        alpha = if (a < 0f) 0f else a
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

        if (chats.size == 0 && archivedChatsCount == 0) {
            item(key = "empty_chats_item") {
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
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { chatViewModel.fetchChats() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh")
                        }
                        TextButton(
                            onClick = { navController.navigate(Screen.ContactPicker.route) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Find Friends")
                        }
                    }
                }
            }
        }

        if (archivedChatsCount > 0) {
            item(key = "archived_header_item") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArchivedClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = AppAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Archived", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(text = archivedChatsCount.toString(), color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        items(chats.distinctBy { it.id }, key = { it.id }) { chat ->
            val story = stories.find { 
                it.id == chat.id || it.userId == chat.id || (chat.otherUserId != null && (it.id == chat.otherUserId || it.userId == chat.otherUserId))
            }
            val hasStory = story != null
            var allStoriesViewed = false
            if (story != null) {
                allStoriesViewed = true
                for (item in story.stories) {
                    if (item.isViewed == false) {
                        allStoriesViewed = false
                        break
                    }
                }
            }
            
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
                    onAvatarClick = { onAvatarClick(chat.id) }
                )
            }
        }
    }
    }

    if (selectedChatForMenu != null) {
        val selectedChat = selectedChatForMenu!!
        AlertDialog(
            onDismissRequest = { selectedChatForMenu = null },
            title = { Text(selectedChat.senderName, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (selectedChat.isPinned) "Unpin Chat" else "Pin Chat", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.PushPin, null, tint = AppAccent, modifier = Modifier.rotate(if (selectedChat.isPinned) 0f else 45f)) },
                        modifier = Modifier.clickable { 
                            chatViewModel.pinChat(selectedChat.id, !selectedChat.isPinned)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(if (selectedChat.isMuted) "Unmute Notifications" else "Mute Notifications", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(if (selectedChat.isMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, null, tint = AppAccent) },
                        modifier = Modifier.clickable { 
                            chatViewModel.muteChat(selectedChat.id, !selectedChat.isMuted)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Archive Chat", color = MaterialTheme.colorScheme.onSurface) },
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SearchResultsView(
    results: com.example.firstapp.search.model.SearchResult?,
    type: com.example.firstapp.search.model.SearchType = com.example.firstapp.search.model.SearchType.GLOBAL,
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    onChatClick: (ChatMessage) -> Unit,
    onMessageClick: (Message) -> Unit,
    onJobClick: (Job) -> Unit = {},
    onChannelClick: (Channel) -> Unit = {},
    onPostClick: (com.example.firstapp.media.model.Post) -> Unit = {},
    onContactClick: (Contact) -> Unit = {}
) {
    if (results == null) return
    
    val allChats = results.chats
    val contacts = java.util.ArrayList<Contact>()
    for (c in results.contacts) {
        var found = false
        for (chat in allChats) {
            if (chat.id == c.id) {
                found = true
                break
            }
        }
        if (!found) contacts.add(c)
    }
    
    val messages = results.messages
    val artists = results.artists
    val jobs = results.jobs
    val channels = results.channels
    val posts = results.posts

    val showChats = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showContacts = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showMessages = type == SearchType.GLOBAL || type == SearchType.CHATS
    val showJobs = type == SearchType.GLOBAL || type == SearchType.JOBS
    val showChannels = type == SearchType.GLOBAL || type == SearchType.CHANNELS
    val showMusic = type == SearchType.GLOBAL || type == SearchType.MUSIC

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showContacts && contacts.size > 0) {
            item { SearchSectionHeader("People & Contacts") }
            items(items = contacts, key = { "contact_" + it.id }) { contactItem: Contact ->
                Column(modifier = Modifier.clickable { onContactClick(contactItem) }) {
                    ContactSearchItem(contactItem)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (showJobs && jobs.size > 0) {
            item { SearchSectionHeader("Jobs") }
            items(jobs, key = { "job_" + it.id }) { job ->
                Column(modifier = Modifier.clickable { onJobClick(job) }) {
                    JobItem(job = job, onClick = { onJobClick(job) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (showChannels && channels.size > 0) {
            item { SearchSectionHeader("Channels") }
            items(channels, key = { "channel_" + it.id }) { channel ->
                Column(modifier = Modifier.clickable { onChannelClick(channel) }) {
                    ChannelItem(channel = channel, onClick = { onChannelClick(channel) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (posts.isNotEmpty()) {
            item { SearchSectionHeader("Media Posts") }
            items(posts, key = { "post_" + it.id }) { post ->
                Column(modifier = Modifier.clickable { onPostClick(post) }) {
                    com.example.firstapp.media.ui.components.PostItem(
                        adManager = com.example.firstapp.common.data.ads.AdMobManagerImpl(), 
                        post = post,
                        autoplayEnabled = autoplayEnabled,
                        autoplayGifsEnabled = autoplayGifsEnabled,
                        onClick = { onPostClick(post) },
                        onLike = { _, _, _ -> },
                        onRepostClick = { },
                        onReplyClick = { _, _ -> },
                        onProfileClick = { }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (showChats && allChats.size > 0) {
            item { SearchSectionHeader("Chats") }
            items(allChats, key = { "chat_" + it.id }) { chat ->
                Column(modifier = Modifier.clickable { onChatClick(chat) }) {
                    ChatItem(chat)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (showMessages && messages.size > 0) {
            item { SearchSectionHeader("Messages") }
            items(messages, key = { "message_" + it.id }) { message ->
                Column(modifier = Modifier.clickable { onMessageClick(message) }) {
                    MessageSearchItem(message)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
                Text(message.senderName ?: "", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                val d = contact.displayName
                val initial = if (d.length > 0) d.toString().substring(0, 1).uppercase(java.util.Locale.ROOT) else "?"
                Text(initial, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(contact.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("@" + contact.username, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
