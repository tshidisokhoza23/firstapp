package com.example.firstapp.common.model

import androidx.compose.ui.graphics.Color
import com.example.firstapp.chat.model.Message

enum class ConversationType {
    DIRECT, GROUP, CHANNEL, JOB_CHAT, MINGLE
}

data class Conversation(
    val id: String,
    val type: ConversationType,
    val title: String,
    val description: String = "",
    val lastMessage: String = "",
    val timestamp: Long,
    val unreadCount: Int = 0,
    val lastSeen: Long = 0L,
    val avatarUrl: String? = null,
    val avatarColor: Color = Color.Gray,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isAdmin: Boolean = false,
    val isVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val createdBy: String? = null,
    val followers: String = "0",
    val category: String = "Community",
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val publicKey: String? = null,
    val messages: List<Message> = emptyList()
)

fun Conversation.canPost(myUserId: String?): Boolean {
    return when (type) {
        ConversationType.DIRECT, ConversationType.JOB_CHAT, ConversationType.MINGLE -> true
        ConversationType.GROUP -> true // Default to true, or add a 'anyoneCanPost' flag
        ConversationType.CHANNEL -> isAdmin || (createdBy != null && createdBy == myUserId)
    }
}
