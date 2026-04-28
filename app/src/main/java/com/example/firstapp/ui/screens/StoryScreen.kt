package com.example.firstapp.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.model.UserStory
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    stories: List<UserStory>,
    onBack: () -> Unit,
    onPostStory: (String, String?) -> Unit,
    onStoryViewed: (Int, Int) -> Unit,
    onMuteStory: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    var viewingUserStory by remember { mutableStateOf<UserStory?>(null) }
    var storyToMute by remember { mutableStateOf<UserStory?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewedFirst by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var composerText by remember { mutableStateOf("") }

    val filteredStories = remember(stories, searchQuery) {
        if (searchQuery.isBlank()) {
            stories
        } else {
            val q = searchQuery.trim().lowercase()
            stories.filter { it.userName.lowercase().contains(q) }
        }
    }
    
    // Assume the user's own status is at ID 1 for now (placeholder)
    val myStatus = stories.find { it.id == 1 }
    val otherStories = filteredStories.filter { it.id != 1 }
    val activeStories = otherStories.filter { !it.isMuted }
    val mutedStories = otherStories.filter { it.isMuted }
    
    val recentUpdates = activeStories.filter { !it.allViewed }
    val viewedUpdates = activeStories.filter { it.allViewed }
    
    var showMutedUpdates by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onPostStory("Image status", it.toString())
            Toast.makeText(context, "Status update shared", Toast.LENGTH_SHORT).show()
        }
    }

    if (showComposer) {
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
                            onPostStory(composerText, null)
                            composerText = ""
                            showComposer = false
                            Toast.makeText(context, "Status update shared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Text("Share status", color = AppBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AppBackground
        ) { p ->
            TextField(
                value = composerText,
                onValueChange = { composerText = it },
                modifier = Modifier.fillMaxSize().padding(p),
                placeholder = { Text("Type a status...", color = Color.Gray, fontSize = 24.sp) },
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    } else if (viewingUserStory != null) {
        StatusViewer(
            userStory = viewingUserStory!!,
            onStoryViewed = { storyIndex -> 
                val storyId = viewingUserStory?.stories?.getOrNull(storyIndex)?.id
                if (storyId != null) {
                    onStoryViewed(viewingUserStory!!.id, storyId)
                }
            },
            onFinish = { viewingUserStory = null },
            onClose = { viewingUserStory = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Updates", color = AppTextPrimary, fontWeight = FontWeight.Bold) },
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
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showComposer = true },
                        containerColor = AppSecondary,
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Text Status")
                    }
                    FloatingActionButton(
                        onClick = { imageLauncher.launch("image/*") },
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
                        StoryListItem(it, onClick = { viewingUserStory = it })
                    } ?: run {
                        // User's own status placeholder if empty
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { imageLauncher.launch("image/*") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(60.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = AppSecondary
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.padding(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(AppAccent, CircleShape)
                                        .border(2.dp, AppBackground, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = AppBackground, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("My status", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("Tap to add status update", color = AppTextSecondary, fontSize = 14.sp)
                            }
                        }
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
                            onClick = { viewingUserStory = userStory },
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
                            onClick = { viewingUserStory = userStory },
                            onLongClick = { storyToMute = userStory }
                        )
                    }
                }

                if (mutedStories.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMutedUpdates = !showMutedUpdates }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Muted updates",
                                color = AppTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                if (showMutedUpdates) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppTextSecondary
                            )
                        }
                    }
                    
                    if (showMutedUpdates) {
                        items(mutedStories) { userStory ->
                            StoryListItem(
                                userStory = userStory,
                                onClick = { viewingUserStory = userStory },
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryListItem(userStory: UserStory, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
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
                text = if (userStory.id == 1) "My Status" else userStory.userName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (userStory.id == 1 && totalStories == 0) "Tap to add status update" else userStory.lastUpdated,
                color = AppTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
