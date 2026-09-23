package com.tpsk.tj.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tpsk.tj.common.model.ConversationType

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String, // Unified Session UUID
    val type: ConversationType,
    val senderName: String, 
    val description: String = "",
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val lastSeen: Long = 0L,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedMessageId: String? = null,
    val isTyping: Boolean = false,
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
