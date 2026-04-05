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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.data.ChatService
import com.example.firstapp.model.UserStory
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(onBack: () -> Unit) {
    var viewingUserStory by remember { mutableStateOf<UserStory?>(null) }
    val stories = remember { mutableStateListOf(*ChatService.dummyStories.toTypedArray()) }
    
    val recentUpdates = stories.filter { !it.allViewed && it.id != 1 }
    val viewedUpdates = stories.filter { it.allViewed && it.id != 1 }
    val myStatus = stories.find { it.id == 1 }

    if (viewingUserStory != null) {
        StatusViewer(
            userStory = viewingUserStory!!,
            onStoryViewed = { storyIndex ->
                val userIndex = stories.indexOfFirst { it.id == viewingUserStory!!.id }
                if (userIndex != -1) {
                    val user = stories[userIndex]
                    if (!user.stories[storyIndex].isViewed) {
                        val updatedStories = user.stories.mapIndexed { index, story ->
                            if (index == storyIndex) story.copy(isViewed = true) else story
                        }
                        stories[userIndex] = user.copy(stories = updatedStories)
                        // Keep viewingUserStory in sync to avoid UI glitches
                        viewingUserStory = stories[userIndex]
                    }
                }
            },
            onFinish = {
                if (viewingUserStory!!.id == 1) {
                    // "My Status" finished, just close
                    viewingUserStory = null
                } else {
                    // Find the next user who has unviewed stories
                    val nextUser = stories.filter { !it.allViewed && it.id != 1 && it.id != viewingUserStory!!.id }.firstOrNull()
                    if (nextUser != null) {
                        viewingUserStory = nextUser
                    } else {
                        viewingUserStory = null
                    }
                }
            },
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
                        IconButton(onClick = {}) { Icon(Icons.Default.CameraAlt, "Camera", tint = AppTextPrimary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Search, "Search", tint = AppTextPrimary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Edit, "Edit", tint = AppTextPrimary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More", tint = AppTextPrimary) }
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
                        StoryListItem(userStory, onClick = { viewingUserStory = userStory })
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
