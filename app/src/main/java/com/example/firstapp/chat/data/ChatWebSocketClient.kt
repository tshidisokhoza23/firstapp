package com.example.firstapp.chat.data

import com.example.firstapp.BuildConfig
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.data.api.ChatSummary
import com.example.firstapp.common.data.api.TokenRefreshRequest
import com.example.firstapp.common.data.repository.mapApiMessage
import com.example.firstapp.common.data.repository.mapApiChatSummary
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.chat.model.MessageStatus
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
    fun onMessage(contactId: Int, message: Message, isGroup: Boolean)
    fun onMessageEdited(contactId: Int, message: Message)
    fun onMessageDeleted(contactId: Int, messageId: String) // Changed from Int
    fun onReaction(contactId: Int, messageId: String, reactions: Map<String, Int>) // Changed from Int
    fun onTyping(contactId: Int, isTyping: Boolean)
    fun onRead(contactId: Int)
    fun onDelivered(contactId: Int, messageId: String) // Changed from Int
    fun onPresence(contactId: Int, isOnline: Boolean, lastSeen: Long)
    fun onError(error: String)
    fun onNewChat(chat: ChatMessage)
}

private data class WsOutgoingMessage(
    val type: String = "message",
    @SerializedName("client_id") val clientId: String? = null,
    val body: String? = null,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("is_typing") val isTyping: Boolean? = null
)

private data class WsIncomingMessage(
    val type: String,
    val message: ApiMessage? = null,
    @SerializedName("client_id") val clientId: String? = null,
    @SerializedName("contact_id") val contactId: Int? = null,
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("message_id") val messageId: String? = null, // Changed from Int?
    val reactions: Map<String, Int>? = null,
    @SerializedName("is_typing") val isTyping: Boolean? = null,
    @SerializedName("is_online") val isOnline: Boolean? = null,
    @SerializedName("last_seen") val lastSeen: Long? = null,
    val chat: ChatSummary? = null
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

        val url = "${BuildConfig.WS_BASE_URL}ws/conversations/$contactId"
        android.util.Log.d("ChatWebSocket", "Connecting to: $url")
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listeners.forEach { it.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("ChatWebSocket", "Received: $text")
                try {
                    val payload = gson.fromJson(text, WsIncomingMessage::class.java)
                    val chatId = payload.groupId ?: payload.contactId ?: 0
                    when (payload.type) {
                        "message" -> {
                            payload.message?.let { msg ->
                                // Ensure clientId from payload is propagated to apiMessage if missing
                                val enrichedApiMessage = if (msg.client_id == null && payload.clientId != null) {
                                    msg.copy(client_id = payload.clientId)
                                } else msg
                                
                                val mapped = mapApiMessage(enrichedApiMessage)
                                val isGroup = payload.groupId != null && payload.groupId != 0
                                listeners.forEach { it.onMessage(chatId, mapped, isGroup) }
                                // Notify server that we've received the message (delivered)
                                if (!mapped.isFromMe) {
                                    sendDelivered(chatId, mapped.id)
                                }
                            }
                        }
                        "edit" -> {
                            payload.message?.let { msg ->
                                val mapped = mapApiMessage(msg)
                                listeners.forEach { it.onMessageEdited(chatId, mapped) }
                            }
                        }
                        "reaction" -> {
                            payload.messageId?.let { msgId ->
                                if (payload.reactions != null) {
                                    listeners.forEach { it.onReaction(chatId, msgId, payload.reactions) }
                                }
                            }
                        }
                        "delete" -> {
                            payload.messageId?.let { msgId ->
                                listeners.forEach { it.onMessageDeleted(chatId, msgId) }
                            }
                        }
                        "typing" -> {
                            listeners.forEach { it.onTyping(chatId, payload.isTyping ?: false) }
                        }
                        "read" -> {
                            listeners.forEach { it.onRead(chatId) }
                        }
                        "delivered" -> {
                            payload.messageId?.let { msgId ->
                                listeners.forEach { it.onDelivered(chatId, msgId) }
                            }
                        }
                        "presence" -> {
                            val lastSeenLong = payload.lastSeen ?: 0L
                            listeners.forEach { it.onPresence(chatId, payload.isOnline ?: false, lastSeenLong) }
                        }
                        "new_chat" -> {
                            payload.chat?.let { summary ->
                                val chat = mapApiChatSummary(summary)
                                listeners.forEach { it.onNewChat(chat) }
                            }
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
                ApiClient.authApi.refresh(TokenRefreshRequest(refresh_token = refreshToken))
            }
            if (!response.isSuccessful) {
                if (response.code() == 401 || response.code() == 400) {
                    TokenManager.clear()
                }
                return false
            }
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

    fun sendDelivered(receiverId: Int, messageId: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "delivered", receiverId = receiverId, body = messageId)
        socket.send(gson.toJson(message))
    }

    fun sendReaction(receiverId: Int, messageId: String, emoji: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "reaction", receiverId = receiverId, clientId = messageId, body = emoji)
        socket.send(gson.toJson(message))
    }

    fun sendEdit(receiverId: Int, messageId: String, newBody: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "edit", receiverId = receiverId, clientId = messageId, body = newBody)
        socket.send(gson.toJson(message))
    }

    fun sendDelete(receiverId: Int, messageId: String) {
        val socket = webSocket ?: return
        val message = WsOutgoingMessage(type = "delete", receiverId = receiverId, clientId = messageId)
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

}
