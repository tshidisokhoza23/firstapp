@file:Suppress("DEPRECATION")

package com.example.firstapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat
import com.example.firstapp.data.realtime.CallEvent
import com.example.firstapp.data.realtime.GlobalCallState
import com.example.firstapp.data.realtime.CallSocketListener
import com.example.firstapp.data.realtime.CallType
import com.example.firstapp.data.realtime.CallWebSocketClient
import com.example.firstapp.data.realtime.ChatSocketListener
import com.example.firstapp.data.realtime.ChatWebSocketClient
import com.example.firstapp.data.realtime.CallTonePlayer
import com.example.firstapp.data.realtime.WebRtcCallManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.UploadResponse
import com.example.firstapp.data.repository.AppRepositoryImpl
import com.example.firstapp.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.model.Message
import com.example.firstapp.model.MessageType
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.util.formatMessageTime
import com.example.firstapp.util.formatWhatsAppTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.provider.OpenableColumns

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chat: ChatMessage,
    viewModel: MainViewModel,
    messagesState: Resource<List<Message>>,
    onBack: () -> Unit,
    onShowSharedMedia: () -> Unit,
    onLoadMore: (Int) -> Unit = {},
    onReact: (Int, String) -> Unit = { _, _ -> },
    onTyping: (Int, Boolean) -> Unit = { _, _ -> },
    onPresence: (Int, Boolean, String?) -> Unit = { _, _, _ -> },
    onNewMessage: (Message) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { com.example.firstapp.AppContainer.repository }

    val messages = remember(messagesState) {
        messagesState.data ?: emptyList()
    }
    val messagesLazyListState = rememberLazyListState()
    var showChatMenu by remember { mutableStateOf(false) }
    var showContactInfo by remember { mutableStateOf(false) }
    var incomingCallEvent by remember { mutableStateOf<CallEvent?>(null) }
    val globalIncomingCall by GlobalCallState.incomingCall.collectAsState()
    val autoAcceptSessionId by GlobalCallState.autoAcceptSessionId.collectAsState()
    var activeCallType by remember { mutableStateOf<CallType?>(null) }
    var activeCallSessionId by remember { mutableStateOf<String?>(null) }
    var isOutgoingRinging by remember { mutableStateOf(false) }
    var isCallConnected by remember { mutableStateOf(false) }
    var callStartedAtMs by remember { mutableLongStateOf(0L) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isSpeakerEnabled by remember { mutableStateOf(false) }
    var isCameraEnabled by remember { mutableStateOf(true) }
    val webRtcManager = remember { com.example.firstapp.AppContainer.webRtcCallManager }
    val callTonePlayer = remember { com.example.firstapp.AppContainer.callTonePlayer }
    val ringtone = remember { mutableStateOf<Ringtone?>(null) }
    
    // We removed manual ringtone logic from here to avoid it getting stuck.
    // The system ringtone for the incoming call notification handles this now.
    
    var selectedMessages = remember { mutableStateListOf<Int>() }
    
    var isSearchMode by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    val chatSearchFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            delay(100)
            chatSearchFocusRequester.requestFocus()
        }
    }
    var currentPage by remember { mutableIntStateOf(1) }
    var isEndReached by remember { mutableStateOf(false) }
    var isPaginationLoading by remember { mutableStateOf(false) }
    
    val isAtBottom = remember {
        derivedStateOf {
            val layoutInfo = messagesLazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 5
            }
        }
    }

    // Auto-scroll logic
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (isAtBottom.value || lastMsg.isFromMe) {
                delay(100)
                messagesLazyListState.animateScrollToItem(messagesLazyListState.layoutInfo.totalItemsCount)
            }
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty() && isAtBottom.value) {
            messagesLazyListState.animateScrollToItem(messagesLazyListState.layoutInfo.totalItemsCount)
        }
    }
    
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var infoMessage by remember { mutableStateOf<Message?>(null) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var messagesToForward = remember { mutableStateListOf<Message>() }
    var showReactionPicker by remember { mutableStateOf(false) }

    fun showToast(message: String) {
        scope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showToast("Permission denied")
        }
    }

    // Consume app-wide incoming call state when this chat matches the caller.
    LaunchedEffect(globalIncomingCall, chat.id) {
        if (globalIncomingCall?.senderId == chat.id) {
            incomingCallEvent = globalIncomingCall
        } else if (globalIncomingCall == null) {
            incomingCallEvent = null
        }
    }

    val globalIsCallActive by GlobalCallState.isCallActive.collectAsState()
    val globalActiveSessionId by GlobalCallState.activeSessionId.collectAsState()
    val globalActiveParticipantId by GlobalCallState.activeParticipantId.collectAsState()
    val globalActiveCallType by GlobalCallState.activeCallType.collectAsState()
    val globalIsConnectionEstablished by GlobalCallState.isConnectionEstablished.collectAsState()

    LaunchedEffect(globalIsCallActive, globalActiveSessionId, globalActiveParticipantId) {
        if (globalIsCallActive && globalActiveParticipantId == chat.id && activeCallSessionId == null) {
            activeCallSessionId = globalActiveSessionId
            activeCallType = globalActiveCallType
            isCallConnected = globalIsConnectionEstablished
            isOutgoingRinging = !globalIsConnectionEstablished && (activeCallSessionId != null) // Simplification
        }

        if (!globalIsCallActive && activeCallSessionId != null) {
            // Force end local call if global state says no call is active
            webRtcManager.end()
            isOutgoingRinging = false
            isCallConnected = false
            activeCallSessionId = null
            activeCallType = null
            callStartedAtMs = 0L
            callDurationSeconds = 0
        }
    }

    fun ensureCallPermissions(type: CallType): Boolean {
        val micPermission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, micPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(micPermission)
            showToast("Grant microphone permission to place calls")
            return false
        }

        if (type == CallType.VIDEO) {
            val cameraPermission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(context, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(cameraPermission)
                showToast("Grant camera permission to start video calls")
                return false
            }
        }
        return true
    }


    // Load initial messages handled by MainViewModel

    fun loadMoreMessages() {
        if (isPaginationLoading || isEndReached) return
        isPaginationLoading = true
        scope.launch {
            // Trigger load more in ViewModel
            onLoadMore(currentPage + 1)
            // Wait for it to hit DB
            delay(1000)
            val newMessages = (messagesState as? Resource.Success)?.data ?: emptyList()
            if (newMessages.size <= messages.size && currentPage > 1) {
                isEndReached = true
            } else {
                currentPage++
            }
            isPaginationLoading = false
        }
    }

    // Mark as read when entering the chat
    LaunchedEffect(chat.id) {
        currentPage = 1
        isEndReached = false
        repository.markAsRead(chat.id).collect {}
    }

    // WebSocket connection handled by MainViewModel

    // Send typing signal
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            ChatWebSocketClient.sendTyping(chat.id, true)
            delay(3000)
            ChatWebSocketClient.sendTyping(chat.id, false)
        } else {
            ChatWebSocketClient.sendTyping(chat.id, false)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && (isAtBottom.value || messages.lastOrNull()?.isFromMe == true)) {
            messagesLazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    val callSocketListener = remember(chat.id) {
        object : CallSocketListener {
            override fun onOpen() {}

            override fun onEvent(event: CallEvent) {
                scope.launch {
                    if (event.senderId != chat.id && event.receiverId != chat.id) {
                        return@launch
                    }

                    when (event.type) {
                        "call_invite" -> {
                            if (event.senderId == chat.id) {
                                if (activeCallSessionId != null || isOutgoingRinging || isCallConnected) {
                                    CallWebSocketClient.reject(
                                        receiverId = event.senderId,
                                        sessionId = event.sessionId,
                                        callType = event.callType,
                                        reason = "busy"
                                    )
                                    return@launch
                                }
                                incomingCallEvent = event
                            }
                        }
                        "call_accept" -> {
                            if (event.senderId == chat.id && activeCallSessionId == event.sessionId) {
                                callTonePlayer.stop()
                                isOutgoingRinging = false
                                isCallConnected = true
                                GlobalCallState.setConnectionEstablished(true)
                                callStartedAtMs = System.currentTimeMillis()
                                callDurationSeconds = 0
                                webRtcManager.createOffer()
                            }
                        }
                        "call_reject" -> {
                            if (activeCallSessionId == event.sessionId) {
                                callTonePlayer.stop()
                                GlobalCallState.setCallActive(false, null, null, null)
                                webRtcManager.end()
                                incomingCallEvent = null
                                isOutgoingRinging = false
                                isCallConnected = false
                                activeCallSessionId = null
                                activeCallType = null
                                callStartedAtMs = 0L
                                callDurationSeconds = 0
                                val reason = event.endReason ?: "declined"
                                val message = if (reason == "busy") "User is busy on another call" else "Call declined"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        "call_end" -> {
                            val isActiveSession = activeCallSessionId == event.sessionId
                            val isRingingIncomingSession = incomingCallEvent?.sessionId == event.sessionId
                            if (isActiveSession || isRingingIncomingSession) {
                                callTonePlayer.stop()
                                if (isActiveSession) GlobalCallState.setCallActive(false, null, null, null)
                                webRtcManager.end()
                                incomingCallEvent = null
                                GlobalCallState.onCallEnded(event.sessionId)
                                isOutgoingRinging = false
                                isCallConnected = false
                                activeCallSessionId = null
                                activeCallType = null
                                callStartedAtMs = 0L
                                callDurationSeconds = 0
                                val reason = event.endReason ?: "ended"
                                val message = if (reason == "missed" || reason == "timeout") "Missed call" else "Call ended"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        "call_signal" -> {
                            if (activeCallSessionId == event.sessionId) {
                                webRtcManager.handleSignal(event.signalType, event.signalPayload)
                            }
                        }
                    }
                }
            }

            override fun onError(error: String) {
                scope.launch {
                    Toast.makeText(context, "Call connection error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(chat.id) {
        CallWebSocketClient.connect(callSocketListener)
        onDispose {
            CallWebSocketClient.disconnect(callSocketListener)
        }
    }

    LaunchedEffect(isCallConnected, callStartedAtMs) {
        if (!isCallConnected || callStartedAtMs == 0L) {
            callDurationSeconds = 0
            return@LaunchedEffect
        }
        while (isCallConnected) {
            callDurationSeconds = ((System.currentTimeMillis() - callStartedAtMs) / 1000L).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    LaunchedEffect(isOutgoingRinging, activeCallSessionId, activeCallType) {
        if (!isOutgoingRinging || activeCallSessionId == null || activeCallType == null) {
            return@LaunchedEffect
        }

        delay(30000)

        if (isOutgoingRinging && !isCallConnected && activeCallSessionId != null && activeCallType != null) {
            CallWebSocketClient.end(
                receiverId = chat.id,
                sessionId = activeCallSessionId!!,
                callType = activeCallType!!,
                durationSeconds = 0,
                reason = "missed"
            )
            webRtcManager.end()
            isOutgoingRinging = false
            activeCallSessionId = null
            activeCallType = null
            callStartedAtMs = 0L
            callDurationSeconds = 0
            Toast.makeText(context, "No answer", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(incomingCallEvent != null) {
        if (incomingCallEvent != null) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }
    }

    fun startOutgoingCall(callType: CallType) {
        if (activeCallSessionId != null || GlobalCallState.isCallActive.value) {
            showToast("You are already in a call")
            return
        }
        if (!ensureCallPermissions(callType)) {
            return
        }

        val sessionId = "${System.currentTimeMillis()}-${chat.id}"
        GlobalCallState.setCallActive(true, sessionId, chat.id, callType)
        activeCallSessionId = sessionId
        activeCallType = callType
        isOutgoingRinging = true
        isCallConnected = false
        callDurationSeconds = 0
        callStartedAtMs = 0L
        isMicMuted = false
        isSpeakerEnabled = callType == CallType.VIDEO
        isCameraEnabled = true

        callTonePlayer.playOutgoingRingtone()
        webRtcManager.start(callType)
        // Signaling is now handled globally by CallListenerService
        CallWebSocketClient.invite(receiverId = chat.id, sessionId = sessionId, callType = callType)
    }

    fun acceptIncomingCall() {
        val call = incomingCallEvent ?: return
        if (!ensureCallPermissions(call.callType)) {
            return
        }
        GlobalCallState.setCallActive(true, call.sessionId, chat.id, call.callType)
        GlobalCallState.setConnectionEstablished(true)
        incomingCallEvent = null
        activeCallSessionId = call.sessionId
        activeCallType = call.callType
        isOutgoingRinging = false
        isCallConnected = true
        callStartedAtMs = System.currentTimeMillis()
        callDurationSeconds = 0
        isMicMuted = false
        isSpeakerEnabled = call.callType == CallType.VIDEO
        isCameraEnabled = true
        GlobalCallState.onCallEnded(call.sessionId)
        webRtcManager.start(call.callType)
        // Signaling is now handled globally by CallListenerService
        CallWebSocketClient.accept(receiverId = chat.id, sessionId = call.sessionId, callType = call.callType)
    }

    fun rejectIncomingCall() {
        val call = incomingCallEvent ?: return
        CallWebSocketClient.reject(receiverId = chat.id, sessionId = call.sessionId, callType = call.callType)
        webRtcManager.end()
        incomingCallEvent = null
        GlobalCallState.onCallEnded(call.sessionId)
    }

    fun endActiveCall() {
        val callType = activeCallType ?: return
        val sessionId = activeCallSessionId ?: return
        callTonePlayer.stop()
        GlobalCallState.setCallActive(false, null, null, null)
        CallWebSocketClient.end(
            receiverId = chat.id,
            sessionId = sessionId,
            callType = callType,
            durationSeconds = callDurationSeconds
        )
        webRtcManager.end()
        isOutgoingRinging = false
        isCallConnected = false
        activeCallSessionId = null
        activeCallType = null
        callStartedAtMs = 0L
        callDurationSeconds = 0
    }

    fun deleteSelectedMessages() {
        val toDelete = selectedMessages.toList()
        selectedMessages.clear()
        
        toDelete.forEach { messageId ->
            viewModel.deleteMessage(messageId)
        }
    }

    // Auto-accept when the user tapped Accept in the notification.
    LaunchedEffect(autoAcceptSessionId, incomingCallEvent?.sessionId) {
        val targetSession = autoAcceptSessionId ?: return@LaunchedEffect
        val call = incomingCallEvent ?: return@LaunchedEffect
        if (call.sessionId == targetSession) {
            GlobalCallState.clearAutoAccept()
            // If the receiver already started the call, we just need to update local UI state.
            if (activeCallSessionId == targetSession) {
                isCallConnected = true
                isOutgoingRinging = false
                callStartedAtMs = System.currentTimeMillis()
            } else {
                acceptIncomingCall()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Do NOT call ChatWebSocketClient.close() here as it is shared with MainViewModel.
            // MainViewModel handles its own connection lifecycle.

            // Do NOT release webRtcManager or setCallActive(false) here.
            // That should only happen when endActiveCall() is explicitly called
            // or when the call is ended by the remote side via websocket.
            try {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            } catch (_: Exception) {
            }
        }
    }

    var showAttachments by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                recordingDurationSeconds = ((System.currentTimeMillis() - recordingStartMs) / 1000L).toInt()
                delay(100)
            }
        } else {
            recordingDurationSeconds = 0
        }
    }

    fun copyUriToTempFile(uri: Uri): File? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
        resolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        return tempFile
    }

    fun resolveDisplayName(uri: Uri): String {
        val resolver = context.contentResolver
        var name: String? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = cursor.getString(idx)
                }
            }
        }
        return name ?: "file_${System.currentTimeMillis()}"
    }

    suspend fun uploadMedia(uri: Uri): UploadResponse? {
        return try {
            val localFile = copyUriToTempFile(uri) ?: return null
            val mediaType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val requestBody = localFile.asRequestBody(mediaType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", resolveDisplayName(uri), requestBody)
            val response = ApiClient.apiService.uploadMedia(part)
            localFile.delete()
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) {
            null
        }
    }

    fun sendToServer(
        body: String,
        messageType: String = "text",
        mediaUrl: String? = null,
        duration: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val replyId = replyingToMessage?.id
        replyingToMessage = null // Clear reply state after sending
        scope.launch {
            repository.sendMessage(
                chatId = chat.id,
                body = body,
                messageType = messageType,
                mediaUrl = mediaUrl,
                duration = duration,
                fileName = fileName,
                fileSize = fileSize,
                contactName = contactName,
                contactPhone = contactPhone,
                latitude = latitude,
                longitude = longitude,
                replyToId = replyId
            ).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        // New message will be added via ViewModel/WebSocket
                    }
                    is Resource.Error -> {
                        Toast.makeText(context, resource.message ?: "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun sendMediaMessage(uri: Uri, type: String, fallbackBody: String, duration: String? = null) {
        scope.launch {
            val upload = uploadMedia(uri)
            if (upload == null) {
                Toast.makeText(context, "Failed to upload media", Toast.LENGTH_SHORT).show()
                return@launch
            }

            sendToServer(
                body = fallbackBody,
                messageType = type,
                mediaUrl = upload.url,
                duration = duration,
                fileName = upload.file_name,
                fileSize = upload.file_size.toString()
            )
        }
    }

    // Media Launchers
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            sendMediaMessage(it, "image", "Photo")
            showAttachments = false
        }
    }
    
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                sendMediaMessage(uri, "image", "Photo from Camera")
            }
        }
        showAttachments = false
    }

    fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            sendMediaMessage(it, "video", "Video")
            showAttachments = false
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            sendMediaMessage(it, "document", "Document")
            showAttachments = false
        }
    }
    
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            sendMediaMessage(it, "audio", "Audio File", "0:30")
            showAttachments = false
        }
    }

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let {
            val contactData = getContactDetails(context, it)
            sendToServer(
                body = "Contact",
                messageType = "contact",
                contactName = contactData.first,
                contactPhone = contactData.second
            )
            showAttachments = false
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                location?.let {
                    sendToServer(
                        body = "Location Shared",
                        messageType = "location",
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                    showAttachments = false
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.mp3")
            audioFile = file
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recordingStartMs = System.currentTimeMillis()
            isRecording = true

            // Vibration feedback
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopRecording(cancelled: Boolean = false) {
        try {
            recorder?.apply { stop(); release() }
            recorder = null
            isRecording = false
            
            if (!cancelled) {
                val seconds = ((System.currentTimeMillis() - recordingStartMs) / 1000L).coerceAtLeast(1L)
                val durationLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
                audioFile?.let {
                    sendMediaMessage(Uri.fromFile(it), "audio", "Voice Note", durationLabel)
                }
            } else {
                audioFile?.delete()
                showToast("Recording cancelled")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    val chatSearchResults by viewModel.chatSearchResults.collectAsState()

    LaunchedEffect(chatSearchQuery) {
        if (isSearchMode) {
            viewModel.searchInChat(chat.id, chatSearchQuery)
        }
    }

    val groupedMessages = remember(messages, chatSearchQuery, chatSearchResults) {
        val filtered = if (chatSearchQuery.isBlank()) messages else chatSearchResults
        filtered.groupBy { msg ->
            val date = com.example.firstapp.util.parseIso(msg.time) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            val now = Calendar.getInstance()
            
            val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && 
                          now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && 
                              yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

            when {
                isToday -> "Today"
                isYesterday -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchMode) {
                        TextField(
                            value = chatSearchQuery,
                            onValueChange = { chatSearchQuery = it },
                            placeholder = { Text("Search messages...", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AppAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    } else if (selectedMessages.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showContactInfo = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(chat.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (chat.avatarUrl != null) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(chat.avatarUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = chat.senderName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chat.senderName, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val presenceText = when {
                                    chat.isGroup -> "Tap for group info"
                                    chat.isOnline -> "online"
                                    chat.time.isNotBlank() -> "last seen ${formatWhatsAppTime(chat.time)}"
                                    else -> "offline"
                                }
                                Text(
                                    text = presenceText,
                                    fontSize = 11.sp,
                                    color = if (chat.isOnline) AppAccent else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        Text("${selectedMessages.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    if (isSearchMode) {
                        IconButton(onClick = { 
                            isSearchMode = false
                            chatSearchQuery = ""
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    } else if (selectedMessages.isEmpty()) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    } else {
                        IconButton(onClick = { selectedMessages.clear() }) { Icon(Icons.Default.Close, "Cancel", tint = Color.White) }
                    }
                },
                actions = {
                    if (isSearchMode) {
                        if (chatSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { chatSearchQuery = "" }) { Icon(Icons.Default.Clear, "Clear", tint = Color.White) }
                        }
                    } else if (selectedMessages.isEmpty()) {
                        IconButton(onClick = { isSearchMode = true }) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
                        IconButton(onClick = { startOutgoingCall(CallType.VIDEO) }) { Icon(Icons.Filled.VideoCall, "Video", tint = Color.White) }
                        IconButton(onClick = { startOutgoingCall(CallType.VOICE) }) { Icon(Icons.Filled.Call, "Call", tint = Color.White) }
                        Box {
                            IconButton(onClick = { showChatMenu = true }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
                            DropdownMenu(
                                expanded = showChatMenu,
                                onDismissRequest = { showChatMenu = false },
                                modifier = Modifier.background(AppSecondary)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View contact", color = Color.White) },
                                    onClick = { showChatMenu = false; showContactInfo = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Shared media", color = Color.White) },
                                    onClick = { showChatMenu = false; onShowSharedMedia() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search", color = Color.White) },
                                    onClick = { showChatMenu = false; isSearchMode = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear chat", color = Color.White) },
                                    onClick = { 
                                        showChatMenu = false
                                        scope.launch { repository.clearMessages(chat.id) }
                                    }
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { deleteSelectedMessages() }) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
                        IconButton(onClick = {
                            if (selectedMessages.size == 1) {
                                val msg = messages.find { it.id == selectedMessages.first() }
                                replyingToMessage = msg
                                selectedMessages.clear()
                            }
                        }) { Icon(Icons.Default.Reply, "Reply", tint = Color.White) }
                        IconButton(onClick = {
                            val msgId = selectedMessages.firstOrNull()
                            if (msgId != null) {
                                val msg = messages.find { it.id == msgId }
                                viewModel.starMessage(msgId, !(msg?.isStarred ?: false))
                            }
                            selectedMessages.clear()
                        }) { Icon(Icons.Default.Star, "Star", tint = Color.Yellow) }
                        IconButton(onClick = { showReactionPicker = true }) { Icon(Icons.Default.AddReaction, "React", tint = Color.White) }
                        IconButton(onClick = {
                            if (selectedMessages.size == 1) {
                                val msg = messages.find { it.id == selectedMessages.first() }
                                infoMessage = msg
                                selectedMessages.clear()
                            }
                        }) { Icon(Icons.Default.Info, "Info", tint = Color.White) }
                        IconButton(onClick = {
                            val textToCopy = messages.filter { selectedMessages.contains(it.id) }.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Messages", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            selectedMessages.clear()
                            showToast("Copied to clipboard")
                        }) { Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White) }
                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { selectedMessages.contains(it.id) }
                            messagesToForward.clear()
                            messagesToForward.addAll(selectedMsgs)
                            showForwardDialog = true
                            selectedMessages.clear()
                        }) { Icon(Icons.Default.Forward, "Forward", tint = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(AppSecondary).navigationBarsPadding().imePadding()) {
                if (chat.isTyping) {
                    Text(
                        text = "typing...",
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                        color = AppAccent,
                        fontSize = 12.sp
                    )
                }
                if (showEmojiPicker) {
                    EmojiPicker(onEmojiSelect = { messageText += it })
                }
                if (showAttachments) {
                    AttachmentPanel(
                        onImageClick = { imageLauncher.launch("image/*") },
                        onVideoClick = { videoLauncher.launch("video/*") },
                        onDocClick = { documentLauncher.launch("*/*") },
                        onContactClick = { contactLauncher.launch(null) },
                        onCameraClick = { 
                            val uri = createImageFile()
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        },
                        onAudioClick = { audioPickerLauncher.launch("audio/*") },
                        onLocationClick = {
                            val locPermission = Manifest.permission.ACCESS_FINE_LOCATION
                            if (ContextCompat.checkSelfPermission(context, locPermission) == PackageManager.PERMISSION_GRANTED) {
                                fetchLocation()
                            } else {
                                permissionLauncher.launch(locPermission)
                            }
                        }
                    )
                }
                
                replyingToMessage?.let { replyMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppSecondary)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(AppAccent))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (replyMsg.isFromMe) "You" else chat.senderName,
                                color = AppAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = replyMsg.text.ifBlank { replyMsg.type.name },
                                color = AppTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                editingMessage?.let { editMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppSecondary)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Edit message",
                                color = AppAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = editMsg.text,
                                color = AppTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { 
                            editingMessage = null 
                            messageText = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                ChatBottomBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            if (editingMessage != null) {
                                val msgId = editingMessage!!.id
                                val newText = messageText.trim()
                                editingMessage = null
                                messageText = ""
                                scope.launch {
                                    repository.editMessage(msgId, newText).collect { resource ->
                                        if (resource is Resource.Error) {
                                            showToast("Failed to edit: ${resource.message}")
                                        }
                                        // Success handled by websocket or manual refresh in production
                                    }
                                }
                            } else {
                                sendToServer(
                                    body = messageText.trim(),
                                    messageType = "text"
                                )
                                messageText = ""
                            }
                        }
                    },
                    onAttachClick = { 
                        showAttachments = !showAttachments
                        showEmojiPicker = false
                    },
                    onEmojiClick = {
                        showEmojiPicker = !showEmojiPicker
                        showAttachments = false
                    },
                    isRecording = isRecording,
                    recordingDurationSeconds = recordingDurationSeconds,
                    onMicClick = {
                        val recordPermission = Manifest.permission.RECORD_AUDIO
                        if (ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED) {
                            if (isRecording) stopRecording() else startRecording()
                        } else {
                            permissionLauncher.launch(recordPermission)
                        }
                    },
                    onMicDragCancel = { stopRecording(cancelled = true) },
                    onMicDragRelease = { stopRecording(cancelled = false) },
                    onCameraIconClick = {
                        val cameraPermission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                            val uri = createImageFile()
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(cameraPermission)
                        }
                    }
                )
            }
        },
        containerColor = AppBackground
    ) { padding ->
        val wallpaperColor = remember(chat.wallpaperUrl) {
            chat.wallpaperUrl?.let { 
                try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
            }
        }

        Box(modifier = Modifier.padding(padding).fillMaxSize().background(wallpaperColor ?: Color.Transparent)) {
            LazyColumn(
                state = messagesLazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (messages.isEmpty() && !isPaginationLoading && chatSearchQuery.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 100.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = CircleShape,
                                    color = AppSecondary.copy(alpha = 0.5f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Message,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = AppAccent.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "No messages here yet",
                                    color = AppTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Say hello to ${chat.senderName}!",
                                    color = AppTextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (!isEndReached && chatSearchQuery.isBlank() && messages.isNotEmpty()) {
                    item {
                        LaunchedEffect(Unit) {
                            loadMoreMessages()
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = AppAccent)
                        }
                    }
                }

                groupedMessages.forEach { (date, msgs) ->
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = date,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    items(msgs, key = { it.id }) { message ->
                        val isSelected = selectedMessages.contains(message.id)
                        MessageBubble(
                            message = message, 
                            isGroup = chat.isGroup,
                            isSelected = isSelected,
                            modifier = Modifier.animateItemPlacement(),
                            onLongClick = {
                                if (!isSelected) {
                                    selectedMessages.add(message.id)
                                }
                            },
                            onReply = { replyingToMessage = message },
                            onClick = {
                                if (selectedMessages.isNotEmpty()) {
                                    if (isSelected) {
                                        selectedMessages.remove(message.id)
                                    } else {
                                        selectedMessages.add(message.id)
                                    }
                                }
                            },
                            onVideoClick = { uri -> fullScreenVideoUri = uri },
                            onImageClick = { uri -> fullScreenImageUri = uri }
                        ) 
                    }
                }
            }
            
            fullScreenVideoUri?.let { uri ->
                FullScreenVideoPlayer(uri = uri, onBack = { fullScreenVideoUri = null })
            }
            fullScreenImageUri?.let { uri ->
                FullScreenImagePlayer(uri = uri, onBack = { fullScreenImageUri = null })
            }

            if (showReactionPicker) {
                ReactionPickerOverlay(
                    onDismiss = { showReactionPicker = false },
                    onEmojiSelect = { emoji ->
                        val msgId = selectedMessages.firstOrNull()
                        if (msgId != null) {
                            onReact(msgId, emoji)
                        }
                        selectedMessages.clear()
                        showReactionPicker = false
                    }
                )
            }

            infoMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { infoMessage = null },
                    containerColor = AppSecondary,
                    title = { Text("Message Info", color = Color.White) },
                    text = {
                        Column {
                            Text("Sent: ${com.example.firstapp.util.formatWhatsAppTime(msg.time)}", color = AppTextPrimary)
                            if (msg.isRead) {
                                Text("Read: Yes", color = AppAccent)
                            } else {
                                Text("Read: No", color = AppTextSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Type: ${msg.type.name}", color = AppTextSecondary, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { infoMessage = null }) {
                            Text("Close", color = AppAccent)
                        }
                    }
                )
            }

            if (showForwardDialog) {
                ForwardDialog(
                    chats = (viewModel.chats.collectAsState().value as? Resource.Success)?.data ?: emptyList(),
                    onDismiss = { showForwardDialog = false },
                    onForward = { selectedChats ->
                        scope.launch {
                            selectedChats.forEach { targetChat ->
                                messagesToForward.forEach { msg ->
                                    viewModel.sendMessage(
                                        chatId = targetChat.id,
                                        body = msg.text,
                                        messageType = when(msg.type) {
                                            MessageType.IMAGE -> "image"
                                            MessageType.VIDEO -> "video"
                                            MessageType.AUDIO_MESSAGE -> "audio"
                                            MessageType.LOCATION -> "location"
                                            MessageType.CONTACT -> "contact"
                                            MessageType.DOCUMENT -> "document"
                                            else -> "text"
                                        },
                                        mediaUrl = msg.mediaUri,
                                        fileName = msg.fileName,
                                        fileSize = msg.fileSize,
                                        contactName = msg.contactName,
                                        contactPhone = msg.contactPhone,
                                        latitude = msg.latitude,
                                        longitude = msg.longitude
                                    )
                                }
                            }
                            showForwardDialog = false
                            messagesToForward.clear()
                            showToast("Messages forwarded")
                        }
                    }
                )
            }

            val showScrollToBottom by remember {
                derivedStateOf {
                    messagesLazyListState.firstVisibleItemIndex < messages.size - 10
                }
            }
            if (showScrollToBottom) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            messagesLazyListState.animateScrollToItem(messagesLazyListState.layoutInfo.totalItemsCount)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 20.dp, end = 16.dp)
                        .size(40.dp),
                    containerColor = AppSecondary,
                    contentColor = AppAccent,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Scroll to bottom", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

        if (isOutgoingRinging || isCallConnected) {
            ActiveCallScreen(
                callerName = chat.senderName,
                isVideoCall = activeCallType == CallType.VIDEO,
                isConnected = isCallConnected,
                callDurationSeconds = callDurationSeconds,
                webRtcManager = webRtcManager,
                isMicMuted = isMicMuted,
                isSpeakerEnabled = isSpeakerEnabled,
                isCameraEnabled = isCameraEnabled,
                onToggleMic = {
                    isMicMuted = !isMicMuted
                    webRtcManager.setMicEnabled(!isMicMuted)
                },
                onToggleSpeaker = {
                    isSpeakerEnabled = !isSpeakerEnabled
                    webRtcManager.setSpeakerEnabled(isSpeakerEnabled)
                },
                onToggleCamera = {
                    isCameraEnabled = !isCameraEnabled
                    webRtcManager.setCameraEnabled(isCameraEnabled)
                },
                onSwitchCamera = { webRtcManager.switchCamera() },
                onEnd = { endActiveCall() }
            )
        }

        incomingCallEvent?.let { event ->
            IncomingCallScreen(
                callerName = chat.senderName,
                isVideoCall = event.callType == CallType.VIDEO,
                onAccept = { acceptIncomingCall() },
                onDecline = { rejectIncomingCall() }
            )
        }

        if (showContactInfo) {
            ContactInfoOverlay(
                chat = chat,
                onBack = { showContactInfo = false },
                onShowSharedMedia = {
                    showContactInfo = false
                    onShowSharedMedia()
                },
                onVideoCall = {
                    showContactInfo = false
                    startOutgoingCall(CallType.VIDEO)
                },
                onVoiceCall = {
                    showContactInfo = false
                    startOutgoingCall(CallType.VOICE)
                },
                onMute = { viewModel.muteChat(chat.id, it) },
                onClearChat = { 
                    viewModel.clearChat(chat.id)
                    showContactInfo = false
                },
                onBlock = {
                    showToast("Contact blocked")
                    showContactInfo = false
                },
                onSetWallpaper = { viewModel.updateWallpaper(chat.id, it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoOverlay(
    chat: ChatMessage,
    onBack: () -> Unit,
    onShowSharedMedia: () -> Unit,
    onVideoCall: () -> Unit,
    onVoiceCall: () -> Unit,
    onMute: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onBlock: () -> Unit,
    onSetWallpaper: (String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary, titleContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(chat.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.senderName.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(chat.senderName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Text(if (chat.isOnline) "Online" else "Last seen ${formatWhatsAppTime(chat.time)}", fontSize = 14.sp, color = AppTextSecondary)
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InfoActionButton(icon = Icons.Default.Call, label = "Audio", onClick = onVoiceCall)
                InfoActionButton(icon = Icons.Default.Videocam, label = "Video", onClick = onVideoCall)
                InfoActionButton(icon = Icons.Default.Search, label = "Search", onClick = { /* already in top bar */ })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = AppSecondary, thickness = 8.dp)
            
            ListItem(
                headlineContent = { Text("Shared Media", color = AppTextPrimary) },
                leadingContent = { Icon(Icons.Default.PermMedia, null, tint = AppTextSecondary) },
                modifier = Modifier.clickable { onShowSharedMedia() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = AppSecondary, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            
            ListItem(
                headlineContent = { Text("Mute Notifications", color = AppTextPrimary) },
                leadingContent = { Icon(Icons.Default.NotificationsOff, null, tint = AppTextSecondary) },
                trailingContent = { Switch(checked = chat.isMuted, onCheckedChange = { onMute(it) }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = AppSecondary, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Wallpaper", color = AppTextPrimary) },
                leadingContent = { Icon(Icons.Default.Wallpaper, null, tint = AppTextSecondary) },
                modifier = Modifier.clickable { 
                    // Simple logic: cycle through some solid colors for now or clear
                    val options = listOf(null, "#FFCDD2", "#C8E6C9", "#BBDEFB", "#FFF9C4")
                    val currentIdx = options.indexOf(chat.wallpaperUrl)
                    val nextIdx = (currentIdx + 1) % options.size
                    onSetWallpaper(options[nextIdx])
                },
                supportingContent = { Text(if (chat.wallpaperUrl == null) "Default" else "Custom color set", color = AppTextSecondary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = AppSecondary, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            
            ListItem(
                headlineContent = { Text("Clear Chat", color = AppTextPrimary) },
                leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = AppTextSecondary) },
                modifier = Modifier.clickable { onClearChat() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = AppSecondary, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("Encryption", color = AppTextPrimary) },
                supportingContent = { Text("Messages and calls are end-to-end encrypted.", color = AppTextSecondary) },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = AppTextSecondary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            HorizontalDivider(color = AppSecondary, thickness = 8.dp)
            
            ListItem(
                headlineContent = { Text("Block ${chat.senderName}", color = Color.Red) },
                leadingContent = { Icon(Icons.Default.Block, null, tint = Color.Red) },
                modifier = Modifier.clickable { onBlock() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text("Report ${chat.senderName}", color = Color.Red) },
                leadingContent = { Icon(Icons.Default.ThumbDown, null, tint = Color.Red) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun InfoActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, contentDescription = label, tint = AppAccent, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = AppAccent, fontSize = 12.sp)
    }
}

@Composable
private fun IncomingCallScreen(
    callerName: String,
    isVideoCall: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AppPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(callerName, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (isVideoCall) "Incoming video call" else "Incoming voice call",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = Color.White, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppAccent)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accept", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ActiveCallScreen(
    callerName: String,
    isVideoCall: Boolean,
    isConnected: Boolean,
    callDurationSeconds: Int,
    webRtcManager: WebRtcCallManager,
    isMicMuted: Boolean,
    isSpeakerEnabled: Boolean,
    isCameraEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEnd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isVideoCall && isConnected) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    org.webrtc.SurfaceViewRenderer(ctx).apply {
                        webRtcManager.attachRemoteRenderer(this)
                    }
                },
                onRelease = { 
                    webRtcManager.detachRemoteRenderer()
                    it.release() 
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 12.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.webrtc.SurfaceViewRenderer(ctx).apply {
                            webRtcManager.attachLocalRenderer(this)
                        }
                    },
                    onRelease = { 
                        webRtcManager.detachLocalRenderer()
                        it.release() 
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVideoCall || !isConnected) {
                val infiniteTransition = rememberInfiniteTransition(label = "ring")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (!isConnected) 1.15f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(AppPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(callerName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isConnected) formatCallDuration(callDurationSeconds) else "Ringing...",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
                .padding(bottom = 48.dp, top = 64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = "Mute",
                    active = isMicMuted,
                    activeColor = Color.Red.copy(alpha = 0.8f),
                    onClick = onToggleMic
                )
                
                if (isVideoCall) {
                    CallControlButton(
                        icon = Icons.Default.FlipCameraAndroid,
                        label = "Flip",
                        active = false,
                        activeColor = Color.Transparent,
                        onClick = onSwitchCamera
                    )
                    CallControlButton(
                        icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = "Video",
                        active = !isCameraEnabled,
                        activeColor = Color.Red.copy(alpha = 0.8f),
                        onClick = onToggleCamera
                    )
                } else {
                    CallControlButton(
                        icon = if (isSpeakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                        label = "Speaker",
                        active = isSpeakerEnabled,
                        activeColor = AppAccent.copy(alpha = 0.8f),
                        onClick = onToggleSpeaker
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onEnd,
                        modifier = Modifier.size(64.dp),
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End", modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("End", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (active) activeColor else Color.White.copy(alpha = 0.15f),
            tonalElevation = if (active) 4.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun FullScreenImagePlayer(uri: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        androidx.activity.compose.BackHandler {
            onBack()
        }
    }
}

@Composable
fun FullScreenVideoPlayer(uri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        // Handle physical back button for full screen video
        androidx.activity.compose.BackHandler {
            onBack()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message, 
    modifier: Modifier = Modifier,
    isGroup: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onReply: () -> Unit = {},
    onVideoClick: (String) -> Unit, 
    onImageClick: (String) -> Unit
) {
    val bubbleColor = if (isSelected) AppAccent.copy(alpha = 0.3f) else if (message.isFromMe) AppPrimary else AppSecondary
    val textColor = AppTextPrimary
    val context = LocalContext.current
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) AppAccent.copy(alpha = 0.1f) else Color.Transparent)
            .padding(vertical = 1.dp, horizontal = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, 100f)
                    },
                    onDragEnd = {
                        if (swipeOffset >= 80f) onReply()
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f }
                )
            }
            .offset { IntOffset(swipeOffset.roundToInt(), 0) },
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (swipeOffset > 0) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = null,
                tint = AppAccent,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
                    .scale(swipeOffset / 80f)
            )
        }

        if (!message.isFromMe && isGroup) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppAccent.copy(alpha = 0.2f))
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderAvatarUrl != null) {
                    AsyncImage(
                        model = message.senderAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.senderName?.take(1)?.uppercase() ?: "?",
                        color = AppAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Column {
                    if (isGroup && !message.isFromMe && message.senderName != null) {
                        Text(
                            text = message.senderName,
                            color = AppAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (message.replyToId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .background(AppAccent, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.replyToSenderName ?: "Unknown",
                                    color = AppAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = message.replyToText ?: "",
                                    color = AppTextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.text, 
                                color = textColor, 
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        MessageType.IMAGE -> {
                            Column(modifier = Modifier.clickable { 
                                if (isSelected) onClick() else message.mediaUri?.let { onImageClick(it) } 
                            }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.1f))
                                ) {
                                    AsyncImage(
                                        model = message.mediaUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                if (message.text.isNotEmpty()) {
                                    Text(
                                        text = message.text,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                                    )
                                }
                            }
                        }
                        MessageType.VIDEO -> {
                            Column(modifier = Modifier.clickable { 
                                if (isSelected) onClick() else message.mediaUri?.let { onVideoClick(it) } 
                            }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = message.mediaUri, // Thumbnail if available, or uri for player
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().alpha(0.7f),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = Color.White
                                    )
                                }
                                if (message.text.isNotEmpty()) {
                                    Text(
                                        text = message.text,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                                    )
                                }
                            }
                        }
                        MessageType.CONTACT -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(message.contactName ?: "Unknown", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(message.contactPhone ?: "", color = AppTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        MessageType.DOCUMENT -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                message.mediaUri?.let {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(it)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                }
                            }) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(message.fileName ?: "File", color = Color.White)
                            }
                        }
                        MessageType.LOCATION -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Shared Location", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Lat: ${message.latitude}, Lon: ${message.longitude}", color = AppTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        MessageType.VOICE_CALL -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF374045)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = AppTextPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Voice call", color = textColor, fontWeight = FontWeight.Bold)
                                    Text(message.duration ?: "", color = AppTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        MessageType.AUDIO_MESSAGE -> {
                            VoiceNoteBubbleContent(
                                message = message,
                                isSelected = isSelected,
                                onClick = onClick
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.reactions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                message.reactions.forEach { (emoji, count) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = emoji, fontSize = 12.sp)
                                        if (count > 1) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(text = count.toString(), color = AppTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = formatMessageTime(message.time),
                            color = AppTextSecondary,
                            fontSize = 11.sp
                        )
                        if (message.isStarred) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Yellow,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (message.isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = null,
                                tint = if (message.isRead) AppAccent else AppTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onEmojiClick: () -> Unit,
    isRecording: Boolean,
    recordingDurationSeconds: Int = 0,
    onMicClick: () -> Unit,
    onMicDragCancel: () -> Unit = {},
    onMicDragRelease: () -> Unit = {},
    onCameraIconClick: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppSecondary)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                if (isRecording) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Row(
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60),
                            color = AppTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "< Slide to cancel",
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
                        )
                    }
                } else {
                IconButton(onClick = onEmojiClick) {
                    Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji", tint = AppTextSecondary, modifier = Modifier.size(26.dp))
                }
                
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Message", color = AppTextSecondary, fontSize = 16.sp) },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(color = AppTextPrimary, fontSize = 16.sp),
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AppAccent,
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary
                    )
                )
                
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = AppTextSecondary, modifier = Modifier.size(24.dp))
                }
                
                if (text.isEmpty()) {
                    IconButton(onClick = onCameraIconClick) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = AppTextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        
        val recordButtonModifier = if (text.isBlank()) {
            Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { if (!isRecording) onMicClick() },
                    onDragEnd = {
                        if (offsetX < -200) {
                            onMicDragCancel()
                        } else {
                            onMicDragRelease()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        onMicDragCancel()
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        if (offsetX > 0) offsetX = 0f
                    }
                )
            }
        } else Modifier

        FloatingActionButton(
            onClick = { if (text.isNotBlank()) onSend() else onMicClick() },
            modifier = Modifier.size(48.dp).then(recordButtonModifier),
            containerColor = AppAccent,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            val icon = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic
            Icon(
                imageVector = icon, 
                contentDescription = if (text.isNotBlank()) "Send" else "Record",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(onEmojiSelect: (String) -> Unit) {
    val emojis = listOf("😀", "😂", "😍", "👍", "🙏", "🔥", "💯", "❤️", "🙌", "😎", "🤔", "😊", "✨", "🎉", "🤣", "🥺")
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSecondary),
        shape = RoundedCornerShape(16.dp)
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(48.dp),
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(emojis.size) { index ->
                Box(
                    modifier = Modifier.size(48.dp).clickable { onEmojiSelect(emojis[index]) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emojis[index], fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun AttachmentPanel(onImageClick: () -> Unit, onVideoClick: () -> Unit, onDocClick: () -> Unit, onContactClick: () -> Unit, onCameraClick: () -> Unit, onAudioClick: () -> Unit, onLocationClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSecondary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val rows = listOf(
                listOf(AttachmentItem("Document", Icons.Default.Description, AppPrimary, onDocClick), AttachmentItem("Camera", Icons.Default.CameraAlt, Color(0xFFEC407A), onCameraClick), AttachmentItem("Gallery", Icons.Default.Image, Color(0xFF42A5F5), onImageClick), AttachmentItem("Audio", Icons.Default.Headset, AppAccent, onAudioClick)),
                listOf(AttachmentItem("Location", Icons.Default.LocationOn, Color(0xFF2196F3), onLocationClick), AttachmentItem("Contact", Icons.Default.Person, Color(0xFF7E57C2), onContactClick), AttachmentItem("Video", Icons.Default.Videocam, Color(0xFFE91E63), onVideoClick))
            )
            
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    row.forEach { item ->
                        Column(
                            modifier = Modifier.weight(1f).clickable { item.onClick() },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(item.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.label, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.label, color = AppTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class AttachmentItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

private fun getContactDetails(context: Context, contactUri: Uri): Pair<String, String> {
    var name = ""
    var phone = ""
    val cursor = context.contentResolver.query(contactUri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val hasPhone = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
            if (hasPhone) {
                val pCursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(id), null)
                pCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        phone = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }
            }
        }
    }
    return Pair(name, phone)
}

@Composable
fun ReactionPickerOverlay(
    onDismiss: () -> Unit,
    onEmojiSelect: (String) -> Unit
) {
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AppSecondary,
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                emojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.clickable { onEmojiSelect(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
fun ForwardDialog(
    chats: List<ChatMessage>,
    onDismiss: () -> Unit,
    onForward: (List<ChatMessage>) -> Unit
) {
    val selectedChats = remember { mutableStateListOf<ChatMessage>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Forward to...", color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(chats) { chat ->
                    val isSelected = selectedChats.any { it.id == chat.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedChats.removeAll { it.id == chat.id }
                                else selectedChats.add(chat)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(chat.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chat.avatarUrl != null) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(chat.avatarUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(chat.senderName.take(1).uppercase(), color = Color.White)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(chat.senderName, color = Color.White, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onForward(selectedChats.toList()) },
                enabled = selectedChats.isNotEmpty()
            ) {
                Text("Forward", color = if (selectedChats.isNotEmpty()) AppAccent else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun VoiceNoteBubbleContent(
    message: Message,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isSelected) onClick() else isPlaying = !isPlaying }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (message.isFromMe) Color.White.copy(alpha = 0.2f) else AppPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (message.isFromMe) Color.White else AppAccent,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Simulated waveform or simple progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Waveform Placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(15) { index ->
                        val height = remember { (10..25).random().dp }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(height)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (progress > index / 15f) 
                                        (if (message.isFromMe) Color.White else AppAccent)
                                    else 
                                        (if (message.isFromMe) Color.White.copy(alpha = 0.3f) else AppTextSecondary.copy(alpha = 0.3f))
                                )
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) "0:02" else message.duration ?: "0:00",
                    color = if (message.isFromMe) Color.White.copy(alpha = 0.7f) else AppTextSecondary,
                    fontSize = 11.sp
                )
                if (!message.isFromMe) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = AppAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DateHeader(timestamp: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
        ) {
            Text(
                text = com.example.firstapp.util.formatChatDateHeader(timestamp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    FirstAppTheme {
        ChatScreen(
            chat = ChatMessage(
                id = 1,
                senderName = "Joel",
                lastMessage = "Hello!",
                time = "10:00",
                avatarColor = Color(0xFF8B4513)
            ),
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            messagesState = Resource.Success(emptyList()),
            onBack = {},
            onShowSharedMedia = {},
            onTyping = { _, _ -> },
            onPresence = { _, _, _ -> },
            onNewMessage = {}
        )
    }
}
