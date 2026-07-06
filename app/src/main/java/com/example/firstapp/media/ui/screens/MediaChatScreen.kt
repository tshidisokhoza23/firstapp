package com.example.firstapp.media.ui.screens

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
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.media.model.MediaMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaChatScreen(
    userId: String,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    
    val messages = remember {
        mutableStateListOf<MediaMessage>(
            MediaMessage("1", "system", userId, "Hello! How can I help you with Media today?", System.currentTimeMillis() - 5000),
            MediaMessage("2", userId, "system", "I have a question about my latest post.", System.currentTimeMillis() - 2000)
        )
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
                                .background(Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("User $userId", color = Color.White, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        bottomBar = {
            Surface(
                color = AppBackground,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSecondary,
                            unfocusedContainerColor = AppSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText != "") {
                                messages.add(
                                    MediaMessage(
                                        id = System.currentTimeMillis().toString(),
                                        senderId = "system",
                                        receiverId = userId,
                                        content = messageText,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = AppPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        val reversedMessages = messages.toList().reversed()
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
            color = if (isOwnMessage) AppPrimary else AppSecondary,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 0.dp,
                bottomEnd = if (isOwnMessage) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
