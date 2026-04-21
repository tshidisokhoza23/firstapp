package com.example.firstapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.UserStory
import com.example.firstapp.model.StoryType
import kotlinx.coroutines.delay

@Composable
fun StatusViewer(
    userStory: UserStory,
    onStoryViewed: (Int) -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    var currentStoryIndex by remember(userStory.id) { mutableIntStateOf(0) }
    val currentStory = userStory.stories[currentStoryIndex]
    val progress = remember(userStory.id, currentStoryIndex) { Animatable(0f) }

    LaunchedEffect(userStory.id, currentStoryIndex) {
        onStoryViewed(currentStoryIndex)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 5000,
                easing = LinearEasing
            )
        )
        if (currentStoryIndex < userStory.stories.size - 1) {
            currentStoryIndex++
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (currentStory.type == StoryType.TEXT) currentStory.backgroundColor else Color.Black)
            .pointerInput(userStory.id, currentStoryIndex) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.x < size.width / 3) {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                            }
                        } else {
                            if (currentStoryIndex < userStory.stories.size - 1) {
                                currentStoryIndex++
                            } else {
                                onFinish()
                            }
                        }
                    }
                )
            }
    ) {
        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (currentStory.type == StoryType.TEXT) {
                Text(
                    text = currentStory.content,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                if (!currentStory.contentUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentStory.contentUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                if (!currentStory.content.isNullOrBlank()) {
                    Text(
                        text = currentStory.content,
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }

        // Top bars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                userStory.stories.forEachIndexed { index, _ ->
                    val barProgress = when {
                        index < currentStoryIndex -> 1f
                        index == currentStoryIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            // User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(userStory.userAvatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userStory.userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        userStory.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        currentStory.timestamp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
