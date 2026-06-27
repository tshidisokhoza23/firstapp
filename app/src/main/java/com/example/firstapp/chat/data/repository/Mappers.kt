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

fun mapApiChatSummary(chat: ChatSummary): ChatMessage {
    val timestamp = chat.time ?: System.currentTimeMillis()
    val id = if (chat.is_group == true && chat.group_id != null) chat.group_id else chat.contact_id

    val resolvedName = chat.contact_name?.takeIf { it.isNotBlank() && !it.startsWith("User ") } 
        ?: GlobalCallState.getContactName(id)
        ?: chat.phone_number?.takeIf { it.isNotBlank() }
        ?: GlobalCallState.getContactPhone(id)
        ?: chat.contact_name?.takeIf { it.isNotBlank() }
        ?: (if (chat.is_group == true) "Group" else "User $id")

    return ChatMessage(
        id = id,
        senderName = resolvedName,
        lastMessage = chat.last_message ?: "",
        timestamp = timestamp,
        unreadCount = chat.unread_count ?: 0,
        lastSeen = chat.last_seen ?: 0L,
        avatarColor = BoyishPurple,
        avatarUrl = chat.avatar_url,
        isOnline = chat.is_online ?: false,
        isMuted = chat.is_muted ?: false,
        isPinned = chat.is_pinned ?: false,
        isTyping = false,
        isGroup = chat.is_group ?: false,
        isArchived = chat.is_archived ?: false,
        wallpaperUrl = chat.wallpaper_url,
        lastMessageIsFromMe = false,
        lastMessageRead = false
    )
}

fun mapApiMessage(apiMessage: ApiMessage): Message {
    var body = apiMessage.body
    val isEncrypted = apiMessage.body.startsWith("ENC:")
    if (isEncrypted) {
        val encryptedText = apiMessage.body.removePrefix("ENC:")
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
    val senderId = apiMessage.sender_id ?: 0

    val senderName = apiMessage.sender_name?.takeIf { 
        it.isNotBlank() && 
        !it.startsWith("User ") && 
        it.lowercase() != "unknown" && 
        it.lowercase() != "null" && 
        it.lowercase() != "undefined" 
    } ?: GlobalCallState.getContactName(senderId)
      ?: apiMessage.contact_phone?.takeIf { it.isNotBlank() && it.lowercase() != "unknown" }
      ?: GlobalCallState.getContactPhone(senderId)
      ?: "Unknown"

    return Message(
        id = apiMessage.id,
        chatId = 0, // Should be populated by caller
        content = body,
        senderId = senderId,
        senderName = senderName,
        timestamp = timestamp,
        isFromMe = apiMessage.direction == "outgoing",
        type = when (apiMessage.message_type?.lowercase()) {
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "audio", "audio_message" -> MessageType.AUDIO_MESSAGE
            "voice_call" -> MessageType.VOICE_CALL
            "location" -> MessageType.LOCATION
            "contact" -> MessageType.CONTACT
            "document" -> MessageType.DOCUMENT
            else -> MessageType.TEXT
        },
        duration = apiMessage.duration,
        mediaUri = formatUrl(apiMessage.media_url),
        fileName = apiMessage.file_name,
        fileSize = apiMessage.file_size,
        contactName = apiMessage.contact_name,
        contactPhone = apiMessage.contact_phone,
        latitude = apiMessage.latitude,
        longitude = apiMessage.longitude,
        isRead = apiMessage.read_at != null,
        isDelivered = apiMessage.delivered_at != null || apiMessage.read_at != null,
        replyToId = apiMessage.parent_id,
        replyToText = apiMessage.parent_body,
        replyToSenderName = apiMessage.parent_sender_name,
        senderAvatarUrl = null,
        isStarred = false,
        isViewOnce = apiMessage.is_view_once ?: false,
        expiresAt = apiMessage.expires_at,
        status = if (apiMessage.read_at != null) MessageStatus.READ else if (apiMessage.delivered_at != null) MessageStatus.DELIVERED else MessageStatus.SENT,
        reactions = apiMessage.reactions ?: emptyMap(),
        isEncrypted = isEncrypted,
        clientId = apiMessage.client_id
    )
}

fun ChatMessage.toEntity() = ChatEntity(
    id = id,
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
    wallpaperUrl = wallpaperUrl,
    customNotificationTone = customNotificationTone,
    mediaVisibility = mediaVisibility,
    disappearingMessagesDuration = disappearingMessagesDuration,
    lastMessageIsFromMe = lastMessageIsFromMe,
    lastMessageRead = lastMessageRead,
    publicKey = publicKey
)

fun ChatEntity.toModel() = ChatMessage(
    id = id,
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
    wallpaperUrl = wallpaperUrl,
    customNotificationTone = customNotificationTone,
    mediaVisibility = mediaVisibility,
    disappearingMessagesDuration = disappearingMessagesDuration,
    lastMessageIsFromMe = lastMessageIsFromMe,
    lastMessageRead = lastMessageRead,
    publicKey = publicKey
)

fun Message.toEntity(chatId: Int) = MessageEntity(id, chatId, content, senderId, senderName, timestamp, isFromMe, type, duration, mediaUri, fileName, fileSize, contactName, contactPhone, latitude, longitude, isRead, isDelivered, replyToId, replyToText, replyToSenderName, senderAvatarUrl, isStarred, isViewOnce, expiresAt, status, reactions, isEncrypted, clientId)
fun MessageEntity.toModel() = Message(id = id, chatId = chatId, content = content, senderId = senderId, senderName = senderName, timestamp = timestamp, isFromMe = isFromMe, type = type, duration = duration, mediaUri = mediaUri, fileName = fileName, fileSize = fileSize, contactName = contactName, contactPhone = contactPhone, latitude = latitude, longitude = longitude, isRead = isRead, isDelivered = isDelivered, replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName, senderAvatarUrl = senderAvatarUrl, isStarred = isStarred, isViewOnce = isViewOnce, expiresAt = expiresAt, status = status, reactions = reactions, isEncrypted = isEncrypted, clientId = clientId)

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
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/v1").removeSuffix("/api/v1/")
    return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
}
