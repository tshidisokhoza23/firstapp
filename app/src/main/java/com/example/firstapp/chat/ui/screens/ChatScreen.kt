@file:Suppress("DEPRECATION")
@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.firstapp.chat.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.firstapp.R
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.chat.ui.components.*
import com.example.firstapp.chat.ui.components.expression.GifStickerPicker
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.common.util.GlobalNavigation
import com.example.firstapp.common.util.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BusinessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = company,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by chatViewModel.uiState.collectAsState()
    val activeChat by chatViewModel.activeChat.collectAsState()
    val currentChat = activeChat ?: chat

    val fontSize by chatViewModel.fontSize.collectAsState()
    val enterIsSend by chatViewModel.enterIsSend.collectAsState()
    val animationsEnabled by chatViewModel.animationsEnabled.collectAsState()
    val showReadReceipts by chatViewModel.showReadReceipts.collectAsState()

    var messageText by remember(currentChat.id) { mutableStateOf("") }
    val messagesResource by chatViewModel.messages.collectAsState()
    val messages = messagesResource.data.orEmpty()
    val messagesLazyListState = rememberLazyListState()

    var showAttachments by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    val messagesToForward = remember { mutableStateListOf<Message>() }

    var replyingToMessage by remember(currentChat.id) { mutableStateOf<Message?>(null) }
    var editingMessage by remember(currentChat.id) { mutableStateOf<Message?>(null) }
    val selectedMessages = remember(currentChat.id) { mutableStateListOf<String>() }

    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var deleteForEveryone by remember { mutableStateOf(false) }
    var showSchedulePicker by remember { mutableStateOf(false) }

    var selectedMediaForPreview by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaPreviewType by remember { mutableStateOf("") }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Search Mode State
    val chatSearchResults by chatViewModel.chatSearchResults.collectAsState()
    val playingAudioUri by chatViewModel.currentlyPlayingAudio.collectAsState()
    var isSearchMode by remember(currentChat.id) { mutableStateOf(false) }
    var chatSearchQuery by remember(currentChat.id) { mutableStateOf("") }
    var activeSearchId by remember(currentChat.id) { mutableStateOf<String?>(null) }

    // Audio Recording State
    var isRecording by remember { mutableStateOf(false) }
    val capturedAmplitudes = remember { mutableStateListOf<Int>() }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var reviewingVoiceNoteUri by remember { mutableStateOf<Uri?>(null) }
    val reviewMediaPlayer = remember { MediaPlayer() }
    var isPlayingReview by remember { mutableStateOf(false) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }
    var recordedDurationMs by remember { mutableLongStateOf(0L) }

    BackHandler(
        enabled = fullScreenImageUri != null ||
            fullScreenVideoUri != null ||
            showAttachments ||
            showEmojiPicker ||
            showGifPicker ||
            selectedMessages.isNotEmpty() ||
            selectedMediaForPreview.isNotEmpty() ||
            reviewingVoiceNoteUri != null
    ) {
        when {
            fullScreenImageUri != null -> fullScreenImageUri = null
            fullScreenVideoUri != null -> fullScreenVideoUri = null
            showAttachments -> showAttachments = false
            showEmojiPicker -> showEmojiPicker = false
            showGifPicker -> showGifPicker = false
            selectedMessages.isNotEmpty() -> selectedMessages.clear()
            selectedMediaForPreview.isNotEmpty() -> selectedMediaForPreview = emptyList()
            reviewingVoiceNoteUri != null -> {
                reviewingVoiceNoteUri?.path?.let { File(it).delete() }
                reviewingVoiceNoteUri = null
            }
        }
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun sendToServer(
        body: String,
        messageType: String = "text",
        mediaUrls: List<String> = emptyList(),
        duration: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        scheduledAt: Long? = null
    ) {
        chatViewModel.sendMessage(
            chatId = currentChat.id,
            body = body,
            messageType = messageType,
            mediaUrls = mediaUrls,
            duration = duration,
            fileName = fileName,
            fileSize = fileSize,
            contactName = contactName,
            contactPhone = contactPhone,
            latitude = latitude,
            longitude = longitude,
            replyToId = replyingToMessage?.id,
            replyToText = replyingToMessage?.content,
            replyToSenderName = if (replyingToMessage?.isFromMe == true) "You" else (replyingToMessage?.senderName ?: currentChat.senderName),
            scheduledAt = scheduledAt
        )
        replyingToMessage = null
    }

    fun stopRecording(cancelled: Boolean = false) {
        try {
            recordingJob?.cancel()
            recordingJob = null
            recordedDurationMs = System.currentTimeMillis() - recordingStartMs
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            if (!cancelled) {
                audioFile?.let { reviewingVoiceNoteUri = Uri.fromFile(it) }
            } else {
                audioFile?.delete()
                showToast("Recording cancelled")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    fun startRecording() {
        try {
            capturedAmplitudes.clear()
            val file = File(context.cacheDir, "VOICE_${System.currentTimeMillis()}.m4a")
            audioFile = file
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            isRecording = true
            recordingStartMs = System.currentTimeMillis()
            recordingDurationSeconds = 0

            recordingJob = scope.launch {
                while (isActive && isRecording) {
                    delay(100)
                    val amp = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                    val normalized = (amp.toFloat() / 32767f * 100f).toInt().coerceIn(1, 100)
                    capturedAmplitudes.add(normalized)

                    if (capturedAmplitudes.size % 10 == 0) {
                        recordingDurationSeconds++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            showToast("Recording initialization failed")
        }
    }

    fun sendVoiceNote() {
        val uri = reviewingVoiceNoteUri ?: return
        val seconds = (recordedDurationMs / 1000L).coerceAtLeast(1L)
        val durationLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
        val amps = capturedAmplitudes.toList()

        uri.path?.let { path ->
            chatViewModel.sendMediaMessage(
                currentChat.id,
                File(path),
                "audio_message",
                "Voice Note",
                durationLabel,
                amps
            )
        }
        reviewingVoiceNoteUri = null
    }

    fun discardVoiceNote() {
        reviewingVoiceNoteUri?.path?.let { File(it).delete() }
        reviewingVoiceNoteUri = null
    }

    // Audio Review MediaPlayer Lifecycle
    DisposableEffect(reviewingVoiceNoteUri) {
        if (reviewingVoiceNoteUri != null) {
            try {
                reviewMediaPlayer.reset()
                reviewMediaPlayer.setDataSource(context, reviewingVoiceNoteUri!!)
                reviewMediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                reviewMediaPlayer.prepareAsync()
                reviewMediaPlayer.setOnCompletionListener {
                    isPlayingReview = false
                    reviewMediaPlayer.seekTo(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    // Activity Result Launchers
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
                context.contentResolver.query(it, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        val id = cursor.getString(1)

                        context.contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val phone = phoneCursor.getString(0)
                                sendToServer(
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) showToast("Permission denied")
    }

    // Initial Fetch & Typing Sync
    LaunchedEffect(currentChat.id) {
        chatViewModel.fetchMessages(currentChat.id)
        chatViewModel.markAsRead(currentChat.id)
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
        } else if (isTypingSent) {
            chatViewModel.sendTyping(false)
            isTypingSent = false
        }
    }

    DisposableEffect(currentChat.id) {
        onDispose {
            if (isTypingSent) chatViewModel.sendTyping(false)
            if (isRecording) stopRecording(cancelled = true)
        }
    }

    // Dynamic Auto-scroll on Inbound and Outbound Messages
    LaunchedEffect(messages.lastOrNull()?.id, messages.lastOrNull()?.status) {
        if (messages.isNotEmpty()) {
            chatViewModel.markAsRead(currentChat.id)
            val totalIndex = (messagesLazyListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            if (animationsEnabled) {
                messagesLazyListState.animateScrollToItem(totalIndex)
            } else {
                messagesLazyListState.scrollToItem(totalIndex)
            }
        }
    }

    // Load Older Messages on Scroll Up
    val shouldLoadOlder by remember {
        derivedStateOf {
            messages.isNotEmpty() &&
                messagesLazyListState.firstVisibleItemIndex <= 2 &&
                messagesLazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) {
            messages.minOfOrNull { it.timestamp }?.let { oldestTs ->
                chatViewModel.loadMoreMessages(currentChat.id, oldestTs)
            }
        }
    }

    // Grouping Messages by Calendar Day
    val groupedMessages = remember(messages, chatSearchQuery, chatSearchResults) {
        val filtered = if (chatSearchQuery.isBlank()) messages else chatSearchResults
        filtered.groupBy { msg ->
            val date = Date(msg.timestamp)
            val cal = Calendar.getInstance().apply { time = date }
            val now = Calendar.getInstance()
            val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            when {
                isToday -> "Today"
                isYesterday -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    val sanitizedName = remember(currentChat.senderName) {
        val cachedName = GlobalCallState.getContactName(currentChat.id)
        val name = currentChat.senderName.takeIf {
            it.isNotBlank() &&
                it.lowercase() != "unknown" &&
                it.lowercase() != "null" &&
                !it.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
        } ?: cachedName
        name ?: currentChat.phoneNumber?.takeIf { it.isNotBlank() } ?: if (currentChat.type == ConversationType.CHANNEL) "Channel" else ""
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
                        isTyping = (uiState.typingStatus[currentChat.id] ?: uiState.typingStatus[currentChat.otherUserId ?: ""]) ?: currentChat.isTyping,
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
                        onDeleteSelectedMessages = {
                            val canDeleteForEveryone = selectedMessages.size == 1 && (messages.find { it.id == selectedMessages.first() }?.isFromMe == true)
                            if (canDeleteForEveryone) {
                                showDeleteConfirmDialog = selectedMessages.first()
                            } else {
                                selectedMessages.forEach { chatViewModel.deleteMessage(it) }
                                selectedMessages.clear()
                            }
                        },
                        onShowMessageInfo = {
                            selectedMessages.firstOrNull()?.let { msgId ->
                                GlobalNavigation.navigateTo("message_info/$msgId")
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
                            selectedMessages.firstOrNull()?.let { msgId ->
                                messages.find { it.id == msgId }?.let { msg ->
                                    if (msg.isFromMe && msg.type == MessageType.TEXT) {
                                        editingMessage = msg
                                        messageText = msg.content
                                        selectedMessages.clear()
                                    }
                                }
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
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        val myUserId = uiState.currentUser?.id
                        val isConversationClosed = uiState.closedConversationIds.contains(currentChat.id)
                        val canPost = when (currentChat.type) {
                            ConversationType.DIRECT, ConversationType.GROUP, ConversationType.JOB_CHAT -> true
                            ConversationType.CHANNEL -> {
                                val isCreator = currentChat.createdBy != null && currentChat.createdBy == myUserId
                                isCreator || (currentChat.isAdmin && (currentChat.adminsCanPost || currentChat.createdBy == null))
                            }
                        }

                        val canCompose = canPost && !isConversationClosed
                        val showFollowButton = currentChat.type == ConversationType.CHANNEL && !currentChat.isSubscribed

                        if (canCompose) {
                            editingMessage?.let { editMsg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Editing message",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = editMsg.content,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                editingMessage = null
                                                messageText = ""
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            replyingToMessage?.let { replyMsg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (replyMsg.isFromMe) "You" else currentChat.senderName,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = replyMsg.content,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { replyingToMessage = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
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
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    tempPhotoUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                onMicClick = {
                                    if (isRecording) stopRecording() else startRecording()
                                },
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
                        } else if (!isConversationClosed && showFollowButton) {
                            var isSubscribing by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    isSubscribing = true
                                    chatViewModel.subscribeToChannel(currentChat.id)
                                },
                                enabled = !isSubscribing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isSubscribing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Follow channel")
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
                messages = messages,
                selectedMessages = selectedMessages,
                chat = currentChat,
                fontSize = fontSize,
                showReadReceipts = showReadReceipts,
                animationsEnabled = animationsEnabled,
                scope = scope,
                messagesCount = messages.size,
                onMessageClick = { message ->
                    if (selectedMessages.isNotEmpty()) {
                        if (selectedMessages.contains(message.id)) {
                            selectedMessages.remove(message.id)
                        } else {
                            selectedMessages.add(message.id)
                        }
                    } else if (message.type == MessageType.LOCATION) {
                        val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        context.startActivity(mapIntent)
                    }
                },
                onMessageLongClick = { message ->
                    if (selectedMessages.isEmpty()) selectedMessages.add(message.id)
                },
                onReply = { replyingToMessage = it },
                onMediaClick = { url, type ->
                    GlobalNavigation.navigateTo("media_viewer?url=${Uri.encode(url)}&type=$type")
                },
                playingAudioUri = playingAudioUri,
                onPlayAudio = { chatViewModel.setPlayingAudio(it) },
                highlightId = activeSearchId
            )
        }

        // Attachments Bottom Sheet / Overlay
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
                        onImageClick = {
                            showAttachments = false
                            imageLauncher.launch("image/*")
                        },
                        onVideoClick = {
                            showAttachments = false
                            videoLauncher.launch("video/*")
                        },
                        onDocClick = {
                            showAttachments = false
                            documentLauncher.launch("*/*")
                        },
                        onContactClick = {
                            showAttachments = false
                            contactLauncher.launch(null)
                        },
                        onCameraClick = {
                            showAttachments = false
                            val file = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        },
                        onAudioClick = {
                            showAttachments = false
                            audioPickerLauncher.launch("audio/*")
                        },
                        onLocationClick = {
                            showAttachments = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                LocationServices.getFusedLocationProviderClient(context)
                                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            sendToServer(
                                                body = "Shared Location",
                                                messageType = "location",
                                                latitude = location.latitude,
                                                longitude = location.longitude
                                            )
                                        } else {
                                            showToast("Could not retrieve precise location")
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
                GifStickerPicker(
                    onGifSelected = { url ->
                        chatViewModel.sendMessage(currentChat.id, "GIF", "image", listOf(url))
                    },
                    onStickerSelected = { url ->
                        chatViewModel.sendMessage(currentChat.id, "Sticker", "image", listOf(url))
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
                chats = allChats.data.orEmpty(),
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

        showDeleteConfirmDialog?.let { msgId ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Delete Message") },
                text = {
                    Column {
                        Text("Delete this message?")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { deleteForEveryone = !deleteForEveryone }
                        ) {
                            Checkbox(checked = deleteForEveryone, onCheckedChange = { deleteForEveryone = it })
                            Text("Delete for everyone", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        chatViewModel.deleteMessage(msgId, deleteForEveryone)
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
                    chatViewModel.sendMediaMessages(currentChat.id, uris, context, mediaPreviewType, caption)
                    selectedMediaForPreview = emptyList()
                }
            )
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaPreviewOverlay(
    uris: List<Uri>,
    type: String,
    onDismiss: () -> Unit,
    onSend: (String, List<Uri>) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (uris.size > 1) "${uris.size} ${type.replaceFirstChar { it.uppercase() }}s" else "Preview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

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
                            }
                        }
                    }
                } else {
                    val uri = uris.first()
                    when (type) {
                        "image" -> {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        "video" -> {
                            val previewPlayer = remember(uri) {
                                ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(uri))
                                    prepare()
                                    playWhenReady = false
                                }
                            }
                            DisposableEffect(previewPlayer) {
                                onDispose { previewPlayer.release() }
                            }
                            AndroidView(
                                factory = { viewContext ->
                                    PlayerView(viewContext).apply {
                                        player = previewPlayer
                                        useController = true
                                        controllerShowTimeoutMs = 2500
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp)
                            )
                        }
                        else -> {
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

            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    )
    val timePickerState = rememberTimePickerState(
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
