package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Int,
    val senderName: String,
    val lastMessage: String,
    val timestamp: Long, // Changed from time: String
    val unreadCount: Int,
    val lastSeen: Long = 0L,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isTyping: Boolean,
    val isGroup: Boolean,
    val isArchived: Boolean = false,
    val wallpaperUrl: String? = null,
    val customNotificationTone: String? = null,
    val mediaVisibility: Boolean = true,
    val disappearingMessagesDuration: Int = 0,
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val publicKey: String? = null
)
