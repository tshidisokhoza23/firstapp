package com.example.firstapp.chat.ui.stories

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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.chat.model.UserStory
import com.example.firstapp.chat.model.StoryItem
import com.example.firstapp.chat.model.StoryType
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
    initialPlay: Boolean = true,
    onBack: () -> Unit,
    onPostStory: (String, String?, String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onStoryViewed: (String, String) -> Unit,
    onMuteStory: (String, Boolean) -> Unit,
    onNavigateToNewStory: () -> Unit = {}
) {
    val context = LocalContext.current
    
    var viewingUserStory by remember { 
        mutableStateOf(if (initialPlay) stories.find { it.id == initialViewingUserId || it.userId == initialViewingUserId } else null)
    }
    var viewingStoryIndex by remember { mutableIntStateOf(0) }

    var storyToMute by remember { mutableStateOf<UserStory?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewedFirst by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(initialShowComposer) }
    var composerText by remember { mutableStateOf("") }

    // Persistent ad counter across users
    var usersViewedCount by remember { mutableIntStateOf(0) }
    var loadedAdObject by remember { mutableStateOf<Any?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    var nextUserIndexAfterAd by remember { mutableIntStateOf(-1) }

    // Preload ad as soon as we start viewing
    LaunchedEffect(viewingUserStory) {
        if (viewingUserStory != null && loadedAdObject == null && !isAdLoading) {
            isAdLoading = true
            adManager.loadNativeAd(
                context = context,
                adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
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
            // Backend already returns contacts-only. If local contacts have not
            // synced yet, don't hide the server list; once they load, keep only
            // people who are actually in the phone book.
            if (contacts.isEmpty()) return@filter true
            contactIds.contains(story.id) || contactIds.contains(story.userId) ||
                contactNames.contains(story.userName.lowercase().trim())
        }
    }
    
    val activeStories = otherStories.filter { !it.isMuted }
    val recentUpdates = activeStories.filter { !it.allViewed }
    val viewedUpdates = activeStories.filter { it.allViewed }
    
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

    // Release an ad when the viewer closes; keep the user cadence for this screen session.
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
        StoryViewer(
            userStory = viewingUserStory!!,
            isOwnStory = viewingUserStory!!.id == currentUserId,
            initialStoryIndex = viewingStoryIndex,
            onStoryViewed = { storyIndex -> 
                val storyId = viewingUserStory?.stories?.getOrNull(storyIndex)?.id
                if (storyId != null) {
                    if (viewingUserStory?.id != "system_ad") {
                          onStoryViewed(viewingUserStory!!.userId.ifBlank { viewingUserStory!!.id }, storyId)
                    }
                }
            },
            onDeleteStory = { storyId ->
                onDeleteStory(storyId)
            },
            onFinish = { 
                if (viewingUserStory?.id != "system_ad") {
                    usersViewedCount++
                }

                val adObj = loadedAdObject
                val isAdTriggered = usersViewedCount > 0 && usersViewedCount % 4 == 0

                if (isAdTriggered && adObj != null && viewingUserStory?.id != "system_ad") {
                    nextUserIndexAfterAd = (storiesWithAds.indexOfFirst { it.id == viewingUserStory?.id } + 1)
                    viewingUserStory = UserStory(
                        id = "system_ad",
                        userName = "Sponsored",
                        userAvatarColor = Color.DarkGray,
                        lastUpdated = "Promoted",
                        stories = listOf(
                            StoryItem(
                                id = "ad_${System.currentTimeMillis()}",
                                content = "",
                                type = StoryType.AD,
                                timestamp = "Promoted",
                                durationMs = 10000, // Fixed 10s for ad
                                adObject = adObj
                            )
                        )
                    )
                    viewingStoryIndex = 0
                    loadedAdObject = null // Consumed
                } else {
                    val currentIndex = storiesWithAds.indexOfFirst { it.id == viewingUserStory?.id }
                    if (currentIndex != -1 && currentIndex < storiesWithAds.size - 1) {
                        viewingUserStory = storiesWithAds[currentIndex + 1]
                        viewingStoryIndex = 0
                    } else if (viewingUserStory?.id == "system_ad") {
                        val nextIndex = nextUserIndexAfterAd
                        nextUserIndexAfterAd = -1
                        if (nextIndex in storiesWithAds.indices) {
                            viewingUserStory = storiesWithAds[nextIndex]
                            viewingStoryIndex = 0
                        } else {
                            viewingUserStory = null
                        }
                    } else {
                        if (initialViewingUserId != null && initialViewingUserId.isNotBlank()) {
                            onBack()
                        } else {
                            viewingUserStory = null 
                        }
                    }
                }
            },
              onNavigateToUser = { direction ->
                  val currentIndex = storiesWithAds.indexOfFirst { it.id == viewingUserStory?.id }
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
                com.example.firstapp.common.ui.components.BannerAdView(
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

                item(key = "my_stories_horizontal_row") {
                    if (myStory != null) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                 items(myStory.stories.reversed(), key = { it.id }) { story ->
                                     Column(
                                         horizontalAlignment = Alignment.CenterHorizontally,
                                         modifier = Modifier.clickable {
                                             val idx = myStory.stories.indexOf(story)
                                             if (idx != -1) {
                                                 viewingUserStory = myStory
                                                 viewingStoryIndex = idx
                                             }
                                         }
                                     ) {
                                         Box(
                                             modifier = Modifier
                                                 .size(65.dp)
                                                 .clip(CircleShape)
                                                 .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                 .padding(3.dp)
                                                 .clip(CircleShape)
                                                 .background(Color.Gray)
                                         ) {
                                             if (!story.contentUrl.isNullOrBlank()) {
                                                 AsyncImage(
                                                     model = story.contentUrl,
                                                     contentDescription = null,
                                                     modifier = Modifier.fillMaxSize(),
                                                     contentScale = ContentScale.Crop
                                                 )
                                             } else {
                                                 Box(
                                                     modifier = Modifier.fillMaxSize().background(story.backgroundColor),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Text(
                                                         story.content.take(1),
                                                         color = Color.White,
                                                         fontWeight = FontWeight.Bold,
                                                         fontSize = 20.sp
                                                     )
                                                 }
                                             }
                                         }
                                         Text(
                                             "Story",
                                             fontSize = 12.sp,
                                             modifier = Modifier.padding(top = 4.dp),
                                             color = MaterialTheme.colorScheme.onSurface
                                         )
                                     }
                                 }
                            }
                        }
                    }
                }

                item(key = "posted_stories_header_section") {
                    Text(
                        "Posted stories",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                itemsIndexed(recentUpdates, key = { _, item -> "recent_v2_${item.id}" }) { idx, item ->
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
                        com.example.firstapp.common.ui.components.NativeAdCard(
                            adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (viewedUpdates.isNotEmpty()) {
                    item(key = "viewed_updates_header_v3") {
                        Text(
                            "Viewed updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(viewedUpdates, key = { _, item -> "viewed_v3_${item.id}" }) { idx, item ->
                        StoryListItem(
                            userStory = item, 
                            isOwnStory = false,
                            onClick = { 
                                viewingUserStory = item
                                viewingStoryIndex = 0
                            },
                            onLongClick = { storyToMute = item }
                        )
                    }
                }
                
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
