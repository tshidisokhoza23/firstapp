package com.example.firstapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.model.Message
import com.example.firstapp.model.MessageType
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    chat: ChatMessage,
    messages: List<Message>,
    onBack: () -> Unit
) {
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    val mediaMessages = remember(messages) {
        messages.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chat.senderName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Media, links and docs", fontSize = 12.sp, color = AppTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        if (mediaMessages.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No shared media found", color = AppTextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(mediaMessages) { message ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { 
                                if (message.type == MessageType.VIDEO) {
                                    fullScreenVideoUri = message.mediaUri
                                } else {
                                    fullScreenImageUri = message.mediaUri
                                }
                            }
                    ) {
                        AsyncImage(
                            model = message.mediaUri ?: message.text,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (message.type == MessageType.VIDEO) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center).size(32.dp)
                            )
                        }
                    }
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
}
