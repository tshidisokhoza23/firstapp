package com.example.firstapp.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.ui.components.ChatItem
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.common.ui.theme.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    onBack: () -> Unit,
    onChatClick: (ChatMessage) -> Unit,
    viewModel: ChatViewModel
) {
    val archivedChatsResource by viewModel.archivedChats.collectAsState()
    val chats = archivedChatsResource.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Chats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No archived chats", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chats) { chat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).clickable { onChatClick(chat) }) {
                            ChatItem(chat)
                        }
                        IconButton(onClick = { viewModel.archiveChat(chat.id, false) }) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
                }
            }
        }
    }
}
