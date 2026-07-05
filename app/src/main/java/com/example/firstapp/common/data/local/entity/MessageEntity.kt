package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.chat.model.MessageStatus
import com.example.firstapp.chat.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String, 
    val content: String,
    val senderId: String,
    val senderName: String?,
    val timestamp: Long,
    val isFromMe: Boolean,
    val type: MessageType,
    val mediaUri: String?,
    val isRead: Boolean,
    val isDelivered: Boolean = false,
    val replyToId: String?,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, Int> = emptyMap(),
    val clientId: String? = null
)
