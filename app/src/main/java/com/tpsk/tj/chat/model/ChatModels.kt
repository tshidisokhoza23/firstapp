package com.tpsk.tj.chat.model

import androidx.compose.ui.graphics.Color
import com.tpsk.tj.chat.data.GlobalCallState
import com.tpsk.tj.common.model.ConversationType
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
    val localContactName: String? = null,
    val serverProfileName: String? = null,
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
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
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
    val otherUserId: String? = null,
    val isMingle: Boolean = false
) {
    /**
     * True if the contact is saved in the local device contacts / address book or has a known profile name.
     */
    val isSavedContact: Boolean
        get() = !localContactName.isNullOrBlank() 
            || (!senderName.isBlank() && !com.tpsk.tj.common.util.isIdString(senderName, otherUserId ?: id))
            || (!serverProfileName.isNullOrBlank() && !com.tpsk.tj.common.util.isIdString(serverProfileName, otherUserId ?: id))

    /**
     * Display Name Resolution Hierarchy:
     * 1. Local Device Contact Name (Address Book)
     * 2. Server Profile Name / User Display Name
     * 3. Formatted E.164 Phone Number
     * 4. Conversation Type Default (Group / Channel)
     */
    val displayName: String
        get() {
            if (!localContactName.isNullOrBlank()) {
                return localContactName.trim()
            }
            val profile = serverProfileName?.takeIf { !com.tpsk.tj.common.util.isIdString(it, otherUserId ?: id) }
                ?: senderName.takeIf { !com.tpsk.tj.common.util.isIdString(it, otherUserId ?: id) }
            if (!profile.isNullOrBlank()) {
                return profile.trim()
            }
            if (!phoneNumber.isNullOrBlank()) {
                return com.tpsk.tj.common.util.ContactUtils.formatDisplayPhone(phoneNumber)
            }
            return when (type) {
                ConversationType.GROUP -> "Group"
                ConversationType.CHANNEL -> "Channel"
                else -> ""
            }
        }

    /**
     * Profile name hint (~Name) shown alongside unsaved phone numbers.
     */
    val profileHint: String?
        get() {
            if (isSavedContact) return null
            val profile = serverProfileName?.takeIf { !com.tpsk.tj.common.util.isIdString(it, otherUserId ?: id) }
                ?: senderName.takeIf { !com.tpsk.tj.common.util.isIdString(it, otherUserId ?: id) }
            return if (!profile.isNullOrBlank() && profile != displayName) "~$profile" else null
        }
}

/**
 * Same name the chat list shows: saved contact, then profile name, then phone.
 * A phone is never preferred when a real name is already known.
 */
fun ChatMessage.consistentName(): String {
    val key = otherUserId ?: id
    val cached = sequenceOf(otherUserId, id)
        .filterNotNull()
        .map { GlobalCallState.getContactName(it) }
        .firstOrNull { !it.isNullOrBlank() && !com.tpsk.tj.common.util.isIdString(it, key) && !com.tpsk.tj.common.util.looksLikePhone(it) }
    val current = displayName
    if (!cached.isNullOrBlank() && (current.isBlank() || com.tpsk.tj.common.util.looksLikePhone(current) || com.tpsk.tj.common.util.isIdString(current, key))) {
        return cached
    }
    if (current.isNotBlank() && !com.tpsk.tj.common.util.isIdString(current, key)) return current
    return cached ?: phoneNumber.orEmpty()
}
