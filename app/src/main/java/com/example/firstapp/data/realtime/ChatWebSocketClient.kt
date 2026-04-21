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
    private val reconnectScope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private val activeContactId = AtomicReference<Int?>(null)
    private var activeListener: ChatSocketListener? = null
    @Volatile
    private var manualClose = false
    @Volatile
    private var refreshInProgress = false

    fun connect(contactId: Int, listener: ChatSocketListener) {
        activeContactId.set(contactId)
        activeListener = listener
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            listener.onError("Missing auth token")
            return
        }

        manualClose = false

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}ws/conversations/$contactId")
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
                this@ChatWebSocketClient.webSocket = null
                if (response?.code == 401) {
                    if (refreshTokenIfPossible()) {
                        scheduleReconnect(immediate = true)
                    } else {
                        listener.onError("Chat auth expired. Please log in again.")
                    }
                    return
                }
                listener.onError(t.localizedMessage ?: "WebSocket failed")
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
        val listener = activeListener ?: return
        reconnectJob = reconnectScope.launch {
            if (!immediate) {
                delay(2000)
            }
            if (!manualClose && webSocket == null) {
                connect(contactId, listener)
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
        val message = WsOutgoingMessage(body = body, receiverId = receiverId)
        socket.send(gson.toJson(message))
    }

    fun close() {
        manualClose = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        activeContactId.set(null)
        activeListener = null
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
