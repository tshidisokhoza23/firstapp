package com.example.firstapp.chat.data.repository

import com.example.firstapp.chat.data.api.ChatSummary
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.data.api.ApiCallLog
import com.example.firstapp.chat.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.local.entity.MessageEntity
import com.example.firstapp.chat.data.local.entity.CallLogEntity
import com.example.firstapp.chat.data.local.entity.ContactEntity
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.common.util.CryptoUtils
import kotlin.collections.map
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.data.api.ApiUser
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.chat.jobs.data.local.entity.JobEntity
import androidx.compose.ui.graphics.Color

fun mapApiChatSummary(chat: ChatSummary): ChatMessage {
    val timestamp = chat.time
    val id = chat.conversation_id
             
    val rawType = chat.type.trim().uppercase()
    Log.d("Mappers", "mapApiChatSummary: id=$id, rawType='$rawType', title='${chat.title}'")
    
    val type = when {
        rawType.contains("MINGLE") || id.startsWith("mingle_") -> ConversationType.MINGLE
        rawType.contains("GROUP") -> ConversationType.GROUP
        rawType.contains("CHANNEL") -> ConversationType.CHANNEL
        rawType.contains("JOB") -> ConversationType.JOB_CHAT
        else -> ConversationType.DIRECT
    }

    val name = if (chat.title.isNotBlank() && chat.title.lowercase() != "unknown" && chat.title != "null" && chat.title != chat.contact_id) {
        chat.title
    } else {
        ""
    }

    return ChatMessage(
        id = id,
        type = type,
        senderName = name,
        lastMessage = chat.last_message,
        timestamp = timestamp,
        unreadCount = chat.unread_count,
        avatarUrl = formatUrl(chat.avatar_url),
        avatarColor = BoyishPurple,
        isMuted = chat.is_muted,
        isPinned = chat.is_pinned,
        pinnedMessageId = chat.pinned_message_id,
        isOnline = chat.is_online,
        lastSeen = chat.last_seen,
        isSubscribed = chat.is_subscribed || chat.is_following,
        isGroup = type == ConversationType.GROUP,
        isAdmin = chat.is_admin,
        adminsCanPost = chat.admins_can_post,
        otherUserId = chat.contact_id,
        phoneNumber = chat.phone_number, // Ensure this is mapped
        createdBy = chat.created_by
    )
}

fun mapApiMessage(apiMessage: ApiMessage, privateKeyStr: String? = null, currentUserId: String? = null): Message {
    var body = apiMessage.message_text
    val isEncrypted = body.startsWith("ENC:")
    if (isEncrypted && privateKeyStr != null) {
        val encryptedText = body.removePrefix("ENC:")
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
    } else if (isEncrypted) {
        body = "[Encrypted Message]"
    }

    val timestamp = apiMessage.created_at
    val senderId = apiMessage.sender_id

    // Use currentUserId if provided to determine direction, otherwise fallback to API field
    val isFromMe = if (currentUserId != null) {
        senderId == currentUserId
    } else {
        apiMessage.direction == "outgoing"
    }

    val sName = if (apiMessage.sender_name.isNotBlank() && apiMessage.sender_name.lowercase() != "unknown" && apiMessage.sender_name != "null" && apiMessage.sender_name != apiMessage.sender_id) {
        apiMessage.sender_name
    } else {
        apiMessage.contact_phone ?: ""
    }

    return Message(
        id = apiMessage.id,
        conversationId = apiMessage.conversation_id,
        content = body,
        senderId = senderId,
        senderName = sName,
        receiverId = apiMessage.receiver_id,
        timestamp = timestamp,
        isFromMe = isFromMe,
        type = when (apiMessage.media_type.lowercase()) {
            "image" -> MessageType.IMAGE
            "video", "short_video" -> MessageType.VIDEO
            "audio", "audio_message" -> MessageType.AUDIO_MESSAGE
            "voice_call", "call" -> MessageType.VOICE_CALL
            "location" -> MessageType.LOCATION
            "contact" -> MessageType.CONTACT
            "document", "file" -> MessageType.DOCUMENT
            "application_link" -> MessageType.APPLICATION_LINK
            "job_post" -> MessageType.JOB_POST
            else -> MessageType.TEXT
        },
        mediaUrls = if (apiMessage.media_urls != null && apiMessage.media_urls.isNotEmpty()) {
            apiMessage.media_urls.map { formatUrl(it) }
        } else if (!apiMessage.file_name.isNullOrBlank() && apiMessage.media_type.lowercase() != "text") {
            // Heuristic: reconstruct URL if media_urls is missing but file_name is present
            // Server structure: /uploads/chat/FILENAME
            listOf(formatUrl("chat/${apiMessage.file_name}"))
        } else {
            emptyList()
        },
        isRead = false,
        isDelivered = true,
        isStarred = apiMessage.is_starred ?: false,
        status = MessageStatus.SENT,
        reactions = apiMessage.reactions ?: emptyMap(),
        isEncrypted = isEncrypted,
        clientId = apiMessage.client_uuid,
        duration = apiMessage.duration,
        fileName = apiMessage.file_name,
        fileSize = apiMessage.file_size,
        contactName = apiMessage.contact_name,
        contactPhone = apiMessage.contact_phone,
        latitude = apiMessage.latitude,
        longitude = apiMessage.longitude,
        replyToId = apiMessage.reply_to_id,
        replyToText = apiMessage.reply_to_text,
        replyToSenderName = apiMessage.reply_to_sender_name,
        applicantProfile = apiMessage.applicant_profile?.let { 
            ApplicantProfile(it.full_name, it.bio, it.cv_url) 
        },
        jobDetails = apiMessage.job_details?.let { 
            JobDetails(it.title) 
        },
        linkPreview = apiMessage.link_preview,
        audioAmplitudes = apiMessage.audio_amplitudes,
        scheduledAt = apiMessage.scheduled_at
    )
}

fun ChatMessage.toEntity(): ChatEntity {
    Log.d("Mappers", "Mapping ChatMessage to ChatEntity: id=$id, type=$type, name='$senderName'")
    return ChatEntity(
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
        pinnedMessageId = pinnedMessageId,
        isTyping = isTyping,
        isGroup = isGroup,
        isArchived = isArchived,
        isAdmin = isAdmin,
        adminsCanPost = adminsCanPost,
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
        phoneNumber = phoneNumber,
        otherUserId = otherUserId
    )
}

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
    pinnedMessageId = pinnedMessageId,
    isPinned = isPinned,
    isTyping = isTyping,
    isGroup = isGroup,
    isArchived = isArchived,
    isAdmin = isAdmin,
    adminsCanPost = adminsCanPost,
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
    phoneNumber = phoneNumber,
    otherUserId = otherUserId,
    isMingle = type == ConversationType.MINGLE || id.startsWith("mingle_")
)

fun Message.toEntity(chatId: String) = MessageEntity(
    id = id,
    conversationId = chatId,
    content = content,
    senderId = senderId,
    senderName = senderName,
    receiverId = receiverId,
    timestamp = timestamp,
    isFromMe = isFromMe,
    type = type,
    mediaUrls = mediaUrls,
    isRead = isRead,
    isDelivered = isDelivered,
    isStarred = isStarred,
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
    replyToSenderName = replyToSenderName,
    applicantName = applicantProfile?.full_name,
    applicantBio = applicantProfile?.bio,
    jobTitle = jobDetails?.title,
    linkPreview = linkPreview,
    audioAmplitudes = audioAmplitudes
)

fun MessageEntity.toModel() = Message(
    id = id,
    conversationId = conversationId,
    content = content,
    senderId = senderId,
    senderName = senderName ?: "",
    receiverId = receiverId,
    timestamp = timestamp,
    isFromMe = isFromMe,
    type = type,
    mediaUrls = mediaUrls,
    isRead = isRead,
    isDelivered = isDelivered,
    isStarred = isStarred,
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
    replyToSenderName = replyToSenderName,
    applicantProfile = if (applicantName != null) ApplicantProfile(applicantName, applicantBio, null) else null,
    jobDetails = if (jobTitle != null) JobDetails(jobTitle) else null,
    linkPreview = linkPreview,
    audioAmplitudes = audioAmplitudes
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

fun Contact.toEntity() = ContactEntity(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey, isFollowing)
fun ContactEntity.toModel() = Contact(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey, isFollowing)

fun mapApiContact(api: ApiUser): Contact {
    return Contact(
        id = api.id,
        username = api.username,
        displayName = api.display_name,
        avatarUrl = api.avatar_url,
        phoneNumber = api.phone_number ?: "",
        isVerified = api.is_verified,
        bio = api.bio,
        publicKey = api.public_key,
        isFollowing = api.is_following
    )
}

fun formatUrl(url: String?): String {
    if (url == null || url.isBlank()) return ""
    
    val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/v1/").removeSuffix("/")
    val baseUri = try { android.net.Uri.parse(baseUrl) } catch (e: Exception) { null }
    val targetHost = baseUri?.host ?: "192.168.100.7"

    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("data:") || url.startsWith("local:") || url.startsWith("file:")) {
        if (url.contains("://localhost") || url.contains("://127.0.0.1")) {
            return url.replace("localhost", targetHost).replace("127.0.0.1", targetHost)
        }
        return url
    }
    
    var cleanUrl = if (url.startsWith("/")) url.substring(1) else url
    if (!cleanUrl.startsWith("uploads/")) {
        cleanUrl = "uploads/$cleanUrl"
    }
    
    return "$baseUrl/$cleanUrl"
}
