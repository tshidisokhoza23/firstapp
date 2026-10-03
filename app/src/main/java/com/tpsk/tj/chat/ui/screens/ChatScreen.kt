@file:Suppress("DEPRECATION")
@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.chat.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import coil.request.ImageRequest
import com.tpsk.tj.R
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.consistentName
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.model.MessageType
import com.tpsk.tj.chat.ui.components.*
import com.tpsk.tj.chat.ui.components.expression.GifStickerPicker
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.chat.data.CallType
import com.tpsk.tj.chat.data.GlobalCallState
import com.tpsk.tj.common.util.GlobalNavigation
import com.tpsk.tj.common.util.Resource
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
    onOpenStory: ((userId: String, storyId: String) -> Unit)? = null,
    jobId: String? = null,
    jobTitle: String? = null,
    companyName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by chatViewModel.uiState.collectAsState()
    val activeChat by chatViewModel.activeChat.collectAsState()
    val currentChat = if (activeChat?.id == chat.id) activeChat!! else chat
    val blockedUserIds by chatViewModel.blockedUserIds.collectAsState()
    val isDirectBlocked = currentChat.type == ConversationType.DIRECT &&
        (currentChat.otherUserId ?: currentChat.id).let { blockedUserIds.contains(it) }

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
    var showReactionPicker by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
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

    LaunchedEffect(currentChat.id, chatSearchQuery) {
        chatViewModel.searchInChat(currentChat.id, chatSearchQuery)
    }

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

        val file = if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            File(uri.path!!)
        } else {
            try {
                val out = File(context.cacheDir, "VOICE_${System.currentTimeMillis()}.m4a")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                } ?: return
                out
            } catch (_: Exception) {
                null
            }
        }
        if (file == null || !file.exists() || file.length() == 0L) {
            showToast("Voice note was not saved")
            return
        }
        chatViewModel.sendMediaMessage(
            currentChat.id,
            file,
            "audio_message",
            "Voice Note",
            durationLabel,
            amps
        )
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
                var name: String? = null
                var contactId: String? = null
                context.contentResolver.query(it, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0)
                        contactId = cursor.getString(1)
                    }
                }
                var phone: String? = null
                if (!contactId.isNullOrBlank()) {
                    context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) phone = phoneCursor.getString(0)
                    }
                }
                if (phone.isNullOrBlank() || name.isNullOrBlank()) {
                    context.contentResolver.query(
                        it,
                        arrayOf(
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null, null, null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            if (phone.isNullOrBlank()) phone = phoneCursor.getString(0)
                            if (name.isNullOrBlank()) name = phoneCursor.getString(1)
                        }
                    }
                }
                if (!name.isNullOrBlank() || !phone.isNullOrBlank()) {
                    sendToServer(
                        body = "Shared Contact",
                        messageType = "contact",
                        contactName = name,
                        contactPhone = phone
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) showToast("Permission denied")
    }

    var pendingCallTargetId by remember { mutableStateOf<String?>(null) }
    var pendingCallName by remember { mutableStateOf<String?>(null) }
    var pendingCallType by remember { mutableStateOf<CallType?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[android.Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] == true
        val targetId = pendingCallTargetId
        val type = pendingCallType
        val name = pendingCallName
        
        if (micGranted && (type != CallType.VIDEO || cameraGranted)) {
            if (targetId != null && type != null) {
                chatViewModel.startVoiceOrVideoCall(receiverId = targetId, callType = type, name = name)
            }
        } else {
            showToast("Microphone and Camera permissions are required for calling")
        }
    }

    // Initial Fetch & Typing Sync
    LaunchedEffect(chat.id) {
        chatViewModel.fetchMessages(chat.id)
        chatViewModel.markAsRead(chat.id)
    }

    DisposableEffect(chat.id) {
        onDispose {
            chatViewModel.clearActiveChat()
        }
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
                        sanitizedName = currentChat.consistentName().ifBlank { currentChat.displayName },
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
                        onInitiateCall = { targetId, name, type ->
                            val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasCam = if (type == CallType.VIDEO) {
                                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else true

                            if (hasMic && hasCam) {
                                chatViewModel.startVoiceOrVideoCall(receiverId = targetId, callType = type, name = name)
                            } else {
                                pendingCallTargetId = targetId
                                pendingCallName = name
                                pendingCallType = type
                                val perms = if (type == CallType.VIDEO) {
                                    arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
                                } else {
                                    arrayOf(android.Manifest.permission.RECORD_AUDIO)
                                }
                                callPermissionLauncher.launch(perms)
                            }
                        },
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
                        onPinSelectedMessage = {
                            selectedMessages.firstOrNull()?.let { msgId ->
                                val alreadyPinned = currentChat.pinnedMessageId == msgId
                                chatViewModel.pinMessage(currentChat.id, if (alreadyPinned) null else msgId)
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
                        onReactSelectedMessage = { if (selectedMessages.size == 1) showReactionPicker = true },
                        onReportSelectedMessage = {
                            if (selectedMessages.size == 1) {
                                reportReason = ""
                                showReportDialog = true
                            }
                        },
                        onClearSelectedMessages = { selectedMessages.clear() }
                    )
                    if (currentChat.type == ConversationType.DIRECT && !currentChat.isSavedContact && !currentChat.phoneNumber.isNullOrBlank()) {
                        UnsavedContactSafetyBanner(
                            phoneNumber = currentChat.phoneNumber!!,
                            onAddToContacts = {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, currentChat.phoneNumber)
                                    putExtra(ContactsContract.Intents.Insert.NAME, currentChat.serverProfileName ?: "")
                                }
                                context.startActivity(intent)
                            },
                            onBlock = { currentChat.otherUserId?.let { chatViewModel.blockUser(it); showToast("User blocked") } },
                            onReport = { currentChat.otherUserId?.let { chatViewModel.reportUser(it, "Spam"); showToast("Reported") } }
                        )
                    }
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
                            ConversationType.DIRECT, ConversationType.GROUP, ConversationType.JOB_CHAT, ConversationType.MINGLE -> true
                            ConversationType.CHANNEL -> {
                                val isCreator = currentChat.createdBy != null && currentChat.createdBy == myUserId
                                isCreator || (currentChat.isAdmin && (currentChat.adminsCanPost || currentChat.createdBy == null))
                            }
                        }

                        val canCompose = canPost && !isConversationClosed && !isDirectBlocked
                        val showFollowButton = currentChat.type == ConversationType.CHANNEL && !currentChat.isSubscribed && !currentChat.isAdmin

                        if (isDirectBlocked) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "You blocked this contact. Unblock to send messages.",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (canCompose) {
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
                highlightId = activeSearchId,
                onQuoteClick = { replyToId, replyToText ->
                    val isStoryReply = replyToId?.startsWith("story_") == true ||
                        replyToText?.contains("[Story]", ignoreCase = true) == true ||
                        replyToText?.contains("[mediaUrl:", ignoreCase = true) == true
                    if (isStoryReply) {
                        val parsed = com.tpsk.tj.chat.model.MessageMarkers.parseStoryReplyId(replyToId)
                        val storyId = parsed?.storyId?.takeIf { it.isNotBlank() }
                            ?: replyToId?.removePrefix("story_")?.substringBefore("|")?.takeIf { it.isNotBlank() }
                        val storyUser = parsed?.userId?.takeIf { it.isNotBlank() }
                            ?: currentChat.otherUserId
                            ?: currentChat.id
                        if (!storyUser.isNullOrBlank() && !storyId.isNullOrBlank()) {
                            onOpenStory?.invoke(storyUser, storyId)
                        } else {
                            android.widget.Toast.makeText(context, "This story has expired or is no longer available.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val targetIdx = messages.indexOfFirst { it.id == replyToId }
                        if (targetIdx != -1) {
                            scope.launch {
                                messagesLazyListState.animateScrollToItem(targetIdx)
                                activeSearchId = replyToId
                                delay(2500)
                                if (activeSearchId == replyToId) activeSearchId = null
                            }
                        }
                    }
                }
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
                EmojiPicker(
                    onEmojiSelect = {
                        messageText += it
                        showEmojiPicker = false
                    }
                )
            }
        }

        if (showReactionPicker) {
            ReactionPickerOverlay(
                onDismiss = { showReactionPicker = false },
                onEmojiSelect = { emoji ->
                    selectedMessages.firstOrNull()?.let { chatViewModel.reactToMessage(it, emoji) }
                    showReactionPicker = false
                    selectedMessages.clear()
                }
            )
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Report message") },
                text = {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val reason = reportReason.trim().ifBlank { "Inappropriate" }
                        selectedMessages.firstOrNull()?.let { chatViewModel.reportMessage(it, reason) }
                        showReportDialog = false
                        selectedMessages.clear()
                    }) { Text("Report") }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
                }
            )
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

data class DrawnPath(val points: List<Offset>, val color: Color, val strokeWidth: Float = 8f)

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaPreviewOverlay(
    uris: List<Uri>,
    type: String,
    onDismiss: () -> Unit,
    onSend: (String, List<Uri>) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var aspectRatioMode by remember { mutableStateOf<Float?>(null) }
    var overlayText by remember { mutableStateOf("") }
    var selectedTextColor by remember { mutableStateOf(Color.White) }
    var showTextInput by remember { mutableStateOf(false) }
    var showCropMenu by remember { mutableStateOf(false) }
    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedDrawColor by remember { mutableStateOf(Color.Red) }
    var isMuted by remember { mutableStateOf(false) }

    val drawnPaths = remember { mutableStateListOf<DrawnPath>() }
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var normalizedCropRect by remember { mutableStateOf(android.graphics.RectF(0f, 0f, 1f, 1f)) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val colorPalette = listOf(
        Color.White, Color.Yellow, Color(0xFFE53935), Color(0xFF4CAF50), Color(0xFF00ACC1), Color(0xFF8E24AA)
    )

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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uris.size > 1) "${uris.size} ${type.replaceFirstChar { it.uppercase() }}s" else "Preview",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (type == "video") {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }
                    }
                    if (type == "image") {
                        IconButton(onClick = { showCropMenu = !showCropMenu }) {
                            Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color.White)
                        }
                        IconButton(onClick = {
                            isDrawingMode = !isDrawingMode
                            showTextInput = false
                        }) {
                            Icon(
                                Icons.Default.Brush,
                                contentDescription = "Draw",
                                tint = if (isDrawingMode) AppAccent else Color.White
                            )
                        }
                    }
                    IconButton(onClick = {
                        showTextInput = !showTextInput
                        isDrawingMode = false
                    }) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Add Text",
                            tint = if (showTextInput) AppAccent else Color.White
                        )
                    }
                    if (drawnPaths.isNotEmpty()) {
                        IconButton(onClick = { if (drawnPaths.isNotEmpty()) drawnPaths.removeAt(drawnPaths.size - 1) }) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
                        }
                    }
                }
            }

            var trimStartMs by remember { mutableLongStateOf(0L) }
            var trimEndMs by remember { mutableLongStateOf(0L) }

            if (type == "video" && uris.size == 1) {
                com.tpsk.tj.common.ui.components.VideoTrimmerScrubber(
                    context = context,
                    videoUri = uris.first(),
                    maxDurationMs = 60_000L,
                    onTrimRangeChanged = { startMs, endMs ->
                        trimStartMs = startMs
                        trimEndMs = endMs
                    }
                )
            }

            if (showCropMenu && type == "image") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray.copy(alpha = 0.8f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                        Icon(Icons.Default.CropRotate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rotate", color = Color.White, fontSize = 12.sp)
                    }
                    TextButton(onClick = { aspectRatioMode = null }) {
                        Text("Free", color = if (aspectRatioMode == null) AppAccent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { aspectRatioMode = 1f }) {
                        Text("1:1", color = if (aspectRatioMode == 1f) AppAccent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { aspectRatioMode = 4f / 3f }) {
                        Text("4:3", color = if (aspectRatioMode == 4f / 3f) AppAccent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { aspectRatioMode = 16f / 9f }) {
                        Text("16:9", color = if (aspectRatioMode == 16f / 9f) AppAccent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isDrawingMode || showTextInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPalette.forEach { color ->
                        val isSelected = if (isDrawingMode) selectedDrawColor == color else selectedTextColor == color
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) AppAccent else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (isDrawingMode) selectedDrawColor = color else selectedTextColor = color
                                }
                        )
                    }
                }
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
                                    model = ImageRequest.Builder(context)
                                        .data(uri)
                                        .crossfade(true)
                                        .build(),
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
                            val baseModifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 550.dp)
                            val imageModifier = if (aspectRatioMode != null) baseModifier.aspectRatio(aspectRatioMode!!) else baseModifier

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(isDrawingMode, selectedDrawColor) {
                                        if (isDrawingMode) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    currentPathPoints = listOf(offset)
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    currentPathPoints = currentPathPoints + change.position
                                                },
                                                onDragEnd = {
                                                    if (currentPathPoints.isNotEmpty()) {
                                                        drawnPaths.add(DrawnPath(currentPathPoints, selectedDrawColor))
                                                        currentPathPoints = emptyList()
                                                    }
                                                }
                                            )
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = imageModifier.graphicsLayer(rotationZ = rotationAngle),
                                    contentScale = ContentScale.Fit,
                                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray)
                                )

                                if (showCropMenu) {
                                    InteractiveCropOverlay(
                                        cropRect = normalizedCropRect,
                                        onCropRectChanged = { normalizedCropRect = it },
                                        modifier = imageModifier
                                    )
                                }

                                Canvas(modifier = Modifier.matchParentSize()) {
                                    for (path in drawnPaths) {
                                        if (path.points.size > 1) {
                                            val p = Path().apply {
                                                moveTo(path.points.first().x, path.points.first().y)
                                                for (i in 1 until path.points.size) {
                                                    lineTo(path.points[i].x, path.points[i].y)
                                                }
                                            }
                                            drawPath(
                                                path = p,
                                                color = path.color,
                                                style = Stroke(width = path.strokeWidth, cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                    if (currentPathPoints.size > 1) {
                                        val p = Path().apply {
                                            moveTo(currentPathPoints.first().x, currentPathPoints.first().y)
                                            for (i in 1 until currentPathPoints.size) {
                                                lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = p,
                                            color = selectedDrawColor,
                                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                                        )
                                    }
                                }

                                if (overlayText.isNotBlank()) {
                                    Text(
                                        text = overlayText,
                                        color = selectedTextColor,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        "video" -> {
                            val previewPlayer = remember(uri) {
                                ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(uri))
                                    prepare()
                                    playWhenReady = false
                                }
                            }
                            LaunchedEffect(trimStartMs) {
                                if (trimStartMs > 0) {
                                    previewPlayer.seekTo(trimStartMs)
                                }
                            }
                            LaunchedEffect(isMuted) {
                                previewPlayer.volume = if (isMuted) 0f else 1f
                            }
                            val previewView = remember(uri) { mutableStateOf<PlayerView?>(null) }
                            DisposableEffect(previewPlayer) {
                                onDispose {
                                    com.tpsk.tj.media.ui.components.ActiveVideoPlayback.detachAndRelease(previewView.value, previewPlayer)
                                    previewView.value = null
                                }
                            }
                            AndroidView(
                                factory = { viewContext ->
                                    PlayerView(viewContext).apply {
                                        player = previewPlayer
                                        useController = true
                                        controllerShowTimeoutMs = 2500
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        previewView.value = this
                                    }
                                },
                                update = { view -> previewView.value = view },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 550.dp)
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

            if (showTextInput) {
                OutlinedTextField(
                    value = overlayText,
                    onValueChange = { overlayText = it },
                    placeholder = { Text("Type overlay text...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.8f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
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
                        onClick = {
                            if (isExporting) return@FloatingActionButton
                            isExporting = true
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val processedUris = mutableListOf<Uri>()
                                    for (sourceUri in uris) {
                                        if (type == "video" && trimEndMs > trimStartMs) {
                                            val exportedFile = com.tpsk.tj.common.util.VideoTransformerManager.trimAndExportVideo(
                                                context, sourceUri, trimStartMs, trimEndMs
                                            )
                                            if (exportedFile != null && exportedFile.exists() && exportedFile.length() > 0) {
                                                processedUris.add(Uri.fromFile(exportedFile))
                                            } else {
                                                processedUris.add(sourceUri)
                                            }
                                        } else if (type == "image" && (showCropMenu || aspectRatioMode != null || rotationAngle != 0f || drawnPaths.isNotEmpty() || overlayText.isNotBlank())) {
                                            val tempFile = com.tpsk.tj.common.util.MediaCacheManager.createTempFile(context, "CROP_", ".webp")
                                            val cropRect = normalizedCropRect
                                            val success = com.tpsk.tj.common.util.ImageExportEngine.processAndCompressImage(
                                                context, sourceUri, tempFile, cropRect, rotationAngle.toInt()
                                            )
                                            if (success && tempFile.exists() && tempFile.length() > 0) {
                                                processedUris.add(Uri.fromFile(tempFile))
                                            } else {
                                                processedUris.add(sourceUri)
                                            }
                                        } else {
                                            processedUris.add(sourceUri)
                                        }
                                    }

                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isExporting = false
                                        onSend(caption, processedUris)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isExporting = false
                                        onSend(caption, uris)
                                    }
                                }
                            }
                        },
                        containerColor = AppAccent,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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

@Composable
fun UnsavedContactSafetyBanner(
    phoneNumber: String,
    onAddToContacts: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This sender is not in your contacts list",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onAddToContacts,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Contact", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onBlock,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = onReport,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun InteractiveCropOverlay(
    cropRect: android.graphics.RectF,
    onCropRectChanged: (android.graphics.RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    var rectLeft by remember { mutableFloatStateOf(cropRect.left) }
    var rectTop by remember { mutableFloatStateOf(cropRect.top) }
    var rectRight by remember { mutableFloatStateOf(cropRect.right.coerceAtLeast(cropRect.left + 0.2f)) }
    var rectBottom by remember { mutableFloatStateOf(cropRect.bottom.coerceAtLeast(cropRect.top + 0.2f)) }
    var dragHandle by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val curL = rectLeft * widthPx
                            val curT = rectTop * heightPx
                            val curR = rectRight * widthPx
                            val curB = rectBottom * heightPx
                            val hitSize = 140f
                            val nearLeft = kotlin.math.abs(position.x - curL) < hitSize
                            val nearRight = kotlin.math.abs(position.x - curR) < hitSize
                            val nearTop = kotlin.math.abs(position.y - curT) < hitSize
                            val nearBottom = kotlin.math.abs(position.y - curB) < hitSize

                            dragHandle = when {
                                nearLeft && nearTop -> 1
                                nearRight && nearTop -> 2
                                nearLeft && nearBottom -> 3
                                nearRight && nearBottom -> 4
                                nearLeft -> 5
                                nearRight -> 6
                                nearTop -> 7
                                nearBottom -> 8
                                else -> 9
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dx = dragAmount.x / widthPx
                            val dy = dragAmount.y / heightPx

                            var nL = rectLeft
                            var nT = rectTop
                            var nR = rectRight
                            var nB = rectBottom

                            if (dragHandle == 1) {
                                nL = (rectLeft + dx).coerceIn(0f, rectRight - 0.15f)
                                nT = (rectTop + dy).coerceIn(0f, rectBottom - 0.15f)
                            } else if (dragHandle == 2) {
                                nR = (rectRight + dx).coerceIn(rectLeft + 0.15f, 1f)
                                nT = (rectTop + dy).coerceIn(0f, rectBottom - 0.15f)
                            } else if (dragHandle == 3) {
                                nL = (rectLeft + dx).coerceIn(0f, rectRight - 0.15f)
                                nB = (rectBottom + dy).coerceIn(rectTop + 0.15f, 1f)
                            } else if (dragHandle == 4) {
                                nR = (rectRight + dx).coerceIn(rectLeft + 0.15f, 1f)
                                nB = (rectBottom + dy).coerceIn(rectTop + 0.15f, 1f)
                            } else if (dragHandle == 5) {
                                nL = (rectLeft + dx).coerceIn(0f, rectRight - 0.15f)
                            } else if (dragHandle == 6) {
                                nR = (rectRight + dx).coerceIn(rectLeft + 0.15f, 1f)
                            } else if (dragHandle == 7) {
                                nT = (rectTop + dy).coerceIn(0f, rectBottom - 0.15f)
                            } else if (dragHandle == 8) {
                                nB = (rectBottom + dy).coerceIn(rectTop + 0.15f, 1f)
                            } else {
                                val w = rectRight - rectLeft
                                val h = rectBottom - rectTop
                                nL = (rectLeft + dx).coerceIn(0f, 1f - w)
                                nT = (rectTop + dy).coerceIn(0f, 1f - h)
                                nR = nL + w
                                nB = nT + h
                            }

                            rectLeft = nL
                            rectTop = nT
                            rectRight = nR
                            rectBottom = nB
                            onCropRectChanged(android.graphics.RectF(rectLeft, rectTop, rectRight, rectBottom))
                        }
                    )
                }
        ) {
            val cL = rectLeft * size.width
            val cT = rectTop * size.height
            val cR = rectRight * size.width
            val cB = rectBottom * size.height
            val cW = (cR - cL).coerceAtLeast(1f)
            val cH = (cB - cT).coerceAtLeast(1f)

            // Dim 4 outer regions (inner image stays 100% visible and unblackened)
            if (cT > 0f) {
                drawRect(color = Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, 0f), size = Size(size.width, cT))
            }
            if (cB < size.height) {
                drawRect(color = Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, cB), size = Size(size.width, size.height - cB))
            }
            if (cL > 0f) {
                drawRect(color = Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, cT), size = Size(cL, cH))
            }
            if (cR < size.width) {
                drawRect(color = Color.Black.copy(alpha = 0.55f), topLeft = Offset(cR, cT), size = Size(size.width - cR, cH))
            }

            // White Crop Frame Border
            drawRect(
                color = Color.White,
                topLeft = Offset(cL, cT),
                size = Size(cW, cH),
                style = Stroke(width = 2.dp.toPx())
            )

            // 3x3 Rule-of-Thirds Grid Lines
            val thirdW = cW / 3f
            val thirdH = cH / 3f
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cL + thirdW, cT), Offset(cL + thirdW, cB), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cL + 2 * thirdW, cT), Offset(cL + 2 * thirdW, cB), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cL, cT + thirdH), Offset(cR, cT + thirdH), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cL, cT + 2 * thirdH), Offset(cR, cT + 2 * thirdH), strokeWidth = 1.dp.toPx())

            // Thick White Corner Brackets
            val hLen = 22.dp.toPx()
            val hThick = 4.5f.dp.toPx()

            // Top-Left
            drawLine(Color.White, Offset(cL, cT), Offset(cL + hLen, cT), strokeWidth = hThick)
            drawLine(Color.White, Offset(cL, cT), Offset(cL, cT + hLen), strokeWidth = hThick)

            // Top-Right
            drawLine(Color.White, Offset(cR, cT), Offset(cR - hLen, cT), strokeWidth = hThick)
            drawLine(Color.White, Offset(cR, cT), Offset(cR, cT + hLen), strokeWidth = hThick)

            // Bottom-Left
            drawLine(Color.White, Offset(cL, cB), Offset(cL + hLen, cB), strokeWidth = hThick)
            drawLine(Color.White, Offset(cL, cB), Offset(cL, cB - hLen), strokeWidth = hThick)

            // Bottom-Right
            drawLine(Color.White, Offset(cR, cB), Offset(cR - hLen, cB), strokeWidth = hThick)
            drawLine(Color.White, Offset(cR, cB), Offset(cR, cB - hLen), strokeWidth = hThick)
        }
    }
}

@Composable
fun VideoTrimmerBar(
    context: android.content.Context,
    videoUri: Uri,
    onRangeChanged: (Long, Long) -> Unit
) {
    var thumbnails by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var totalFileSizeBytes by remember { mutableLongStateOf(0L) }
    var sliderRange by remember { mutableStateOf(0f..1f) }

    LaunchedEffect(videoUri) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            totalDurationMs = durStr?.toLongOrNull() ?: 0L
            val pfd = context.contentResolver.openFileDescriptor(videoUri, "r")
            totalFileSizeBytes = pfd?.statSize ?: 0L
            pfd?.close()
        } catch (_: Exception) {} finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        thumbnails = com.tpsk.tj.common.util.ThumbnailStripGenerator.extractThumbnails(context, videoUri, count = 8)
    }

    val selectedStartMs = (sliderRange.start * totalDurationMs).toLong()
    val selectedEndMs = (sliderRange.endInclusive * totalDurationMs).toLong()
    val estimatedSizeMb = com.tpsk.tj.common.util.ThumbnailStripGenerator.estimateTrimmedSizeMb(
        selectedStartMs, selectedEndMs, totalDurationMs, totalFileSizeBytes
    )

    LaunchedEffect(sliderRange, totalDurationMs) {
        onRangeChanged(selectedStartMs, selectedEndMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (thumbnails.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                thumbnails.forEach { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        RangeSlider(
            value = sliderRange,
            onValueChange = { range -> sliderRange = range },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = AppAccent,
                activeTrackColor = AppAccent,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        val durationSec = ((selectedEndMs - selectedStartMs) / 1000).coerceAtLeast(0)
        val durationFormatted = String.format(java.util.Locale.US, "%d:%02d", durationSec / 60, durationSec % 60)
        val sizeFormatted = String.format(java.util.Locale.US, "%.1f MB", estimatedSizeMb)

        Text(
            text = "$durationFormatted • $sizeFormatted",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
