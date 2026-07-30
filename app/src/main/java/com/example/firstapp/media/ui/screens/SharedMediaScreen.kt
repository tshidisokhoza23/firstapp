package com.example.firstapp.media.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

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
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.common.ui.theme.*
import kotlin.collections.*
import kotlin.text.isNotBlank

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.ui.components.FullScreenImagePlayer
import com.example.firstapp.chat.ui.components.FullScreenVideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    chat: ChatMessage,
    onBack: () -> Unit,
    viewModel: SharedMediaViewModel = hiltViewModel()
) {
    LaunchedEffect(chat.id) {
        viewModel.loadSharedMedia(chat.id)
    }

    val mediaMessagesResource by viewModel.mediaMessages.collectAsState()
    val mediaMessages = mediaMessagesResource.data ?: emptyList()

    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

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
                    val firstMedia = message.mediaUrls.firstOrNull() ?: ""
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { 
                                if (message.type == MessageType.VIDEO) {
                                    fullScreenVideoUri = firstMedia
                                } else {
                                    fullScreenImageUri = firstMedia
                                }
                            }
                    ) {
                        AsyncImage(
                            model = if (firstMedia.isNotBlank()) firstMedia else message.content,
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
