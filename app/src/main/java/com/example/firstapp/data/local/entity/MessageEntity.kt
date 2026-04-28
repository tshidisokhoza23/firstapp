package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val chatId: Int, // Link to ChatEntity.id
    val text: String,
    val senderId: Int?,
    val senderName: String?,
    val time: String,
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
    val replyToId: Int?,
    val replyToText: String?,
    val replyToSenderName: String?,
    val senderAvatarUrl: String?,
    val isStarred: Boolean = false
)
