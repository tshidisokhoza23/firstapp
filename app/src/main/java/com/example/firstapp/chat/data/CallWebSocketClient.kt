package com.example.firstapp.chat.data

import android.util.Log
import com.example.firstapp.BuildConfig
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.data.AuthSessionManager
import com.example.firstapp.common.data.api.ApiClient
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.*
import kotlin.jvm.Volatile

enum class CallType {
    VOICE,
    VIDEO;

    fun wireValue(): String = if (this == VIDEO) "video" else "voice"

    companion object {
        fun fromWire(value: String?): CallType = if (value == "video") VIDEO else VOICE
    }
}

data class CallEvent(
    val type: String?,
    val senderId: String,
    val receiverId: String,
    val sessionId: String,
    val callType: CallType,
    val senderName: String = "",
    val signalType: String? = null,
    val signalPayload: JsonElement? = null,
    val durationSeconds: Int = 0,
    val endReason: String? = null,
    val timestamp: String? = null
)

interface CallSocketListener {
    fun onOpen()
    fun onEvent(event: CallEvent)
    fun onError(error: String)
}

private data class WireCallEvent(
    val type: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("call_type") val callType: String? = null,
    @SerializedName("signal_type") val signalType: String? = null,
    @SerializedName("signal_payload") val signalPayload: JsonElement? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int = 0,
    @SerializedName("end_reason") val endReason: String? = null,
    val timestamp: String? = null
)

private data class OutgoingCallEvent(
    val type: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("call_type") val callType: String? = null,
    @SerializedName("signal_type") val signalType: String? = null,
    @SerializedName("signal_payload") val signalPayload: JsonElement? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int = 0,
    @SerializedName("end_reason") val endReason: String? = null
)

@Singleton
class CallWebSocketClient @Inject constructor(
    private val sessionManager: SessionManager,
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val listeners = CopyOnWriteArraySet<CallSocketListener>()
    private val connectMutex = Mutex()
    private val isReconnecting = AtomicBoolean(false)

    @Volatile
    private var manualClose = false

    fun connect(listener: CallSocketListener) {
        listeners.add(listener)
        // Use a background scope instead of manual Thread
        CoroutineScope(Dispatchers.IO).launch {
            ensureConnected()
        }
    }

    fun disconnect(listener: CallSocketListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            close()
        }
    }

    private suspend fun ensureConnected() {
        if (webSocket != null) return
        connectMutex.withLock {
            if (webSocket != null) return@withLock

            val token = sessionManager.getToken()
            if (token.isNullOrBlank()) {
                notifyError("Missing auth token")
                return@withLock
            }

            manualClose = false
            val baseUrl = BuildConfig.WS_BASE_URL
            val url = (if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/") + "ws/calls?token=$token"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    notifyOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val payload = gson.fromJson(text, WireCallEvent::class.java)
                        notifyEvent(
                            CallEvent(
                                type = payload.type,
                                senderId = payload.senderId,
                                receiverId = payload.receiverId,
                                sessionId = payload.sessionId,
                                callType = CallType.fromWire(payload.callType),
                                signalType = payload.signalType,
                                signalPayload = payload.signalPayload,
                                durationSeconds = payload.durationSeconds,
                                endReason = payload.endReason,
                                timestamp = payload.timestamp
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("CallWebSocket", "Error parsing message: ${e.message}", e)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("CallWebSocket", "WebSocket failure: ${t.message}", t)
                    this@CallWebSocketClient.webSocket = null
                    // Reconnect handled via scheduleReconnect
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("CallWebSocket", "WebSocket closed: $code / $reason")
                    this@CallWebSocketClient.webSocket = null
                    if (!manualClose) scheduleReconnect()
                }
            })
        }
    }

    private fun notifyOpen() {
        Log.d("CallWebSocket", "WebSocket connection opened")
        listeners.forEach { it.onOpen() }
    }

    private fun notifyEvent(event: CallEvent) {
        Log.d("CallWebSocket", "WebSocket event received: ${event.type}")
        listeners.forEach { it.onEvent(event) }
    }

    private fun notifyError(error: String) {
        Log.e("CallWebSocket", "WebSocket error: $error")
        listeners.forEach { it.onError(error) }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (manualClose || listeners.isEmpty()) {
            Log.d("CallWebSocket", "Reconnect skipped")
            return
        }
        if (!isReconnecting.compareAndSet(false, true)) return
        
        Log.d("CallWebSocket", "Scheduling reconnect, immediate=$immediate")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!immediate) delay(5000)
                if (!manualClose && listeners.isNotEmpty() && webSocket == null) {
                    ensureConnected()
                }
            } finally {
                isReconnecting.set(false)
            }
        }
    }

    fun invite(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type = "call_invite", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue()))
    }

    fun ringing(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type = "call_ringing", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue()))
    }

    fun accept(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type = "call_accept", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue()))
    }

    fun reject(receiverId: String, sessionId: String, callType: CallType, reason: String = "rejected") {
        send(OutgoingCallEvent(type = "call_reject", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue(), endReason = reason))
    }

    fun end(receiverId: String, sessionId: String, callType: CallType, durationSeconds: Int, reason: String = "ended") {
        send(OutgoingCallEvent(type = "call_end", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue(), durationSeconds = durationSeconds, endReason = reason))
    }

    fun signal(receiverId: String, sessionId: String, callType: CallType, signalType: String, signalPayload: JsonElement) {
        send(OutgoingCallEvent(type = "call_signal", receiverId = receiverId, sessionId = sessionId, callType = callType.wireValue(), signalType = signalType, signalPayload = signalPayload))
    }

    private fun send(event: OutgoingCallEvent) {
        webSocket?.send(gson.toJson(event))
    }

    fun close() {
        manualClose = true
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        listeners.clear()
    }
}
