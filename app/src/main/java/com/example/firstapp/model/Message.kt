package com.example.firstapp.model

data class Message(
    val id: Int,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val duration: String? = null,
    val mediaUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class MessageType {
    TEXT, VOICE_CALL, AUDIO_MESSAGE, IMAGE, VIDEO, DOCUMENT, LOCATION, CONTACT
}
