package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.model.Message
import com.example.firstapp.model.MessageType
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary
import com.example.firstapp.ui.theme.FirstAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chat: ChatMessage, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            Message(1, "K", "13:37", false),
            Message(2, "My brada ke batla o nzame ka 2K", "19:20", true),
            Message(3, "Voice call", "19:29", false, MessageType.VOICE_CALL, "2 mins"),
            Message(4, "Voice call", "20:36", false, MessageType.VOICE_CALL, "1 min"),
            Message(5, "Audio message", "20:43", false, MessageType.AUDIO_MESSAGE, "0:13"),
            Message(6, "Oryt my brada", "21:44", true),
            Message(7, "my brada", "19:34", true),
            Message(8, "Voice call", "19:34", true, MessageType.VOICE_CALL, "55 secs"),
            Message(9, "E xap", "19:39", false)
        )
    }
    var showAttachments by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(chat.avatarColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(chat.senderName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("last seen today at 20:00", fontSize = 12.sp, color = AppTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.VideoCall, "Video Call") }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Call, "Call") }
                    IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = AppTextPrimary,
                    navigationIconContentColor = AppTextPrimary,
                    actionIconContentColor = AppTextPrimary
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(AppSecondary) // Match the bottom bar color
                    .navigationBarsPadding() // Adds padding for the system navigation bar
                    .imePadding() // Moves the input up when the keyboard appears
            ) {
                if (showAttachments) {
                    AttachmentPanel()
                }
                ChatBottomBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            messages.add(Message(messages.size + 1, messageText, "20:50", true))
                            messageText = ""
                        }
                    },
                    onAttachClick = { showAttachments = !showAttachments }
                )
            }
        },
        containerColor = AppBackground
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                reverseLayout = false
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message ->
                    MessageBubble(message)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) Color(0xFF005C4B) else AppSecondary
    val textColor = AppTextPrimary

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppTextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.height(20.dp).width(100.dp).background(AppTextSecondary.copy(alpha = 0.3f)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(message.duration ?: "", color = AppTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        text = message.time,
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
    onAttachClick: () -> Unit
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
            IconButton(onClick = {}) {
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
                IconButton(onClick = {}) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = AppTextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { if (text.isNotBlank()) onSend() else {} },
            modifier = Modifier.size(50.dp).clip(CircleShape).background(AppAccent)
        ) {
            Icon(
                imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                contentDescription = "Send/Mic",
                tint = AppBackground
            )
        }
    }
}

@Composable
fun AttachmentPanel() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSecondary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val rows = listOf(
                listOf(AttachmentItem("Document", Icons.Default.Description, Color(0xFF7E57C2)), AttachmentItem("Camera", Icons.Default.CameraAlt, Color(0xFFEC407A)), AttachmentItem("Gallery", Icons.Default.Image, Color(0xFF42A5F5)), AttachmentItem("Audio", Icons.Default.Headset, Color(0xFFFFA726))),
                listOf(AttachmentItem("Order", Icons.Default.Receipt, Color(0xFF26A69A)), AttachmentItem("Catalogue", Icons.Default.Store, Color(0xFF78909C)), AttachmentItem("Quick Reply", Icons.Default.Bolt, Color(0xFFFFCA28)), AttachmentItem("Location", Icons.Default.LocationOn, Color(0xFF66BB6A))),
                listOf(AttachmentItem("Contact", Icons.Default.Person, Color(0xFF29B6F6)), AttachmentItem("Poll", Icons.Default.Poll, Color(0xFFFFD54F)), AttachmentItem("Event", Icons.Default.Event, Color(0xFFF06292)))
            )
            
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    row.forEach { item ->
                        Column(
                            modifier = Modifier.weight(1f),
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

data class AttachmentItem(val label: String, val icon: ImageVector, val color: Color)

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
