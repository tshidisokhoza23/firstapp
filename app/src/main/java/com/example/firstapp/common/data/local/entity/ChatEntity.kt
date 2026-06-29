package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.common.model.ConversationType

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Int,
    val type: ConversationType,
    val senderName: String, // Maps to title
    val description: String = "",
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val lastSeen: Long = 0L,
    val avatarUrl: String?,
    val avatarColor: Int = 0,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isTyping: Boolean,
    val isGroup: Boolean, // Deprecated, use type
    val isArchived: Boolean = false,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val isSubscribed: Boolean = false,
    val createdBy: Int? = null,
    val followers: String = "0",
    val category: String = "Community",
    val wallpaperUrl: String? = null,
    val customNotificationTone: String? = null,
    val mediaVisibility: Boolean = true,
    val disappearingMessagesDuration: Int = 0,
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val publicKey: String? = null
)
