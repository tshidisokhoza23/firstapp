package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.ui.components.DataStateHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortCommentsSheet(
    shortId: String,
    repliesState: Resource<List<Post>>,
    currentUserAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onSendReply: (String, String?) -> Unit,
    onLikeReply: (Post) -> Unit = {}
) {
    var replyText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Post?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.75f).padding(bottom = 16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            // List
            Box(modifier = Modifier.weight(1f)) {
                DataStateHandler(
                    resource = repliesState,
                    onLoading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } },
                    onEmpty = { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No comments yet", color = Color.Gray) } }
                ) { replies: List<Post> ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(replies) { reply ->
                            CommentItem(
                                comment = reply,
                                onLike = { onLikeReply(reply) },
                                onReplyClick = { replyingTo = reply }
                            )
                        }
                    }
                }
            }

            // Input field
            Column {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to @${replyingTo?.authorHandle}", color = Color.LightGray, fontSize = 12.sp)
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray)) {
                        AsyncImage(
                            model = currentUserAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            if (replyText.length > 0) {
                                onSendReply(replyText, replyingTo?.id)
                                replyText = ""
                                replyingTo = null
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Post,
    onLike: () -> Unit,
    onReplyClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray)) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text("@${comment.authorHandle}", color = Color.Gray, fontSize = 12.sp)
            Text(comment.content, color = Color.White, fontSize = 14.sp)
            
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reply", 
                    color = Color.Gray, 
                    fontSize = 12.sp, 
                    modifier = Modifier.clickable { onReplyClick() }
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLike) {
                Icon(
                    if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    null,
                    tint = if (comment.isLiked) Color.Red else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(comment.likes.toString(), color = Color.Gray, fontSize = 10.sp)
        }
    }
}
