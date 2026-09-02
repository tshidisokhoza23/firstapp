package com.example.firstapp.drive.data.network

import android.util.Log
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.data.network.NetworkEnvironmentManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed class RideEvent {
    data class RequestReceived(
        val tripId: String, 
        val passengerName: String, 
        val pickup: String, 
        val dest: String, 
        val fare: Double,
        val pickupLat: Double = 0.0,
        val pickupLng: Double = 0.0,
        val destLat: Double = 0.0,
        val destLng: Double = 0.0
    ) : RideEvent()
    data class RequestAccepted(val tripId: String, val driverName: String, val vehicle: String, val plate: String) : RideEvent()
    data class DriverArrived(val tripId: String) : RideEvent()
    data class TripStarted(val tripId: String) : RideEvent()
    data class TripCompleted(val tripId: String, val fare: Double) : RideEvent()
    data class LocationUpdated(val tripId: String, val lat: Double, val lng: Double, val rotation: Float) : RideEvent()
}

private data class WireRideEvent(
    val type: String,
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("passenger_name") val passengerName: String? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    val pickup: String? = null,
    val dest: String? = null,
    val fare: Double? = null,
    val vehicle: String? = null,
    val plate: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerializedName("dest_lat") val destLat: Double? = null,
    @SerializedName("dest_lng") val destLng: Double? = null,
    val rotation: Float? = null
)

@Singleton
class RideWebSocketClient @Inject constructor(
    private val sessionManager: SessionManager,
    private val envManager: NetworkEnvironmentManager,
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _events = MutableSharedFlow<RideEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RideEvent> = _events.asSharedFlow()

    private val isReconnecting = AtomicBoolean(false)
    private var manualClose = false

    fun connect() {
        manualClose = false
        ensureConnected()
    }

    private fun ensureConnected() {
        if (webSocket != null) return
        
        val token = sessionManager.getToken() ?: return
        val baseUrl = envManager.currentWsUrl
        val url = if (baseUrl.endsWith("/")) "${baseUrl}ws/rides?token=$token" else "$baseUrl/ws/rides?token=$token"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("RideWebSocket", "Connected")
                isReconnecting.set(false)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val wireEvent = gson.fromJson(text, WireRideEvent::class.java)
                    handleWireEvent(wireEvent)
                } catch (e: Exception) {
                    Log.e("RideWebSocket", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("RideWebSocket", "Failure: ${t.message}")
                this@RideWebSocketClient.webSocket = null
                if (!manualClose) scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@RideWebSocketClient.webSocket = null
                if (!manualClose) scheduleReconnect()
            }
        })
    }

    private fun handleWireEvent(wire: WireRideEvent) {
        scope.launch {
            val event = when (wire.type) {
                "RIDE_REQUESTED" -> RideEvent.RequestReceived(
                    wire.tripId, wire.passengerName ?: "User", wire.pickup ?: "", wire.dest ?: "", wire.fare ?: 0.0,
                    wire.lat ?: 0.0, wire.lng ?: 0.0, wire.destLat ?: 0.0, wire.destLng ?: 0.0
                )
                "RIDE_ACCEPTED" -> RideEvent.RequestAccepted(wire.tripId, wire.driverName ?: "Driver", wire.vehicle ?: "", wire.plate ?: "")
                "DRIVER_ARRIVED" -> RideEvent.DriverArrived(wire.tripId)
                "TRIP_STARTED" -> RideEvent.TripStarted(wire.tripId)
                "TRIP_COMPLETED" -> RideEvent.TripCompleted(wire.tripId, wire.fare ?: 0.0)
                "LOCATION_UPDATE" -> RideEvent.LocationUpdated(wire.tripId, wire.lat ?: 0.0, wire.lng ?: 0.0, wire.rotation ?: 0f)
                else -> null
            }
            event?.let { _events.emit(it) }
        }
    }

    private fun scheduleReconnect() {
        if (isReconnecting.compareAndSet(false, true)) {
            scope.launch {
                delay(5000L)
                if (!manualClose) ensureConnected()
            }
        }
    }

    fun disconnect() {
        manualClose = true
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
}
