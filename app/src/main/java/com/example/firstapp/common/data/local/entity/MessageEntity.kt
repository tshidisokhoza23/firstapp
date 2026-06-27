package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.chat.model.MessageStatus
import com.example.firstapp.chat.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // Changed from Int to String (UUID)
    val chatId: Int, // Required by rule 6
    val content: String, // Required by rule 6 (previously 'text')
    val senderId: Int, // Required by rule 6 (previously Int?)
    val senderName: String?,
    val timestamp: Long, // Required by rule 6 (numeric, replaces 'time: String')
    val isFromMe: Boolean,
    val type: MessageType,
    val duration: String?,
    val mediaUri: String?,
    val fileName: String?,
    val fileSize: String?,
    val contactName: String?,
    val contactPhone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isRead: Boolean,
    val isDelivered: Boolean = false,
    val replyToId: String?,
    val replyToText: String?,
    val replyToSenderName: String?,
    val senderAvatarUrl: String?,
    val isStarred: Boolean = false,
    val isViewOnce: Boolean = false,
    val expiresAt: Long? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, Int> = emptyMap(),
    val isEncrypted: Boolean = false,
    val clientId: String? = null // Keeping for compatibility
)
