package com.example.firstapp.data.realtime

import com.example.firstapp.BuildConfig
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.ApiMessage
import com.example.firstapp.data.api.TokenRefreshRequest
import com.example.firstapp.model.Message
import com.example.firstapp.model.MessageType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

interface ChatSocketListener {
    fun onOpen()
    fun onMessage(contactId: Int, message: Message)
    fun onMessageEdited(contactId: Int, message: Message)
    fun onMessageDeleted(contactId: Int, messageId: Int)
    fun onReaction(contactId: Int, messageId: Int, reactions: Map<String, Int>)
    fun onTyping(contactId: Int, isTyping: Boolean)
    fun onRead(contactId: Int)
    fun onPresence(contactId: Int, isOnline: Boolean, lastSeen: String?)
    fun onError(error: String)
}

private data class WsOutgoingMessage(
    val type: String = "message",
    val body: String? = null,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("is_typing") val isTyping: Boolean? = null
)

private data class WsIncomingMessage(
    val type: String,
    val message: ApiMessage? = null,
    @SerializedName("contact_id") val contactId: Int,
    @SerializedName("message_id") val messageId: Int? = null,
    val reactions: Map<String, Int>? = null,
    @SerializedName("is_typing") val isTyping: Boolean? = null,
    @SerializedName("is_online") val isOnline: Boolean? = null,
    @SerializedName("last_seen") val lastSeen: String? = null
)

object ChatWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = ApiClient.client
    private val reconnectScope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private val activeContactId = AtomicReference<Int?>(null)
    private val listeners = java.util.concurrent.CopyOnWriteArraySet<ChatSocketListener>()
    @Volatile
    private var manualClose = false
    @Volatile
    private var refreshInProgress = false

    fun connect(contactId: Int, listener: ChatSocketListener) {
        activeContactId.set(contactId)
        listeners.add(listener)
        ensureConnected(contactId)
    }

    fun disconnect(listener: ChatSocketListener) {
        listeners.remove(listener)
    }

    private fun ensureConnected(contactId: Int) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            listeners.forEach { it.onError("Missing auth token") }
            return
        }

        if (webSocket != null) return

        manualClose = false

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}ws/conversations/$contactId")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listeners.forEach { it.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val payload = gson.fromJson(text, WsIncomingMessage::class.java)
                    when (payload.type) {
                        "message" -> {
                            payload.message?.let { msg ->
                                val mapped = mapApiMessage(msg)
                                listeners.forEach { it.onMessage(payload.contactId, mapped) }
                            }
                        }
                        "edit" -> {
                            payload.message?.let { msg ->
                                val mapped = mapApiMessage(msg)
                                listeners.forEach { it.onMessageEdited(payload.contactId, mapped) }
                            }
                        }
                        "reaction" -> {
                            payload.message?.let { msg ->
                                if (msg.reactions != null) {
                                    listeners.forEach { it.onReaction(payload.contactId, msg.id, msg.reactions) }
                                }
                            }
                        }
                        "delete" -> {
                            payload.message?.let { msg ->
                                listeners.forEach { it.onMessageDeleted(payload.contactId, msg.id) }
                            }
                        }
                        "typing" -> {
                            listeners.forEach { it.onTyping(payload.contactId, payload.isTyping ?: false) }
                        }
                        "read" -> {
                            listeners.forEach { it.onRead(payload.contactId) }
                        }
                        "presence" -> {
                            listeners.forEach { it.onPresence(payload.contactId, payload.isOnline ?: false, payload.lastSeen) }
                        }
                    }
                } catch (e: Exception) {
                    listeners.forEach { it.onError("Failed to parse websocket message: ${e.localizedMessage}") }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@ChatWebSocketClient.webSocket = null
                if (response?.code == 401) {
                    if (refreshTokenIfPossible()) {
                        scheduleReconnect(immediate = true)
                    } else {
                        listeners.forEach { it.onError("Chat auth expired. Please log in again.") }
                    }
                    return
                }
                listeners.forEach { it.onError(t.localizedMessage ?: "WebSocket failed") }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@ChatWebSocketClient.webSocket = null
                if (!manualClose) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (manualClose) return
        if (reconnectJob?.isActive == true) return
        val contactId = activeContactId.get() ?: return
        reconnectJob = reconnectScope.launch {
            if (!immediate) {
                delay(2000)
            }
            if (!manualClose && webSocket == null) {
                ensureConnected(contactId)
            }
        }
    }

    private fun refreshTokenIfPossible(): Boolean {
        if (refreshInProgress) return false
        val refreshToken = TokenManager.getRefreshToken() ?: return false
        refreshInProgress = true
        return try {
            val response = runBlocking {
                ApiClient.apiService.refresh(TokenRefreshRequest(refresh_token = refreshToken))
            }
            if (!response.isSuccessful) return false
            val body = response.body() ?: return false
            if (body.token.isBlank() || body.refreshToken.isBlank()) return false
            TokenManager.setToken(body.token)
            TokenManager.setRefreshToken(body.refreshToken)
            true
        } catch (_: Exception) {
            false
        } finally {
            refreshInProgress = false
        }
    }

    fun sendMessage(receiverId: Int, body: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "message", body = body, receiverId = receiverId)
        socket.send(gson.toJson(message))
    }

    fun sendTyping(receiverId: Int, isTyping: Boolean) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "typing", receiverId = receiverId, isTyping = isTyping)
        socket.send(gson.toJson(message))
    }

    fun sendRead(receiverId: Int) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "read", receiverId = receiverId)
        socket.send(gson.toJson(message))
    }

    fun close() {
        manualClose = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        activeContactId.set(null)
        listeners.clear()
    }

    private fun mapApiMessage(apiMessage: ApiMessage): Message {
        return Message(
            id = apiMessage.id,
            text = apiMessage.body,
            senderId = apiMessage.sender_id,
            senderName = apiMessage.sender_name,
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
            longitude = apiMessage.longitude,
            isRead = apiMessage.read_at != null,
            replyToId = apiMessage.parent_id,
            replyToText = apiMessage.parent_body,
            replyToSenderName = apiMessage.parent_sender_name,
            reactions = apiMessage.reactions ?: emptyMap()
        )
    }
}
