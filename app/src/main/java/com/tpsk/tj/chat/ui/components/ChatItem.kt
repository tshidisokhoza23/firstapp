package com.tpsk.tj.chat.ui.components

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
import com.tpsk.tj.common.util.isIdString
import com.tpsk.tj.common.util.sanitizeDisplayName
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
import com.tpsk.tj.chat.data.GlobalCallState
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.MessageStatus
import com.tpsk.tj.chat.model.consistentName
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.util.formatWhatsAppTime

@Composable
fun ChatItem(
    chat: ChatMessage,
    modifier: Modifier = Modifier,
    hasStory: Boolean = false,
    allStoriesViewed: Boolean = false,
    onAvatarClick: () -> Unit = {}
) {
    val displayName = remember(chat.displayName, chat.senderName, chat.otherUserId, chat.phoneNumber, chat.id) {
        chat.consistentName().ifBlank {
            val phone = chat.phoneNumber?.takeIf { it.isNotBlank() }
                ?: chat.otherUserId?.let { GlobalCallState.getContactPhone(it) }
            sanitizeDisplayName(chat.senderName, chat.otherUserId, phone).ifBlank { phone.orEmpty() }
        }
    }

    val formattedTime = remember(chat.timestamp) { formatWhatsAppTime(chat.timestamp) }

    val isPhoto = remember(chat.lastMessage) { chat.lastMessage.contains("photo", ignoreCase = true) }
    val isVideo = remember(chat.lastMessage) { chat.lastMessage.contains("video", ignoreCase = true) }
    val isSticker = remember(chat.lastMessage) { chat.lastMessage.contains("sticker", ignoreCase = true) }

    Row(
        modifier = modifier
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

        Box {
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
            if (chat.isOnline && !chat.isMingle && !chat.isGroup && chat.type != com.tpsk.tj.common.model.ConversationType.CHANNEL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(Color(0xFF25D366), CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                )
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
                    text = displayName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = formattedTime,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.unreadCount == 0 && !chat.isTyping && chat.lastMessage.isNotEmpty() && chat.lastMessageIsFromMe) {
                    val tick = when {
                        chat.lastMessageStatus == MessageStatus.READ || chat.lastMessageRead -> MessageStatus.READ
                        chat.lastMessageStatus == MessageStatus.DELIVERED -> MessageStatus.DELIVERED
                        chat.lastMessageStatus == MessageStatus.FAILED -> MessageStatus.FAILED
                        chat.lastMessageStatus == MessageStatus.SENDING || chat.lastMessageStatus == MessageStatus.PENDING -> MessageStatus.SENDING
                        else -> MessageStatus.SENT
                    }
                    Icon(
                        imageVector = when (tick) {
                            MessageStatus.READ, MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            MessageStatus.FAILED -> Icons.Default.ErrorOutline
                            MessageStatus.SENDING, MessageStatus.PENDING -> Icons.Default.Schedule
                            else -> Icons.Default.Done
                        },
                        contentDescription = null,
                        tint = when (tick) {
                            MessageStatus.READ -> Color(0xFF34B7F1)
                            MessageStatus.FAILED -> Color(0xFFE53935)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                
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
