package com.tpsk.tj.media.ui.screens

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
import com.tpsk.tj.common.ui.theme.AppBackground
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.ui.theme.DividerColor
import com.tpsk.tj.common.ui.theme.SecondaryText
import com.tpsk.tj.media.model.MediaConversation

import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMessagesScreen(
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatsState by viewModel.chats.collectAsState()
    val conversations: List<MediaConversation> = remember(chatsState) {
        val success = chatsState as? Resource.Success<List<com.tpsk.tj.chat.model.ChatMessage>>
        success?.data?.mapNotNull { chat ->
            val otherId = chat.otherUserId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val isMingle = chat.type == ConversationType.MINGLE || chat.isMingle || chat.id.startsWith("mingle_")
            if (isMingle || chat.isGroup || chat.type != ConversationType.DIRECT) return@mapNotNull null
            MediaConversation(
                otherUserId = otherId,
                otherUserName = chat.displayName.ifBlank { chat.senderName },
                otherUserAvatar = chat.avatarUrl,
                lastMessage = chat.lastMessage,
                lastMessageTimestamp = chat.timestamp,
                unreadCount = chat.unreadCount
            )
        } ?: emptyList()
    }

    var searchQuery by remember { mutableStateOf("") }
    
    val filteredConversations = if (searchQuery == "") {
        conversations
    } else {
        conversations.filter { 
            it.otherUserName.contains(searchQuery, ignoreCase = true) || 
            it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search messages", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (filteredConversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No social messages yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredConversations, key = { it.otherUserId }) { conversation ->
                        ConversationItem(conversation = conversation) {
                            onConversationClick(conversation.otherUserId)
                        }
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formatInboxTime(conversation.lastMessageTimestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatInboxTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val minutes = (System.currentTimeMillis() - timestamp) / 60000L
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> "${minutes / 1440}d"
    }
}
