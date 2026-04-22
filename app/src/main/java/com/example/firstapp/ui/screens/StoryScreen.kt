package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.firstapp.model.UserStory
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    stories: List<UserStory>,
    onBack: () -> Unit,
    onPostStory: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var viewingUserStory by remember { mutableStateOf<UserStory?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewedFirst by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var composerText by remember { mutableStateOf("") }

    val storyList = stories
    val filteredStories = remember(storyList, searchQuery) {
        if (searchQuery.isBlank()) {
            storyList
        } else {
            val q = searchQuery.trim().lowercase()
            storyList.filter { it.userName.lowercase().contains(q) }
        }
    }
    val recentUpdates = filteredStories.filter { !it.allViewed }
    val viewedUpdates = filteredStories.filter { it.allViewed }
    val myStatus = storyList.firstOrNull()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onPostStory("Image story", it.toString())
            Toast.makeText(context, "Story posted!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showComposer) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Status") },
                    navigationIcon = {
                        IconButton(onClick = { showComposer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = { imageLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Photo, "Gallery")
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
                            Toast.makeText(context, "Story posted!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Text("Post Status", color = AppBackground, fontWeight = FontWeight.Bold)
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
            onStoryViewed = { /* no-op for backend story state */ },
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
                        IconButton(onClick = {
                            myStatus?.let { viewingUserStory = it }
                                ?: Toast.makeText(context, "No status available", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.CameraAlt, "Camera", tint = AppTextPrimary) }
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Search", tint = AppTextPrimary) }
                        IconButton(onClick = {
                            showComposer = true
                        }) { Icon(Icons.Default.Edit, "Edit", tint = AppTextPrimary) }
                        IconButton(onClick = { showViewedFirst = !showViewedFirst }) { Icon(Icons.Default.MoreVert, "More", tint = AppTextPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
                )
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
                    }
                }

                val firstUpdates = if (showViewedFirst) viewedUpdates else recentUpdates
                val secondUpdates = if (showViewedFirst) recentUpdates else viewedUpdates

                if (firstUpdates.isNotEmpty()) {
                    item {
                        Text(
                            if (showViewedFirst) "Viewed updates" else "Recent updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(firstUpdates) { userStory ->
                        StoryListItem(userStory, onClick = { viewingUserStory = userStory })
                    }
                }

                if (secondUpdates.isNotEmpty()) {
                    item {
                        Text(
                            if (showViewedFirst) "Recent updates" else "Viewed updates",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(secondUpdates) { userStory ->
                        StoryListItem(userStory, onClick = { viewingUserStory = userStory })
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StoryListItem(userStory: UserStory, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Story Ring
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(
                    width = 2.dp,
                    color = if (userStory.allViewed) AppTextSecondary.copy(alpha = 0.5f) else AppAccent,
                    shape = CircleShape
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(userStory.userAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    userStory.userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userStory.userName,
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (userStory.id == 1 && userStory.stories.all { !it.isViewed }) "Tap to add status update" else userStory.lastUpdated,
                color = AppTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
