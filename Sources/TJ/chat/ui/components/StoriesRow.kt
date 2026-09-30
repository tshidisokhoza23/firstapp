package com.tpsk.tj.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.chat.model.UserStory

fun formatMediaUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("content://") || url.startsWith("file://") || url.startsWith("data:")) return url
    val cleanPath = url.removePrefix("/")
    val base = com.tpsk.tj.common.data.network.NetworkConstants.BASE_URL.removeSuffix("/api/v1/").removeSuffix("/")
    return "$base/$cleanPath"
}

@Composable
fun StoriesRow(
    stories: List<UserStory>,
    currentUserId: String? = null,
    onStoryClick: (UserStory) -> Unit,
    onMediaStoryClick: () -> Unit,
    onTextStoryClick: () -> Unit,
    onPostedStoriesClick: () -> Unit = {}
) {
    val myStory = remember(stories, currentUserId) {
        stories.find { 
            it.id == currentUserId ||
            it.userId == currentUserId ||
            it.id == "user_$currentUserId" || 
            it.userId == "user_$currentUserId" ||
            (currentUserId != null && (
                it.id == currentUserId.removePrefix("user_") ||
                it.userId == currentUserId.removePrefix("user_")
            )) ||
            it.userName == "My Story" ||
            it.userName == "My Number"
        }
    }

    val otherStories = remember(stories, currentUserId) {
        val filtered = stories.filter { 
            it.id != currentUserId &&
            it.userId != currentUserId &&
            it.id != "user_$currentUserId" &&
            it.userId != "user_$currentUserId" &&
            (currentUserId == null || (
                it.id != currentUserId.removePrefix("user_") &&
                it.userId != currentUserId.removePrefix("user_")
            )) &&
            it.userName != "My Story" && 
            it.userName != "My Number"
        }
        
        // Sort: Unviewed first (sorted by latest timestamp), then Viewed (sorted by latest timestamp)
        val unviewed = filtered.filter { !it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
        val viewed = filtered.filter { it.allViewed }
            .sortedByDescending { it.stories.maxOfOrNull { s -> s.createdAt } ?: 0L }
        unviewed + viewed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(contentType = "my_story") {
                if (myStory != null) {
                    MyStoryThumbnail(
                        story = myStory, 
                        onClick = { onStoryClick(myStory) }, 
                        onMediaClick = onMediaStoryClick,
                        onTextClick = onTextStoryClick,
                        onPostedClick = onPostedStoriesClick
                    )
                } else {
                    AddStoryItem(
                        onMediaClick = onMediaStoryClick,
                        onTextClick = onTextStoryClick,
                        onPostedClick = onPostedStoriesClick
                    )
                }
            }

            items(
                items = otherStories,
                key = { it.id },
                contentType = { "story_item" }
            ) { story ->
                StoryThumbnail(story = story, onClick = { onStoryClick(story) })
            }
        }
    }
}

@Composable
private fun MyStoryThumbnail(
    story: UserStory, 
    onClick: () -> Unit, 
    onMediaClick: () -> Unit,
    onTextClick: () -> Unit,
    onPostedClick: () -> Unit
) {
    val allViewed = story.allViewed
    var showMenu by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderBrush = remember(allViewed, primaryColor) {
        if (allViewed) Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
        else Brush.linearGradient(listOf(primaryColor, Color(0xFF7C4DFF), primaryColor))
    }

    val latestStory = remember(story.stories) { story.stories.lastOrNull() }
    val storyThumbnailUrl = remember(latestStory) { formatMediaUrl(latestStory?.contentUrl) ?: story.userAvatarUrl }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable(onClick = {
                        if (story.stories.isNotEmpty()) {
                            onClick()
                        } else {
                            showMenu = true
                        }
                    })
                    .border(
                        width = 2.dp,
                        brush = borderBrush,
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
                    if (!storyThumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = storyThumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (latestStory?.type == StoryType.TEXT && latestStory.content.isNotBlank()) {
                        Text(
                            text = latestStory.content,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Text(
                            text = story.userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            
            // Small Add button overlay
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = { showMenu = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
            
            StoryContextMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onMediaStory = { showMenu = false; onMediaClick() },
                onTextStory = { showMenu = false; onTextClick() },
                onPostedStories = { showMenu = false; onPostedClick() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "My Story",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddStoryItem(
    onMediaClick: () -> Unit,
    onTextClick: () -> Unit,
    onPostedClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { showMenu = true }
        )
    ) {
        Box(
            modifier = Modifier
                .size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add Story",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        StoryContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onMediaStory = { showMenu = false; onMediaClick() },
            onTextStory = { showMenu = false; onTextClick() },
            onPostedStories = { showMenu = false; onPostedClick() }
        )
    }
}

@Composable
private fun StoryContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onMediaStory: () -> Unit,
    onTextStory: () -> Unit,
    onPostedStories: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = { Text("Media story", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onMediaStory,
            leadingIcon = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
        DropdownMenuItem(
            text = { Text("Text story", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onTextStory,
            leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
        DropdownMenuItem(
            text = { Text("Posted stories", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onPostedStories,
            leadingIcon = { Icon(Icons.Default.Update, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
    }
}

@Composable
private fun StoryThumbnail(story: UserStory, onClick: () -> Unit) {
    val allViewed = remember(story.stories) { story.stories.all { it.isViewed } }
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderBrush = remember(allViewed, primaryColor) {
        if (allViewed) Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
        else Brush.linearGradient(listOf(primaryColor, Color(0xFF7C4DFF), primaryColor))
    }

    val latestStory = remember(story.stories) { story.stories.lastOrNull() }
    val thumbnailMediaUrl = remember(latestStory) { formatMediaUrl(latestStory?.contentUrl) ?: story.userAvatarUrl }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick() 
            }
        )
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = borderBrush,
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
                    if (!thumbnailMediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailMediaUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (latestStory?.type == StoryType.TEXT && latestStory.content.isNotBlank()) {
                        Text(
                            text = latestStory.content,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Text(
                            text = story.userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = story.userName.split(" ").first(),
            color = if (allViewed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (allViewed) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp)
        )
    }
}
