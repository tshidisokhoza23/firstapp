package com.example.firstapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.CallLog
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatWhatsAppTime

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

// Colors for DM avatar initials
private val avatarColors = listOf(
    com.example.firstapp.ui.theme.BoyishPurple,
    com.example.firstapp.ui.theme.NiceOrange,
    Color(0xFF3949AB), Color(0xFF00897B),
    Color(0xFFD81B60), Color(0xFF0288D1),
    Color(0xFF2E7D32)
)

private fun avatarColorFor(name: String): Color =
    avatarColors[(name.hashCode() and 0x7FFFFFFF) % avatarColors.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(
    chats: List<ChatMessage>,
    calls: List<CallLog>,
    onBack: () -> Unit,
    onChatSelect: (ChatMessage) -> Unit,
    onCallSelect: (CallLog) -> Unit,
    onNewDM: () -> Unit,
    onPinChat: (Int, Boolean) -> Unit = { _, _ -> },
    onMuteChat: (Int, Boolean) -> Unit = { _, _ -> },
    onDeleteChat: (Int) -> Unit = { _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedChatForMenu by remember { mutableStateOf<ChatMessage?>(null) }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else chats.filter {
            it.senderName.contains(searchQuery, ignoreCase = true) ||
            it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    // Split: contacts with recent messages vs active (online heuristic)
    val activeContacts = remember(chats) { 
        chats.filter { it.isOnline }.take(10).ifEmpty { chats.take(5) }
    }

    val filteredCalls = remember(calls, searchQuery) {
        if (searchQuery.isBlank()) {
            calls
        } else {
            calls.filter {
                it.contactName.contains(searchQuery, ignoreCase = true) ||
                    it.callType.contains(searchQuery, ignoreCase = true) ||
                    it.status.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTextPrimary
                )
            }
            Text(
                text = "Direct Messages",
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNewDM) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "New Message",
                    tint = AppAccent
                )
            }
        }

        // Search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppSecondary,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppAccent),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search messages", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppBackground,
            contentColor = AppAccent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]).padding(horizontal = 32.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    color = AppAccent,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0, 
                onClick = { selectedTab = 0 }, 
                text = { 
                    Text(
                        "Messages", 
                        color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                    ) 
                }
            )
            Tab(
                selected = selectedTab == 1, 
                onClick = { selectedTab = 1 }, 
                text = { 
                    Text(
                        "Calls", 
                        color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                    ) 
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Active contacts row (story-ring style)
            if (selectedTab == 0 && searchQuery.isBlank() && activeContacts.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Active Now",
                            color = AppTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(activeContacts) { chat ->
                                ActiveContactBubble(chat = chat, onClick = { onChatSelect(chat) })
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AppSecondary, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Header
            item {
                Text(
                    text = when {
                        selectedTab == 0 && searchQuery.isBlank() -> "Messages"
                        selectedTab == 1 && searchQuery.isBlank() -> "Recent Calls"
                        else -> "Results"
                    },
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (selectedTab == 0 && filteredChats.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = if (searchQuery.isBlank()) "No conversations yet" else "No results found",
                        message = if (searchQuery.isBlank()) "Tap the edit icon to start a new chat with your contacts." else "We couldn't find any chats matching \"$searchQuery\".",
                        actionLabel = if (searchQuery.isBlank()) "Start Chat" else "Clear Search",
                        onAction = { if (searchQuery.isBlank()) onNewDM() else searchQuery = "" }
                    )
                }
            } else {
                items(filteredChats, key = { it.id }) { chat ->
                    DirectMessageItem(
                        chat = chat,
                        onClick = { onChatSelect(chat) },
                        onLongClick = { selectedChatForMenu = chat }
                    )
                }
            }

            if (selectedTab == 1 && filteredCalls.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Call,
                        title = if (searchQuery.isBlank()) "No call history" else "No results found",
                        message = if (searchQuery.isBlank()) "Your voice and video calls will appear here once you start one." else "We couldn't find any calls matching \"$searchQuery\".",
                        actionLabel = if (searchQuery.isBlank()) null else "Clear Search",
                        onAction = { if (searchQuery.isNotEmpty()) searchQuery = "" }
                    )
                }
            } else if (selectedTab == 1) {
                items(filteredCalls, key = { it.id }) { call ->
                    DirectCallItem(call = call, onClick = { onCallSelect(call) })
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    selectedChatForMenu?.let { chat ->
        AlertDialog(
            onDismissRequest = { selectedChatForMenu = null },
            containerColor = AppSecondary,
            title = { Text(chat.senderName, color = Color.White) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (chat.isPinned) "Unpin chat" else "Pin chat", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.PushPin, null, tint = AppTextSecondary, modifier = Modifier.graphicsLayer { rotationZ = if (chat.isPinned) 0f else 45f }) },
                        modifier = Modifier.clickable { 
                            onPinChat(chat.id, !chat.isPinned)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(if (chat.isMuted) "Unmute notifications" else "Mute notifications", color = Color.White) },
                        leadingContent = { Icon(if (chat.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, null, tint = AppTextSecondary) },
                        modifier = Modifier.clickable { 
                            onMuteChat(chat.id, !chat.isMuted)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Delete chat", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        modifier = Modifier.clickable { 
                            onDeleteChat(chat.id)
                            selectedChatForMenu = null 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppSecondary.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppAccent.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title, 
            color = Color.White, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.ExtraBold, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message, 
            color = AppTextSecondary, 
            fontSize = 14.sp, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
            ) {
                Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DirectCallItem(call: CallLog, onClick: () -> Unit) {
    val avatarColor = avatarColorFor(call.contactName)
    val (icon, statusColor, subtitle) = when (call.status.lowercase()) {
        "rejected", "declined" -> Triple(Icons.AutoMirrored.Filled.CallMissed, Color(0xFFE53935), "Declined")
        "missed", "timeout" -> Triple(Icons.AutoMirrored.Filled.CallMissed, Color(0xFFE53935), "Missed")
        "connected" -> Triple(Icons.AutoMirrored.Filled.CallReceived, AppAccent, "Connected")
        else -> Triple(Icons.Default.Call, AppTextSecondary, "${call.callType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} • ${call.durationSeconds}s")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            if (!call.contactAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = call.contactAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = call.contactName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = formatWhatsAppTime(call.startedAt),
            color = AppTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = if (call.callType == "video") Icons.AutoMirrored.Filled.CallMade else Icons.Default.Call,
            contentDescription = null,
            tint = AppAccent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ActiveContactBubble(chat: ChatMessage, onClick: () -> Unit) {
    val color = chat.avatarColor.takeIf { it != androidx.compose.ui.graphics.Color.Gray }
        ?: avatarColorFor(chat.senderName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            // Avatar ring
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(AppAccent, Color(0xFF7C4DFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    if (!chat.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = chat.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = chat.senderName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Online dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AppBackground)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = chat.senderName.split(" ").first(),
            color = AppTextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectMessageItem(
    chat: ChatMessage,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val color = chat.avatarColor.takeIf { it != androidx.compose.ui.graphics.Color.Gray }
        ?: avatarColorFor(chat.senderName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = chat.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chat.senderName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(AppBackground)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.senderName,
                    color = AppTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatWhatsAppTime(chat.time),
                    color = if (chat.unreadCount > 0) AppAccent else AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = 45f })
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.isMuted) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.isTyping) {
                    Text(
                        text = "typing...",
                        color = AppAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = chat.lastMessage.ifBlank { "Start a conversation" },
                        color = if (chat.unreadCount > 0) AppTextPrimary else AppTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
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
