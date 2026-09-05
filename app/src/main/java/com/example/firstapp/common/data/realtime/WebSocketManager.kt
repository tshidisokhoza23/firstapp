package com.example.firstapp.common.data.realtime

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.domain.auth.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    private val sessionManager: SessionManager,
    private val envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
) : DefaultLifecycleObserver {

    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _events = MutableSharedFlow<WSEnvelope>(
        extraBufferCapacity = 200,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<WSEnvelope> = _events.asSharedFlow()

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val webSocketRef = AtomicReference<WebSocket?>(null)
    private var isConnected = false
    private var retryCount = 0
    private var heartbeatJob: Job? = null
    private var isManualDisconnect = false

    init {
        // Observe process lifecycle to manage connection automatically
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
        if (isConnected) return
        isManualDisconnect = false
        
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.w("WebSocketManager", "No token available, scheduling retry")
            _connectionState.value = ConnectionState.DISCONNECTED
            scope.launch {
                delay(2000)
                if (!isConnected && !sessionManager.getToken().isNullOrEmpty()) {
                    connect()
                }
            }
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        val base = envManager.currentWsUrl.trimEnd('/')
        val wsUrl = if (base.endsWith("/ws/chat")) "$base?token=$token" else "$base/ws/chat?token=$token"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        Log.d("WebSocketManager", "Connecting to chat websocket at $wsUrl")
        okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("WebSocketManager", "WebSocket Opened successfully")
                webSocketRef.set(webSocket)
                isConnected = true
                retryCount = 0
                _connectionState.value = ConnectionState.CONNECTED
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v("WebSocketManager", "Received WS message: $text")
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
                _connectionState.value = ConnectionState.DISCONNECTED
                handleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "WebSocket Failure: ${t.message}", t)
                _connectionState.value = ConnectionState.DISCONNECTED
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
        val maxDelay = 15000L // 15s max
        val delay = min(maxDelay, baseDelay * (1L shl min(retries, 5)))
        val jitter = Random.nextLong(0, 500)
        return delay + jitter
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(15000) // 15s heartbeat
                send("presence", "heartbeat", buildJsonObject {
                    put("status", "online")
                    put("timestamp", System.currentTimeMillis())
                })
            }
        }
    }

    fun send(topic: String, event: String, payload: JsonObject? = null, op: String? = null) {
        val webSocket = webSocketRef.get()
        if (webSocket != null && isConnected) {
            val envelope = WSEnvelope(topic, event, payload, op)
            val jsonStr = json.encodeToString(WSEnvelope.serializer(), envelope)
            webSocket.send(jsonStr)
        } else {
            Log.w("WebSocketManager", "Cannot send, socket not connected: $topic/$event")
            // If socket was disconnected unexpectedly, attempt reconnect
            if (!isConnected && !isManualDisconnect) {
                connect()
            }
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
