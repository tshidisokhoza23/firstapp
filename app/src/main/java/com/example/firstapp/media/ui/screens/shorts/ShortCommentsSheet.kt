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
import java.util.*
import kotlin.collections.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.ui.components.DataStateHandler
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary

@Composable
fun ShortCommentsOverlay(
    shortId: String,
    repliesState: Resource<List<Post>>,
    currentUserAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onSendReply: (String, String?) -> Unit,
    onLikeReply: (Post) -> Unit = {}
) {
    var replyText by remember(shortId) { mutableStateOf("") }
    var replyingTo by remember(shortId) { mutableStateOf<Post?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) {}
        ) {
            // Drag handle / Notch
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }

            // List
            Box(modifier = Modifier.weight(1f)) {
                DataStateHandler(
                    resource = repliesState,
                    onLoading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } },
                    onEmpty = { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No comments yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
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

            // Quick Reaction Bar (Matching the image)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val reactions = listOf("❤️", "👏", "🔥", "🙌", "😢", "😍", "😲", "😂")
                reactions.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.clickable { onSendReply(emoji, null) }
                    )
                }
            }

            // Input field
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().imePadding()
            ) {
                Column {
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Replying to @${replyingTo?.authorHandle}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
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
                            placeholder = { Text("Join the conversation...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.Gif, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                            }
                        )
                        
                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendReply(replyText, replyingTo?.id)
                                    replyText = ""
                                    replyingTo = null
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
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
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorHandle, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "2w", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(comment.content, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 18.sp)
            
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reply", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onReplyClick() }
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLike, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    null,
                    tint = if (comment.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(comment.likes.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
