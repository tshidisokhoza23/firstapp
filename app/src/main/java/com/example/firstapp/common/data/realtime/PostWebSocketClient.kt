package com.example.firstapp.common.data.realtime

import android.util.Log
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.data.TokenManager
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PostWebSocketClient {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val listeners = mutableSetOf<PostSocketListener>()

    fun connect(listener: PostSocketListener) {
        listeners.add(listener)
        if (webSocket != null) return

        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val token = TokenManager.getToken() ?: return
        val url = BuildConfig.API_BASE_URL.replace("http", "ws") + "/posts/ws?token=$token"
        val request = Request.Builder().url(url).build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PostWebSocket", "Connected")
                listeners.forEach { it.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("PostWebSocket", "Message: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    val action = json.optString("action")
                    
                    when (type) {
                        "post" -> {
                            val postId = if (json.has("post_id")) json.getString("post_id") else null
                            listeners.forEach { it.onPostsEvent(action, postId) }
                        }
                        "short" -> {
                            val shortId = if (json.has("short_id")) json.getString("short_id") else null
                            listeners.forEach { it.onShortsEvent(action, shortId) }
                        }
                        "story" -> {
                            val storyId = if (json.has("story_id")) json.getString("story_id") else null
                            listeners.forEach { it.onStoriesEvent(action, storyId) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostWebSocket", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("PostWebSocket", "Failure", t)
                listeners.forEach { it.onError(t.message ?: "Unknown error") }
                this@PostWebSocketClient.webSocket = null
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                this@PostWebSocketClient.webSocket = null
            }
        })
    }

    fun disconnect(listener: PostSocketListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            webSocket?.close(1000, "Goodbye")
            webSocket = null
        }
    }
}

interface PostSocketListener {
    fun onOpen()
    fun onPostsEvent(action: String, postId: String?)
    fun onShortsEvent(action: String, shortId: String?)
    fun onStoriesEvent(action: String, storyId: String?)
    fun onError(error: String)
}
