package com.example.firstapp.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import com.example.firstapp.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortCommentsSheet(
    shortId: Int,
    repliesState: Resource<List<ShortReply>>,
    onDismiss: () -> Unit,
    onSendReply: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Comments",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Comments List
            Box(modifier = Modifier.weight(1f)) {
                when (repliesState) {
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppAccent)
                        }
                    }
                    is Resource.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = repliesState.message ?: "Error loading comments", color = Color.Red)
                        }
                    }
                    is Resource.Success -> {
                        val replies = repliesState.data ?: emptyList()
                        if (replies.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No comments yet. Be the first to reply!", color = AppTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(replies) { reply ->
                                    ShortReplyItem(reply)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Reply Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Add a comment...", color = AppTextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2A2D31),
                        unfocusedContainerColor = Color(0xFF2A2D31),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AppAccent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onSendReply(replyText)
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (replyText.isNotBlank()) AppAccent else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ShortReplyItem(reply: ShortReply) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(reply.authorAvatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reply.authorName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.authorName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reply.timestamp,
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reply.content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
