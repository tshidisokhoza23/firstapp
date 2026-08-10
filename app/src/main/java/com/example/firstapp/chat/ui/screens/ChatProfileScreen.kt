@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.firstapp.chat.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.SecondaryText
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.common.util.Resource

@Composable
fun ChatProfileScreen(
    chat: ChatMessage,
    onBack: () -> Unit,
    onMediaClick: (String, String) -> Unit,
    sharedMedia: List<Message> = emptyList(),
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onMuteToggle: (Boolean) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header: Large Avatar & Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.DarkGray)
            ) {
                if (chat.avatarUrl != null) {
                    AsyncImage(
                        model = chat.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
                
                // Name Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = chat.senderName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (chat.phoneNumber != null) {
                            Text(
                                text = chat.phoneNumber,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(16.dp)) {
                if (chat.isGroup) {
                    Text(
                        "Group description",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "This is a chat group", // Fallback or use real description
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "Status and phone number",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Hey there! I am using TJ.",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = chat.phoneNumber ?: chat.id,
                        color = SecondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Media, Links, and Docs
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Media, links, and docs",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${sharedMedia.size} >",
                        color = SecondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { /* Navigate to shared media */ }
                    )
                }
                
                if (sharedMedia.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sharedMedia.take(4).forEach { message ->
                            val url = message.mediaUrls.firstOrNull() ?: ""
                            val type = if (message.type == MessageType.VIDEO) "video" else "image"
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .clickable { onMediaClick(url, type) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (message.type == MessageType.VIDEO) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No shared media yet",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Settings
            Column {
                ListItem(
                    headlineContent = { Text("Mute notifications") },
                    leadingContent = { Icon(Icons.Default.NotificationsOff, null) },
                    trailingContent = { Switch(checked = chat.isMuted, onCheckedChange = onMuteToggle) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Custom notifications") },
                    leadingContent = { Icon(Icons.Default.MusicNote, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    headlineContent = { Text("Media visibility") },
                    leadingContent = { Icon(Icons.Default.Image, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { }
                )
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)
            
            // Encryption
            ListItem(
                headlineContent = { Text("Encryption") },
                supportingContent = { Text("Messages and calls are end-to-end encrypted. Tap to verify.") },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = AppAccent) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { }
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Actions
            TextButton(
                onClick = onClearChat,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Clear Chat", fontWeight = FontWeight.Bold)
                }
            }
            
            TextButton(
                onClick = onDeleteChat,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete Chat", fontWeight = FontWeight.Bold)
                }
            }
            
            TextButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Report, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Report Contact", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(50.dp))
        }
    }
}
