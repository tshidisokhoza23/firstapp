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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.firstapp.util.formatMessageTime
import com.example.firstapp.util.formatWhatsAppTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.provider.OpenableColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: ChatMessage,
    onBack: () -> Unit,
    onShowSharedMedia: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AppRepositoryImpl() }

    val messages = remember { mutableStateListOf<Message>() }
    val messagesScrollState = rememberScrollState()
    var isWebSocketConnected by remember { mutableStateOf(false) }
    var isLoadingMessages by remember { mutableStateOf(true) }
    var showChatMenu by remember { mutableStateOf(false) }
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
    val webRtcManager = remember { WebRtcCallManager(context) }
    val ringtone = remember { mutableStateOf<Ringtone?>(null) }

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
    LaunchedEffect(globalIncomingCall?.sessionId, chat.id) {
        val pending = globalIncomingCall ?: return@LaunchedEffect
        if (pending.senderId == chat.id) {
            incomingCallEvent = pending
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

    fun bindWebRtcSignal(sessionId: String, callType: CallType) {
        webRtcManager.onSignal = { signalType, payload ->
            CallWebSocketClient.signal(
                receiverId = chat.id,
                sessionId = sessionId,
                callType = callType,
                signalType = signalType,
                signalPayload = payload
            )
        }
    }

    // Load initial messages
    LaunchedEffect(chat.id) {
        repository.getMessages(chat.id).collect { resource ->
            when (resource) {
                is com.example.firstapp.util.Resource.Success -> {
                    messages.clear()
                    messages.addAll(resource.data ?: emptyList())
                    isLoadingMessages = false
                }
                is com.example.firstapp.util.Resource.Error -> {
                    Toast.makeText(context, "Failed to load messages", Toast.LENGTH_SHORT).show()
                    isLoadingMessages = false
                }
                is com.example.firstapp.util.Resource.Loading -> {
                    isLoadingMessages = true
                }
            }
        }
    }

    // WebSocket connection
    LaunchedEffect(chat.id) {
        val socketListener = object : ChatSocketListener {
            override fun onOpen() {
                isWebSocketConnected = true
            }

            override fun onMessage(message: Message) {
                messages.add(message)
            }

            override fun onError(error: String) {
                isWebSocketConnected = false
                showToast("Connection error: $error")
            }
        }

        ChatWebSocketClient.connect(chat.id, socketListener)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            messagesScrollState.animateScrollTo(messagesScrollState.maxValue)
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
                                isOutgoingRinging = false
                                isCallConnected = true
                                callStartedAtMs = System.currentTimeMillis()
                                callDurationSeconds = 0
                                webRtcManager.createOffer()
                            }
                        }
                        "call_reject" -> {
                            if (activeCallSessionId == event.sessionId) {
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
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val rt = RingtoneManager.getRingtone(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt.isLooping = true
                }
                rt.play()
                ringtone.value = rt

                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)
                }
            } catch (_: Exception) {
            }
        } else {
            try {
                ringtone.value?.stop()
                ringtone.value = null
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            } catch (_: Exception) {
            }
        }
    }

    fun startOutgoingCall(callType: CallType) {
        if (activeCallSessionId != null) {
            return
        }
        if (!ensureCallPermissions(callType)) {
            return
        }

        val sessionId = "${System.currentTimeMillis()}-${chat.id}"
        activeCallSessionId = sessionId
        activeCallType = callType
        isOutgoingRinging = true
        isCallConnected = false
        callDurationSeconds = 0
        callStartedAtMs = 0L
        isMicMuted = false
        isSpeakerEnabled = callType == CallType.VIDEO
        isCameraEnabled = true

        webRtcManager.start(callType)
        bindWebRtcSignal(sessionId, callType)
        CallWebSocketClient.invite(receiverId = chat.id, sessionId = sessionId, callType = callType)
    }

    fun acceptIncomingCall() {
        val call = incomingCallEvent ?: return
        if (!ensureCallPermissions(call.callType)) {
            return
        }
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
        bindWebRtcSignal(call.sessionId, call.callType)
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

    // Auto-accept when the user tapped Accept in the notification.
    LaunchedEffect(autoAcceptSessionId, incomingCallEvent?.sessionId) {
        val targetSession = autoAcceptSessionId ?: return@LaunchedEffect
        val call = incomingCallEvent ?: return@LaunchedEffect
        if (call.sessionId == targetSession) {
            GlobalCallState.clearAutoAccept()
            acceptIncomingCall()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ChatWebSocketClient.close()
            webRtcManager.release()
            try {
                ringtone.value?.stop()
                ringtone.value = null
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
                longitude = longitude
            ).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { messages.add(it) }
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopRecording() {
        try {
            recorder?.apply { stop(); release() }
            recorder = null
            isRecording = false
            val seconds = ((System.currentTimeMillis() - recordingStartMs) / 1000L).coerceAtLeast(1L)
            val durationLabel = String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
            audioFile?.let {
                sendMediaMessage(Uri.fromFile(it), "audio", "Voice Note", durationLabel)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(chat.avatarColor))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(chat.senderName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val presenceText = when {
                                chat.isOnline -> "online"
                                chat.time.isNotBlank() -> "offline • last seen ${formatWhatsAppTime(chat.time)}"
                                else -> "offline"
                            }
                            Text(
                                text = presenceText,
                                fontSize = 12.sp,
                                color = if (chat.isOnline) AppAccent else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { startOutgoingCall(CallType.VIDEO) }) { Icon(Icons.Filled.VideoCall, "Video") }
                    IconButton(onClick = { startOutgoingCall(CallType.VOICE) }) { Icon(Icons.Filled.Call, "Call") }
                    Box {
                        IconButton(onClick = { showChatMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(
                            expanded = showChatMenu,
                            onDismissRequest = { showChatMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh messages") },
                                onClick = {
                                    showChatMenu = false
                                    scope.launch {
                                        repository.getMessages(chat.id).collect { resource ->
                                            when (resource) {
                                                is com.example.firstapp.util.Resource.Success -> {
                                                    messages.clear()
                                                    messages.addAll(resource.data ?: emptyList())
                                                    isLoadingMessages = false
                                                }
                                                is com.example.firstapp.util.Resource.Error -> {
                                                    Toast.makeText(context, "Failed to refresh messages", Toast.LENGTH_SHORT).show()
                                                    isLoadingMessages = false
                                                }
                                                is com.example.firstapp.util.Resource.Loading -> {
                                                    isLoadingMessages = true
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Shared media") },
                                onClick = {
                                    showChatMenu = false
                                    onShowSharedMedia()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear current view") },
                                onClick = {
                                    showChatMenu = false
                                    messages.clear()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary, titleContentColor = AppTextPrimary)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(AppSecondary).navigationBarsPadding().imePadding()) {
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
                ChatBottomBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            sendToServer(
                                body = messageText.trim(),
                                messageType = "text"
                            )
                            messageText = ""
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
                    onMicClick = {
                        val recordPermission = Manifest.permission.RECORD_AUDIO
                        if (ContextCompat.checkSelfPermission(context, recordPermission) == PackageManager.PERMISSION_GRANTED) {
                            if (isRecording) stopRecording() else startRecording()
                        } else {
                            permissionLauncher.launch(recordPermission)
                        }
                    },
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(messagesScrollState)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                messages.forEach { message ->
                    MessageBubble(
                        message = message, 
                        onVideoClick = { uri -> fullScreenVideoUri = uri },
                        onImageClick = { uri -> fullScreenImageUri = uri }
                    ) 
                }
            }
            
            fullScreenVideoUri?.let { uri ->
                FullScreenVideoPlayer(uri = uri, onBack = { fullScreenVideoUri = null })
            }
            fullScreenImageUri?.let { uri ->
                FullScreenImagePlayer(uri = uri, onBack = { fullScreenImageUri = null })
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
            .background(Color(0xFF0D1B2A))
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
                        .background(Color(0xFF1E88E5)),
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
                            .background(Color(0xFF43A047))
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
                onRelease = { it.release() }
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
                    onRelease = { it.release() }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1C3A58))))
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
                        .background(Color(0xFF1E88E5)),
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
                .background(Color.Black.copy(alpha = if (isVideoCall && isConnected) 0.45f else 0.7f))
                .padding(bottom = 44.dp, top = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMicMuted) "Unmute" else "Mute",
                    active = isMicMuted,
                    activeColor = Color(0xFF8E24AA),
                    onClick = onToggleMic
                )
                CallControlButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Speaker",
                    active = isSpeakerEnabled,
                    activeColor = Color(0xFF00897B),
                    onClick = onToggleSpeaker
                )
                if (isVideoCall) {
                    CallControlButton(
                        icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (isCameraEnabled) "Camera" else "Cam Off",
                        active = !isCameraEnabled,
                        activeColor = Color(0xFF1565C0),
                        onClick = onToggleCamera
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onEnd,
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("End", color = Color.White, fontSize = 12.sp)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(if (active) activeColor else Color(0xFF37474F))
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
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

@Composable
fun MessageBubble(message: Message, onVideoClick: (String) -> Unit, onImageClick: (String) -> Unit) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) Color(0xFF005C4B) else AppSecondary
    val textColor = AppTextPrimary
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isFromMe) 12.dp else 0.dp,
                bottomEnd = if (message.isFromMe) 0.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Column {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(text = message.text, color = textColor, fontSize = 16.sp)
                        }
                        MessageType.IMAGE -> {
                            Column(modifier = Modifier.clickable { message.mediaUri?.let { onImageClick(it) } }) {
                                Image(
                                    painter = rememberAsyncImagePainter(message.mediaUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Photo", color = textColor, fontSize = 14.sp)
                            }
                        }
                        MessageType.VIDEO -> {
                            Column(modifier = Modifier.clickable { message.mediaUri?.let { onVideoClick(it) } }) {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp))) {
                                    val exoPlayer = remember {
                                        ExoPlayer.Builder(context).build().apply {
                                            message.mediaUri?.let { setMediaItem(MediaItem.fromUri(Uri.parse(it))) }
                                            prepare()
                                        }
                                    }
                                    DisposableEffect(exoPlayer) {
                                        onDispose { exoPlayer.release() }
                                    }
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Icon(
                                        Icons.Default.PlayCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Video", color = textColor)
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
                            val exoPlayer = remember {
                                ExoPlayer.Builder(context).build().apply {
                                    message.mediaUri?.let { setMediaItem(MediaItem.fromUri(Uri.parse(it))) }
                                    prepare()
                                }
                            }
                            DisposableEffect(exoPlayer) {
                                onDispose { exoPlayer.release() }
                            }
                            var isPlaying by remember { mutableStateOf(false) }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    isPlaying = !isPlaying
                                }) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = AppTextPrimary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.height(20.dp).width(100.dp).background(AppTextSecondary.copy(alpha = 0.3f)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(message.duration ?: "0:10", color = AppTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        text = formatMessageTime(message.time),
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
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
    onMicClick: () -> Unit,
    onCameraIconClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(25.dp))
                .background(AppSecondary)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEmojiClick) {
                Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji", tint = AppTextSecondary)
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message", color = AppTextSecondary) },
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
                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = AppTextSecondary)
            }
            if (text.isEmpty()) {
                IconButton(onClick = onCameraIconClick) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = AppTextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { if (text.isNotBlank()) onSend() else onMicClick() },
            modifier = Modifier.size(50.dp).clip(CircleShape).background(if (isRecording) Color.Red else AppAccent)
        ) {
            Icon(
                imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                contentDescription = "Send/Mic",
                tint = AppBackground
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
                listOf(AttachmentItem("Document", Icons.Default.Description, Color(0xFF7E57C2), onDocClick), AttachmentItem("Camera", Icons.Default.CameraAlt, Color(0xFFEC407A), onCameraClick), AttachmentItem("Gallery", Icons.Default.Image, Color(0xFF42A5F5), onImageClick), AttachmentItem("Audio", Icons.Default.Headset, Color(0xFFFFA726), onAudioClick)),
                listOf(AttachmentItem("Location", Icons.Default.LocationOn, Color(0xFF66BB6A), onLocationClick), AttachmentItem("Contact", Icons.Default.Person, Color(0xFF29B6F6), onContactClick), AttachmentItem("Video", Icons.Default.Videocam, Color(0xFFE91E63), onVideoClick))
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
            onBack = {},
            onShowSharedMedia = {}
        )
    }
}
