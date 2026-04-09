package com.example.firstapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.firstapp.data.realtime.ChatSocketListener
import com.example.firstapp.data.realtime.ChatWebSocketClient
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.UploadResponse
import com.example.firstapp.data.repository.AppRepositoryImpl
import com.example.firstapp.util.Resource
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.provider.OpenableColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chat: ChatMessage, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AppRepositoryImpl() }

    val messages = remember { mutableStateListOf<Message>() }
    var isWebSocketConnected by remember { mutableStateOf(false) }
    var isLoadingMessages by remember { mutableStateOf(true) }

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
                Toast.makeText(context, "Connection error: $error", Toast.LENGTH_SHORT).show()
            }
        }

        ChatWebSocketClient.connect(chat.id, socketListener)
    }

    DisposableEffect(Unit) {
        onDispose {
            ChatWebSocketClient.close()
        }
    }

    var showAttachments by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(chat.avatarColor))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(chat.senderName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isWebSocketConnected) "online" else "connecting...",
                                fontSize = 12.sp,
                                color = if (isWebSocketConnected) AppAccent else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.VideoCall, "Video") }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Call, "Call") }
                    IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, "More") }
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
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message -> 
                    MessageBubble(
                        message = message, 
                        onVideoClick = { uri -> fullScreenVideoUri = uri },
                        onImageClick = { uri -> fullScreenImageUri = uri }
                    ) 
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            
            fullScreenVideoUri?.let { uri ->
                FullScreenVideoPlayer(uri = uri, onBack = { fullScreenVideoUri = null })
            }
            fullScreenImageUri?.let { uri ->
                FullScreenImagePlayer(uri = uri, onBack = { fullScreenImageUri = null })
            }
        }
    }
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
                        else -> { Text(message.text, color = textColor) }
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppAccent
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
            onBack = {}
        )
    }
}
