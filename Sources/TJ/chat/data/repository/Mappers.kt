package com.tpsk.tj.chat.data.repository

import com.tpsk.tj.chat.data.api.ChatSummary
import com.tpsk.tj.chat.data.api.ApiMessage
import com.tpsk.tj.chat.data.api.ApiCallLog
import com.tpsk.tj.chat.data.local.entity.ChatEntity
import com.tpsk.tj.chat.data.local.entity.MessageEntity
import com.tpsk.tj.chat.data.local.entity.CallLogEntity
import com.tpsk.tj.chat.data.local.entity.ContactEntity
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.chat.data.GlobalCallState
import com.tpsk.tj.common.util.CryptoUtils
import com.tpsk.tj.common.util.isIdString
import com.tpsk.tj.common.util.sanitizeDisplayName
import kotlin.collections.map
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import com.tpsk.tj.common.ui.theme.AppPrimary
import com.tpsk.tj.common.ui.theme.BoyishPurple
import com.tpsk.tj.BuildConfig
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.data.api.ApiUser
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.data.local.entity.JobEntity
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

    val name = if (!isIdString(chat.title, chat.contact_id)) {
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
        phoneNumber = if (type == ConversationType.MINGLE) null else chat.phone_number,
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

    val embeddedStory = com.tpsk.tj.chat.model.MessageMarkers.parseStory(body)
    val embeddedContact = com.tpsk.tj.chat.model.MessageMarkers.parseContact(body)
    val visibleBody = com.tpsk.tj.chat.model.MessageMarkers.strip(body).ifBlank {
        if (embeddedContact != null) "Shared Contact" else body
    }
    val timestamp = apiMessage.created_at
    val senderId = apiMessage.sender_id

    // Use currentUserId if provided to determine direction, otherwise fallback to API field
    val isFromMe = if (currentUserId != null) {
        senderId == currentUserId
    } else {
        apiMessage.direction == "outgoing"
    }

    val sName = if (!isIdString(apiMessage.sender_name, apiMessage.sender_id)) {
        apiMessage.sender_name
    } else {
        ""
    }
    val replyFromId = com.tpsk.tj.chat.model.MessageMarkers.parseStoryReplyId(apiMessage.reply_to_id)
    val story = embeddedStory
    val replyId = story?.let {
        com.tpsk.tj.chat.model.MessageMarkers.storyReplyId(it.storyId, it.userId.ifBlank { replyFromId?.userId.orEmpty() })
    } ?: apiMessage.reply_to_id?.takeIf { it.isNotBlank() }
    val replySummary = apiMessage.reply_to_text?.takeIf { it.isNotBlank() }
        ?: story?.let {
            buildString {
                append("[Story] ")
                append(it.summary.ifBlank { "Story" })
                if (it.mediaUrl.isNotBlank()) append(" [mediaUrl:${it.mediaUrl}]")
            }
        }

    return Message(
        id = apiMessage.id,
        conversationId = apiMessage.conversation_id,
        content = visibleBody,
        senderId = senderId,
        senderName = sName,
        receiverId = apiMessage.receiver_id,
        timestamp = timestamp,
        isFromMe = isFromMe,
        type = when {
            embeddedContact != null -> MessageType.CONTACT
            else -> when (apiMessage.media_type.lowercase()) {
            "system" -> MessageType.SYSTEM
            "image", "gif", "sticker" -> MessageType.IMAGE
            "video", "short_video", "short_share" -> MessageType.VIDEO
            "audio", "audio_message", "voice", "voice_note" -> MessageType.AUDIO_MESSAGE
            "voice_call", "call" -> MessageType.VOICE_CALL
            "location" -> MessageType.LOCATION
            "contact" -> MessageType.CONTACT
            "document", "file" -> MessageType.DOCUMENT
            "application_link" -> MessageType.APPLICATION_LINK
            "job_post" -> MessageType.JOB_POST
            else -> MessageType.TEXT
            }
        },
        mediaUrls = when {
            !apiMessage.media_urls.isNullOrEmpty() && apiMessage.media_urls.any { it.isNotBlank() } -> 
                apiMessage.media_urls.filter { it.isNotBlank() }.map { formatUrl(it) }
            !apiMessage.media_url.isNullOrBlank() -> listOf(formatUrl(apiMessage.media_url))
            !apiMessage.file_name.isNullOrBlank() && apiMessage.media_type.lowercase() != "text" -> {
                val fn = apiMessage.file_name
                val path = if (fn.startsWith("http") || fn.startsWith("uploads/") || fn.startsWith("/")) fn else "uploads/chat/${apiMessage.media_type.lowercase()}/$fn"
                listOf(formatUrl(path))
            }
            !apiMessage.message_text.isNullOrBlank() && (
                apiMessage.message_text.startsWith("http") ||
                apiMessage.message_text.startsWith("/") ||
                apiMessage.message_text.startsWith("uploads/") ||
                apiMessage.message_text.startsWith("chat/")
            ) -> listOf(formatUrl(apiMessage.message_text))
            else -> emptyList()
        },
        isRead = apiMessage.is_read == true || apiMessage.status.equals("read", true) || apiMessage.status == "READ",
        isDelivered = apiMessage.is_delivered == true || apiMessage.is_read == true || apiMessage.status.equals("delivered", true) || apiMessage.status.equals("read", true),
        isStarred = apiMessage.is_starred ?: false,
        status = when {
            apiMessage.is_read == true || apiMessage.status.equals("read", true) || apiMessage.status == "READ" -> MessageStatus.READ
            apiMessage.is_delivered == true || apiMessage.status.equals("delivered", true) || apiMessage.status == "DELIVERED" -> MessageStatus.DELIVERED
            apiMessage.status.equals("failed", true) || apiMessage.status == "FAILED" -> MessageStatus.FAILED
            else -> MessageStatus.SENT
        },
        reactions = apiMessage.reactions ?: emptyMap(),
        isEncrypted = isEncrypted,
        clientId = apiMessage.client_uuid,
        duration = apiMessage.duration,
        fileName = apiMessage.file_name,
        fileSize = apiMessage.file_size,
        contactName = apiMessage.contact_name?.takeIf { it.isNotBlank() } ?: embeddedContact?.name,
        contactPhone = apiMessage.contact_phone?.takeIf { it.isNotBlank() } ?: embeddedContact?.phone,
        latitude = apiMessage.latitude,
        longitude = apiMessage.longitude,
        replyToId = replyId,
        replyToText = replySummary,
        replyToSenderName = apiMessage.reply_to_sender_name?.takeIf { it.isNotBlank() }
            ?: story?.author?.takeIf { it.isNotBlank() },
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

fun ChatEntity.toModel(localContactName: String? = null, serverProfileName: String? = null) = ChatMessage(
    id = id,
    type = type,
    senderName = senderName,
    localContactName = localContactName,
    serverProfileName = serverProfileName ?: senderName,
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
    id = api.id?.takeIf { it.isNotBlank() } ?: api.session_id.orEmpty(),
    sessionId = api.session_id.orEmpty(),
    callerId = api.caller_id.orEmpty(),
    calleeId = api.callee_id.orEmpty(),
    contactId = api.contact_id.orEmpty(),
    contactName = api.contact_name.orEmpty(),
    contactAvatar = api.contact_avatar,
    callType = api.call_type?.ifBlank { "voice" } ?: "voice",
    status = api.status.orEmpty(),
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
        avatarUrl = formatUrl(api.avatar_url),
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

    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("data:") || url.startsWith("local:") || url.startsWith("file:") || url.startsWith("content:")) {
        var formatted = url
        if (formatted.contains("://localhost") || formatted.contains("://127.0.0.1")) {
            formatted = formatted
                .replace("http://127.0.0.1:8000", baseUrl)
                .replace("http://localhost:8000", baseUrl)
                .replace("http://127.0.0.1", baseUrl)
                .replace("http://localhost", baseUrl)
                .replace("127.0.0.1", targetHost)
                .replace("localhost", targetHost)
        }
        if (formatted.startsWith("http://") && baseUrl.startsWith("https://")) {
            formatted = formatted.replace("http://", "https://")
        }
        return formatted
    }
    
    var cleanUrl = if (url.startsWith("/")) url.substring(1) else url
    if (!cleanUrl.startsWith("uploads/")) {
        cleanUrl = "uploads/$cleanUrl"
    }
    
    return "$baseUrl/$cleanUrl"
}
