package com.example.firstapp.chat.model

data class Message(
    val id: String, // Changed from Int to String (UUID)
    val chatId: Int, // Required by rule 6
    val content: String, // Required by rule 6 (previously 'text')
    val senderName: String? = null,
    val senderId: Int, // Required by rule 6 (previously senderId: Int?)
    val timestamp: Long, // Required by rule 6 (numeric, replaces 'time: String')
    val isFromMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val duration: String? = null,
    val mediaUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val senderAvatarUrl: String? = null,
    val isStarred: Boolean = false,
    val isViewOnce: Boolean = false,
    val expiresAt: Long? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, Int> = emptyMap(),
    val isEncrypted: Boolean = false,
    val clientId: String? = null // Keeping for backward compatibility or reconciliation
)

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED
}

enum class MessageType {
    TEXT, VOICE_CALL, AUDIO_MESSAGE, IMAGE, VIDEO, DOCUMENT, LOCATION, CONTACT
}
