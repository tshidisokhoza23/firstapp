package com.tpsk.tj.media.ui.screens.shorts

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
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.common.ui.components.DataStateHandler
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.theme.AppBackground
import com.tpsk.tj.common.ui.theme.AppSecondary

@Composable
fun ShortCommentsOverlay(
    shortId: String,
    repliesState: Resource<List<Post>>,
    currentUserAvatarUrl: String? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit,
    onSendReply: (String, String?) -> Unit,
    onLikeReply: (Post) -> Unit = {}
) {
    var replyText by remember(shortId) { mutableStateOf("") }
    var replyingTo by remember(shortId) { mutableStateOf<Post?>(null) }
    var isSubmitting by remember(shortId) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF1A1C1E))
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
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // List
            Box(modifier = Modifier.weight(1f)) {
                DataStateHandler(
                    resource = repliesState,
                    onLoading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AppAccent) } },
                    onEmpty = { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No comments yet. Be the first!", color = Color.White.copy(alpha = 0.6f)) } }
                ) { replies: List<Post> ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
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
            Surface(
                color = Color(0xFF24272B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding)
            ) {
                Column {
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Replying to @${replyingTo?.authorHandle}", color = AppAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedUserAvatar = remember(currentUserAvatarUrl) {
                            com.tpsk.tj.common.util.MediaUtils.formatUrl(currentUserAvatarUrl)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!formattedUserAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = formattedUserAvatar,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AppAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(10.dp))
                        
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            placeholder = { Text("Add a comment...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AppAccent
                            )
                        )
                        
                        if (replyText.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                enabled = !isSubmitting,
                                onClick = {
                                    if (!isSubmitting) {
                                        isSubmitting = true
                                        onSendReply(replyText, replyingTo?.id)
                                        replyText = ""
                                        replyingTo = null
                                        isSubmitting = false
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (isSubmitting) AppAccent.copy(alpha = 0.5f) else AppAccent)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val formattedAuthorAvatar = remember(comment.authorAvatarUrl) {
            com.tpsk.tj.common.util.MediaUtils.formatUrl(comment.authorAvatarUrl)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (!formattedAuthorAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = formattedAuthorAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val initial = comment.authorName.take(1).uppercase(java.util.Locale.ROOT).ifBlank {
                    comment.authorHandle.take(1).uppercase(java.util.Locale.ROOT).ifBlank { "U" }
                }
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.authorHandle}", 
                    color = Color.White, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${comment.timestamp.ifBlank { "Just now" }}", 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.content, 
                color = Color.White.copy(alpha = 0.9f), 
                fontSize = 14.sp, 
                lineHeight = 18.sp
            )
            
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reply", 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onReplyClick() }
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLike, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (comment.isLiked) Color.Red else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (comment.likes > 0) {
                Text(
                    text = comment.likes.toString(), 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 11.sp
                )
            }
        }
    }
}
