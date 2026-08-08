package com.example.firstapp.media.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.media.model.UserStory
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.media.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun StoryScreen(
    stories: List<UserStory>,
    contacts: List<com.example.firstapp.chat.model.Contact> = emptyList(),
    currentUserId: String?,
    mediaViewModel: MediaViewModel,
    adManager: AdManager,
    initialViewingUserId: String? = null,
    initialShowComposer: Boolean = false,
    onBack: () -> Unit,
    onPostStory: (String, String?, String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onStoryViewed: (String, String) -> Unit,
    onMuteStory: (String, Boolean) -> Unit,
    onNavigateToNewStory: () -> Unit = {}
) {
    val context = LocalContext.current
    val preloadedAds by mediaViewModel.preloadedAds.collectAsState()
    
    var viewingUserStory by remember { 
        mutableStateOf(stories.find { it.id == initialViewingUserId }) 
    }
    var viewingStoryIndex by remember { mutableIntStateOf(0) }

    // Persistent ad counter across users
    var totalStoriesViewedInViewer by remember { mutableIntStateOf(0) }
    var showInViewerAd by remember { mutableStateOf(false) }

    var storyToMute by remember { mutableStateOf<UserStory?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewedFirst by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(initialShowComposer) }
    var composerText by remember { mutableStateOf("") }

    val filteredStories = remember(stories, searchQuery) {
        if (searchQuery.isBlank()) {
            stories
        } else {
            val q = searchQuery.trim().lowercase()
            stories.filter { it.userName.lowercase().contains(q) }
        }
    }
    
    val myStory = stories.find { it.id == currentUserId }
    val contactIds = remember(contacts) { 
        contacts.map { it.id }.toSet() + contacts.map { "user_${it.id}" }.toSet() + contacts.map { it.id.removePrefix("user_") }.toSet()
    }
    val contactNames = remember(contacts) { contacts.map { it.displayName.lowercase().trim() }.toSet() }
    
    // User Requirement: Only show stories from contacts
    val otherStories = filteredStories.filter { story -> 
        if (story.id == currentUserId) return@filter false
        if (story.id == "system_ad") return@filter true
        
        // Match by ID (normalized) or by Saved Name
        val isFromContact = contactIds.contains(story.id) || contactNames.contains(story.userName.lowercase().trim())
        
        // If contacts list is empty (still loading), show everything to avoid a blank screen.
        if (contacts.isEmpty()) true else isFromContact
    }
    val activeStories = otherStories.filter { !it.isMuted }
    val mutedStories = otherStories.filter { it.isMuted }
    
    val recentUpdates = activeStories.filter { !it.allViewed }
    val viewedUpdates = activeStories.filter { it.allViewed }
    
    var showMutedUpdates by remember { mutableStateOf(false) }
    var showPostedStoriesList by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onNavigateToNewStory() 
        }
    }

    val allViewableStories = remember(myStory, recentUpdates, viewedUpdates, viewingUserStory, initialViewingUserId) {
        val list = mutableListOf<UserStory>()
        
        // If we explicitly clicked our own story from the main row
        if (viewingUserStory?.id == currentUserId && myStory != null) {
            list.add(myStory)
        } else if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
            val story = stories.find { it.id == initialViewingUserId }
            if (story != null) list.add(story)
        }
        
        list.addAll(recentUpdates)
        list.addAll(viewedUpdates)
        
        list.distinctBy { it.id }
    }

    androidx.activity.compose.BackHandler(enabled = viewingUserStory != null || showComposer || showSearch || showPostedStoriesList) {
        when {
            viewingUserStory != null -> {
                if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                    onBack()
                } else {
                    viewingUserStory = null 
                }
            }
            showPostedStoriesList -> showPostedStoriesList = false
            showComposer -> showComposer = false
            showSearch -> {
                showSearch = false
                searchQuery = ""
            }
        }
    }

    // Sync viewing user story with latest data to handle deletions/updates
    LaunchedEffect(stories, initialViewingUserId) {
        if (viewingUserStory == null && initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
            val story = stories.find { it.id == initialViewingUserId }
            if (story != null) {
                viewingUserStory = story
                viewingStoryIndex = 0
            }
        } else {
            viewingUserStory?.let { current ->
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
    }

    // Reset ad counters when closing viewer
    LaunchedEffect(viewingUserStory) {
        if (viewingUserStory == null) {
            totalStoriesViewedInViewer = 0
            showInViewerAd = false
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
    } else if (showPostedStoriesList) {
        PostedStoriesList(
            myStory = myStory,
            onBack = { showPostedStoriesList = false },
            onDeleteStory = onDeleteStory,
            onStoryClick = { idx ->
                viewingUserStory = myStory
                viewingStoryIndex = idx
            }
        )
    } else if (viewingUserStory != null && viewingUserStory!!.stories.isNotEmpty()) {
        StoryViewer(
            userStory = viewingUserStory!!,
            isOwnStory = viewingUserStory!!.id == currentUserId,
            initialStoryIndex = viewingStoryIndex,
            showAd = showInViewerAd,
            onAdFinish = { showInViewerAd = false },
            onStoryViewed = { storyIndex -> 
                val storyId = viewingUserStory?.stories?.getOrNull(storyIndex)?.id
                if (storyId != null) {
                    onStoryViewed(viewingUserStory!!.id, storyId)
                    totalStoriesViewedInViewer++
                    if (totalStoriesViewedInViewer % 4 == 0) {
                        showInViewerAd = true
                    }
                }
            },
            onDeleteStory = { storyId ->
                onDeleteStory(storyId)
            },
            onFinish = { 
                val currentIndex = allViewableStories.indexOfFirst { it.id == viewingUserStory?.id }
                if (currentIndex != -1 && currentIndex < allViewableStories.size - 1) {
                    viewingUserStory = allViewableStories[currentIndex + 1]
                    viewingStoryIndex = 0
                } else {
                    if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                        onBack()
                    } else {
                        viewingUserStory = null 
                    }
                }
            },
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
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("My stories", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onBackground) }
                        IconButton(onClick = { showViewedFirst = !showViewedFirst }) { Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                com.example.firstapp.media.ui.components.BannerAdView(
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    SmallFloatingActionButton(
                        onClick = { showComposer = true },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Text Story")
                    }
                    FloatingActionButton(
                        onClick = { onNavigateToNewStory() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "New Camera Story")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (showSearch) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search story") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    Text(
                        "Story",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item(key = "my_story_item_main") {
                    myStory?.let {
                        StoryListItem(it, isOwnStory = true, onClick = { 
                            viewingUserStory = it
                            viewingStoryIndex = 0
                        })
                    } ?: run {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToNewStory() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier.size(36.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("My story", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("Tap to add story update", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (myStory != null && myStory.stories.isNotEmpty()) {
                    item(key = "posted_stories_header_row") {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showPostedStoriesList = true }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Posted stories",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(myStory.stories.reversed().take(3), key = { _, s -> "my_posted_preview_${s.id}" }) { idx, story ->
                        MyStoryListItem(
                            story = story,
                            onDelete = { onDeleteStory(story.id) },
                            onClick = { 
                                viewingUserStory = myStory
                                viewingStoryIndex = myStory.stories.indexOf(story)
                            }
                        )
                    }
                }

                if (recentUpdates.isNotEmpty()) {
                    item(key = "recent_updates_header") {
                        Text(
                            "Recent updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(recentUpdates, key = { _, item -> "recent_${item.id}" }) { idx: Int, item: UserStory ->
                        StoryListItem(
                            userStory = item, 
                            isOwnStory = false,
                            onClick = { 
                                viewingUserStory = item
                                viewingStoryIndex = 0
                            },
                            onLongClick = { storyToMute = item }
                        )
                        if ((idx + 1) % 4 == 0) {
                            com.example.firstapp.media.ui.components.NativeAdCard(
                                adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (viewedUpdates.isNotEmpty()) {
                    item(key = "viewed_updates_header") {
                        Text(
                            "Viewed updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(viewedUpdates, key = { _, item -> "viewed_${item.id}" }) { idx: Int, item: UserStory ->
                        StoryListItem(
                            userStory = item, 
                            isOwnStory = false,
                            onClick = { 
                                viewingUserStory = item
                                viewingStoryIndex = 0
                            },
                            onLongClick = { storyToMute = item }
                        )
                        if ((idx + 1) % 4 == 0) {
                            com.example.firstapp.media.ui.components.NativeAdCard(
                                adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // Final Banner Ad at the end of the list (Optional, now we have bottomBar)
                // item { com.example.firstapp.media.ui.components.BannerAdView(...) }
                
                item { Spacer(modifier = Modifier.height(150.dp)) }
            }
        }
    }

    storyToMute?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToMute = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (story.isMuted) "Unmute ${story.userName}?" else "Mute ${story.userName}?", color = MaterialTheme.colorScheme.onSurface) },
            text = { 
                Text(
                    if (story.isMuted) "New story updates from ${story.userName} will appear in recent updates." 
                    else "New story updates from ${story.userName} won't appear under recent updates anymore.", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                TextButton(onClick = { 
                    onMuteStory(story.id, !story.isMuted)
                    storyToMute = null
                }) {
                    Text(if (story.isMuted) "UNMUTE" else "MUTE", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToMute = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun MyStoryListItem(
    story: com.example.firstapp.media.model.StoryItem,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (story.type == com.example.firstapp.media.model.StoryType.TEXT) Color.DarkGray else Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!story.contentUrl.isNullOrBlank()) {
                AsyncImage(
                    model = story.contentUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    story.content.take(10),
                    color = Color.White,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
            
            if (story.type == com.example.firstapp.media.model.StoryType.VIDEO) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (story.content.isNotBlank()) story.content else "Media story",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = story.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete story?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This story will be deleted for everyone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryListItem(
    userStory: UserStory,
    isOwnStory: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val totalStories = userStory.stories.size
    
    val viewedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val activeColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (totalStories > 0) onClick() },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .drawBehind {
                    if (totalStories > 0) {
                        val strokeWidth = 2.dp.toPx()
                        val gapAngle = if (totalStories > 1) 4f else 0f
                        val sweepAngle = (360f / totalStories) - gapAngle
                        
                        for (i in 0 until totalStories) {
                            val color = if (userStory.stories[i].isViewed) viewedColor else activeColor
                            
                            drawArc(
                                color = color,
                                startAngle = i * (360f / totalStories) - 90f + (gapAngle / 2f),
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(userStory.userAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                if (!userStory.userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userStory.userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        userStory.userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isOwnStory) "My story" else userStory.userName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isOwnStory && totalStories == 0) "Tap to add story update" else userStory.lastUpdated,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PostedStoriesList(
    myStory: com.example.firstapp.media.model.UserStory?,
    onBack: () -> Unit,
    onDeleteStory: (String) -> Unit,
    onStoryClick: (Int) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Posted stories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (myStory == null || myStory.stories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No posted stories", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 100.dp)
            ) {
                itemsIndexed(myStory.stories.reversed()) { idx, story ->
                    MyStoryListItem(
                        story = story,
                        onDelete = { onDeleteStory(story.id) },
                        onClick = { onStoryClick(myStory.stories.indexOf(story)) }
                    )
                }
            }
        }
    }
}
