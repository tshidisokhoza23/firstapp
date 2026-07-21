@file:Suppress("DEPRECATION")

package com.example.firstapp.chat.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.example.firstapp.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.data.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import com.example.firstapp.common.util.*
import com.example.firstapp.common.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.chat.ui.components.AttachmentPanel
import com.example.firstapp.chat.ui.components.EmojiPicker
import com.example.firstapp.chat.ui.components.ForwardDialog
import com.example.firstapp.chat.ui.components.FullScreenImagePlayer
import com.example.firstapp.chat.ui.components.FullScreenVideoPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatMessagesScreen(
    chat: ChatMessage,
    chatViewModel: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onShowProfile: (String, Boolean) -> Unit,
    jobId: String? = null,
    jobTitle: String? = null,
    companyName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    
    val fontSize by chatViewModel.fontSize.collectAsState()
    val enterIsSend by chatViewModel.enterIsSend.collectAsState()
    val animationsEnabled by chatViewModel.animationsEnabled.collectAsState()
    val showReadReceipts by chatViewModel.showReadReceipts.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val messagesResource by chatViewModel.messages.collectAsState()
    val messages = messagesResource.data ?: emptyList()
    val messagesLazyListState = rememberLazyListState()
    var lastMessageCount by remember(chat.id) { mutableIntStateOf(0) }
    
    var showAttachments by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    val messagesToForward = remember { mutableStateListOf<Message>() }
    
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    val selectedMessages = remember { mutableStateListOf<String>() }

    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = fullScreenImageUri != null || fullScreenVideoUri != null || showAttachments || showEmojiPicker || selectedMessages.isNotEmpty()) {
        when {
            fullScreenImageUri != null -> fullScreenImageUri = null
            fullScreenVideoUri != null -> fullScreenVideoUri = null
            showAttachments -> showAttachments = false
            showEmojiPicker -> showEmojiPicker = false
            selectedMessages.isNotEmpty() -> selectedMessages.clear()
        }
    }

    // Media capturing
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    fun sendToServer(body: String, messageType: String = "text", mediaUrl: String? = null, duration: String? = null, fileName: String? = null, fileSize: String? = null) {
        chatViewModel.sendMessage(chatId = chat.id, body = body, messageType = messageType, mediaUrl = mediaUrl, duration = duration, fileName = fileName, fileSize = fileSize, replyToId = replyingToMessage?.id)
        replyingToMessage = null
    }

    fun sendMediaMessage(uri: Uri, type: String, name: String? = null, duration: String? = null) {
        scope.launch {
            val file = MediaUtils.getFileFromUri(context, uri)
            if (file != null) {
                chatViewModel.sendMediaMessage(chat.id, file, type, name, duration)
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { sendMediaMessage(it, "image") } }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { sendMediaMessage(it, "video") } }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { sendMediaMessage(it, "document", it.lastPathSegment ?: "File") } }
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { sendMediaMessage(it, "audio") } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) tempPhotoUri?.let { sendMediaMessage(it, "image") } }
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri -> uri?.let { /* contact handling */ } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) { /* handle granted */ } }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var recorder: MediaRecorder? = null
    var audioFile: File? = null
    var recordingStartMs = 0L

    fun showToast(msg: String) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    fun onReact(messageId: String, emoji: String) { chatViewModel.reactToMessage(messageId, emoji) }
    fun deleteSelectedMessages() { selectedMessages.forEach { chatViewModel.deleteMessage(it) }; selectedMessages.clear() }

    fun startRecording() {
        try {
            audioFile = File(context.cacheDir, "VOICE_" + System.currentTimeMillis() + ".m4a")
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true; recordingStartMs = System.currentTimeMillis(); recordingDurationSeconds = 0
            scope.launch { while(isRecording) { delay(1000); recordingDurationSeconds++ } }
        } catch (e: Exception) { e.printStackTrace(); showToast("Recording failed") }
    }
    fun stopRecording(cancelled: Boolean = false) {
        try {
            recorder?.apply { stop(); release() }; recorder = null; isRecording = false
            if (!cancelled) {
                val seconds = ((System.currentTimeMillis() - recordingStartMs) / 1000L).coerceAtLeast(1L)
                val durationLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
                audioFile?.let { sendMediaMessage(Uri.fromFile(it), "audio_message", "Voice Note", durationLabel) }
            } else { audioFile?.delete(); showToast("Recording cancelled") }
        } catch (e: Exception) { e.printStackTrace() }
    }

    val chatSearchResults by chatViewModel.chatSearchResults.collectAsState()
    var isSearchMode by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    LaunchedEffect(chatSearchQuery) { if (isSearchMode) chatViewModel.searchInChat(chat.id, chatSearchQuery) }

    LaunchedEffect(chat.id) {
        chatViewModel.fetchMessages(chat.id)
        chatViewModel.markAsRead(chat.id)
    }

    LaunchedEffect(messagesResource, jobTitle, companyName) {
        if (messagesResource is Resource.Success && (messagesResource.data?.isEmpty() == true) && jobTitle != null && companyName != null) {
            val systemMessage = "Hi, I would like to apply for the position of $jobTitle at $companyName. Attached is my profile summary."
            sendToServer(body = systemMessage, messageType = "application_link")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastMsg = messages[messages.size - 1]
            val isAtBottom = messagesLazyListState.firstVisibleItemIndex >= (messages.size - 8).coerceAtLeast(0)
            
            // Force scroll to bottom if it's our own message or we are already at the bottom
            if (lastMessageCount == 0 || lastMsg.isFromMe || isAtBottom) {
                delay(200)
                if (animationsEnabled) {
                    messagesLazyListState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                } else {
                    messagesLazyListState.scrollToItem((messages.size - 1).coerceAtLeast(0))
                }
            }
        }
        lastMessageCount = messages.size
    }

    val shouldLoadOlder by remember {
        derivedStateOf {
            messages.isNotEmpty() &&
                messagesLazyListState.firstVisibleItemIndex <= 2 &&
                messagesLazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(shouldLoadOlder, messages.firstOrNull()?.timestamp, chat.id) {
        if (!shouldLoadOlder) return@LaunchedEffect
        val oldestTs = messages.minOfOrNull { it.timestamp } ?: return@LaunchedEffect
        chatViewModel.loadMoreMessages(chat.id, oldestTs)
    }

    var isTypingSent by remember { mutableStateOf(false) }
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            if (!isTypingSent) {
                ChatWebSocketClient.sendTyping(chat.id.toString(), true)
                isTypingSent = true
            }
            delay(3000)
            if (isTypingSent) {
                ChatWebSocketClient.sendTyping(chat.id.toString(), false)
                isTypingSent = false
            }
        } else {
            if (isTypingSent) {
                ChatWebSocketClient.sendTyping(chat.id.toString(), false)
                isTypingSent = false
            }
        }
    }

    DisposableEffect(chat.id) {
        onDispose {
            if (isTypingSent) {
                ChatWebSocketClient.sendTyping(chat.id, false)
            }
        }
    }

    val groupedMessages = remember(messages, chatSearchQuery, chatSearchResults) {
        val filtered = if (chatSearchQuery.isBlank()) messages else chatSearchResults
        filtered.groupBy { msg ->
            val date = Date(msg.timestamp)
            val cal = Calendar.getInstance().apply { time = date }
            val now = Calendar.getInstance()
            val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            when { isToday -> "Today"; isYesterday -> "Yesterday"; else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date) }
        }
    }

    val sanitizedName = remember(chat.senderName) {
        val cachedName = GlobalCallState.getContactName(chat.id)
        chat.senderName.takeIf { 
            it.isNotBlank() && 
            !it.startsWith("User ") && 
            it.lowercase() != "unknown" && 
            it.lowercase() != "null" && 
            it.lowercase() != "undefined" 
        } ?: cachedName ?: chat.id.toString()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppBackground.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearchMode) {
                                IconButton(onClick = { 
                                    isSearchMode = false
                                    chatSearchQuery = ""
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                                }
                                TextField(
                                    value = chatSearchQuery,
                                    onValueChange = { chatSearchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search messages...", color = Color.White.copy(alpha = 0.5f)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = AppAccent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                                if (chatSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { chatSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Clear", tint = Color.White)
                                    }
                                }
                            } else {
                                IconButton(onClick = { if (selectedMessages.isNotEmpty()) selectedMessages.clear() else onBack() }) {
                                    Icon(if (selectedMessages.isEmpty()) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, "Back", tint = Color.White)
                                }

                                if (selectedMessages.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onShowProfile(chat.id, chat.isGroup) }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(AppPrimary, AppAccent)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (chat.avatarUrl != null) {
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
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = sanitizedName,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (chat.type == ConversationType.CHANNEL && chat.isAdmin) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Default.AdminPanelSettings,
                                                        "Admin",
                                                        tint = AppAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                if (chat.publicKey != null) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Default.Lock,
                                                        "Encrypted",
                                                        tint = AppAccent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            
                                            val presenceText = when {
                                                chat.type == ConversationType.CHANNEL -> "${chat.followers} followers"
                                                chat.isGroup -> "group info"
                                                chat.isOnline -> "online"
                                                chat.lastSeen > 0L -> "last seen ${formatWhatsAppTime(chat.lastSeen)}"
                                                else -> "offline"
                                            }
                                            Text(
                                                text = presenceText,
                                                fontSize = 12.sp,
                                                color = if (chat.isOnline) AppAccent else Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Row {
                                        if (chat.type == ConversationType.DIRECT) {
                                            IconButton(onClick = { chatViewModel.initiateCall(chat.id, CallType.VOICE) }) {
                                                Icon(Icons.Default.Call, "Voice Call", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                            IconButton(onClick = { chatViewModel.initiateCall(chat.id, CallType.VIDEO) }) {
                                                Icon(Icons.Default.VideoCall, "Video Call", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                        var showMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(Icons.Default.MoreVert, "More", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                modifier = Modifier.background(AppSecondary)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("View Contact", color = Color.White) },
                                                    onClick = { showMenu = false; onShowProfile(chat.id, chat.isGroup) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Search", color = Color.White) },
                                                    onClick = { showMenu = false; isSearchMode = true }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Mute Notifications", color = Color.White) },
                                                    onClick = { showMenu = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Clear Chat", color = Color.White) },
                                                    onClick = { showMenu = false; chatViewModel.clearChat(chat.id) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete Chat", color = Color.White) },
                                                    onClick = { 
                                                        showMenu = false
                                                        chatViewModel.deleteChat(chat.id)
                                                        onBack()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "${selectedMessages.size}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                                    )
                                    IconButton(onClick = { deleteSelectedMessages() }) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
                                    IconButton(onClick = { /* Forward */ }) { Icon(Icons.Default.Forward, "Forward", tint = Color.White) }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppBackground.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                        if (chat.isTyping) {
                            Text(
                                text = stringResource(R.string.typing),
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 4.dp),
                                color = AppAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        val uiState by chatViewModel.uiState.collectAsState()
                        val myUserId = uiState.currentUser?.id
                        val isConversationClosed = uiState.closedConversationIds.contains(chat.id)
                        val canPost = when (chat.type) {
                            ConversationType.DIRECT, ConversationType.GROUP -> true
                            ConversationType.CHANNEL -> {
                                val isAdm = chat.isAdmin || (chat.createdBy != null && chat.createdBy == myUserId)
                                if (!isAdm) {
                                    android.util.Log.d("ChatMessagesScreen", "Post Denied: chat.isAdmin=${chat.isAdmin}, chat.createdBy=${chat.createdBy}, myUserId=$myUserId")
                                }
                                isAdm
                            }
                        }

                        val canCompose = canPost && !isConversationClosed

                        if (canCompose) {
                            replyingToMessage?.let { replyMsg ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.05f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.width(3.dp).height(32.dp).clip(CircleShape).background(AppAccent))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = if (replyMsg.isFromMe) "You" else chat.senderName, color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = replyMsg.content.ifBlank { replyMsg.type.name }, color = AppTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            ChatBottomBarUnique(
                                text = messageText,
                                onTextChange = { messageText = it },
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        sendToServer(body = messageText.trim(), messageType = "text")
                                        messageText = ""
                                        keyboardController?.hide()
                                    }
                                },
                                onAttachClick = { showAttachments = !showAttachments },
                                onEmojiClick = { showEmojiPicker = !showEmojiPicker },
                                onMicClick = { if (isRecording) stopRecording() else startRecording() },
                                isRecording = isRecording,
                                recordingDuration = recordingDurationSeconds,
                                enterIsSend = enterIsSend
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = if (isConversationClosed) "Conversation closed by employer" else "Only admins can send messages to this channel",
                                    color = AppTextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AppBackground, Color.Black)
                        )
                    )
            ) {
                LazyColumn(
                    state = messagesLazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    if (chat.publicKey != null) {
                        item {
                            EncryptionInfoBubble()
                        }
                    }
                    
                    groupedMessages.forEach { (date, messagesInDay) ->
                        item { ModernDateHeader(date) }
                        items(messagesInDay, key = { it.id }) { message ->
                            val isSelected = selectedMessages.contains(message.id)
                            MessageItemUnique(
                                message = message,
                                isSelected = isSelected,
                                fontSize = fontSize,
                                showReadReceipts = showReadReceipts,
                                onClick = {
                                    if (selectedMessages.isNotEmpty()) {
                                        if (isSelected) selectedMessages.remove(message.id)
                                        else selectedMessages.add(message.id)
                                    } else {
                                        when (message.type) {
                                            MessageType.LOCATION -> {
                                                val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            }
                                            else -> { /* Other clicks like links handled in text */ }
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (selectedMessages.isEmpty()) selectedMessages.add(message.id)
                                },
                                onReply = { replyingToMessage = it },
                                onMediaClick = { url, type ->
                                    com.example.firstapp.common.util.GlobalNavigation.navigateTo(
                                        com.example.firstapp.ui.navigation.Screen.MediaViewer.createRoute(url, type)
                                    )
                                }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // Floating Scroll to Bottom
                val showScrollToBottom by remember { derivedStateOf { messagesLazyListState.firstVisibleItemIndex < messages.size - 5 } }
                
                if (animationsEnabled) {
                    AnimatedVisibility(
                        visible = showScrollToBottom,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp)
                    ) {
                        ScrollToBottomButton(messagesLazyListState, scope, messages.size, true)
                    }
                } else if (showScrollToBottom) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp)) {
                        ScrollToBottomButton(messagesLazyListState, scope, messages.size, false)
                    }
                }
            }
        }

        if (showAttachments) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAttachments = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.padding(bottom = 80.dp)) {
                    AttachmentPanel(
                        onImageClick = { showAttachments = false; imageLauncher.launch("image/*") },
                        onVideoClick = { showAttachments = false; videoLauncher.launch("video/*") },
                        onDocClick = { showAttachments = false; documentLauncher.launch("*/*") },
                        onContactClick = { showAttachments = false; contactLauncher.launch(null) },
                        onCameraClick = {
                            showAttachments = false
                            val file = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        },
                        onAudioClick = { showAttachments = false; audioPickerLauncher.launch("audio/*") },
                        onLocationClick = { 
                            showAttachments = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            chatViewModel.sendMessage(
                                                chatId = chat.id,
                                                body = "Shared Location",
                                                messageType = "location",
                                                latitude = location.latitude,
                                                longitude = location.longitude
                                            )
                                        } else {
                                            showToast("Could not get location")
                                        }
                                    }
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    )
                }
            }
        }

        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showEmojiPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                EmojiPicker(onEmojiSelect = { 
                    messageText += it
                    showEmojiPicker = false 
                })
            }
        }

        if (showForwardDialog) {
            val allChats by chatViewModel.chats.collectAsState()
            ForwardDialog(
                chats = allChats.data ?: emptyList(),
                onDismiss = { showForwardDialog = false },
                onForward = { selectedChats ->
                    messagesToForward.forEach { msg ->
                        selectedChats.forEach { chatItem ->
                            chatViewModel.sendMessage(
                                chatId = chatItem.id,
                                body = msg.content,
                                messageType = msg.type.name.lowercase(),
                                mediaUrl = msg.mediaUri,
                                duration = msg.duration,
                                fileName = msg.fileName,
                                fileSize = msg.fileSize
                            )
                        }
                    }
                    showForwardDialog = false
                    messagesToForward.clear()
                    selectedMessages.clear()
                }
            )
        }

        fullScreenImageUri?.let { uri ->
            FullScreenImagePlayer(uri = uri, onBack = { fullScreenImageUri = null })
        }

        fullScreenVideoUri?.let { uri ->
            FullScreenVideoPlayer(uri = uri, onBack = { fullScreenVideoUri = null })
        }
    }
}

@Composable
fun ScrollToBottomButton(
    state: LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
    messageCount: Int,
    animated: Boolean
) {
    FloatingActionButton(
        onClick = {
            scope.launch {
                if (animated) state.animateScrollToItem((messageCount - 1).coerceAtLeast(0))
                else state.scrollToItem((messageCount - 1).coerceAtLeast(0))
            }
        },
        modifier = Modifier.size(44.dp),
        containerColor = AppSecondary,
        contentColor = AppAccent,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Icon(Icons.Default.KeyboardDoubleArrowDown, null, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun EncryptionInfoBubble() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = Color(0xFFFFF9C4).copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, Color(0xFFFFF9C4).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    tint = Color(0xFFFFF9C4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Messages and calls are end-to-end encrypted. No one outside of this chat, not even FirstApp, can read or listen to them. Tap to learn more.",
                    color = Color(0xFFFFF9C4),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ModernDateHeader(date: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(
                text = date,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ChatBottomBarUnique(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean = false,
    recordingDuration: Int = 0,
    enterIsSend: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isRecording) {
                    Icon(
                        Icons.Default.Mic,
                        null,
                        tint = Color.Red,
                        modifier = Modifier.padding(12.dp).size(20.dp)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%d:%02d", recordingDuration / 60, recordingDuration % 60),
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                } else {
                    IconButton(onClick = onEmojiClick) {
                        Icon(Icons.Default.SentimentSatisfiedAlt, null, tint = AppTextSecondary)
                    }
                    
                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Message...", color = AppTextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AppAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (enterIsSend) ImeAction.Send else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (text.isNotBlank()) onSend() }
                        )
                    )
                    
                    IconButton(onClick = onAttachClick) {
                        Icon(Icons.Default.AttachFile, null, tint = AppTextSecondary, modifier = Modifier.rotate(45f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable { if (text.isNotBlank()) onSend() else onMicClick() },
            shape = CircleShape,
            color = if (text.isNotBlank()) AppAccent else if (isRecording) Color.Red else Color.White.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (text.isNotBlank()) AppBackground else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemUnique(
    message: Message,
    isSelected: Boolean,
    fontSize: Float = 15f,
    showReadReceipts: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReply: (Message) -> Unit,
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromMe) AppPrimary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f)
    val shape = if (message.isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val dragAmount = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, drag ->
                        if (!message.isFromMe && drag > 0) {
                            scope.launch { dragAmount.snapTo((dragAmount.value + drag).coerceIn(0f, 100f)) }
                        } else if (message.isFromMe && drag < 0) {
                            scope.launch { dragAmount.snapTo((dragAmount.value + drag).coerceIn(-100f, 0f)) }
                        }
                    },
                    onDragEnd = {
                        if (Math.abs(dragAmount.value) > 60f) {
                            onReply(message)
                        }
                        scope.launch { dragAmount.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { dragAmount.animateTo(0f) } }
                )
            }
            .background(if (isSelected) AppAccent.copy(alpha = 0.1f) else Color.Transparent)
            .padding(vertical = 4.dp)
    ) {
        // Reply Icon Indicator
        if (dragAmount.value != 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = if (dragAmount.value > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    null,
                    tint = AppAccent.copy(alpha = (Math.abs(dragAmount.value) / 100f).coerceIn(0f, 1f)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(alignment)
                .offset(x = dragAmount.value.dp)
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Surface(
                shape = shape,
                color = bubbleColor,
                border = if (!message.isFromMe) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)) else null,
                shadowElevation = 2.dp
            ) {
                Column {
                    // Reply Preview inside bubble
                    if (message.replyToId != null) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(30.dp).clip(CircleShape).background(AppAccent))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName ?: "Unknown",
                                        color = AppAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = message.replyToText ?: "Message",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Attachment Handling
                    if (message.mediaUri?.isNotBlank() == true || message.type == MessageType.LOCATION || message.type == MessageType.APPLICATION_LINK) {
                        AttachmentPreview(message, onMediaClick = onMediaClick)
                    }

                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        val isMediaTypePlaceholder = message.content == "Voice Note" || 
                                                   message.content == "Image" || 
                                                   message.content == "Video" || 
                                                   message.content == "Shared Location" ||
                                                   message.content == "File"

                        if (message.content.isNotBlank() && (message.type == MessageType.TEXT || !isMediaTypePlaceholder)) {
                            Text(
                                text = message.content,
                                color = Color.White,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.33f).sp
                            )
                        }
                        
                        Row(
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatWhatsAppTime(message.timestamp),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            if (message.isFromMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (message.isRead && showReadReceipts) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = null,
                                    tint = if (message.isRead && showReadReceipts) Color(0xFF34B7F1) else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Reactions
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(if (message.isFromMe) Alignment.End else Alignment.Start),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.entries.forEach { entry ->
                        val emoji = entry.key
                        val count = entry.value
                        Surface(
                            color = AppSecondary,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 12.sp)
                                if (count > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = count.toString(), color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentPreview(
    message: Message,
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val mediaUri = message.mediaUri?.replace("local://", "file://") ?: ""
    
    when (message.type) {
        MessageType.IMAGE -> {
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clickable { onMediaClick(mediaUri, "image") },
                contentScale = ContentScale.Crop
            )
        }
        MessageType.VIDEO -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onMediaClick(mediaUri, "video") },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.duration ?: "0:00",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        MessageType.AUDIO_MESSAGE -> {
            VoiceNotePlayer(uri = mediaUri, duration = message.duration)
        }
        MessageType.DOCUMENT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.fileName ?: "Document",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.fileSize ?: "",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        MessageType.LOCATION -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable {
                        val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = AppAccent, modifier = Modifier.size(40.dp))
                Text("View Location", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${message.latitude}, ${message.longitude}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        MessageType.APPLICATION_LINK -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = AppAccent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Work, null, tint = AppBackground, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Job Application",
                            color = AppAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Profile Summary Attached",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun VoiceNotePlayer(uri: String, duration: String?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPrepared by remember { mutableStateOf(false) }
    val mediaPlayer = remember { android.media.MediaPlayer() }

    DisposableEffect(uri) {
        if (uri.isNotBlank()) {
            val parsedUri = Uri.parse(uri)
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, parsedUri)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    isPrepared = true
                }
                mediaPlayer.setOnCompletionListener { 
                    isPlaying = false 
                    progress = 0f
                    mediaPlayer.seekTo(0)
                }
                mediaPlayer.setOnErrorListener { _, what, extra ->
                    isPrepared = false
                    isPlaying = false
                    true
                }
            } catch (_: Exception) {
                isPrepared = false
            }
        }
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (_: Exception) {}
            try {
                mediaPlayer.reset()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && isPrepared) {
                try {
                    if (mediaPlayer.duration > 0) {
                        progress = mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }

    Row(
        modifier = Modifier
            .width(220.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = AppAccent,
                modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
            onClick = {
                if (!isPrepared) return@IconButton
                try {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                } catch (_: Exception) {}
            }, 
            modifier = Modifier.size(32.dp),
            enabled = isPrepared
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val barHeights = remember { List(15) { (5..15).random().dp } }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(15) { index ->
                        val isPlayed = progress > (index.toFloat() / 15f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeights[index])
                                .clip(CircleShape)
                                .background(if (isPlayed) AppAccent else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            Text(
                text = duration ?: "0:00",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
