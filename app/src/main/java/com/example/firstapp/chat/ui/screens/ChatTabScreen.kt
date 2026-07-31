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
import com.example.firstapp.chat.ui.viewmodel.*
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
import com.example.firstapp.media.ui.viewmodel.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.ui.components.*
import java.util.*
import kotlin.collections.*
import kotlin.text.*
import com.example.firstapp.media.ui.components.StoriesRow
import com.example.firstapp.home.ui.screens.RewardsScreen
import com.example.firstapp.chat.jobs.ui.screens.JobList
import com.example.firstapp.chat.channels.ChannelList
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.ui.components.DynamicAdCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    navController: NavController,
    onLogout: () -> Unit,
    adManager: AdManager,
    onShowMore: () -> Unit = {}, 
    chatViewModel: ChatViewModel = hiltViewModel(),
    advertsViewModel: AdvertsViewModel = hiltViewModel(),
    channelsViewModel: ChannelsViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    jobViewModel: JobViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel()
) {
    var selectedTabItem by remember { mutableIntStateOf(0) }
    val chatsState by chatViewModel.chats.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val unreadChatsCount = remember(chatsState) {
        val data = chatsState.data
        var sum = 0
        if (data != null) {
            for (c in data) sum += c.unreadCount
        }
        sum
    }
    
    val sanitizedChats = remember(chatsState) {
        chatsState.data ?: emptyList()
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var isChatInitialScrollDone by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val storiesState by storyViewModel.stories.collectAsState()
    val archivedChatsState by chatViewModel.archivedChats.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val points by advertsViewModel.points.collectAsState()
    val jobsState by jobViewModel.jobs.collectAsState()
    val advertsState by advertsViewModel.adverts.collectAsState()
    val viewedAdvertsState by advertsViewModel.viewedAdverts.collectAsState()
    val channelsState by channelsViewModel.channels.collectAsState()
    val hasRewardedAds by advertsViewModel.hasRewardedAdsAvailable.collectAsState()

    LaunchedEffect(selectedTabItem) {
        if (selectedTabItem == 1) {
            advertsViewModel.initializePool(context, "ca-app-pub-3940256099942544/5224354917")
        }
    }

    val tabs = listOf("Chats", "Rewards", "Jobs", "Channels")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        topBarHeight = (coords.size.height.toFloat() / density.density).dp
                    }
                    .zIndex(1f),
                color = AppBackground,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(AppBackground, AppBackground.copy(alpha = 0.95f))
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchActive) {
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
                                    IconButton(onClick = { isSearchActive = false; searchQuery = "" ; searchViewModel.search("", searchType) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = ""; searchViewModel.search("", searchType) }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            AppLogo(size = 32.dp, modifier = Modifier.clickable { navController.navigate(Screen.Story.createRoute(userId = currentUser?.id)) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "FirstApp",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            IconButton(onClick = { isSearchActive = true }) { 
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary) 
                            }
                            
                            Box {
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) { 
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextPrimary) 
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(AppSecondary)
                                ) {
                                    when (selectedTabItem) {
                                        0 -> {
                                            DropdownMenuItem(
                                                text = { Text("Contacts", color = Color.White) },
                                                onClick = { showMenu = false; navController.navigate(Screen.ContactPicker.route) },
                                                leadingIcon = { Icon(Icons.Default.Person, null, tint = AppAccent) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("New Group", color = Color.White) },
                                                onClick = { showMenu = false; navController.navigate(Screen.ContactPicker.createRoute(isGroupMode = true)) },
                                                leadingIcon = { Icon(Icons.Default.GroupAdd, null, tint = AppAccent) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Starred Messages", color = Color.White) },
                                                onClick = { showMenu = false; navController.navigate(Screen.StarredMessages.route) },
                                                leadingIcon = { Icon(Icons.Default.Star, null, tint = Color.Yellow) }
                                            )
                                        }
                                        1 -> {
                                            DropdownMenuItem(
                                                text = { Text("Wallet", color = Color.White) },
                                                onClick = { showMenu = false; navController.navigate(Screen.Wallet.route) },
                                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.Green) }
                                            )
                                        }
                                        2 -> {
                                            DropdownMenuItem(
                                                text = { Text("Job Profile", color = Color.White) },
                                                onClick = { showMenu = false; navController.navigate(Screen.JobProfile.route) },
                                                leadingIcon = { Icon(Icons.Default.Work, null, tint = AppAccent) }
                                            )
                                        }
                                        3 -> {
                                            DropdownMenuItem(
                                                text = { Text("Add Channel", color = Color.White) },
                                                onClick = { 
                                                    showMenu = false
                                                    onShowMore()
                                                },
                                                leadingIcon = { Icon(Icons.Default.Add, null, tint = AppAccent) }
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = Color.White) },
                                        onClick = { showMenu = false; navController.navigate(Screen.Settings.route) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.Gray) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign Out", color = Color(0xFFE57373)) },
                                        onClick = { showMenu = false; onLogout() },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFE57373)) }
                                    )
                                }
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
                                targetValue = if (isSelected) AppAccent else AppSecondary,
                                label = "tabColor"
                            )
                            val animatedContentColor by animateColorAsState(
                                targetValue = if (isSelected) AppBackground else Color.White,
                                label = "tabContentColor"
                            )
                            
                            Surface(
                                onClick = { selectedTabItem = index },
                                color = animatedColor,
                                shape = RoundedCornerShape(24.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
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
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        floatingActionButton = {
            if (selectedTabItem == 0 && !isSearchActive) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ContactPicker.route) },
                    containerColor = AppAccent,
                    contentColor = AppBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
                }
            }
        },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabItem) {
                    0 -> {
                        val searchResults by searchViewModel.searchResults.collectAsState()
                        if (isSearchActive && searchQuery.length > 0) {
                            SearchResultsView(
                                results = searchResults,
                                type = SearchType.CHATS,
                                onChatClick = { chat -> navController.navigate(Screen.Chat.createRoute(chat.id)) },
                                onMessageClick = { msg -> navController.navigate(Screen.Chat.createRoute(msg.conversationId)) },
                                onSongClick = { song, queue -> musicViewModel.playSong(song, queue) },
                                onArtistClick = { artist -> musicViewModel.requestArtist(artist.displayName) },
                                onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                            )
                        } else {
                            if (chatsState is Resource.Loading && sanitizedChats.isEmpty()) {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = topBarHeight)) {
                                    items(10) { ChatItemShimmer() }
                                }
                            } else {
                                ChatList(
                                    chats = sanitizedChats,
                                    archivedChatsCount = archivedChatsState.data?.size ?: 0,
                                    stories = storiesState.data ?: emptyList<UserStory>(),
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
                                    onStoryClick = { story -> navController.navigate(Screen.Story.createRoute(userId = story.id)) },
                                    onMediaStoryClick = { navController.navigate(Screen.NewStory.route) },
                                    onTextStoryClick = { navController.navigate(Screen.Story.createRoute(showComposer = true)) },
                                    onArchivedClick = { navController.navigate(Screen.ArchivedChats.route) },
                                    onPostedStoriesClick = { navController.navigate(Screen.Story.createRoute(userId = currentUser?.id)) },
                                    onAvatarClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
                                )
                            }
                        }
                    }
                    1 -> {
                        val ads = advertsState.data ?: emptyList<Advert>()
                        val viewedAds = viewedAdvertsState.data ?: emptyList<Advert>()
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                DataStateHandler(
                                    resource = advertsState,
                                    onLoading = { SkeletonLoader() },
                                    onEmpty = {
                                        RewardsScreen(
                                            adverts = emptyList(),
                                            viewedAdverts = viewedAds,
                                            points = points,
                                            topBarHeight = topBarHeight,
                                            onAdvertClick = { ad -> 
                                                val activity = context as? android.app.Activity
                                                if (activity != null) {
                                                    advertsViewModel.adManager.loadRewardedAd(
                                                        context = context,
                                                        adUnitId = "ca-app-pub-3940256099942544/5224354917", // AdMob Rewarded Test ID
                                                        onAdLoaded = { adObject ->
                                                            advertsViewModel.adManager.showRewardedAd(
                                                                activity = activity,
                                                                adObject = adObject,
                                                                onRewardEarned = {
                                                                    advertsViewModel.markAdvertAsViewed(ad.id)
                                                                    Toast.makeText(context, "Reward Earned!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                onAdClosed = { }
                                                            )
                                                        },
                                                        onAdFailed = { error ->
                                                            Toast.makeText(context, "Ad Failed: $error", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            },
                                            onRefresh = { advertsViewModel.fetchAdverts() },
                                            onResetAds = { advertsViewModel.resetAds() }
                                        )
                                    }
                                ) {
                                     RewardsScreen(
                                        adverts = ads,
                                        viewedAdverts = viewedAds,
                                        points = points,
                                        hasAvailableAds = hasRewardedAds,
                                        topBarHeight = topBarHeight,
                                        onAdvertClick = { ad -> 
                                            val activity = context as? android.app.Activity
                                            if (activity != null) {
                                                advertsViewModel.showRewardedAd(
                                                    activity = activity,
                                                    adUnitId = "ca-app-pub-3940256099942544/5224354917",
                                                    advertId = ad.id
                                                )
                                            }
                                        },
                                        onRefresh = { advertsViewModel.fetchAdverts() },
                                        onResetAds = { advertsViewModel.resetAds() }
                                    )
                                }
                            }
                            
                            // Banner Ad only on Rewards tab
                            DynamicAdCard(
                                adManager = adManager,
                                adUnitId = "ca-app-pub-3940256099942544/6300978111", // AdMob Banner Test ID
                                isBanner = true
                            )
                        }
                    }
                    2 -> {
                        val searchResults by searchViewModel.searchResults.collectAsState()
                        if (isSearchActive && searchQuery.length > 0) {
                            SearchResultsView(
                                results = searchResults,
                                type = SearchType.JOBS,
                                onChatClick = {},
                                onMessageClick = {},
                                onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                            )
                        } else {
                            DataStateHandler(jobsState) { jobs -> 
                                JobList(jobs, topBarHeight = topBarHeight) { 
                                    navController.navigate(Screen.JobDetail.createRoute(it.id)) 
                                } 
                            }
                        }
                    }
                    3 -> {
                        val searchResults by searchViewModel.searchResults.collectAsState()
                        if (isSearchActive && searchQuery.length > 0) {
                            SearchResultsView(
                                results = searchResults,
                                type = SearchType.CHANNELS,
                                onChatClick = {},
                                onMessageClick = {},
                                onChannelClick = { navController.navigate(Screen.Chat.createRoute(it.id, type = "channel")) },
                                onContactClick = { contact -> navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) }
                            )
                        } else {
                            DataStateHandler(channelsState) { channels -> 
                                ChannelList(channels, topBarHeight = topBarHeight) { 
                                    navController.navigate(Screen.Chat.createRoute(it.id, type = "channel"))
                                } 
                            }
                        }
                    }
                }
            }
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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
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
            var story: UserStory? = null
            for (s in stories) {
                if (s.userId == chat.id) {
                    story = s
                    break
                }
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
                    onAvatarClick = {
                        if (hasStory && story != null) {
                            onStoryClick(story)
                        } else {
                            onAvatarClick(chat.id)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
            }
        }
    }

    if (selectedChatForMenu != null) {
        val selectedChat = selectedChatForMenu!!
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
        if (showContacts && contacts.size > 0) {
            item { SearchSectionHeader("Contacts") }
            items(items = contacts, key = { "contact_" + it.id }) { contactItem: Contact ->
                Column(modifier = Modifier.clickable { onContactClick(contactItem) }) {
                    ContactSearchItem(contactItem)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMusic && artists.size > 0) {
            item { SearchSectionHeader("Artists") }
            items(artists, key = { "artist_" + it.id }) { artist ->
                Column(modifier = Modifier.clickable { onArtistClick(artist) }) {
                    ContactSearchItem(artist)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMusic && songs.size > 0) {
            item { SearchSectionHeader("Songs") }
            items(songs, key = { "song_" + it.id }) { song ->
                Column(modifier = Modifier.clickable { onSongClick(song, songs) }) {
                    SongSearchItem(song)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showJobs && jobs.size > 0) {
            item { SearchSectionHeader("Jobs") }
            items(jobs, key = { "job_" + it.id }) { job ->
                Column(modifier = Modifier.clickable { onJobClick(job) }) {
                    JobItem(job = job, onClick = { onJobClick(job) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showChannels && channels.size > 0) {
            item { SearchSectionHeader("Channels") }
            items(channels, key = { "channel_" + it.id }) { channel ->
                Column(modifier = Modifier.clickable { onChannelClick(channel) }) {
                    com.example.firstapp.media.ui.components.ChannelItem(channel = channel, onClick = { onChannelClick(channel) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showChats && allChats.size > 0) {
            item { SearchSectionHeader("Chats") }
            items(allChats, key = { "chat_" + it.id }) { chat ->
                Column(modifier = Modifier.clickable { onChatClick(chat) }) {
                    ChatItem(chat)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }

        if (showMessages && messages.size > 0) {
            item { SearchSectionHeader("Messages") }
            items(messages, key = { "message_" + it.id }) { message ->
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
                Text(message.senderName ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                val d = contact.displayName
                val initial = if (d.length > 0) d.toString().substring(0, 1).uppercase(java.util.Locale.ROOT) else "?"
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(contact.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("@" + contact.username, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
