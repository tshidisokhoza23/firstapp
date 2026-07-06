package com.example.firstapp.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.DividerColor
import com.example.firstapp.common.ui.theme.SecondaryText
import com.example.firstapp.media.model.MediaConversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMessagesScreen(
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit
) {
    val conversations = remember {
        listOf(
            MediaConversation("1", "John Doe", null, "Hey, how are you?", System.currentTimeMillis() - 3600000, 2),
            MediaConversation("2", "Jane Smith", null, "See you later!", System.currentTimeMillis() - 86400000, 0)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search messages", color = SecondaryText) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SecondaryText) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerColor,
                    focusedBorderColor = Color.White,
                    unfocusedContainerColor = AppSecondary,
                    focusedContainerColor = AppSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations) { conversation ->
                    ConversationItem(conversation = conversation) {
                        onConversationClick(conversation.otherUserId)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: MediaConversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        ) {
            if (conversation.otherUserAvatar != null) {
                AsyncImage(
                    model = conversation.otherUserAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.otherUserName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "1h",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    color = if (conversation.unreadCount > 0) Color.White else SecondaryText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .background(Color.Blue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
