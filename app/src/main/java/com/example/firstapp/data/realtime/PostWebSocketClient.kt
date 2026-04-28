package com.example.firstapp.data.realtime

import com.example.firstapp.BuildConfig
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.TokenRefreshRequest
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.concurrent.CopyOnWriteArraySet
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

interface PostSocketListener {
    fun onOpen()
    fun onPostsEvent(action: String, postId: Int?)
    fun onShortsEvent(action: String, shortId: Int?)
    fun onError(error: String)
}

private data class WsEvent(
    val type: String,
    val action: String,
    @SerializedName("post_id") val postId: Int?,
    @SerializedName("short_id") val shortId: Int?
)

object PostWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = ApiClient.client
    private val listeners = CopyOnWriteArraySet<PostSocketListener>()
    private val reconnectScope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    @Volatile
    private var manualClose = false
    @Volatile
    private var refreshInProgress = false

    fun connect(listener: PostSocketListener) {
        listeners.add(listener)
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            notifyError("Missing auth token")
            return
        }

        if (webSocket != null) {
            return
        }

        manualClose = false

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}ws/posts")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listeners.forEach { it.onOpen() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, WsEvent::class.java)
                    when (event.type) {
                        "posts_event" -> {
                            listeners.forEach { it.onPostsEvent(event.action, event.postId) }
                        }
                        "shorts_event" -> {
                            listeners.forEach { it.onShortsEvent(event.action, event.shortId) }
                        }
                    }
                } catch (e: Exception) {
                    notifyError("Failed to parse websocket message: ${e.localizedMessage}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@PostWebSocketClient.webSocket = null
                if (response?.code == 401) {
                    if (refreshTokenIfPossible()) {
                        scheduleReconnect(immediate = true)
                    } else {
                        notifyError("Posts auth expired. Please log in again.")
                    }
                    return
                }
                notifyError(t.localizedMessage ?: "Posts WebSocket failed")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@PostWebSocketClient.webSocket = null
                if (!manualClose) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun disconnect(listener: PostSocketListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            close()
        }
    }

    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (manualClose || listeners.isEmpty()) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = reconnectScope.launch {
            if (!immediate) {
                delay(2000)
            }
            if (!manualClose && webSocket == null && listeners.isNotEmpty()) {
                connect(listeners.first())
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

    fun close() {
        manualClose = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        listeners.clear()
    }
}
