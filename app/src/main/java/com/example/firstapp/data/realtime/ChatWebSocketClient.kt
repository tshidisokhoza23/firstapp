package com.example.firstapp.data.realtime

import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiMessage
import com.example.firstapp.model.Message
import com.example.firstapp.model.MessageType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

interface ChatSocketListener {
    fun onOpen()
    fun onMessage(message: Message)
    fun onError(error: String)
}

private data class WsOutgoingMessage(
    val type: String = "message",
    val body: String,
    @SerializedName("receiver_id") val receiverId: Int
)

private data class WsIncomingMessage(
    val type: String,
    val message: ApiMessage,
    @SerializedName("contact_id") val contactId: Int
)

object ChatWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient()

    fun connect(contactId: Int, listener: ChatSocketListener) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            listener.onError("Missing auth token")
            return
        }

        val request = Request.Builder()
            .url("ws://192.168.100.7:8082/api/v1/ws/conversations/$contactId")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket?.close(1000, null)
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val payload = gson.fromJson(text, WsIncomingMessage::class.java)
                    if (payload.type == "message") {
                        listener.onMessage(mapApiMessage(payload.message))
                    }
                } catch (e: Exception) {
                    listener.onError("Failed to parse websocket message: ${e.localizedMessage}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t.localizedMessage ?: "WebSocket failed")
            }
        })
    }

    fun sendMessage(receiverId: Int, body: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(body = body, receiverId = receiverId)
        socket.send(gson.toJson(message))
    }

    fun close() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }

    private fun mapApiMessage(apiMessage: ApiMessage): Message {
        return Message(
            id = apiMessage.id,
            text = apiMessage.body,
            time = apiMessage.created_at ?: "",
            isFromMe = apiMessage.direction == "outgoing",
            type = when (apiMessage.message_type?.lowercase()) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO_MESSAGE
                "voice_call" -> MessageType.VOICE_CALL
                "location" -> MessageType.LOCATION
                "contact" -> MessageType.CONTACT
                "document" -> MessageType.DOCUMENT
                else -> MessageType.TEXT
            },
            duration = apiMessage.duration,
            mediaUri = apiMessage.media_url,
            fileName = apiMessage.file_name,
            fileSize = apiMessage.file_size,
            contactName = apiMessage.contact_name,
            contactPhone = apiMessage.contact_phone,
            latitude = apiMessage.latitude,
            longitude = apiMessage.longitude
        )
    }
}
