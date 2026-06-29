package com.example.firstapp.chat.data

import com.example.firstapp.BuildConfig
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.api.TokenRefreshRequest
import com.google.gson.Gson
import com.google.gson.JsonElement
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

enum class CallType {
    VOICE,
    VIDEO;

    fun wireValue(): String = if (this == VIDEO) "video" else "voice"

    companion object {
        fun fromWire(value: String?): CallType = if (value == "video") VIDEO else VOICE
    }
}

data class CallEvent(
    val type: String,
    val senderId: Int,
    val receiverId: Int,
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
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
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
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("call_type") val callType: String? = null,
    @SerializedName("signal_type") val signalType: String? = null,
    @SerializedName("signal_payload") val signalPayload: JsonElement? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int = 0,
    @SerializedName("end_reason") val endReason: String? = null
)

object CallWebSocketClient {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val httpClient = ApiClient.client
    private val listeners = CopyOnWriteArraySet<CallSocketListener>()
    private val reconnectScope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    @Volatile
    private var manualClose = false
    @Volatile
    private var refreshInProgress = false

    fun connect(listener: CallSocketListener) {
        listeners.add(listener)
        ensureConnected()
    }

    fun disconnect(listener: CallSocketListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            close()
        }
    }

    @Synchronized
    private fun ensureConnected() {
        if (webSocket != null) {
            return
        }

        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            notifyError("Missing auth token")
            return
        }

        manualClose = false

        val url = "${BuildConfig.WS_BASE_URL}ws/calls"
        android.util.Log.d("CallWebSocket", "Connecting to: $url")
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
                    notifyError("Failed to parse calls websocket message: ${e.localizedMessage}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@CallWebSocketClient.webSocket = null
                if (response?.code == 401) {
                    if (refreshTokenIfPossible()) {
                        scheduleReconnect(immediate = true)
                    } else {
                        notifyError("Calls auth expired. Please log in again.")
                    }
                    return
                }
                notifyError(t.localizedMessage ?: "Calls WebSocket failed")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@CallWebSocketClient.webSocket = null
                if (!manualClose) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun notifyOpen() {
        listeners.forEach { it.onOpen() }
    }

    private fun notifyEvent(event: CallEvent) {
        listeners.forEach { it.onEvent(event) }
    }

    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (manualClose || listeners.isEmpty()) {
            return
        }
        if (reconnectJob?.isActive == true) {
            return
        }
        reconnectJob = reconnectScope.launch {
            if (!immediate) {
                delay(2000)
            }
            if (!manualClose && listeners.isNotEmpty() && webSocket == null) {
                ensureConnected()
            }
        }
    }

    private fun refreshTokenIfPossible(): Boolean {
        if (refreshInProgress) return false
        val refreshToken = TokenManager.getRefreshToken() ?: return false
        refreshInProgress = true
        return try {
            val response = runBlocking {
                ApiClient.authApi.refresh(TokenRefreshRequest(refresh_token = refreshToken))
            }
            if (!response.isSuccessful) {
                if (response.code() == 401 || response.code() == 400) {
                    TokenManager.clear()
                }
                return false
            }
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

    fun invite(receiverId: Int, sessionId: String, callType: CallType) {
        send(
            OutgoingCallEvent(
                type = "call_invite",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue()
            )
        )
    }

    fun ringing(receiverId: Int, sessionId: String, callType: CallType) {
        send(
            OutgoingCallEvent(
                type = "call_ringing",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue()
            )
        )
    }

    fun accept(receiverId: Int, sessionId: String, callType: CallType) {
        send(
            OutgoingCallEvent(
                type = "call_accept",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue()
            )
        )
    }

    fun reject(receiverId: Int, sessionId: String, callType: CallType, reason: String = "rejected") {
        send(
            OutgoingCallEvent(
                type = "call_reject",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue(),
                endReason = reason
            )
        )
    }

    fun end(
        receiverId: Int,
        sessionId: String,
        callType: CallType,
        durationSeconds: Int,
        reason: String = "ended"
    ) {
        send(
            OutgoingCallEvent(
                type = "call_end",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue(),
                durationSeconds = durationSeconds,
                endReason = reason
            )
        )
    }

    fun signal(
        receiverId: Int,
        sessionId: String,
        callType: CallType,
        signalType: String,
        signalPayload: JsonElement
    ) {
        send(
            OutgoingCallEvent(
                type = "call_signal",
                receiverId = receiverId,
                sessionId = sessionId,
                callType = callType.wireValue(),
                signalType = signalType,
                signalPayload = signalPayload
            )
        )
    }

    private fun send(event: OutgoingCallEvent) {
        webSocket?.send(gson.toJson(event))
    }

    @Synchronized
    fun close() {
        manualClose = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        listeners.clear()
    }
}
