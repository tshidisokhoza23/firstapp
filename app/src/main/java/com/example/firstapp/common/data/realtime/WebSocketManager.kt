package com.example.firstapp.common.data.realtime

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.firstapp.common.domain.auth.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.channels.BufferOverflow

@Serializable
data class WSEnvelope(
    val topic: String? = null,
    val event: String? = null,
    val payload: JsonObject? = null,
    val op: String? = null
)

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) : DefaultLifecycleObserver {

    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _events = MutableSharedFlow<WSEnvelope>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<WSEnvelope> = _events.asSharedFlow()

    private val webSocketRef = AtomicReference<WebSocket?>(null)
    private var isConnected = false
    private var retryCount = 0
    private var heartbeatJob: Job? = null
    private var isManualDisconnect = false

    init {
        // Observe process lifecycle to manage connection automatically
        // Use GlobalScope or similar to ensure the observer is added on Main thread once
        CoroutineScope(Dispatchers.Main).launch {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@WebSocketManager)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isManualDisconnect = false
        connect()
    }

    override fun onStop(owner: LifecycleOwner) {
        disconnect()
    }

    @Synchronized
    fun connect() {
        if (isConnected || isManualDisconnect) return
        
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.w("WebSocketManager", "No token available, skipping connection")
            return
        }

        // Multiplexed endpoint - handles all topics via single connection
        val wsUrl = "wss://firstapp-backend.fly.dev/api/v1/ws/posts?token=$token"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        Log.d("WebSocketManager", "Connecting to $wsUrl")
        okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("WebSocketManager", "WebSocket Opened")
                webSocketRef.set(webSocket)
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
                        Log.e("WebSocketManager", "Failed to parse message: $text", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WebSocketManager", "WebSocket Closing: $code / $reason")
                webSocket.close(1000, null)
                handleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "WebSocket Failure", t)
                handleReconnect()
            }
        })
    }

    private fun handleReconnect() {
        isConnected = false
        webSocketRef.set(null)
        heartbeatJob?.cancel()

        if (isManualDisconnect) return

        val delayMs = calculateExponentialBackoff(retryCount++)
        Log.d("WebSocketManager", "Scheduling reconnect in ${delayMs}ms (attempt $retryCount)")
        
        scope.launch {
            delay(delayMs)
            connect()
        }
    }

    private fun calculateExponentialBackoff(retries: Int): Long {
        val baseDelay = 1000L // 1s
        val maxDelay = 30000L // 30s
        val delay = min(maxDelay, baseDelay * (1L shl min(retries, 10)))
        val jitter = Random.nextLong(0, 500) // 500ms max jitter
        return delay + jitter
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(30000) // 30s ping
                send("system", "ping", null)
            }
        }
    }

    fun send(topic: String, event: String, payload: JsonObject?) {
        val webSocket = webSocketRef.get()
        if (webSocket != null && isConnected) {
            val envelope = WSEnvelope(topic, event, payload)
            val jsonStr = json.encodeToString(WSEnvelope.serializer(), envelope)
            webSocket.send(jsonStr)
        } else {
            Log.w("WebSocketManager", "Cannot send, socket not connected: $topic/$event")
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        isConnected = false
        heartbeatJob?.cancel()
        webSocketRef.get()?.close(1000, "App paused/stopped")
        webSocketRef.set(null)
    }

    fun subscribe(topic: String): Flow<WSEnvelope> {
        return events.filter { it.topic == topic }
    }
}
