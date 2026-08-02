package com.example.firstapp.media.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.media.model.MediaMessage

import androidx.compose.material.icons.filled.Image
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.media.ui.viewmodel.MediaChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaChatScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: MediaChatViewModel = hiltViewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messagesState = viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val receiverUser by viewModel.receiverUser.collectAsState()
    
    LaunchedEffect(userId) {
        viewModel.loadMessages(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            val avatar = receiverUser?.avatarUrl
                            if (avatar != null) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = receiverUser?.displayName ?: "User $userId",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
                        if (uri != null) {
                            val context = com.example.firstapp.common.util.ContextProvider.context
                            val file = com.example.firstapp.common.util.MediaUtils.getFileFromUri(context, uri)
                            if (file != null) {
                                viewModel.sendMediaMessage(userId, file, "image")
                            }
                        }
                    }
                    IconButton(onClick = { mediaLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, "Attach Image", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText != "") {
                                viewModel.sendMessage(userId, messageText)
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val msgList = messagesState.value
        val reversedMessages = remember<List<MediaMessage>>(msgList) { msgList.reversed() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(reversedMessages) { message ->
                MessageBubble(message = message, isOwnMessage = message.senderId == "system")
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: MediaMessage, isOwnMessage: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isOwnMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 0.dp,
                bottomEnd = if (isOwnMessage) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.mediaUrl != null && message.type == "image") {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.content != "") Spacer(Modifier.height(4.dp))
                }
                
                if (message.content != "") {
                    Text(
                        text = message.content,
                        color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
