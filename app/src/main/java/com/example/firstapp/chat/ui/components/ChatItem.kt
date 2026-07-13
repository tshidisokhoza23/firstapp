package com.example.firstapp.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.util.formatWhatsAppTime

@Composable
fun ChatItem(
    chat: ChatMessage,
    hasStory: Boolean = false,
    allStoriesViewed: Boolean = false,
    onAvatarClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarModifier = if (hasStory) {
            val borderColor = if (allStoriesViewed) Color.Gray.copy(alpha = 0.5f) else AppAccent
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = if (allStoriesViewed) Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                            else Brush.linearGradient(listOf(AppAccent, Color(0xFF7C4DFF))),
                    shape = CircleShape
                )
                .padding(3.dp)
        } else {
            Modifier
                .size(52.dp)
                .clip(CircleShape)
        }

        Box(
            modifier = avatarModifier
                .then(if (onAvatarClick != null) Modifier.clickable { onAvatarClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(chat.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                if (chat.avatarUrl != null) {
                    AsyncImage(
                        model = chat.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chat.senderName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(AppBackground, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4CAF50), CircleShape)
                            .shadow(elevation = 4.dp, shape = CircleShape, ambientColor = Color(0xFF4CAF50), spotColor = Color(0xFF4CAF50))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.senderName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = formatWhatsAppTime(chat.timestamp),
                    color = if (chat.unreadCount > 0) AppAccent else Color.Gray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.unreadCount == 0 && !chat.isTyping && chat.lastMessage.isNotEmpty() && chat.lastMessageIsFromMe) {
                    Icon(
                        imageVector = if (chat.lastMessageRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (chat.lastMessageRead) Color(0xFF34B7F1) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Text(
                    text = if (chat.isTyping) "typing..." else chat.lastMessage,
                    color = if (chat.isTyping) AppAccent else Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (chat.isMuted) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, null, tint = Color.Gray, modifier = Modifier.size(14.dp).rotate(45f))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.unreadCount > 0) {
                    Badge(containerColor = AppAccent) {
                        Text(chat.unreadCount.toString(), color = Color.Black)
                    }
                }
            }
        }
    }
}
