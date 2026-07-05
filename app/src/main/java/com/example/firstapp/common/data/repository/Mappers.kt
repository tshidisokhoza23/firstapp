package com.example.firstapp.common.data.repository

import com.example.firstapp.BuildConfig
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.common.data.api.*
import com.example.firstapp.common.data.local.entity.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.jobs.model.*
import androidx.compose.ui.graphics.Color

fun mapApiChatSummary(apiChat: ChatSummary): ChatMessage {
    return ChatMessage(
        id = apiChat.conversation_id,
        type = when (apiChat.type) {
            "GROUP" -> ConversationType.GROUP
            "CHANNEL" -> ConversationType.CHANNEL
            else -> ConversationType.DIRECT
        },
        senderName = apiChat.title,
        lastMessage = apiChat.last_message,
        timestamp = apiChat.time,
        unreadCount = apiChat.unread_count,
        isMuted = apiChat.is_muted,
        isPinned = apiChat.is_pinned,
        avatarColor = Color.Gray
    )
}

fun mapApiMessage(apiMessage: ApiMessage): Message {
    return Message(
        id = apiMessage.id,
        conversationId = apiMessage.conversation_id,
        content = apiMessage.message_text,
        senderId = apiMessage.sender_id,
        senderName = apiMessage.sender_name,
        timestamp = apiMessage.created_at,
        isFromMe = apiMessage.direction == "outgoing",
        type = when (apiMessage.media_type) {
            "IMAGE" -> MessageType.IMAGE
            "SHORT_VIDEO" -> MessageType.SHORT_SHARE
            "JOB_POST" -> MessageType.JOB_POST
            else -> MessageType.TEXT
        },
        mediaUri = apiMessage.media_url,
        isRead = false,
        status = MessageStatus.SENT,
        clientId = apiMessage.client_uuid,
        reactions = apiMessage.reactions ?: emptyMap()
    )
}

fun ChatMessage.toEntity() = ChatEntity(
    id = id,
    type = type,
    senderName = senderName,
    lastMessage = lastMessage,
    timestamp = timestamp,
    unreadCount = unreadCount,
    isMuted = isMuted,
    isPinned = isPinned,
    isOnline = isOnline,
    lastSeen = lastSeen,
    avatarUrl = avatarUrl,
    isTyping = isTyping
)

fun ChatEntity.toModel() = ChatMessage(
    id = id,
    type = type,
    senderName = senderName,
    lastMessage = lastMessage,
    timestamp = timestamp,
    unreadCount = unreadCount,
    isMuted = isMuted,
    isPinned = isPinned,
    isOnline = isOnline,
    lastSeen = lastSeen,
    avatarUrl = avatarUrl,
    isTyping = isTyping,
    avatarColor = Color.Gray
)

fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    content = content,
    senderId = senderId,
    senderName = senderName,
    timestamp = timestamp,
    isFromMe = isFromMe,
    type = type,
    mediaUri = mediaUri,
    isRead = isRead,
    isDelivered = isDelivered,
    replyToId = replyToId,
    status = status,
    reactions = reactions,
    clientId = clientId
)

fun MessageEntity.toModel() = Message(
    id = id,
    conversationId = conversationId,
    content = content,
    senderId = senderId,
    senderName = senderName ?: "Unknown",
    timestamp = timestamp,
    isFromMe = isFromMe,
    type = type,
    mediaUri = mediaUri,
    isRead = isRead,
    replyToId = replyToId,
    status = status,
    reactions = reactions,
    clientId = clientId
)

fun mapApiContact(api: ApiUser): Contact {
    return Contact(
        id = api.id,
        username = api.username,
        displayName = api.display_name,
        avatarUrl = api.avatar_url,
        phoneNumber = api.phone_number ?: "",
        isVerified = api.is_verified,
        bio = api.bio,
        publicKey = api.public_key
    )
}

fun Job.toEntity() = JobEntity(id, title, companyName, companyLogoUrl, location, salaryRange, jobType, postedTime, description, requirements, isVerified, isApplied, isSaved)
fun JobEntity.toModel() = Job(id, title, companyName, companyLogoUrl, location, Color.Gray, salaryRange, jobType, postedTime, description, requirements, isVerified, isApplied, isSaved)

fun formatUrl(url: String?): String {
    if (url == null || url.isBlank()) return ""
    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("data:") || url.startsWith("local://") || url.startsWith("file://")) {
        return url
    }
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/v1/").removeSuffix("/")
    return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
}
