package com.example.firstapp.chat.data

import com.example.firstapp.BuildConfig
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.repository.mapApiMessage
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.*
import java.util.HashMap

object ChatWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = ApiClient.client
    
    private val _messageFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val messageFlow = _messageFlow

    fun connect() {
        if (webSocket != null) return
        val token = TokenManager.getToken() ?: return
        
        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}ws/chat")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val response = gson.fromJson(text, WsResponse::class.java)
                    if (response.type == "message") {
                        val apiMsgJson = gson.toJson(response.message)
                        val apiMsg = gson.fromJson(apiMsgJson, ApiMessage::class.java)
                        _messageFlow.tryEmit(mapApiMessage(apiMsg))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatWS", "Error: ${e.message}")
                }
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
    val type: String,
    val message: Any? = null,
    val conversation_id: String? = null,
    val is_typing: Boolean = false
)
