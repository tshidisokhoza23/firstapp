@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tpsk.tj.chat.nearby.model.NearbyChatRequest
import com.tpsk.tj.chat.nearby.ui.viewmodel.NearbyViewModel
import com.tpsk.tj.ui.navigation.Screen

@Composable
fun ChatRequestsScreen(
    navController: NavController,
    onBack: () -> Unit,
    nearbyViewModel: NearbyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val chatRequests by nearbyViewModel.chatRequests.collectAsState()
    val uiState by nearbyViewModel.uiState.collectAsState()
    var respondingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        nearbyViewModel.refreshChatRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.requestsError != null && chatRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.requestsError ?: "Could not load requests", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { nearbyViewModel.refreshChatRequests() }) { Text("Retry") }
                }
            }
        } else if (uiState.requestsLoading && chatRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (chatRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No pending chat requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Private inquiries regarding your nearby posts will appear here.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatRequests, key = { it.id }) { req ->
                    ChatRequestItemCard(
                        request = req,
                        onAccept = {
                            if (respondingId == null) {
                            respondingId = req.id
                            nearbyViewModel.respondChatRequest(
                                req.id,
                                true,
                                onSuccess = { senderId ->
                                    respondingId = null
                                    val target = senderId.ifBlank { req.senderId }
                                    if (target.isBlank()) {
                                        Toast.makeText(context, "Could not open the chat", Toast.LENGTH_LONG).show()
                                        return@respondChatRequest
                                    }
                                    Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show()
                                    navController.navigate(
                                        Screen.Chat.createRoute(
                                            chatId = target,
                                            type = "initiate",
                                            displayName = req.senderName
                                        )
                                    )
                                },
                                onError = { err ->
                                    respondingId = null
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                            }
                        },
                        onDecline = {
                            if (respondingId == null) {
                            respondingId = req.id
                            nearbyViewModel.respondChatRequest(
                                req.id,
                                false,
                                onSuccess = {
                                    respondingId = null
                                    Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    respondingId = null
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRequestItemCard(
    request: NearbyChatRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4D8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!request.senderAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = request.senderAvatarUrl,
                            contentDescription = request.senderName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = request.senderName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = request.postReferenceSnippet,
                        fontSize = 12.sp,
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = request.inquiryMessage,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Decline")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Text("Accept & Chat", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
