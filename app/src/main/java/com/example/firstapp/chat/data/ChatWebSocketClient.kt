package com.example.firstapp.chat.data

import android.util.Log
import com.example.firstapp.BuildConfig
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.chat.data.repository.mapApiMessage
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val sessionManager: SessionManager,
    private val httpClient: OkHttpClient,
    private val gson: Gson,
    private val envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0

    private val _messageFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val messageFlow = _messageFlow

    private val _eventFlow = MutableSharedFlow<WSFrame>(extraBufferCapacity = 64)
    val eventFlow = _eventFlow

    fun connect() {
        if (webSocket != null) return
        scope.launch {
            val token = sessionManager.getToken()
            if (token != null) {
                reconnectAttempts = 0
                connectWithToken(token)
            }
        }
    }

    private fun connectWithToken(token: String) {
        val baseUrl = envManager.currentWsUrl
        val wsUrl = if (baseUrl.endsWith("/")) {
            baseUrl + "ws/chat?token=" + token
        } else {
            baseUrl + "/ws/chat?token=" + token
        }

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer " + token)
            .build()

        Log.d("ChatWS", "Connecting to Chat WS: $wsUrl")
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                Log.d("ChatWS", "Connected to Chat WebSocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("ChatWS", "Received Chat WS message: $text")
                    val frame = gson.fromJson(text, WSFrame::class.java)
                    
                    // Normalize event/op for processing
                    val op = frame.op ?: frame.event
                    if (op == null) return

                    val normalizedFrame = frame.copy(op = op)
                    _eventFlow.tryEmit(normalizedFrame)

                    if (op == "INCOMING_MSG" || op == "message") {
                        val payload = frame.payload
                        if (payload != null) {
                            try {
                                val apiMsg = gson.fromJson(payload, ApiMessage::class.java)
                                val privKey = sessionManager.getPrivateKey()
                                val myUserId = sessionManager.getUserId()
                                val message = mapApiMessage(apiMsg, privKey, myUserId)
                                Log.d("ChatWS", "Decoded message: ${message.content}")
                                _messageFlow.tryEmit(message)
                            } catch (e: Exception) {
                                Log.e("ChatWS", "Failed to parse INCOMING_MSG payload", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatWS", "Error parsing frame: " + e.message)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWS", "Chat WS Failure code=${response?.code}: ${t.message}", t)
                this@ChatWebSocketClient.webSocket = null
                
                // Exponential backoff for reconnection
                if (reconnectAttempts < 5) {
                    val delayMs = (2000 * Math.pow(2.0, reconnectAttempts.toDouble())).toLong()
                    scope.launch {
                        delay(delayMs)
                        reconnectAttempts++
                        connect()
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatWS", "Closing Chat WS: $reason")
                webSocket.close(1000, null)
                this@ChatWebSocketClient.webSocket = null
            }
        })
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        val payload = HashMap<String, Any>()
        payload.put("conversation_id", conversationId)
        payload.put("state", if (isTyping) "START" else "STOP")
        
        val frame = WSFrame(op = "TYPING", payload = gson.toJsonTree(payload))
        webSocket?.send(gson.toJson(frame))
    }

    fun sendDeliveryAck(conversationId: String, messageId: String) {
        val payload = HashMap<String, Any>()
        payload.put("conversation_id", conversationId)
        payload.put("message_id", messageId)
        
        val frame = WSFrame(op = "DELIVERY_ACK", payload = gson.toJsonTree(payload))
        webSocket?.send(gson.toJson(frame))
    }

    fun sendReadAck(conversationId: String, messageId: String) {
        val payload = HashMap<String, Any>()
        payload.put("conversation_id", conversationId)
        payload.put("message_id", messageId)
        
        val frame = WSFrame(op = "READ_ACK", payload = gson.toJsonTree(payload))
        webSocket?.send(gson.toJson(frame))
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}

data class WSFrame(
    @SerializedName("op") val op: String? = null,
    @SerializedName("event") val event: String? = null,
    @SerializedName("payload") val payload: JsonElement? = null
)
