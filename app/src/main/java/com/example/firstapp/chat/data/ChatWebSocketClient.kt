package com.example.firstapp.chat.data

import android.util.Log
import com.example.firstapp.BuildConfig
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.data.AuthSessionManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.repository.mapApiMessage
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.HashMap

object ChatWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = ApiClient.client
    private var reconnectAttempts = 0

    private val _messageFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val messageFlow = _messageFlow

    private val _eventFlow = MutableSharedFlow<WsResponse>(extraBufferCapacity = 64)
    val eventFlow = _eventFlow

    fun connect() {
        if (webSocket != null) return
        val token = AuthSessionManager.currentAccessTokenForWebSocket(BuildConfig.API_BASE_URL) ?: return
        reconnectAttempts = 0
        connectWithToken(token)
    }

    private fun connectWithToken(token: String) {
        val wsUrl = BuildConfig.WS_BASE_URL.trimEnd('/') + "/ws/chat?token=$token"

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                Log.d("ChatWS", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val response = gson.fromJson(text, WsResponse::class.java)
                    _eventFlow.tryEmit(response)

                    if (response.type == "message") {
                        val apiMsgJson = gson.toJson(response.message)
                        val apiMsg = gson.fromJson(apiMsgJson, ApiMessage::class.java)
                        _messageFlow.tryEmit(mapApiMessage(apiMsg))
                    }
                } catch (e: RuntimeException) {
                    Log.e("ChatWS", "Error: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWS", "Failure code=${response?.code}: ${t.message}", t)
                this@ChatWebSocketClient.webSocket = null
                if (response?.code == 401 && reconnectAttempts < 1) {
                    reconnectAttempts++
                    val fresh = AuthSessionManager.refreshTokensBlocking(BuildConfig.API_BASE_URL)
                    if (!fresh.isNullOrBlank()) {
                        connectWithToken(fresh)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                this@ChatWebSocketClient.webSocket = null
            }
        })
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        val event = HashMap<String, Any>()
        event["type"] = "typing"
        event["conversation_id"] = conversationId
        event["is_typing"] = isTyping
        webSocket?.send(gson.toJson(event))
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}

data class WsResponse(
    val type: String?,
    val message: Any? = null,
    val conversation_id: String? = null,
    val user_id: String? = null,
    val message_id: String? = null,
    val is_typing: Boolean = false,
    val is_online: Boolean = false
)
