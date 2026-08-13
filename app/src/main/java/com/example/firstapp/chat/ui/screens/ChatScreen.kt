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
import kotlin.collections.emptyList
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
import kotlin.collections.*
import kotlin.text.*
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
import com.example.firstapp.chat.ui.components.MessageItemUnique
import com.example.firstapp.chat.ui.components.EncryptionInfoBubble
import com.example.firstapp.chat.ui.components.ModernDateHeader
import com.example.firstapp.chat.ui.components.ChatBottomBarUnique
import com.example.firstapp.chat.ui.components.ScrollToBottomButton
import com.example.firstapp.chat.ui.components.ChatTopBar
import com.example.firstapp.chat.ui.components.ChatMessageList

@Composable
fun JobInfoHeader(title: String, company: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BusinessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text = company, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatMessagesScreen(
    chat: ChatMessage,
    chatViewModel: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onShowProfile: (String, Boolean) -> Unit,
    onAvatarClick: (String) -> Unit = {},
    jobId: String? = null,
    jobTitle: String? = null,
    companyName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    
    val uiState by chatViewModel.uiState.collectAsState()
    val fontSize by chatViewModel.fontSize.collectAsState()
    val enterIsSend by chatViewModel.enterIsSend.collectAsState()
    val animationsEnabled by chatViewModel.animationsEnabled.collectAsState()
    val showReadReceipts by chatViewModel.showReadReceipts.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val messagesResource by chatViewModel.messages.collectAsState()
    val messages = messagesResource.data ?: kotlin.collections.emptyList()
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
    
    fun sendToServer(
        body: String, 
        messageType: String = "text", 
        mediaUrls: List<String> = emptyList(), 
        duration: String? = null, 
        fileName: String? = null, 
        fileSize: String? = null
    ) {
        chatViewModel.sendMessage(
            chatId = chat.id, 
            body = body, 
            messageType = messageType, 
            mediaUrls = mediaUrls, 
            duration = duration, 
            fileName = fileName, 
            fileSize = fileSize, 
            replyToId = replyingToMessage?.id,
            replyToText = replyingToMessage?.content,
            replyToSenderName = if (replyingToMessage?.isFromMe == true) "You" else (replyingToMessage?.senderName ?: chat.senderName)
        )
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
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let {
            try {
                val projection = arrayOf(
                    android.provider.ContactsContract.Contacts.DISPLAY_NAME,
                    android.provider.ContactsContract.Contacts._ID
                )
                val cursor = context.contentResolver.query(it, projection, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val name = c.getString(0)
                        val id = c.getString(1)
                        
                        // Now get phone number
                        val phoneCursor = context.contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phone = pc.getString(0)
                                chatViewModel.sendMessage(
                                    chatId = chat.id,
                                    body = "Shared Contact",
                                    messageType = "contact",
                                    contactName = name,
                                    contactPhone = phone
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
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

    var systemMessageSent by remember { mutableStateOf(false) }
    LaunchedEffect(messagesResource, jobTitle, companyName) {
        if (!systemMessageSent && messagesResource is Resource.Success && (messagesResource.data?.isEmpty() == true) && jobTitle != null && companyName != null) {
            systemMessageSent = true
            val systemMessage = "Hi, I would like to apply for the position of $jobTitle at $companyName. Attached is my profile summary."
            sendToServer(body = systemMessage, messageType = "application_link")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Mark as read if we are on the screen
            chatViewModel.markAsRead(chat.id)

            val lastMsg = messages[messages.size - 1]
            val isAtBottom = messagesLazyListState.firstVisibleItemIndex >= (messages.size - 8).coerceAtLeast(0)
            
            // Force scroll to bottom if it's our own message or we are already at the bottom
            if (lastMessageCount == 0 || lastMsg.isFromMe || isAtBottom) {
                delay(100)
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
                chatViewModel.sendTyping(true)
                isTypingSent = true
            }
            delay(3000)
            if (isTypingSent) {
                chatViewModel.sendTyping(false)
                isTypingSent = false
            }
        } else {
            if (isTypingSent) {
                chatViewModel.sendTyping(false)
                isTypingSent = false
            }
        }
    }

    DisposableEffect(chat.id) {
        onDispose {
            if (isTypingSent) {
                chatViewModel.sendTyping(false)
            }
        }
    }

    val groupedMessages: Map<String, List<Message>> = remember(messages, chatSearchQuery, chatSearchResults) {
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
            it.lowercase() != "unknown" && 
            it.lowercase() != "null" && 
            it.lowercase() != "undefined" 
        } ?: cachedName ?: chat.id
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    ChatTopBar(
                        chat = chat,
                        sanitizedName = sanitizedName,
                        isSearchMode = isSearchMode,
                        chatSearchQuery = chatSearchQuery,
                        selectedMessagesCount = selectedMessages.size,
                        isTyping = uiState.typingStatus[chat.id] ?: false,
                        onBack = { if (selectedMessages.isNotEmpty()) selectedMessages.clear() else onBack() },
                        onSearchQueryChange = { chatSearchQuery = it },
                        onSearchModeChange = { isSearchMode = it },
                        onClearSearch = { chatSearchQuery = "" },
                        onShowProfile = onShowProfile,
                        onAvatarClick = onAvatarClick,
                        onInitiateCall = { targetId, name, type -> chatViewModel.startVoiceOrVideoCall(receiverId = targetId, callType = type, name = name) },
                        onClearChat = { chatViewModel.clearChat(chat.id) },
                        onDeleteChat = { chatViewModel.deleteChat(chat.id); onBack() },
                        onDeleteSelectedMessages = { deleteSelectedMessages() },
                        onForwardSelectedMessages = { /* Forward */ },
                        onClearSelectedMessages = { selectedMessages.clear() }
                    )
                    if (jobTitle != null && companyName != null) {
                        JobInfoHeader(title = jobTitle, company = companyName)
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding().imePadding()) {
                        
                        // Banner Ad (at the bottom of Jobs or Channel chats)
                        if (jobId != null || chat.type == ConversationType.CHANNEL) {
                            com.example.firstapp.common.ui.components.BannerAdView(
                                modifier = Modifier.navigationBarsPadding()
                            )
                        }

                        if (chat.isTyping) {
                            Text(
                                text = stringResource(R.string.typing),
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        val myUserId = uiState.currentUser?.id
                        val isConversationClosed = uiState.closedConversationIds.contains(chat.id)
                        val canPost = when (chat.type) {
                            ConversationType.DIRECT, ConversationType.GROUP, ConversationType.JOB_CHAT -> true
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
                            val replyMsg = replyingToMessage
                            if (replyMsg != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.width(3.dp).height(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = if (replyMsg.isFromMe) "You" else chat.senderName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = replyMsg.content, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            ChatBottomBarUnique(
                                text = messageText,
                                onTextChange = { 
                                    messageText = it 
                                    chatViewModel.sendTyping(it.isNotBlank())
                                },
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        chatViewModel.sendTyping(false)
                                        val textToSend = messageText.trim()
                                        messageText = ""
                                        sendToServer(body = textToSend, messageType = "text")
                                        keyboardController?.hide()
                                        
                                        // Standard pro app behavior: immediate scroll to bottom on send
                                        scope.launch {
                                            delay(100)
                                            if (messages.isNotEmpty()) {
                                                messagesLazyListState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    }
                                },
                                onAttachClick = { 
                                    keyboardController?.hide()
                                    showAttachments = !showAttachments 
                                    showEmojiPicker = false
                                },
                                onEmojiClick = { 
                                    keyboardController?.hide()
                                    showEmojiPicker = !showEmojiPicker 
                                    showAttachments = false
                                },
                                onCameraClick = {
                                    val file = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    tempPhotoUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                onMicClick = { if (isRecording) stopRecording() else startRecording() },
                                isRecording = isRecording,
                                recordingDuration = recordingDurationSeconds,
                                enterIsSend = enterIsSend,
                                modifier = Modifier.navigationBarsPadding()
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
            ChatMessageList(
                modifier = Modifier.padding(padding),
                messagesLazyListState = messagesLazyListState,
                groupedMessages = groupedMessages,
                messages = messages, // Pass the flat list too
                selectedMessages = selectedMessages,
                chat = chat,
                fontSize = fontSize,
                showReadReceipts = showReadReceipts,
                animationsEnabled = animationsEnabled,
                scope = scope,
                messagesCount = messages.size,
                onMessageClick = { message ->
                    if (selectedMessages.isNotEmpty()) {
                        if (selectedMessages.contains(message.id)) selectedMessages.remove(message.id)
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
                onMessageLongClick = { message ->
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
                chats = allChats.data ?: kotlin.collections.emptyList(),
                onDismiss = { showForwardDialog = false },
                onForward = { selectedChats ->
                    messagesToForward.forEach { msg ->
                        selectedChats.forEach { chatItem ->
                            chatViewModel.sendMessage(
                                chatId = chatItem.id,
                                body = msg.content,
                                messageType = msg.type.name.lowercase(),
                                mediaUrls = msg.mediaUrls,
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
