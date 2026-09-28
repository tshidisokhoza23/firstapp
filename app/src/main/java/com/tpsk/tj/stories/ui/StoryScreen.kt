package com.tpsk.tj.stories.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.chat.model.StoryItem
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.chat.ui.components.formatMediaUrl
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.chat.rewards.data.RewardsManager
import com.tpsk.tj.chat.ui.components.populateNativeAdView
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.ui.components.BannerAdView
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun StoryScreen(
    stories: List<UserStory>,
    contacts: List<com.tpsk.tj.chat.model.Contact> = emptyList(),
    currentUserId: String?,
    mediaViewModel: MediaViewModel,
    adManager: AdManager,
    initialViewingUserId: String? = null,
    initialStoryId: String? = null,
    storiesReady: Boolean = false,
    initialShowComposer: Boolean = false,
    initialPlay: Boolean = true,
    onBack: () -> Unit,
    onPostStory: (String, String?, String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onStoryViewed: (String, String) -> Unit,
    onMuteStory: (String, Boolean) -> Unit,
    onNavigateToNewStory: () -> Unit = {},
    onGetStoryViewers: (String) -> kotlinx.coroutines.flow.Flow<com.tpsk.tj.common.util.Resource<List<com.tpsk.tj.media.data.api.ApiViewer>>> = { kotlinx.coroutines.flow.flowOf() },
    onReplyStory: (userId: String, storyId: String, text: String, storySummary: String, authorName: String) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current

    var viewingUserStory by remember { 
        mutableStateOf(if (initialPlay) stories.find { it.id == initialViewingUserId || it.userId == initialViewingUserId } else null)
    }
    var viewingStoryIndex by remember { mutableIntStateOf(0) }
    val storyPositions = remember { mutableStateMapOf<String, Int>() }

    var storyToMute by remember { mutableStateOf<UserStory?>(null) }
    var selectedStoryForViewers by remember { mutableStateOf<StoryItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(initialShowComposer) }
    var composerText by remember { mutableStateOf("") }
    var updatesTabState by remember { mutableIntStateOf(0) }

    // Full-screen story ads are preloaded, then shown between other people's stories.
    var loadedAdObject by remember { mutableStateOf<Any?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    var showingStoryAd by remember { mutableStateOf(false) }
    var usersSinceAd by remember { mutableIntStateOf(0) }
    val postedArchive = !initialPlay

    LaunchedEffect(viewingUserStory, postedArchive, showingStoryAd) {
        if (!postedArchive && viewingUserStory != null && !showingStoryAd && loadedAdObject == null && !isAdLoading) {
            isAdLoading = true
            adManager.loadNativeAd(
                context = context,
                adUnitId = com.tpsk.tj.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
                onAdLoaded = { ad ->
                    loadedAdObject = ad
                    isAdLoading = false
                },
                onAdFailed = {
                    isAdLoading = false
                }
            )
        }
    }

    val filteredStories = remember(stories, searchQuery) {
        if (searchQuery.isBlank()) {
            stories
        } else {
            val q = searchQuery.trim().lowercase()
            stories.filter { it.userName.lowercase().contains(q) }
        }
    }

    val myStory = remember(stories, currentUserId) {
        stories.find { 
            it.id == currentUserId || 
            it.userId == currentUserId ||
            (currentUserId != null && (it.id == "user_$currentUserId" || it.id == currentUserId.removePrefix("user_")))
        } ?: stories.find { 
            it.userName == "My Story" || it.userName == "My Number" 
        }
    }

    val contactIds = remember(contacts) {
        contacts.map { it.id }.toSet() + contacts.map { "user_${it.id}" }.toSet() + contacts.map { it.id.removePrefix("user_") }.toSet()
    }
    val contactNames = remember(contacts) { contacts.map { it.displayName.lowercase().trim() }.toSet() }

    val otherStories = remember(filteredStories, currentUserId, myStory, contactIds, contactNames) {
        filteredStories.filter { story ->
            if (story.id == currentUserId || story.userId == currentUserId || story.id == myStory?.id) return@filter false
            if (story.id == "system_ad") return@filter false
            if (contactIds.isEmpty()) return@filter true
            val keys = listOf(story.id, story.userId, story.id.removePrefix("user_"), story.userId.removePrefix("user_"))
            keys.any { it.isNotBlank() && it in contactIds }
        }
    }

    val activeStories = remember(otherStories) { otherStories.filter { !it.isMuted } }
    
    val recentUpdates = remember(activeStories) {
        activeStories.filter { !it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
    }
    val viewedUpdates = remember(activeStories) {
        activeStories.filter { it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
    }
    val playbackQueue = remember(postedArchive, myStory, recentUpdates, viewedUpdates) {
        if (postedArchive) {
            listOfNotNull(myStory?.takeIf { it.stories.isNotEmpty() })
        } else {
            val baseList = mutableListOf<UserStory>()
            if (myStory != null && myStory.stories.isNotEmpty()) baseList.add(myStory)
            baseList.addAll(recentUpdates)
            baseList.addAll(viewedUpdates)
            baseList.distinctBy { it.id }
        }
    }

    fun leaveViewer() {
        showingStoryAd = false
        if (postedArchive || initialViewingUserId.isNullOrBlank()) {
            viewingUserStory = null
        } else {
            onBack()
        }
    }

    fun storyPositionKey(story: UserStory): String = story.userId.ifBlank { story.id }

    fun savedStoryIndex(story: UserStory): Int {
        val saved = storyPositions[storyPositionKey(story)] ?: 0
        return saved.coerceIn(0, (story.stories.size - 1).coerceAtLeast(0))
    }

    fun rememberStoryIndex(story: UserStory, index: Int) {
        if (story.stories.isEmpty()) return
        storyPositions[storyPositionKey(story)] = index.coerceIn(0, story.stories.lastIndex)
    }

    fun advanceQueue(current: UserStory, direction: Int, fromIndex: Int = viewingStoryIndex) {
        rememberStoryIndex(current, fromIndex)
        val index = playbackQueue.indexOfFirst { it.id == current.id || it.userId == current.id || it.id == current.userId || it.userId == current.userId }
        val next = if (index == -1) null else playbackQueue.getOrNull(index + direction)
        val own = isOwnStoryGroup(current, currentUserId, myStory)
        if (!postedArchive && direction > 0 && !own) {
            usersSinceAd += 1
        }
        val nativeAd = loadedAdObject as? NativeAd
        if (!postedArchive && direction > 0 && !own && usersSinceAd >= 4 && nativeAd != null && next != null) {
            usersSinceAd = 0
            viewingUserStory = next
            viewingStoryIndex = savedStoryIndex(next)
            showingStoryAd = true
            return
        }
        showingStoryAd = false
        if (next != null) {
            viewingUserStory = next
            viewingStoryIndex = savedStoryIndex(next)
        } else {
            leaveViewer()
        }
    }

    androidx.activity.compose.BackHandler(enabled = showingStoryAd || viewingUserStory != null || showComposer || showSearch) {
        when {
            showingStoryAd || viewingUserStory != null -> leaveViewer()
            showComposer -> showComposer = false
            showSearch -> {
                showSearch = false
                searchQuery = ""
            }
        }
    }

    // Sync viewing user story with latest data
    LaunchedEffect(stories) {
        viewingUserStory?.let { current ->
            if (current.id == "system_ad") return@let
            val updated = stories.find {
                it.id == current.id || it.userId == current.id || it.id == current.userId || it.userId == current.userId
            }
            if (updated != null && updated.stories.isNotEmpty()) {
                if (updated.stories != current.stories || updated.userName != current.userName) {
                    viewingUserStory = updated
                }
                if (viewingStoryIndex >= updated.stories.size) {
                    viewingStoryIndex = (updated.stories.size - 1).coerceAtLeast(0)
                }
            } else if (stories.isNotEmpty()) {
                if (!postedArchive && !initialViewingUserId.isNullOrBlank()) {
                    onBack()
                } else {
                    viewingUserStory = null
                }
            }
        }
    }

    // Initial play logic
    LaunchedEffect(initialViewingUserId, initialStoryId, initialPlay, stories, storiesReady) {
        if (!initialPlay || initialViewingUserId.isNullOrBlank() || viewingUserStory != null) return@LaunchedEffect
        val wantedUser = initialViewingUserId.removePrefix("user_")
        val story = stories.find {
            val keys = listOf(it.id, it.userId, it.id.removePrefix("user_"), it.userId.removePrefix("user_"))
            keys.any { key -> key.isNotBlank() && (key == initialViewingUserId || key == wantedUser) }
        }
        if (story != null && story.stories.isNotEmpty()) {
            val wantedStory = initialStoryId?.removePrefix("story_")?.substringBefore("|")
            val index = if (wantedStory.isNullOrBlank()) 0 else story.stories.indexOfFirst { it.id == wantedStory }.let { if (it >= 0) it else 0 }
            viewingStoryIndex = index
            viewingUserStory = story
        } else if (storiesReady) {
            Toast.makeText(context, "This story has expired or is no longer available.", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // Release an ad when the viewer closes
    LaunchedEffect(viewingUserStory, showingStoryAd) {
        if (viewingUserStory == null && !showingStoryAd) {
            loadedAdObject?.let { adManager.destroyAd(it) }
            loadedAdObject = null
            isAdLoading = false
        }
    }

    if (showComposer) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Text Story") },
                    navigationIcon = {
                        IconButton(onClick = { showComposer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background, 
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        if (composerText.isNotBlank()) {
                            onPostStory(composerText, null, "text")
                            composerText = ""
                            showComposer = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Share story", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                TextField(
                    value = composerText,
                    onValueChange = { composerText = it },
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    placeholder = { 
                        Text(
                            "Type a story...", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                            fontSize = 24.sp, 
                            textAlign = TextAlign.Center, 
                            modifier = Modifier.fillMaxWidth()
                        ) 
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground, 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.Bold, 
                        textAlign = TextAlign.Center
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    } else if (showingStoryAd && loadedAdObject is NativeAd) {
        StoryNativeAdScreen(
            nativeAd = loadedAdObject as NativeAd,
            onContinue = {
                loadedAdObject?.let { adManager.destroyAd(it) }
                loadedAdObject = null
                isAdLoading = false
                showingStoryAd = false
            },
            onClose = { leaveViewer() }
        )
    } else if (viewingUserStory != null && viewingUserStory!!.stories.isNotEmpty()) {
        val currentGroup = viewingUserStory!!
        val isOwnStory = currentGroup.id == currentUserId || 
                         currentGroup.userId == currentUserId || 
                         currentGroup.id == myStory?.id ||
                         (currentUserId != null && (
                             currentGroup.id == "user_$currentUserId" || 
                             currentGroup.id == currentUserId.removePrefix("user_") ||
                             currentGroup.userId == "user_$currentUserId" ||
                             currentGroup.userId == currentUserId.removePrefix("user_")
                         )) ||
                         currentGroup.userName == "My Story" ||
                         currentGroup.userName == "My Number"

        StoryViewer(
            userStory = currentGroup,
            isOwnStory = isOwnStory,
            initialStoryIndex = viewingStoryIndex,
            onStoryViewed = { storyIndex ->
                val storyId = currentGroup.stories.getOrNull(storyIndex)?.id
                if (storyId != null && currentGroup.id != "system_ad") {
                    onStoryViewed(currentGroup.userId.ifBlank { currentGroup.id }, storyId)
                }
            },
            onReplyStory = { storyId, text, summary ->
                val item = currentGroup.stories.find { it.id == storyId }
                val media = item?.contentUrl?.takeIf { it.isNotBlank() }
                val richSummary = buildString {
                    append("[Story] ")
                    append(summary.ifBlank { item?.content?.takeIf { it.isNotBlank() } ?: "Story" })
                    if (!media.isNullOrBlank()) append(" [mediaUrl:$media]")
                }
                onReplyStory(
                    currentGroup.userId.ifBlank { currentGroup.id },
                    storyId,
                    text,
                    richSummary,
                    currentGroup.userName
                )
            },
            onFinish = { advanceQueue(currentGroup, 1, viewingStoryIndex) },
            onStoryIndexChanged = { index ->
                viewingStoryIndex = index
                rememberStoryIndex(currentGroup, index)
            },
            onNavigateToUser = { direction, storyIndex ->
                advanceQueue(currentGroup, direction, storyIndex)
            },
            onGetStoryViewers = onGetStoryViewers,
            onClose = { leaveViewer() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (showSearch) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search stories...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text("Posted stories", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    },
                    actions = {
                        if (showSearch) {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        } else {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search stories")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                BannerAdView(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { showComposer = true },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Text story")
                    }
                    FloatingActionButton(
                        onClick = onNavigateToNewStory,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera story")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // My Posted Stories Horizontal Row
                if (myStory != null && myStory.stories.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(myStory.stories) { index, storyItem ->
                                val rawUrl = storyItem.contentUrl
                                val itemThumb = remember(rawUrl) { formatMediaUrl(rawUrl) }
                                val imageRequest = remember(itemThumb, context) {
                                    if (!itemThumb.isNullOrBlank()) {
                                        coil.request.ImageRequest.Builder(context)
                                            .data(itemThumb)
                                            .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                                            .crossfade(true)
                                            .build()
                                    } else null
                                }

                                val postedTime = remember(storyItem.createdAt, storyItem.timestamp) {
                                    if (storyItem.createdAt > 0L) {
                                        com.tpsk.tj.chat.data.repository.formatTimeAgo(storyItem.createdAt)
                                    } else if (storyItem.timestamp.isNotBlank()) {
                                        storyItem.timestamp
                                    } else {
                                        "recently"
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        viewingUserStory = myStory
                                        viewingStoryIndex = index
                                        rememberStoryIndex(myStory, index)
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(if (storyItem.type == StoryType.TEXT) storyItem.backgroundColor else myStory.userAvatarColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (imageRequest != null) {
                                                    AsyncImage(
                                                        model = imageRequest,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else if (storyItem.type == StoryType.TEXT && storyItem.content.isNotBlank()) {
                                                    Text(
                                                        text = storyItem.content,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        modifier = Modifier.padding(2.dp)
                                                    )
                                                } else if (!myStory.userAvatarUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = myStory.userAvatarUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = myStory.userName.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveRedEye,
                                            contentDescription = "Views",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${storyItem.viewsCount} views",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = postedTime,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty My Story placeholder row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToNewStory() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(AppPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = myStory?.userName?.take(1)?.uppercase() ?: "M",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add story", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("My Story", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text("Tap to add to your story", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (!postedArchive) {
                item {
                    TabRow(
                        selectedTabIndex = updatesTabState,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (updatesTabState in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[updatesTabState]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = updatesTabState == 0,
                            onClick = { updatesTabState = 0 },
                            text = {
                                Text(
                                    "Recent",
                                    fontWeight = FontWeight.Bold,
                                    color = if (updatesTabState == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = updatesTabState == 1,
                            onClick = { updatesTabState = 1 },
                            text = {
                                Text(
                                    "Viewed",
                                    fontWeight = FontWeight.Bold,
                                    color = if (updatesTabState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                if (updatesTabState == 0) {
                    if (recentUpdates.isNotEmpty()) {
                        items(recentUpdates, key = { it.id }) { story ->
                            UserStoryRowItem(
                                story = story,
                                onClick = {
                                    viewingUserStory = story
                                    viewingStoryIndex = savedStoryIndex(story)
                                }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No recent updates",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    if (viewedUpdates.isNotEmpty()) {
                        items(viewedUpdates, key = { it.id }) { story ->
                            UserStoryRowItem(
                                story = story,
                                onClick = {
                                    viewingUserStory = story
                                    viewingStoryIndex = savedStoryIndex(story)
                                }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No viewed updates",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun UserStoryRowItem(
    story: UserStory,
    onClick: () -> Unit
) {
    val latestStory = remember(story.stories) { story.stories.lastOrNull() }
    val thumbnailUrl = remember(latestStory) { formatMediaUrl(latestStory?.contentUrl) ?: story.userAvatarUrl }
    val allViewed = story.allViewed
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (allViewed) Color.Gray.copy(alpha = 0.4f) else primaryColor,
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (latestStory?.type == StoryType.TEXT) latestStory.backgroundColor else story.userAvatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (latestStory?.type == StoryType.TEXT && latestStory.content.isNotBlank()) {
                        Text(
                            text = latestStory.content,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(2.dp)
                        )
                    } else {
                        Text(
                            text = story.userName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(story.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(
                story.lastUpdated,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun isOwnStoryGroup(group: UserStory, currentUserId: String?, myStory: UserStory?): Boolean {
    return group.id == currentUserId ||
        group.userId == currentUserId ||
        group.id == myStory?.id ||
        (currentUserId != null && (
            group.id == "user_$currentUserId" ||
                group.id == currentUserId.removePrefix("user_") ||
                group.userId == "user_$currentUserId" ||
                group.userId == currentUserId.removePrefix("user_")
            )) ||
        group.userName == "My Story" ||
        group.userName == "My Number"
}

@Composable
private fun StoryNativeAdScreen(
    nativeAd: NativeAd,
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    var canSkip by remember { mutableStateOf(false) }
    LaunchedEffect(nativeAd) {
        delay(3_000)
        canSkip = true
        runCatching { RewardsManager.awardPoints(0.10, "STORY_AD", "Viewed story ad") }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                val view = android.view.LayoutInflater.from(context)
                    .inflate(com.tpsk.tj.R.layout.item_story_ad, null) as NativeAdView
                populateNativeAdView(nativeAd, view)
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view -> populateNativeAdView(nativeAd, view) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sponsored", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            Row {
                if (canSkip) {
                    TextButton(onClick = onContinue) {
                        Text("Skip", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
