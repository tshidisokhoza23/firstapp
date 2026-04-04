package com.example.firstapp.model

data class Message(
    val id: Int,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val duration: String? = null
)

enum class MessageType {
    TEXT, VOICE_CALL, AUDIO_MESSAGE
}
