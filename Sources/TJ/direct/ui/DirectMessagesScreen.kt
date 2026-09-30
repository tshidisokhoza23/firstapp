package com.tpsk.tj.direct.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.CallLog
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.ui.components.ChatAvatar

import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.util.DisplayNameResolver
import com.tpsk.tj.common.util.formatWhatsAppTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(
    onBack: () -> Unit,
    onChatSelect: (ChatMessage) -> Unit,
    onCallSelect: (CallLog) -> Unit,
    onNewDM: () -> Unit,
    viewModel: DirectMessagesViewModel
) {
    val callsResource by viewModel.calls.collectAsState()
    val calls = callsResource.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search DMs */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CallList(calls = calls, onCallSelect = onCallSelect)
        }
    }
}

@Composable
fun DMList(chats: List<ChatMessage>, onChatSelect: (ChatMessage) -> Unit) {
    if (chats.isEmpty()) {
        EmptyState("No messages yet")
    } else {
        LazyColumn {
            items(chats) { chat ->
                DMItem(chat = chat, onClick = { onChatSelect(chat) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp), 
                    thickness = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun CallList(calls: List<CallLog>, onCallSelect: (CallLog) -> Unit) {
    if (calls.isEmpty()) {
        EmptyState("No recent calls")
    } else {
        LazyColumn {
            items(calls) { call ->
                CallItem(call = call, onClick = { onCallSelect(call) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp), 
                    thickness = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun DMItem(chat: ChatMessage, onClick: () -> Unit) {
    val context = LocalContext.current
    val displayNameResolver = remember(context) { DisplayNameResolver(context) }
    val resolvedName = remember(chat.id, chat.senderName, chat.otherUserId, chat.phoneNumber) {
        displayNameResolver.resolve(
            User(
                id = chat.otherUserId ?: chat.id,
                username = chat.senderName,
                displayName = chat.senderName,
                phoneNumber = chat.phoneNumber ?: ""
            ),
            chat.phoneNumber ?: ""
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(
            imageUrl = chat.avatarUrl,
            name = resolvedName,
            size = 50.dp,
            fontSize = 20.sp,
            backgroundColor = if (chat.avatarColor != Color.Transparent) chat.avatarColor else MaterialTheme.colorScheme.surfaceVariant,
            textColor = Color.White
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(resolvedName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(formatWhatsAppTime(chat.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(chat.lastMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun CallItem(call: CallLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(
            imageUrl = call.contactAvatar,
            name = call.contactName,
            size = 50.dp,
            fontSize = 20.sp,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(call.contactName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (call.status) {
                        "missed" -> Icons.AutoMirrored.Filled.CallMissed
                        "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                        else -> Icons.AutoMirrored.Filled.CallReceived
                    },
                    contentDescription = null,
                    tint = if (call.status == "missed") Color.Red else Color.Green,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(formatWhatsAppTime(call.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = if (call.callType.equals("video", ignoreCase = true)) Icons.Default.VideoCall else Icons.Default.Call,
                contentDescription = "Call back",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
