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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val activeChat by chatViewModel.activeChat.collectAsState()
    val currentChat = activeChat ?: chat

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
    var showGifPicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    val messagesToForward = remember { mutableStateListOf<Message>() }
    
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    val selectedMessages = remember { mutableStateListOf<String>() }

    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }
    
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var deleteForEveryone by remember { mutableStateOf(false) }

    var showSchedulePicker by remember { mutableStateOf(false) }

    var selectedMediaForPreview by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaPreviewType by remember { mutableStateOf("") }

    BackHandler(enabled = fullScreenImageUri != null || fullScreenVideoUri != null || showAttachments || showEmojiPicker || showGifPicker || selectedMessages.isNotEmpty() || selectedMediaForPreview.isNotEmpty()) {
        when {
            fullScreenImageUri != null -> fullScreenImageUri = null
            fullScreenVideoUri != null -> fullScreenVideoUri = null
            showAttachments -> showAttachments = false
            showEmojiPicker -> showEmojiPicker = false
            showGifPicker -> showGifPicker = false
            selectedMessages.isNotEmpty() -> selectedMessages.clear()
            selectedMediaForPreview.isNotEmpty() -> selectedMediaForPreview = emptyList()
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
        fileSize: String? = null,
        scheduledAt: Long? = null
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
            replyToSenderName = if (replyingToMessage?.isFromMe == true) "You" else (replyingToMessage?.senderName ?: chat.senderName),
            scheduledAt = scheduledAt
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

    fun sendMediaMessages(uris: List<Uri>, type: String, caption: String? = null) {
        scope.launch {
            val files = uris.mapNotNull { MediaUtils.getFileFromUri(context, it) }
            if (files.isNotEmpty()) {
                chatViewModel.sendMediaMessages(chat.id, files, type, caption)
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> 
        if (uris.isNotEmpty()) {
            selectedMediaForPreview = uris
            mediaPreviewType = "image"
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            selectedMediaForPreview = listOf(it)
            mediaPreviewType = "video"
        } 
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            selectedMediaForPreview = listOf(it)
            mediaPreviewType = "document"
        } 
    }
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            selectedMediaForPreview = listOf(it)
            mediaPreviewType = "audio"
        } 
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> 
        if (success) {
            tempPhotoUri?.let { 
                selectedMediaForPreview = listOf(it)
                mediaPreviewType = "image"
            } 
        }
    }
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
    val capturedAmplitudes = remember { mutableStateListOf<Int>() }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var reviewingVoiceNoteUri by remember { mutableStateOf<Uri?>(null) }
    
    // Dedicated MediaPlayer for Voice Note Review
    val reviewMediaPlayer = remember { android.media.MediaPlayer() }
    var isPlayingReview by remember { mutableStateOf(false) }
    
    DisposableEffect(reviewingVoiceNoteUri) {
        if (reviewingVoiceNoteUri != null) {
            try {
                reviewMediaPlayer.reset()
                reviewMediaPlayer.setDataSource(context, reviewingVoiceNoteUri!!)
                reviewMediaPlayer.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                reviewMediaPlayer.prepareAsync()
                reviewMediaPlayer.setOnPreparedListener { 
                    android.util.Log.d("ChatScreen", "Review player ready")
                }
                reviewMediaPlayer.setOnCompletionListener { 
                    isPlayingReview = false 
                    reviewMediaPlayer.seekTo(0)
                }
                reviewMediaPlayer.setOnErrorListener { _, what, extra ->
                    android.util.Log.e("ChatScreen", "Review player error: $what, $extra")
                    isPlayingReview = false
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to setup review player", e)
            }
        }
        onDispose {
            try {
                if (reviewMediaPlayer.isPlaying) reviewMediaPlayer.stop()
                reviewMediaPlayer.reset()
            } catch (_: Exception) {}
            isPlayingReview = false
        }
    }

    LaunchedEffect(isPlayingReview) {
        if (isPlayingReview) {
            try { reviewMediaPlayer.start() } catch (_: Exception) { isPlayingReview = false }
        } else {
            try { if (reviewMediaPlayer.isPlaying) reviewMediaPlayer.pause() } catch (_: Exception) {}
        }
    }

    var recordingStartMs = 0L
    var recordedDurationMs = 0L
    var recorder: MediaRecorder? = null
    var audioFile: File? = null

    fun showToast(msg: String) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    fun onReact(messageId: String, emoji: String) { chatViewModel.reactToMessage(messageId, emoji) }
    fun deleteSelectedMessages() { 
        val canDeleteForEveryone = selectedMessages.size == 1 && (messages.find { it.id == selectedMessages.first() }?.isFromMe == true)
        if (canDeleteForEveryone) {
            showDeleteConfirmDialog = selectedMessages.first()
        } else {
            selectedMessages.forEach { chatViewModel.deleteMessage(it) }
            selectedMessages.clear()
        }
    }

    fun startRecording() {
        try {
            capturedAmplitudes.clear()
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
            scope.launch { 
                while(isRecording) { 
                    delay(100)
                    val amp = try { recorder?.maxAmplitude ?: 0 } catch(_: Exception) { 0 }
                    // Normalize amplitude to 0..100 range
                    val normalized = (amp.toFloat() / 32767f * 100f).toInt().coerceIn(1, 100)
                    capturedAmplitudes.add(normalized)
                    
                    if (capturedAmplitudes.size % 10 == 0) {
                        recordingDurationSeconds++
                    }
                } 
            }
        } catch (e: Exception) { e.printStackTrace(); showToast("Recording failed") }
    }
    fun stopRecording(cancelled: Boolean = false) {
        try {
            recordedDurationMs = System.currentTimeMillis() - recordingStartMs
            recorder?.apply { stop(); release() }; recorder = null; isRecording = false
            if (!cancelled) {
                audioFile?.let { reviewingVoiceNoteUri = Uri.fromFile(it) }
            } else { audioFile?.delete(); showToast("Recording cancelled") }
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun sendVoiceNote() {
        val uri = reviewingVoiceNoteUri ?: return
        val seconds = (recordedDurationMs / 1000L).coerceAtLeast(1L)
        val durationLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
        
        // Pass captured amplitudes
        val amps = capturedAmplitudes.toList()
        
        scope.launch {
            val file = MediaUtils.getFileFromUri(context, uri)
            if (file != null) {
                chatViewModel.sendMediaMessage(chat.id, file, "audio_message", "Voice Note", durationLabel, amps)
            }
        }
        
        reviewingVoiceNoteUri = null
    }

    fun discardVoiceNote() {
        reviewingVoiceNoteUri?.path?.let { File(it).delete() }
        reviewingVoiceNoteUri = null
    }

    val chatSearchResults by chatViewModel.chatSearchResults.collectAsState()
    val playingAudioUri by chatViewModel.currentlyPlayingAudio.collectAsState()
    var isSearchMode by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var activeSearchId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chatSearchQuery) { 
        if (isSearchMode) {
            chatViewModel.searchInChat(chat.id, chatSearchQuery)
            activeSearchId = null
        }
    }

    LaunchedEffect(chatSearchResults, activeSearchId) {
        if (isSearchMode && chatSearchResults.isNotEmpty()) {
            val targetId = activeSearchId ?: chatSearchResults.firstOrNull()?.id
            if (targetId != null) {
                // Find index in full messages list
                val idx = messages.indexOfFirst { it.id == targetId }
                if (idx != -1) {
                    messagesLazyListState.animateScrollToItem(idx)
                    activeSearchId = targetId
                }
            }
        }
    }

    LaunchedEffect(chat.id) {
        chatViewModel.fetchMessages(chat.id)
        chatViewModel.markAsRead(chat.id)
    }

    var systemMessageSent by remember { mutableStateOf(false) }
    LaunchedEffect(messagesResource, jobTitle, companyName) {
        if (!systemMessageSent && messagesResource is Resource.Success<List<Message>> && (messagesResource.data?.isEmpty() == true) && jobTitle != null && companyName != null) {
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

    val sanitizedName = remember(currentChat.senderName) {
        val cachedName = GlobalCallState.getContactName(currentChat.id)
        val name = currentChat.senderName.takeIf { 
            it.isNotBlank() && 
            it.lowercase() != "unknown" && 
            it.lowercase() != "null" && 
            it.lowercase() != "undefined" &&
            !it.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
        } ?: cachedName
        
        name ?: if (currentChat.type == ConversationType.CHANNEL) "Channel" else currentChat.id
    }

    if (showSchedulePicker) {
        ScheduleMessageDialog(
            onDismiss = { showSchedulePicker = false },
            onSchedule = { scheduledTime ->
                if (messageText.isNotBlank()) {
                    sendToServer(body = messageText.trim(), messageType = "text", scheduledAt = scheduledTime)
                    messageText = ""
                    showSchedulePicker = false
                    showToast("Message scheduled")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    ChatTopBar(
                        chat = currentChat,
                        sanitizedName = sanitizedName,
                        isSearchMode = isSearchMode,
                        chatSearchQuery = chatSearchQuery,
                        selectedMessagesCount = selectedMessages.size,
                        isTyping = uiState.typingStatus[currentChat.id] ?: false,
                        onBack = { if (selectedMessages.isNotEmpty()) selectedMessages.clear() else onBack() },
                        onSearchQueryChange = { chatSearchQuery = it },
                        onSearchModeChange = { isSearchMode = it },
                        onSearchNavigate = { forward ->
                            if (chatSearchResults.isNotEmpty()) {
                                val currentIdx = chatSearchResults.indexOfFirst { it.id == activeSearchId }.coerceAtLeast(0)
                                val nextIdx = if (forward) {
                                    (currentIdx + 1) % chatSearchResults.size
                                } else {
                                    if (currentIdx <= 0) chatSearchResults.size - 1 else currentIdx - 1
                                }
                                activeSearchId = chatSearchResults[nextIdx].id
                            }
                        },
                        activeSearchIndex = chatSearchResults.indexOfFirst { it.id == activeSearchId }.coerceAtLeast(0),
                        totalSearchCount = chatSearchResults.size,
                        onClearSearch = { chatSearchQuery = "" },
                        onShowProfile = onShowProfile,
                        onAvatarClick = onAvatarClick,
                        onInitiateCall = { targetId, name, type -> chatViewModel.startVoiceOrVideoCall(receiverId = targetId, callType = type, name = name) },
                        onClearChat = { chatViewModel.clearChat(currentChat.id) },
                        onDeleteChat = { chatViewModel.deleteChat(currentChat.id); onBack() },
                        onDeleteSelectedMessages = { deleteSelectedMessages() },
                        onShowMessageInfo = {
                            val msgId = selectedMessages.firstOrNull()
                            if (msgId != null) {
                                com.example.firstapp.common.util.GlobalNavigation.navigateTo(
                                    com.example.firstapp.ui.navigation.Screen.MessageInfo.createRoute(msgId)
                                )
                                selectedMessages.clear()
                            }
                        },
                        onStarSelectedMessages = {
                            selectedMessages.forEach { id ->
                                val msg = messages.find { it.id == id }
                                val newStarredState = !(msg?.isStarred ?: false)
                                chatViewModel.starMessage(id, newStarredState)
                            }
                            selectedMessages.clear()
                        },
                        onEditSelectedMessage = {
                            val msgId = selectedMessages.firstOrNull()
                            val msg = messages.find { it.id == msgId }
                            if (msg != null && msg.isFromMe && msg.type == MessageType.TEXT) {
                                editingMessage = msg
                                messageText = msg.content
                                selectedMessages.clear()
                            }
                        },
                        onForwardSelectedMessages = {
                            messagesToForward.clear()
                            messagesToForward.addAll(messages.filter { selectedMessages.contains(it.id) })
                            showForwardDialog = true
                        },
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
                        
                        // Banner Ad (at the bottom of Jobs)
                        if (jobId != null) {
                            com.example.firstapp.common.ui.components.BannerAdView(
                                modifier = Modifier.navigationBarsPadding()
                            )
                        }

                        if (currentChat.isTyping) {
                            Text(
                                text = stringResource(R.string.typing),
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        val myUserId = uiState.currentUser?.id
                        val isConversationClosed = uiState.closedConversationIds.contains(currentChat.id)
                        
                        // Robust check for channel posting permissions
                        val canPost = when (currentChat.type) {
                            ConversationType.DIRECT, ConversationType.GROUP, ConversationType.JOB_CHAT -> true
                            ConversationType.CHANNEL -> {
                                val isCreator = currentChat.createdBy != null && currentChat.createdBy == myUserId
                                // Expert: If creator ID is missing but user is admin, assume posting permission in their own view
                                isCreator || (currentChat.isAdmin && (currentChat.adminsCanPost || currentChat.createdBy == null))
                            }
                        }

                        val canCompose = canPost && !isConversationClosed
                        val showFollowButton = currentChat.type == ConversationType.CHANNEL && !currentChat.isSubscribed

                        if (canCompose) {
                            val editMsg = editingMessage
                            if (editMsg != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Editing message", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = editMsg.content, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { 
                                            editingMessage = null
                                            messageText = ""
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

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
                                        
                                        if (editingMessage != null) {
                                            chatViewModel.editMessage(editingMessage!!.id, textToSend)
                                            editingMessage = null
                                        } else {
                                            sendToServer(body = textToSend, messageType = "text")
                                        }
                                        
                                        messageText = ""
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
                                    showGifPicker = false
                                },
                                onEmojiClick = { 
                                    keyboardController?.hide()
                                    showEmojiPicker = !showEmojiPicker 
                                    showAttachments = false
                                    showGifPicker = false
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
                                isReviewingVoiceNote = reviewingVoiceNoteUri != null,
                                onSendVoiceNote = { sendVoiceNote() },
                                onDiscardVoiceNote = { discardVoiceNote() },
                                onTogglePlayReview = { isPlayingReview = !isPlayingReview },
                                isPlayingReview = isPlayingReview,
                                modifier = Modifier.navigationBarsPadding(),
                                onSendLongClick = { showSchedulePicker = true }
                            )
                        } else if (!isConversationClosed) {
                            if (showFollowButton) {
                                var isSubscribing by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { 
                                        isSubscribing = true
                                        chatViewModel.subscribeToChannel(currentChat.id) 
                                    },
                                    enabled = !isSubscribing,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (isSubscribing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Follow channel")
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Subscriber View: Production-grade controls (Mute/Unmute and Share)
                                        val isMuted = currentChat.isMuted
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { chatViewModel.muteChat(currentChat.id, !isMuted) }.padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (isMuted) "Unmute" else "Mute",
                                                fontSize = 11.sp,
                                                color = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { 
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Check out this channel: ${currentChat.senderName}")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                            }.padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Share",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { onShowProfile(currentChat.id, false) }.padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Info",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
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
                chat = currentChat,
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
                },
                playingAudioUri = playingAudioUri,
                onPlayAudio = { chatViewModel.setPlayingAudio(it) },
                highlightId = activeSearchId
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
                        },
                        onGifClick = { 
                            showAttachments = false
                            showGifPicker = true 
                        }
                    )
                }
            }
        }

        if (showGifPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showGifPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                com.example.firstapp.chat.ui.components.expression.GifStickerPicker(
                    onGifSelected = { url ->
                        chatViewModel.sendMessage(chat.id, "GIF", "image", listOf(url))
                    },
                    onStickerSelected = { url ->
                        chatViewModel.sendMessage(chat.id, "Sticker", "image", listOf(url))
                    },
                    onDismiss = { showGifPicker = false }
                )
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

        if (showDeleteConfirmDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Delete Message") },
                text = {
                    Column {
                        Text("Delete this message?")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp).clickable { deleteForEveryone = !deleteForEveryone }
                        ) {
                            Checkbox(checked = deleteForEveryone, onCheckedChange = { deleteForEveryone = it })
                            Text("Delete for everyone", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog?.let { chatViewModel.deleteMessage(it, deleteForEveryone) }
                        showDeleteConfirmDialog = null
                        selectedMessages.clear()
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (selectedMediaForPreview.isNotEmpty()) {
            MediaPreviewOverlay(
                uris = selectedMediaForPreview,
                type = mediaPreviewType,
                onDismiss = { selectedMediaForPreview = emptyList() },
                onSend = { caption, uris ->
                    sendMediaMessages(uris, mediaPreviewType, caption)
                    selectedMediaForPreview = emptyList()
                }
            )
        }
    }
}

@Composable
fun MediaPreviewOverlay(
    uris: List<Uri>,
    type: String,
    onDismiss: () -> Unit,
    onSend: (String, List<Uri>) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (uris.size > 1) "${uris.size} ${type.replaceFirstChar { it.uppercase() }}s" else "Preview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Media Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uris.size > 1) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(16.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uris) { uri ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (type == "video") {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(32.dp)
                                    )
                                } else if (type == "document") {
                                    Icon(
                                        Icons.Default.Description,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(32.dp)
                                    )
                                } else if (type == "audio") {
                                    Icon(
                                        Icons.Default.Audiotrack,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val uri = uris.first()
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (type == "image") {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else if (type == "video") {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Icon(
                                    Icons.Default.PlayCircle,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        } else {
                            // Document or Audio
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (type == "document") Icons.Default.Description else Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = AppAccent,
                                    modifier = Modifier.size(100.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = uri.lastPathSegment ?: "File",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar with Caption
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = caption,
                            onValueChange = { caption = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a caption...", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AppAccent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        FloatingActionButton(
                            onClick = { onSend(caption, uris) },
                            containerColor = AppAccent,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMessageDialog(
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit
) {
    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // Tomorrow
    )
    var timePickerState = rememberTimePickerState(
        initialHour = 12,
        initialMinute = 0
    )
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showTimePicker) "Select Time" else "Schedule Message") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!showTimePicker) {
                    DatePicker(state = datePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!showTimePicker) {
                        showTimePicker = true
                    } else {
                        val calendar = Calendar.getInstance()
                        datePickerState.selectedDateMillis?.let {
                            calendar.timeInMillis = it
                        }
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        onSchedule(calendar.timeInMillis)
                    }
                }
            ) {
                Text(if (showTimePicker) "Schedule" else "Next")
            }
        },
        dismissButton = {
            TextButton(onClick = if (showTimePicker) { { showTimePicker = false } } else onDismiss) {
                Text(if (showTimePicker) "Back" else "Cancel")
            }
        }
    )
}
