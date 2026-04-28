package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.model.MessageType
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.util.formatWhatsAppTime
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon

import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.firstapp.ui.theme.AppPrimary

@Composable
fun ChatItem(chat: ChatMessage) {
    val lastMessageText = remember(chat.lastMessage, chat.isTyping) {
        if (chat.isTyping) "typing..." else chat.lastMessage
    }
    
    val mediaIndicator = remember(chat.lastMessage) {
        val msg = chat.lastMessage.lowercase()
        when {
            msg.contains("photo") || msg.contains("image") -> Icons.Default.CameraAlt to "Photo"
            msg.contains("video") -> Icons.Default.Videocam to "Video"
            msg.contains("voice note") || msg.contains("audio") -> Icons.Default.Mic to "Voice note"
            msg.contains("location") -> Icons.Default.LocationOn to "Location"
            msg.contains("contact") -> Icons.Default.Person to "Contact"
            msg.contains("document") || msg.contains("file") -> Icons.Default.Description to "Document"
            else -> null
        }
    } as Pair<androidx.compose.ui.graphics.vector.ImageVector, String>?

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Adaptive Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(chat.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            if (chat.avatarUrl != null) {
                AsyncImage(
                    model = chat.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = chat.senderName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Online Indicator
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4CAF50), CircleShape)
                        .border(2.dp, AppBackground, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.senderName,
                    color = AppTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatWhatsAppTime(chat.time),
                    color = if (chat.unreadCount > 0) AppAccent else AppTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = 45f })
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.isMuted) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                if (chat.isTyping) {
                    Text(
                        text = "typing...",
                        color = AppAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        mediaIndicator?.let { (icon, label) ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AppTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = chat.lastMessage,
                                color = AppTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } ?: Text(
                            text = chat.lastMessage,
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                            color = AppBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxWithText(text: String, backgroundColor: Color, textColor: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
