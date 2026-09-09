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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    onAvatarClick: (String) -> Unit = { _ -> },
    onInitiateCall: (String, String, CallType) -> Unit,
    onClearChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onShowMessageInfo: () -> Unit = {},
    onStarSelectedMessages: () -> Unit,
    onPinSelectedMessage: () -> Unit = {},
    onEditSelectedMessage: () -> Unit,
    onForwardSelectedMessages: () -> Unit,
    onReactSelectedMessage: () -> Unit = {},
    onReportSelectedMessage: () -> Unit = {},
    onClearSelectedMessages: () -> Unit,
    onSearchNavigate: (Boolean) -> Unit = {},
    activeSearchIndex: Int = 0,
    totalSearchCount: Int = 0
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextField(
                        value = chatSearchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (chatSearchQuery != "") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (totalSearchCount > 0) "${activeSearchIndex + 1}/$totalSearchCount" else "0/0",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = { onSearchNavigate(false) }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Prev", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onSearchNavigate(true) }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else if (selectedMessagesCount > 0) {
                    IconButton(onClick = onClearSelectedMessages) {
                        Icon(Icons.Default.Close, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = "$selectedMessagesCount Selected",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                    if (selectedMessagesCount == 1) {
                        IconButton(onClick = onShowMessageInfo) { Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurface) }
                        IconButton(onClick = onEditSelectedMessage) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurface) }
                        IconButton(onClick = onReactSelectedMessage) { Icon(Icons.Default.AddReaction, "React", tint = MaterialTheme.colorScheme.onSurface) }
                        IconButton(onClick = onReportSelectedMessage) { Icon(Icons.Default.Flag, "Report", tint = MaterialTheme.colorScheme.onSurface) }
                        IconButton(onClick = onPinSelectedMessage) { Icon(Icons.Default.PushPin, "Pin", tint = MaterialTheme.colorScheme.onSurface) }
                    }
                    IconButton(onClick = onStarSelectedMessages) { Icon(Icons.Default.Star, "Star", tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = onDeleteSelectedMessages) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = onForwardSelectedMessages) { Icon(Icons.AutoMirrored.Filled.Forward, "Forward", tint = MaterialTheme.colorScheme.onSurface) }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
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
                        ChatAvatar(
                            imageUrl = chat.avatarUrl,
                            name = sanitizedName,
                            size = 40.dp,
                            fontSize = 18.sp,
                            backgroundColor = if (chat.avatarColor != Color.Transparent && chat.avatarColor != Color.Gray) 
                                chat.avatarColor else MaterialTheme.colorScheme.surfaceVariant,
                            textColor = if (chat.avatarColor != Color.Transparent && chat.avatarColor != Color.Gray) 
                                Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val targetId = if (chat.type == ConversationType.DIRECT) (chat.otherUserId ?: chat.id) else chat.id
                                onAvatarClick(targetId)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val targetId = if (chat.type == ConversationType.DIRECT) (chat.otherUserId ?: chat.id) else chat.id
                                    onShowProfile(targetId, chat.isGroup)
                                }
                        ) {
                            Text(
                                text = sanitizedName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val presenceText = when {
                                isTyping -> "typing..."
                                chat.type == ConversationType.CHANNEL -> "channel · tap for details"
                                chat.type == ConversationType.GROUP || chat.isGroup -> "group · tap for info"
                                chat.isOnline -> "online"
                                chat.lastSeen > 0L -> formatDirectLastSeen(chat.lastSeen)
                                else -> "offline"
                            }
                            Text(
                                text = presenceText,
                                fontSize = 11.sp,
                                color = if (isTyping || chat.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chat.type == ConversationType.CHANNEL) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val deepLink = "firstapp://channel/${chat.id}"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this channel: $sanitizedName")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Follow $sanitizedName on TJ: $deepLink")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Channel"))
                            }) {
                                Icon(Icons.Default.Share, "Share Channel", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            val targetId = chat.otherUserId ?: chat.id
                            IconButton(onClick = { onInitiateCall(targetId, sanitizedName, CallType.VIDEO) }) {
                                Icon(Icons.Default.Videocam, "Video Call", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = { onInitiateCall(targetId, sanitizedName, CallType.VOICE) }) {
                                Icon(Icons.Default.Call, "Voice Call", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when {
                                                chat.type == ConversationType.CHANNEL -> "Channel info"
                                                chat.type == ConversationType.GROUP || chat.isGroup -> "Group info"
                                                else -> "View Profile"
                                            },
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = { 
                                        showMenu = false
                                        val tid = if (chat.type == ConversationType.DIRECT) (chat.otherUserId ?: chat.id) else chat.id
                                        onShowProfile(tid, chat.isGroup || chat.type == ConversationType.GROUP) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search Chat", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { showMenu = false; onSearchModeChange(true) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Messages", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { showMenu = false; onClearChat() }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                DropdownMenuItem(
                                    text = { Text("Delete Conversation", color = MaterialTheme.colorScheme.error) },
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

internal fun formatDirectLastSeen(lastSeenMillis: Long): String {
    if (lastSeenMillis <= 0L) return "offline"
    val now = Calendar.getInstance()
    val seen = Calendar.getInstance().apply { timeInMillis = lastSeenMillis }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSeenMillis))
    return when {
        now.get(Calendar.YEAR) == seen.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == seen.get(Calendar.DAY_OF_YEAR) ->
            "last seen today at $time"
        now.get(Calendar.YEAR) == seen.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - seen.get(Calendar.DAY_OF_YEAR) == 1 ->
            "last seen yesterday at $time"
        else -> "last seen ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastSeenMillis))} at $time"
    }
}
