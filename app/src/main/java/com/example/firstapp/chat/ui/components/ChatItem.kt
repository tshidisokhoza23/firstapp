package com.example.firstapp.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.firstapp.common.util.isIdString
import com.example.firstapp.common.util.sanitizeDisplayName
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatWhatsAppTime

@Composable
fun ChatItem(
    chat: ChatMessage,
    hasStory: Boolean = false,
    allStoriesViewed: Boolean = false,
    onAvatarClick: () -> Unit = {}
) {
    val displayName = remember(chat.senderName, chat.otherUserId, chat.phoneNumber) {
        val phone = chat.phoneNumber?.takeIf { it.isNotBlank() }
            ?: chat.otherUserId?.let { GlobalCallState.getContactPhone(it) }
        val sanitized = sanitizeDisplayName(chat.senderName, chat.otherUserId, phone)
        when {
            sanitized.isNotBlank() -> sanitized
            !phone.isNullOrBlank() -> phone
            !chat.senderName.isBlank() && !isIdString(chat.senderName, chat.otherUserId) -> chat.senderName
            else -> phone.orEmpty()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarModifier = if (hasStory) {
            val borderColor = if (allStoriesViewed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable { onAvatarClick() }
                .padding(3.dp)
        } else {
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onAvatarClick() }
        }

        Box(
            modifier = avatarModifier,
            contentAlignment = Alignment.Center
        ) {
            val backgroundColor = if (chat.avatarColor != Color.Transparent && chat.avatarColor != Color.Gray) 
                chat.avatarColor 
            else MaterialTheme.colorScheme.surfaceVariant
            
            val textColor = if (chat.avatarColor != Color.Transparent && chat.avatarColor != Color.Gray)
                Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant

            ChatAvatar(
                imageUrl = chat.avatarUrl,
                name = displayName,
                size = 56.dp,
                fontSize = 20.sp,
                backgroundColor = backgroundColor,
                textColor = textColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.displayName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = formatWhatsAppTime(chat.timestamp),
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.unreadCount == 0 && !chat.isTyping && chat.lastMessage.isNotEmpty() && chat.lastMessageIsFromMe) {
                    Icon(
                        imageVector = if (chat.lastMessageRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (chat.lastMessageRead) Color(0xFF34B7F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                
                // Check if last message is a placeholder for media
                val isPhoto = chat.lastMessage.lowercase().contains("photo")
                val isVideo = chat.lastMessage.lowercase().contains("video")
                val isSticker = chat.lastMessage.lowercase().contains("sticker")
                
                if (isPhoto && !chat.isTyping) {
                    Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (isVideo && !chat.isTyping) {
                    Icon(Icons.Default.Videocam, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (isSticker && !chat.isTyping) {
                    Icon(Icons.Default.Face, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = if (chat.isTyping) "typing..." else chat.lastMessage,
                    color = if (chat.isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.isMuted) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).rotate(45f))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
