package com.example.firstapp.data.realtime

import com.example.firstapp.data.TokenManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

interface PostSocketListener {
    fun onOpen()
    fun onPostsEvent(action: String, postId: Int?)
    fun onError(error: String)
}

private data class WsPostEvent(
    val type: String,
    val action: String,
    @SerializedName("post_id") val postId: Int?
)

object PostWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient()

    fun connect(listener: PostSocketListener) {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            listener.onError("Missing auth token")
            return
        }

        val request = Request.Builder()
            .url("ws://192.168.100.7:8082/api/v1/ws/posts")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket?.close(1000, null)
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, WsPostEvent::class.java)
                    if (event.type == "posts_event") {
                        listener.onPostsEvent(event.action, event.postId)
                    }
                } catch (e: Exception) {
                    listener.onError("Failed to parse posts websocket message: ${e.localizedMessage}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t.localizedMessage ?: "Posts WebSocket failed")
            }
        })
    }

    fun close() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}
