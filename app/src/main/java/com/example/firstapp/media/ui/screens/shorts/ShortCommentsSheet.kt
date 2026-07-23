package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.firstapp.media.model.PostReply
import com.example.firstapp.common.util.Resource
import com.example.firstapp.home.ui.screens.DataStateHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortCommentsSheet(
    shortId: String,
    repliesState: Resource<List<PostReply>>,
    currentUserAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onSendReply: (String, String?) -> Unit,
    onLikeReply: (String) -> Unit = {}
) {
    var replyText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<PostReply?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.85f)
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
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            // Comments List
            Box(modifier = Modifier.weight(1f)) {
                DataStateHandler(repliesState) { replies ->
                    if (replies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No comments yet", color = Color.Gray)
                                Text("Be the first to comment", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(replies) { reply ->
                                ShortReplyItem(
                                    reply = reply,
                                    onLike = { onLikeReply(reply.id) },
                                    onReplyTo = { replyingTo = reply }
                                )
                            }
                        }
                    }
                }
            }

            // Input Area
            Column(modifier = Modifier.background(Color(0xFF1A1C1E))) {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Replying to @${replyingTo!!.authorHandle}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Gray)) {
                        if (currentUserAvatarUrl != null) {
                            AsyncImage(model = currentUserAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Add a comment...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )
                    
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(replyText, replyingTo?.id)
                                replyText = ""
                                replyingTo = null
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (replyText.isNotBlank()) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortReplyItem(
    reply: PostReply,
    onLike: () -> Unit,
    onReplyTo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(reply.authorAvatarColor))) {
            if (reply.authorAvatarUrl != null) {
                AsyncImage(model = reply.authorAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(reply.timestamp, color = Color.Gray, fontSize = 11.sp)
            }
            
            Text(reply.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reply",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onReplyTo() }
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLike, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (reply.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (reply.isLiked) Color.Red else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(reply.likes.toString(), color = Color.Gray, fontSize = 11.sp)
        }
    }
}
