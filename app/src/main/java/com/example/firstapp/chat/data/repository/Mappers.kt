package com.example.firstapp.chat.data.repository

import com.example.firstapp.chat.data.api.ChatSummary
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.data.api.ApiCallLog
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.common.data.local.entity.MessageEntity
import com.example.firstapp.common.data.local.entity.CallLogEntity
import com.example.firstapp.common.data.local.entity.ContactEntity
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.util.CryptoUtils
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.model.ConversationType

fun mapApiChatSummary(chat: ChatSummary): ChatMessage {
    val timestamp = chat.time
    val id = chat.conversation_id
             
    val type = when (chat.type) {
        "GROUP" -> ConversationType.GROUP
        "CHANNEL" -> ConversationType.CHANNEL
        else -> ConversationType.DIRECT
    }

    return ChatMessage(
        id = id,
        type = type,
        senderName = chat.title,
        lastMessage = chat.last_message,
        timestamp = timestamp,
        unreadCount = chat.unread_count,
        avatarColor = BoyishPurple,
        isMuted = chat.is_muted,
        isPinned = chat.is_pinned,
        isGroup = type == ConversationType.GROUP
    )
}

fun mapApiMessage(apiMessage: ApiMessage): Message {
    var body = apiMessage.message_text
    val isEncrypted = body.startsWith("ENC:")
    if (isEncrypted) {
        val encryptedText = body.removePrefix("ENC:")
        val privateKeyStr = TokenManager.getPrivateKey()
        if (privateKeyStr != null) {
            try {
                val privateBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
                val keySpec = PKCS8EncodedKeySpec(privateBytes)
                val kf = KeyFactory.getInstance("RSA")
                val privKey = kf.generatePrivate(keySpec)
                body = CryptoUtils.decrypt(encryptedText, privKey)
            } catch (e: Exception) {
                Log.e("Mappers", "Decryption failed", e)
                body = "[Encrypted Message]"
            }
        } else {
            body = "[Encrypted Message]"
        }
    }

    val timestamp = apiMessage.created_at
    val senderId = apiMessage.sender_id

    return Message(
        id = apiMessage.id,
        conversationId = apiMessage.conversation_id,
        content = body,
        senderId = senderId,
        senderName = apiMessage.sender_name,
        timestamp = timestamp,
        isFromMe = apiMessage.direction == "outgoing",
        type = when (apiMessage.media_type.lowercase()) {
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "audio", "audio_message" -> MessageType.AUDIO_MESSAGE
            "voice_call" -> MessageType.VOICE_CALL
            "location" -> MessageType.LOCATION
            "contact" -> MessageType.CONTACT
            "document" -> MessageType.DOCUMENT
            "application_link" -> MessageType.APPLICATION_LINK
            "job_post" -> MessageType.JOB_POST
            else -> MessageType.TEXT
        },
        mediaUri = formatUrl(apiMessage.media_url),
        isRead = false,
        isDelivered = true,
        status = MessageStatus.SENT,
        reactions = apiMessage.reactions ?: emptyMap(),
        isEncrypted = isEncrypted,
        clientId = apiMessage.client_uuid
    )
}

fun ChatMessage.toEntity() = ChatEntity(
    id = id,
    type = type,
    senderName = senderName,
    lastMessage = lastMessage,
    timestamp = timestamp,
    unreadCount = unreadCount,
    lastSeen = lastSeen,
    avatarUrl = avatarUrl,
    isOnline = isOnline,
    isMuted = isMuted,
    isPinned = isPinned,
    isTyping = isTyping,
    isGroup = isGroup,
    isArchived = isArchived,
    isAdmin = isAdmin,
    isVerified = isVerified,
    isSubscribed = isSubscribed,
    createdBy = createdBy,
    followers = followers,
    category = category,
    wallpaperUrl = wallpaperUrl,
    customNotificationTone = customNotificationTone,
    mediaVisibility = mediaVisibility,
    disappearingMessagesDuration = disappearingMessagesDuration,
    lastMessageIsFromMe = lastMessageIsFromMe,
    lastMessageRead = lastMessageRead,
    publicKey = publicKey,
    phoneNumber = phoneNumber
)

fun ChatEntity.toModel() = ChatMessage(
    id = id,
    type = type,
    senderName = senderName,
    lastMessage = lastMessage,
    timestamp = timestamp,
    unreadCount = unreadCount,
    lastSeen = lastSeen,
    avatarColor = AppPrimary,
    avatarUrl = avatarUrl,
    isOnline = isOnline,
    isMuted = isMuted,
    isPinned = isPinned,
    isTyping = isTyping,
    isGroup = isGroup,
    isArchived = isArchived,
    isAdmin = isAdmin,
    isVerified = isVerified,
    isSubscribed = isSubscribed,
    createdBy = createdBy,
    followers = followers,
    category = category,
    wallpaperUrl = wallpaperUrl,
    customNotificationTone = customNotificationTone,
    mediaVisibility = mediaVisibility,
    disappearingMessagesDuration = disappearingMessagesDuration,
    lastMessageIsFromMe = lastMessageIsFromMe,
    lastMessageRead = lastMessageRead,
    publicKey = publicKey,
    phoneNumber = phoneNumber
)

fun Message.toEntity(chatId: String) = MessageEntity(
    id = id,
    conversationId = chatId,
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
    clientId = clientId,
    fileName = fileName,
    fileSize = fileSize,
    duration = duration,
    contactName = contactName,
    contactPhone = contactPhone,
    latitude = latitude,
    longitude = longitude,
    replyToText = replyToText,
    replyToSenderName = replyToSenderName
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
    isDelivered = isDelivered,
    replyToId = replyToId,
    status = status,
    reactions = reactions,
    clientId = clientId,
    fileName = fileName,
    fileSize = fileSize,
    duration = duration,
    contactName = contactName,
    contactPhone = contactPhone,
    latitude = latitude,
    longitude = longitude,
    replyToText = replyToText,
    replyToSenderName = replyToSenderName
)

fun CallLog.toEntity() = CallLogEntity(
    id = id,
    sessionId = sessionId,
    callerId = callerId,
    calleeId = calleeId,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    callType = callType,
    status = status,
    durationSeconds = durationSeconds,
    timestamp = timestamp,
    endedAt = endedAt
)

fun mapApiCallLog(api: ApiCallLog) = CallLog(
    id = api.id,
    sessionId = api.session_id,
    callerId = api.caller_id,
    calleeId = api.callee_id,
    contactId = api.contact_id,
    contactName = api.contact_name,
    contactAvatar = api.contact_avatar,
    callType = api.call_type,
    status = api.status,
    durationSeconds = api.duration_seconds,
    timestamp = api.started_at,
    endedAt = api.ended_at?.toString()
)

fun CallLogEntity.toModel() = CallLog(id, sessionId, callerId, calleeId, contactId, contactName, contactAvatar, callType, status, durationSeconds, timestamp, endedAt)

fun Contact.toEntity() = ContactEntity(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey)
fun ContactEntity.toModel() = Contact(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey)

fun formatUrl(url: String?): String {
    if (url == null || url.isBlank()) return ""
    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("data:") || url.startsWith("local://") || url.startsWith("file://")) {
        return url
    }
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/v1/").removeSuffix("/")
    return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
}
