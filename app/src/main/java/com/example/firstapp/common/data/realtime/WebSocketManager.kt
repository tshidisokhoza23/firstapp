package com.example.firstapp.common.data.realtime

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.domain.auth.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random

/**
 * Multiplexed WebSocket Manager
 * Handles real-time communication for Chat, Feed, Presence, and Job events.
 * Features: Automatic reconnect with jitter, heartbeats, lifecycle awareness, and topic routing.
 */
@Serializable
data class WSEnvelope(
    val topic: String,
    val event: String,
    val payload: JsonObject? = null
)

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
) : DefaultLifecycleObserver {

    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _events = MutableSharedFlow<WSEnvelope>(extraBufferCapacity = 100)
    val events: SharedFlow<WSEnvelope> = _events.asSharedFlow()

    private val webSocketRef = AtomicReference<WebSocket?>(null)
    private var isConnected = false
    private var retryCount = 0
    private var heartbeatJob: Job? = null

    init {
        // Observe app lifecycle to connect/disconnect automatically
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@WebSocketManager)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        connect()
    }

    override fun onStop(owner: LifecycleOwner) {
        disconnect()
    }

    @Synchronized
    fun connect() {
        if (isConnected || webSocketRef.get() != null) return
        
        val token = sessionManager.getToken() ?: return
        
        val wsBaseUrl = BuildConfig.WS_BASE_URL.removeSuffix("/")
        val url = "$wsBaseUrl/ws?token=$token"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("WebSocketManager", "WebSocket Connected")
                isConnected = true
                retryCount = 0
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val envelope = json.decodeFromString<WSEnvelope>(text)
                        _events.emit(envelope)
                    } catch (e: Exception) {
                        Log.e("WebSocketManager", "Failed to parse WS message: $text", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "WebSocket Closing: $reason")
                isConnected = false
                webSocketRef.set(null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "WebSocket Failure: ${t.message}", t)
                isConnected = false
                webSocketRef.set(null)
                handleReconnect()
            }
        }

        webSocketRef.set(okHttpClient.newWebSocket(request, listener))
    }

    private fun handleReconnect() {
        // Only reconnect if the app is in the foreground
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        
        val delayMs = (2.0.pow(retryCount.toDouble()) * 1000).toLong()
            .coerceAtMost(30000L) + Random.nextLong(1000, 3000) // Base delay + jitter
        
        retryCount++
        
        scope.launch {
            delay(delayMs)
            Log.w("WebSocketManager", "Attempting reconnect #$retryCount")
            connect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(30000) // 30s heartbeat
                send("system", "ping")
            }
        }
    }

    fun send(topic: String, event: String, payload: JsonObject? = null) {
        scope.launch {
            val envelope = WSEnvelope(topic, event, payload)
            val text = json.encodeToString(envelope)
            webSocketRef.get()?.send(text) ?: Log.e("WebSocketManager", "Failed to send: WebSocket disconnected")
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        webSocketRef.get()?.close(1000, "App backgrounded")
        webSocketRef.set(null)
        isConnected = false
    }
    
    /**
     * Subscribe to a specific topic
     */
    fun subscribe(topic: String): Flow<WSEnvelope> = events
        .filter { it.topic == topic }
}
