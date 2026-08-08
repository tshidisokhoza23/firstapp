package com.example.firstapp.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Update
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
import androidx.compose.ui.draw.shadow
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
import com.example.firstapp.media.model.UserStory

@Composable
fun StoriesRow(
    stories: List<UserStory>,
    currentUserId: String? = null,
    onStoryClick: (UserStory) -> Unit,
    onMediaStoryClick: () -> Unit,
    onTextStoryClick: () -> Unit,
    onPostedStoriesClick: () -> Unit = {}
) {
    val myStory = stories.find { it.id == currentUserId }
    val otherStories = stories.filter { it.id != currentUserId }

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
            item {
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
            items(otherStories, key = { it.id }) { story ->
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
                    .clickable(onClick = onClick)
                    .border(
                        width = 2.dp,
                        brush = if (allViewed) Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f)))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7C4DFF), MaterialTheme.colorScheme.primary)),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(story.userAvatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.userAvatarUrl != null) {
                        AsyncImage(
                            model = story.userAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
            text = "My Story",
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
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Media story", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onMediaStory,
            leadingIcon = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Text story", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onTextStory,
            leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Posted stories", color = MaterialTheme.colorScheme.onSurface) },
            onClick = onPostedStories,
            leadingIcon = { Icon(Icons.Default.Update, null, tint = MaterialTheme.colorScheme.onSurface) }
        )
    }
}

@Composable
private fun StoryThumbnail(story: UserStory, onClick: () -> Unit) {
    val allViewed = story.stories.all { it.isViewed }
    val haptic = LocalHapticFeedback.current
    
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
            modifier = Modifier
                .size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect for unviewed stories
            if (!allViewed) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            spotColor = MaterialTheme.colorScheme.primary
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = if (allViewed) Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7C4DFF), MaterialTheme.colorScheme.primary)),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(story.userAvatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.userAvatarUrl != null) {
                        AsyncImage(
                            model = story.userAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
