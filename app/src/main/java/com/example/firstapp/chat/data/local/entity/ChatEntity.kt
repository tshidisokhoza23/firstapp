package com.example.firstapp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.common.model.ConversationType

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String, // Unified Session UUID
    val type: ConversationType,
    val senderName: String, 
    val description: String = "",
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val lastSeen: Long = 0L,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val pinnedMessageId: String? = null,
    val isTyping: Boolean,
    val isArchived: Boolean = false,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val adminsCanPost: Boolean = false,
    val isGroup: Boolean = false,
    val isSubscribed: Boolean = false,
    val createdBy: String? = null,
    val followers: String = "0",
    val category: String = "Community",
    val wallpaperUrl: String? = null,
    val customNotificationTone: String? = null,
    val mediaVisibility: Boolean = true,
    val disappearingMessagesDuration: Int = 0,
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val publicKey: String? = null,
    val phoneNumber: String? = null,
    val otherUserId: String? = null
)
