package com.tpsk.tj.chat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.ui.viewmodel.StarredMessagesViewModel
import com.tpsk.tj.common.ui.theme.AppPrimary
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.util.formatWhatsAppTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    messagesState: Resource<List<Message>>,
    onBack: () -> Unit,
    onMessageClick: (Message) -> Unit,
    viewModel: StarredMessagesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Starred Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (messagesState) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(messagesState.message ?: "Error loading starred messages", color = MaterialTheme.colorScheme.error)
                    }
                }
                is Resource.Success -> {
                    val messages = messagesState.data ?: emptyList()
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No starred messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(messages) { message ->
                                StarredMessageItem(
                                    message = message, 
                                    onClick = { onMessageClick(message) },
                                    onLongClick = { selectedMessageForMenu = message }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp), 
                                    thickness = 0.5.dp, 
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedMessageForMenu?.let { message ->
            AlertDialog(
                onDismissRequest = { selectedMessageForMenu = null },
                title = { Text("Message Options", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Unstar Message", color = MaterialTheme.colorScheme.onSurface) },
                            leadingContent = { Icon(Icons.Default.StarOutline, null, tint = Color.Yellow) },
                            modifier = Modifier.clickable { 
                                viewModel.unstarMessage(message.id)
                                selectedMessageForMenu = null 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable { 
                                viewModel.deleteMessage(message.id)
                                selectedMessageForMenu = null 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StarredMessageItem(message: Message, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.senderName ?: "", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(formatWhatsAppTime(message.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(message.content, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 2)
        }
    }
}
