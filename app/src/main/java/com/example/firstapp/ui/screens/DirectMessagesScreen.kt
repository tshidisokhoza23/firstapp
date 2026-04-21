package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.CallLog
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatWhatsAppTime

// Colors for DM avatar initials
private val avatarColors = listOf(
    Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF00897B),
    Color(0xFFD81B60), Color(0xFF6D4C41), Color(0xFF0288D1),
    Color(0xFF2E7D32), Color(0xFFF57C00)
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
    onNewDM: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else chats.filter {
            it.senderName.contains(searchQuery, ignoreCase = true) ||
            it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    // Split: contacts with recent messages vs active (online heuristic)
    val activeContacts = remember(chats) { chats.take(5) }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppSecondary)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = AppTextPrimary, fontSize = 15.sp),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text("Search messages", color = AppTextSecondary, fontSize = 15.sp)
                }
                innerTextField()
            }
        )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppBackground,
            contentColor = AppAccent,
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Messages") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Calls") })
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No conversations yet.\nTap the edit icon to start one." else "No results for \"$searchQuery\"",
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredChats, key = { it.id }) { chat ->
                    DirectMessageItem(
                        chat = chat,
                        onClick = { onChatSelect(chat) }
                    )
                }
            }

            if (selectedTab == 1 && filteredCalls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No call history yet." else "No call results for \"$searchQuery\"",
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (selectedTab == 1) {
                items(filteredCalls, key = { it.id }) { call ->
                    DirectCallItem(call = call, onClick = { onCallSelect(call) })
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
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
            Text(
                text = call.contactName.take(1).uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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
                    Text(
                        text = chat.senderName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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

@Composable
private fun DirectMessageItem(chat: ChatMessage, onClick: () -> Unit) {
    val color = chat.avatarColor.takeIf { it != androidx.compose.ui.graphics.Color.Gray }
        ?: avatarColorFor(chat.senderName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Text(
                    text = chat.senderName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd)
                )
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
                Text(
                    text = chat.lastMessage.ifBlank { "Start a conversation" },
                    color = if (chat.unreadCount > 0) AppTextPrimary else AppTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
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
