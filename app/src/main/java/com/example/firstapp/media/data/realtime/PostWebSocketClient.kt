package com.example.firstapp.media.data.realtime

import android.util.Log
import com.example.firstapp.common.domain.auth.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

@Singleton
class PostWebSocketClient @Inject constructor(
    private val sessionManager: SessionManager,
    private val httpClient: OkHttpClient,
    private val envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
) {
    private var webSocket: WebSocket? = null
    private val listeners = CopyOnWriteArraySet<PostSocketListener>()
    private var reconnectAttempts = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(listener: PostSocketListener) {
        listeners.add(listener)
        if (webSocket != null) return

        val token = sessionManager.getToken() ?: return
        reconnectAttempts = 0
        connectWithToken(token)
    }

    private fun connectWithToken(token: String) {
        val baseUrl = envManager.currentWsUrl
        val wsUrl = (if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/") + "ws/posts?token=$token"
        
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d("PostWebSocket", "Connecting to $wsUrl")
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PostWebSocket", "Connected")
                reconnectAttempts = 0
                listeners.forEach { it.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("PostWebSocket", "Message: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    val action = json.optString("action")

                    val likes = if (json.has("likes")) json.getLong("likes") else null
                    val reposts = if (json.has("reposts")) json.getLong("reposts") else null
                    val shares = if (json.has("shares")) json.getLong("shares") else null
                    val views = if (json.has("views")) json.getLong("views") else null

                    when (type) {
                        "post", "posts_event" -> {
                            val postId = if (json.has("post_id")) json.getString("post_id") else null
                            listeners.forEach { it.onPostsEvent(action, postId, likes, reposts, shares, views) }
                        }
                        "short", "shorts_event" -> {
                            val shortId = if (json.has("short_id")) json.getString("short_id") else null
                            listeners.forEach { it.onShortsEvent(action, shortId) }
                        }
                        "story", "stories_event" -> {
                            val storyId = if (json.has("story_id")) json.getString("story_id") else null
                            listeners.forEach { it.onStoriesEvent(action, storyId) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostWebSocket", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("PostWebSocket", "Failure: ${t.message}", t)
                listeners.forEach { it.onError(t.message ?: "Unknown error") }
                this@PostWebSocketClient.webSocket = null
                
                // Exponential backoff for reconnection
                if (reconnectAttempts < 5) {
                    val delayMs = (2000 * Math.pow(2.0, reconnectAttempts.toDouble())).toLong()
                    scope.launch {
                        delay(delayMs)
                        reconnectAttempts++
                        val token = sessionManager.getToken()
                        if (token != null) {
                            connectWithToken(token)
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("PostWebSocket", "Closing: $reason")
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
    fun onPostsEvent(action: String, postId: String?, likes: Long? = null, reposts: Long? = null, shares: Long? = null, views: Long? = null)
    fun onShortsEvent(action: String, shortId: String?)
    fun onStoriesEvent(action: String, storyId: String?)
    fun onError(error: String)
}
