package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Int,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isTyping: Boolean,
    val isGroup: Boolean,
    val wallpaperUrl: String? = null
)
