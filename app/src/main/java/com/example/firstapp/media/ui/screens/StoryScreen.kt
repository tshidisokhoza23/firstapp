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
import com.example.firstapp.media.model.UserStory
import com.example.firstapp.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun StoryScreen(
    stories: List<UserStory>,
    currentUserId: Int?,
    initialViewingUserId: Int? = null,
    initialShowComposer: Boolean = false,
    onBack: () -> Unit,
    onPostStory: (String, String?, String) -> Unit,
    onDeleteStory: (Int) -> Unit,
    onStoryViewed: (Int, Int) -> Unit,
    onMuteStory: (Int, Boolean) -> Unit,
    onNavigateToNewStory: () -> Unit = {}
) {
    val context = LocalContext.current
    var viewingUserStory by remember { 
        mutableStateOf(stories.find { it.id == initialViewingUserId }) 
    }
    var viewingStoryIndex by remember { mutableIntStateOf(0) }
    
    // Use LaunchedEffect to handle initial viewing state if provided
    LaunchedEffect(initialViewingUserId, stories) {
        if (viewingUserStory == null && initialViewingUserId != null) {
            viewingUserStory = stories.find { it.id == initialViewingUserId }
            viewingStoryIndex = 0
        }
    }

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
    
    val myStatus = stories.find { it.id == currentUserId }
    val otherStories = filteredStories.filter { it.id != currentUserId }
    val activeStories = otherStories.filter { !it.isMuted }
    val mutedStories = otherStories.filter { it.isMuted }
    
    val recentUpdates = activeStories.filter { !it.allViewed }
    val viewedUpdates = activeStories.filter { it.allViewed }
    
    var showMutedUpdates by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Instead of posting directly, navigate to NewStory or show a dialog
            // For now, let's assume we want to use the NewStoryScreen flow
            onNavigateToNewStory() 
        }
    }

    val allViewableStories = remember(recentUpdates, viewedUpdates) {
        recentUpdates + viewedUpdates
    }

    // Sync viewing user story with latest data to handle deletions/updates
    LaunchedEffect(stories) {
        viewingUserStory?.let { current ->
            val updated = stories.find { it.id == current.id }
            if (updated != null) {
                // Update the viewing story to the latest version from the repository
                viewingUserStory = updated
                // Adjust index if necessary
                if (viewingStoryIndex >= updated.stories.size) {
                    viewingStoryIndex = (updated.stories.size - 1).coerceAtLeast(0)
                }
            } else if (current.id != -1) {
                // If user's stories are gone, exit the viewer
                if (initialViewingUserId != null) {
                    onBack()
                } else {
                    viewingUserStory = null
                }
            }
        }
    }

    if (showComposer) {
        // ... (Composer code remains the same)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Text Status") },
                    navigationIcon = {
                        IconButton(onClick = { showComposer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White)
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        if (composerText.isNotBlank()) {
                            onPostStory(composerText, null, "text")
                            composerText = ""
                            showComposer = false
                            // Notification handled by viewmodel/navigation
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Text("Share status", color = AppBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AppBackground
        ) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                TextField(
                    value = composerText,
                    onValueChange = { composerText = it },
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    placeholder = { Text("Type a status...", color = Color.Gray.copy(alpha = 0.5f), fontSize = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AppAccent
                    )
                )
            }
        }
    } else if (viewingUserStory != null) {
        StatusViewer(
            userStory = viewingUserStory!!,
            isOwnStory = viewingUserStory!!.id == currentUserId,
            initialStoryIndex = viewingStoryIndex,
            onStoryViewed = { storyIndex -> 
                val storyId = viewingUserStory?.stories?.getOrNull(storyIndex)?.id
                if (storyId != null) {
                    onStoryViewed(viewingUserStory!!.id, storyId)
                }
            },
            onDeleteStory = { storyId ->
                onDeleteStory(storyId)
            },
            onFinish = { 
                if (initialViewingUserId != null) {
                    onBack()
                } else {
                    // Sequential loading: Find the next user in the list
                    val currentIndex = allViewableStories.indexOfFirst { it.id == viewingUserStory?.id }
                    if (currentIndex != -1 && currentIndex < allViewableStories.size - 1) {
                        viewingUserStory = allViewableStories[currentIndex + 1]
                        viewingStoryIndex = 0
                    } else {
                        viewingUserStory = null 
                    }
                }
            },
            onClose = { 
                if (initialViewingUserId != null) {
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
                    title = { Text("My stories", color = AppTextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Search", tint = AppTextPrimary) }
                        IconButton(onClick = { showViewedFirst = !showViewedFirst }) { Icon(Icons.Default.MoreVert, "More", tint = AppTextPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    SmallFloatingActionButton(
                        onClick = { showComposer = true },
                        containerColor = AppSecondary,
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Text Status")
                    }
                    FloatingActionButton(
                        onClick = { onNavigateToNewStory() },
                        containerColor = AppAccent,
                        contentColor = AppBackground
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "New Camera Status")
                    }
                }
            },
            containerColor = AppBackground
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
                            label = { Text("Search status") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    Text(
                        "Status",
                        modifier = Modifier.padding(16.dp),
                        color = AppTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    myStatus?.let {
                        StoryListItem(it, isOwnStatus = true, onClick = { 
                            viewingUserStory = it
                            viewingStoryIndex = 0
                        })
                    } ?: run {
                        // ...
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToNewStory() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ...
                        }
                    }
                }

                // Posted stories section
                if (myStatus != null && myStatus.stories.isNotEmpty()) {
                    item {
                        Text(
                            "Posted stories",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(myStatus.stories.asReversed()) { story ->
                        MyStoryListItem(
                            story = story,
                            onDelete = { onDeleteStory(story.id) },
                            onClick = { 
                                viewingUserStory = myStatus
                                viewingStoryIndex = myStatus.stories.indexOf(story)
                            }
                        )
                    }
                }

                if (recentUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "Recent updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(recentUpdates) { userStory ->
                        StoryListItem(
                            userStory = userStory, 
                            isOwnStatus = false,
                            onClick = { 
                                viewingUserStory = userStory
                                viewingStoryIndex = 0
                            },
                            onLongClick = { storyToMute = userStory }
                        )
                    }
                }

                if (viewedUpdates.isNotEmpty()) {
                    item {
                        Text(
                            "Viewed updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(viewedUpdates) { userStory ->
                        StoryListItem(
                            userStory = userStory, 
                            isOwnStatus = false,
                            onClick = { 
                                viewingUserStory = userStory
                                viewingStoryIndex = 0
                            },
                            onLongClick = { storyToMute = userStory }
                        )
                    }
                }

                if (mutedStories.isNotEmpty()) {
                    // ...
                    if (showMutedUpdates) {
                        items(mutedStories) { userStory ->
                            StoryListItem(
                                userStory = userStory,
                                isOwnStatus = false,
                                onClick = { 
                                    viewingUserStory = userStory
                                    viewingStoryIndex = 0
                                },
                                onLongClick = { storyToMute = userStory }
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    storyToMute?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToMute = null },
            containerColor = AppSecondary,
            title = { Text(if (story.isMuted) "Unmute ${story.userName}?" else "Mute ${story.userName}?", color = Color.White) },
            text = { Text(if (story.isMuted) "New status updates from ${story.userName} will appear in recent updates." else "New status updates from ${story.userName} won't appear under recent updates anymore.", color = AppTextSecondary) },
            confirmButton = {
                TextButton(onClick = { 
                    onMuteStory(story.id, !story.isMuted)
                    storyToMute = null
                }) {
                    Text(if (story.isMuted) "UNMUTE" else "MUTE", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToMute = null }) {
                    Text("CANCEL", color = Color.White)
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
        // Story Thumbnail
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (story.type == com.example.firstapp.media.model.StoryType.TEXT) story.backgroundColor else Color.Black),
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
                text = if (story.content.isNotBlank()) story.content else "Media status",
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = story.timestamp,
                color = AppTextSecondary,
                fontSize = 12.sp
            )
        }
        
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = AppSecondary,
            title = { Text("Delete story?", color = Color.White) },
            text = { Text("This story will be deleted for everyone.", color = AppTextSecondary) },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryListItem(
    userStory: UserStory,
    isOwnStatus: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val totalStories = userStory.stories.size
    
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
        // Avatar with Segmented Story Ring
        Box(
            modifier = Modifier
                .size(60.dp)
                .drawBehind {
                    if (totalStories > 0) {
                        val strokeWidth = 2.dp.toPx()
                        val gapAngle = if (totalStories > 1) 4f else 0f
                        val sweepAngle = (360f / totalStories) - gapAngle
                        
                        for (i in 0 until totalStories) {
                            val color = if (userStory.stories[i].isViewed) {
                                AppTextSecondary.copy(alpha = 0.4f)
                            } else {
                                AppAccent
                            }
                            
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
                text = if (isOwnStatus) "My status" else userStory.userName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isOwnStatus && totalStories == 0) "Tap to add status update" else userStory.lastUpdated,
                color = AppTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
