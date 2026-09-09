package com.example.firstapp.chat.model

import androidx.compose.ui.graphics.Color
import com.example.firstapp.common.model.ConversationType
import kotlin.collections.emptyMap
import kotlin.collections.emptyList

enum class MessageStatus { PENDING, SENDING, SENT, DELIVERED, READ, FAILED }
enum class MessageType { TEXT, IMAGE, VIDEO, AUDIO_MESSAGE, LOCATION, CONTACT, DOCUMENT, SHORT_SHARE, JOB_POST, VOICE_CALL, APPLICATION_LINK }

data class ApplicantProfile(
    val full_name: String,
    val bio: String?,
    val cv_url: String?
)

data class JobDetails(
    val title: String
)

data class LinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val siteName: String? = null
)

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val content: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String? = null,
    val timestamp: Long = 0,
    val isFromMe: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val type: MessageType = MessageType.TEXT,
    val mediaUrls: List<String> = emptyList(),
    val fileName: String? = null,
    val fileSize: String? = null,
    val duration: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val isStarred: Boolean = false,
    val isViewOnce: Boolean = false,
    val expiresAt: Long? = null,
    val isEncrypted: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val clientId: String? = null,
    val applicantProfile: ApplicantProfile? = null,
    val jobDetails: JobDetails? = null,
    val linkPreview: LinkPreview? = null,
    val audioAmplitudes: List<Int>? = null,
    val scheduledAt: Long? = null
)

data class ChatMessage(
    val id: String,
    val type: ConversationType = ConversationType.DIRECT,
    val senderName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null,
    val avatarColor: Color = Color.Gray,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedMessageId: String? = null,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val lastMessageIsFromMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val isArchived: Boolean = false,
    val isAdmin: Boolean = false,
    val adminsCanPost: Boolean = false,
    val isVerified: Boolean = false,
    val isSubscribed: Boolean = false,
    val createdBy: String? = null,
    val followers: String = "0",
    val category: String = "Community",
    val wallpaperUrl: String? = null,
    val customNotificationTone: String? = null,
    val mediaVisibility: Boolean = true,
    val disappearingMessagesDuration: Int = 0,
    val publicKey: String? = null,
    val phoneNumber: String? = null,
    val otherUserId: String? = null
)
