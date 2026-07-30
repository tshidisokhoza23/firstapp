package com.example.firstapp.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.*
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
import com.example.firstapp.chat.data.CallType
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    chat: ChatMessage,
    sanitizedName: String,
    isSearchMode: Boolean,
    chatSearchQuery: String,
    selectedMessagesCount: Int,
    isTyping: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchModeChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onShowProfile: (String, Boolean) -> Unit,
    onInitiateCall: (String, CallType) -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onForwardSelectedMessages: () -> Unit,
    onClearSelectedMessages: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppBackground,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchMode) {
                    IconButton(onClick = { 
                        onSearchModeChange(false)
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppAccent)
                    }
                    TextField(
                        value = chatSearchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search...", color = AppTextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            cursorColor = AppAccent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (chatSearchQuery != "") {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, "Clear", tint = AppTextSecondary)
                        }
                    }
                } else if (selectedMessagesCount > 0) {
                    IconButton(onClick = onClearSelectedMessages) {
                        Icon(Icons.Default.Close, "Back", tint = AppTextPrimary)
                    }
                    Text(
                        text = "$selectedMessagesCount Selected",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                    IconButton(onClick = onDeleteSelectedMessages) { Icon(Icons.Default.Delete, "Delete", tint = AppTextPrimary) }
                    IconButton(onClick = onForwardSelectedMessages) { Icon(Icons.AutoMirrored.Filled.Forward, "Forward", tint = AppTextPrimary) }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppTextPrimary)
                    }
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val targetId = if (chat.type == ConversationType.DIRECT) (chat.otherUserId ?: chat.id) else chat.id
                                onShowProfile(targetId, chat.isGroup)
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppSecondary)
                        ) {
                            if (chat.avatarUrl != null) {
                                AsyncImage(
                                    model = chat.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "?",
                                        color = AppAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sanitizedName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val presenceText = when {
                                isTyping -> "typing..."
                                chat.type == ConversationType.CHANNEL -> "channel"
                                chat.isGroup -> "group"
                                chat.isOnline -> "online"
                                chat.lastSeen > 0L -> "active"
                                else -> "offline"
                            }
                            Text(
                                text = presenceText,
                                fontSize = 11.sp,
                                color = if (isTyping || chat.isOnline) AppAccent else AppTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val targetId = chat.otherUserId ?: chat.id
                        IconButton(onClick = { onInitiateCall(targetId, CallType.VIDEO) }) {
                            Icon(Icons.Default.Videocam, "Video Call", tint = AppTextPrimary, modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { onInitiateCall(targetId, CallType.VOICE) }) {
                            Icon(Icons.Default.Call, "Voice Call", tint = AppTextPrimary, modifier = Modifier.size(20.dp))
                        }
                        
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = AppTextPrimary)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(AppSecondary)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (chat.isGroup) "Group Info" else "View Profile", color = AppTextPrimary) },
                                    onClick = { 
                                        showMenu = false
                                        val tid = if (chat.type == ConversationType.DIRECT) (chat.otherUserId ?: chat.id) else chat.id
                                        onShowProfile(tid, chat.isGroup) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search Chat", color = AppTextPrimary) },
                                    onClick = { showMenu = false; onSearchModeChange(true) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Messages", color = AppTextPrimary) },
                                    onClick = { showMenu = false; onClearChat() }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.05f))
                                DropdownMenuItem(
                                    text = { Text("Delete Conversation", color = Color.Red.copy(alpha = 0.8f)) },
                                    onClick = { showMenu = false; onDeleteChat() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
