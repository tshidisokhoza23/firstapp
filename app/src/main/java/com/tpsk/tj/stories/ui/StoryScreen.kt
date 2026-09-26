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
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.chat.model.StoryItem
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.chat.ui.components.formatMediaUrl
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

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

    var storyToMute by remember { mutableStateOf<UserStory?>(null) }
    var selectedStoryForViewers by remember { mutableStateOf<StoryItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewedFirst by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(initialShowComposer) }
    var composerText by remember { mutableStateOf("") }

    // Persistent ad counter across users
    var loadedAdObject by remember { mutableStateOf<Any?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }

    // Preload ad as soon as we start viewing
    LaunchedEffect(viewingUserStory) {
        if (viewingUserStory != null && loadedAdObject == null && !isAdLoading) {
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
            if (story.id == "system_ad") return@filter true
            true
        }
    }

    val activeStories = remember(otherStories) { otherStories.filter { !it.isMuted } }
    
    // WhatsApp-style Chronological Sorting: Unviewed first (sorted descending by latest story timestamp), then Viewed (sorted descending by latest timestamp)
    val recentUpdates = remember(activeStories) {
        activeStories.filter { !it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
    }
    val viewedUpdates = remember(activeStories) {
        activeStories.filter { it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
    }

    val storiesWithAds = remember(myStory, recentUpdates, viewedUpdates) {
        val baseList = mutableListOf<UserStory>()
        if (myStory != null) baseList.add(myStory)
        baseList.addAll(recentUpdates)
        baseList.addAll(viewedUpdates)
        baseList.distinctBy { it.id }
    }

    androidx.activity.compose.BackHandler(enabled = viewingUserStory != null || showComposer || showSearch) {
        when {
            viewingUserStory != null -> {
                if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                    onBack()
                } else {
                    viewingUserStory = null 
                }
            }
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
            val updated = stories.find { it.id == current.id }
            if (updated != null) {
                viewingUserStory = updated
                if (viewingStoryIndex >= updated.stories.size) {
                    viewingStoryIndex = (updated.stories.size - 1).coerceAtLeast(0)
                }
            } else if (!current.id.isNullOrBlank()) {
                if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                    onBack()
                } else {
                    viewingUserStory = null
                }
            }
        }
    }

    // Initial play logic
    LaunchedEffect(initialViewingUserId, initialPlay, stories) {
        if (initialPlay && initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
            val story = stories.find { it.id == initialViewingUserId || it.userId == initialViewingUserId }
            if (story != null) {
                viewingUserStory = story
                viewingStoryIndex = 0
            }
        }
    }

    // Release an ad when the viewer closes
    LaunchedEffect(viewingUserStory) {
        if (viewingUserStory == null) {
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
                if (storyId != null) {
                    if (currentGroup.id != "system_ad") {
                          onStoryViewed(currentGroup.userId.ifBlank { currentGroup.id }, storyId)
                    }
                }
            },
            onDeleteStory = { storyId ->
                onDeleteStory(storyId)
            },
            onReplyStory = { storyId, text, summary ->
                val targetUser = currentGroup.userId.ifBlank { currentGroup.id }
                val authorName = currentGroup.userName
                if (targetUser.isNotBlank() && currentGroup.id != "system_ad") {
                    onReplyStory(targetUser, storyId, text, summary, authorName)
                }
            },
            onFinish = { 
                val currentIndex = storiesWithAds.indexOfFirst { it.id == currentGroup.id }
                if (currentIndex != -1 && currentIndex < storiesWithAds.size - 1) {
                    viewingUserStory = storiesWithAds[currentIndex + 1]
                    viewingStoryIndex = 0
                } else {
                    if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                        onBack()
                    } else {
                        viewingUserStory = null 
                    }
                }
            },
            onNavigateToUser = { direction ->
                val currentIndex = storiesWithAds.indexOfFirst { it.id == currentGroup.id }
                if (currentIndex != -1) {
                    val nextIndex = currentIndex + direction
                    if (nextIndex in storiesWithAds.indices) {
                        viewingUserStory = storiesWithAds[nextIndex]
                        viewingStoryIndex = 0
                    } else {
                        if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                            onBack()
                        } else {
                            viewingUserStory = null
                        }
                    }
                }
            },
            onGetStoryViewers = onGetStoryViewers,
            onClose = { 
                if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                    onBack()
                } else {
                    viewingUserStory = null 
                }
            }
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
                                placeholder = { Text("Search updates...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text("Updates", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (showSearch) {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        } else {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search updates")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
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
                item {
                    Text("Status", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                // My Status
                item {
                    val myLatestStory = myStory?.stories?.lastOrNull()
                    val myThumbnailUrl = remember(myLatestStory) { formatMediaUrl(myLatestStory?.contentUrl) ?: myStory?.userAvatarUrl }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (myStory != null && myStory.stories.isNotEmpty()) {
                                    viewingUserStory = myStory
                                    viewingStoryIndex = 0
                                } else {
                                    onNavigateToNewStory()
                                }
                            }
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
                                    .background(if (myLatestStory?.type == StoryType.TEXT) myLatestStory.backgroundColor else AppPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!myThumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = myThumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (myLatestStory?.type == StoryType.TEXT && myLatestStory.content.isNotBlank()) {
                                    Text(
                                        text = myLatestStory.content,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                } else {
                                    Text(
                                        text = myStory?.userName?.take(1)?.uppercase() ?: "M",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add status", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("My status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                if (myStory != null && myStory.stories.isNotEmpty()) "Tap to view updates" else "Tap to add status update",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Recent updates section (Unviewed)
                if (recentUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "Recent updates",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(recentUpdates, key = { it.id }) { story ->
                        UserStoryRowItem(
                            story = story,
                            onClick = {
                                viewingUserStory = story
                                viewingStoryIndex = 0
                            }
                        )
                    }
                }

                // Viewed updates section
                if (viewedUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "Viewed updates",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(viewedUpdates, key = { it.id }) { story ->
                        UserStoryRowItem(
                            story = story,
                            onClick = {
                                viewingUserStory = story
                                viewingStoryIndex = 0
                            }
                        )
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
