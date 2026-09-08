package com.example.firstapp.chat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.ui.components.ChatBottomBarUnique
import com.example.firstapp.chat.ui.components.ChatMessageList
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.util.Resource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    chat: ChatMessage,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val group = chat.copy(type = ConversationType.GROUP, isGroup = true)

    LaunchedEffect(group.id) {
        chatViewModel.fetchMessages(group.id)
        chatViewModel.fetchGroupInfo(group.id)
    }

    DisposableEffect(group.id) {
        onDispose {
            chatViewModel.clearActiveChat()
        }
    }

    val uiState by chatViewModel.uiState.collectAsState()
    val activeChat by chatViewModel.activeChat.collectAsState()
    val currentChat = (if (activeChat?.id == group.id) activeChat!! else group).copy(type = ConversationType.GROUP, isGroup = true)
    val fontSize by chatViewModel.fontSize.collectAsState()
    val enterIsSend by chatViewModel.enterIsSend.collectAsState()
    val animationsEnabled by chatViewModel.animationsEnabled.collectAsState()
    val showReadReceipts by chatViewModel.showReadReceipts.collectAsState()
    val messages = uiState.messages.data.orEmpty()
    val listState = rememberLazyListState()
    var messageText by remember(currentChat.id) { mutableStateOf("") }
    var replyingTo by remember(currentChat.id) { mutableStateOf<Message?>(null) }
    val selectedMessages = remember(currentChat.id) { mutableStateListOf<String>() }
    val playingAudioUri by chatViewModel.currentlyPlayingAudio.collectAsState()
    val memberCount = uiState.groupInfo.data?.member_ids?.size

    val groupedMessages = remember(messages) {
        messages.groupBy { msg ->
            val date = Date(msg.timestamp)
            val cal = Calendar.getInstance().apply { time = date }
            val now = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            when {
                now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> "Today"
                yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    val groupName = currentChat.senderName.takeIf {
        it.isNotBlank() && it.lowercase() != "unknown" && it.lowercase() != "null"
    } ?: chatViewModel.getCachedGroupName(currentChat.id) ?: "Group"

    val subtitle = when {
        uiState.typingStatus[currentChat.id] == true -> "Someone is typing…"
        memberCount != null && memberCount > 0 -> "$memberCount members"
        else -> "Group chat"
    }

    fun sendText() {
        val body = messageText.trim()
        if (body.isBlank()) return
        chatViewModel.sendMessage(
            chatId = currentChat.id,
            body = body,
            messageType = "text",
            replyToId = replyingTo?.id,
            replyToText = replyingTo?.content,
            replyToSenderName = if (replyingTo?.isFromMe == true) "You" else replyingTo?.senderName
        )
        messageText = ""
        replyingTo = null
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable(onClick = onOpenGroupInfo),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentChat.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentChat.avatarUrl,
                                contentDescription = groupName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .clickable(onClick = onOpenGroupInfo)
                    ) {
                        Text(
                            text = groupName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onOpenGroupInfo) {
                        Icon(Icons.Default.Info, contentDescription = "Group info")
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                replyingTo?.let { reply ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Replying to ${if (reply.isFromMe) "You" else reply.senderName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(reply.content, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { replyingTo = null }) { Text("Cancel") }
                        }
                    }
                }
                ChatBottomBarUnique(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = { sendText() },
                    onAttachClick = {
                        Toast.makeText(context, "Use the group thread to send photos from the attachment picker", Toast.LENGTH_SHORT).show()
                    },
                    onEmojiClick = { messageText += "🙂" },
                    onCameraClick = {},
                    onMicClick = {},
                    enterIsSend = enterIsSend,
                    placeholder = "Message $groupName"
                )
            }
        }
    ) { padding ->
        if (uiState.messages is Resource.Error && messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text((uiState.messages as Resource.Error).message ?: "Could not load group messages")
            }
        } else {
            ChatMessageList(
                    modifier = Modifier.padding(padding),
                    messagesLazyListState = listState,
                    groupedMessages = groupedMessages,
                    messages = messages,
                    selectedMessages = selectedMessages,
                    chat = currentChat,
                    fontSize = fontSize,
                    showReadReceipts = showReadReceipts,
                    animationsEnabled = animationsEnabled,
                    scope = scope,
                    messagesCount = messages.size,
                    onMessageClick = { msg ->
                        if (selectedMessages.isNotEmpty()) {
                            if (selectedMessages.contains(msg.id)) selectedMessages.remove(msg.id) else selectedMessages.add(msg.id)
                        }
                    },
                    onMessageLongClick = { msg ->
                        if (!selectedMessages.contains(msg.id)) selectedMessages.add(msg.id)
                    },
                    onReply = { replyingTo = it },
                    onMediaClick = { _, _ -> },
                    playingAudioUri = playingAudioUri,
                    onPlayAudio = { chatViewModel.setPlayingAudio(it) }
                )
            }
        }
    }
