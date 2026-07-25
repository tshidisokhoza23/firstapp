package com.example.firstapp.chat.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Intent
import android.app.AlarmManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.firstapp.MainActivity
import com.example.firstapp.R
import com.example.firstapp.common.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import kotlin.collections.*
import kotlin.text.*
import kotlin.*

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallListenerService : Service() {

    @Inject lateinit var webRtcCallManager: WebRtcCallManager
    @Inject lateinit var callTonePlayer: CallTonePlayer

    private val activeIncomingNotifications = ConcurrentHashMap<String, Int>()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (TokenManager.isLoggedIn()) {
                CallWebSocketClient.connect(listener)
            }
        }
    }

    private val listener = object : CallSocketListener {
        override fun onOpen() {
            // No-op
        }

        override fun onEvent(event: CallEvent) {
            val currentUserId = getCurrentUserIdFromToken() ?: return
            
            if (event.senderId == currentUserId) {
                return
            }

            val type = event.type ?: return
            when (type) {
                "call_invite" -> {
                    if (event.receiverId == currentUserId) {
                        if (GlobalCallState.isCallActive.value) {
                            CallWebSocketClient.reject(
                                receiverId = event.senderId,
                                sessionId = event.sessionId,
                                callType = event.callType,
                                reason = "busy"
                            )
                            return
                        }
                        GlobalCallState.onCallInvite(event)
                        showIncomingCallNotification(event)
                        CallWebSocketClient.ringing(event.senderId, event.sessionId, event.callType)
                    }
                }
                "call_ringing" -> {
                    if (GlobalCallState.activeSessionId.value == event.sessionId) {
                        GlobalCallState.setRinging(true)
                    }
                }
                "call_reject", "call_end" -> {
                    val isActiveSession = GlobalCallState.activeSessionId.value == event.sessionId
                    GlobalCallState.onCallEnded(event.sessionId)
                    dismissIncomingCallNotification(event.sessionId)
                    callTonePlayer.stop()
                    if (isActiveSession) {
                        webRtcCallManager.end()
                    }
                }
                "call_accept" -> {
                    callTonePlayer.stop()
                    GlobalCallState.setRinging(false)
                    val wasEstablished = GlobalCallState.isConnectionEstablished.value
                    GlobalCallState.setConnectionEstablished(true)
                    
                    if (GlobalCallState.activeSessionId.value == event.sessionId && !wasEstablished) {
                        webRtcCallManager.createOffer()
                    }
                }
                "call_signal" -> {
                    if (GlobalCallState.activeSessionId.value == event.sessionId) {
                        webRtcCallManager.handleSignal(event.signalType, event.signalPayload)
                    }
                }
            }
        }

        override fun onError(error: String) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        createChannels()
        val ongoingNotification = buildOngoingNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Use remoteMessaging for idle state on Android 14+
                val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification, type)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification, type)
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification)
            }
        } catch (e: Exception) {
            android.util.Log.e("CallListenerService", "Failed to start foreground service in onCreate", e)
        }

        TokenManager.initialize(applicationContext)
        android.util.Log.d("CallListenerService", "Service created and foreground started")
        
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        
        webRtcCallManager.onSignal = { signalType, payload ->
            val participantId = GlobalCallState.activeParticipantId.value
            val sessionId = GlobalCallState.activeSessionId.value
            val callType = GlobalCallState.activeCallType.value
            if (participantId != null && sessionId != null && callType != null) {
                CallWebSocketClient.signal(
                    receiverId = participantId,
                    sessionId = sessionId,
                    callType = callType,
                    signalType = signalType,
                    signalPayload = payload
                )
            }
        }

        serviceScope.launch {
            GlobalCallState.incomingCall.collect { call ->
                if (call == null) {
                    val manager = getSystemService(NotificationManager::class.java)
                    activeIncomingNotifications.values.forEach { id ->
                        manager?.cancel(id)
                    }
                    activeIncomingNotifications.clear()
                }
            }
        }

        serviceScope.launch {
            GlobalCallState.isCallActive.collect { active ->
                updateServiceNotification(active)
            }
        }
    }

    private fun updateServiceNotification(isCallActive: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = if (isCallActive) {
            val sessionId = GlobalCallState.activeSessionId.value
            val participantId = GlobalCallState.activeParticipantId.value ?: ""
            val callType = GlobalCallState.activeCallType.value ?: CallType.VOICE
            val callTypeStr = if (callType == CallType.VIDEO) "video" else "voice"

            val participantName = GlobalCallState.getContactName(participantId)?.takeIf { it.isNotBlank() }
                ?: GlobalCallState.getContactPhone(participantId)
                ?: participantId

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (sessionId != null) putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
                if (participantId.isNotBlank()) putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, participantId)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                9002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                
                val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                // Only request mic/camera if we actually have permissions, otherwise the system kills the FGS
                if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (hasCam) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                
                val n = NotificationCompat.Builder(this, CHANNEL_CALL_SERVICE)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Ongoing Call with $participantName")
                    .setContentText("Tap to return to call")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build()
                
                try {
                    startForeground(ONGOING_NOTIFICATION_ID, n, type)
                } catch (e: Exception) {
                    android.util.Log.e("CallListenerService", "Denied FGS types (likely background restriction), falling back", e)
                    // Fallback to phoneCall only, or just service if even that is denied
                    try {
                        startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } catch (_: Exception) {
                        startForeground(ONGOING_NOTIFICATION_ID, n)
                    }
                }
                return
            }

            NotificationCompat.Builder(this, CHANNEL_CALL_SERVICE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Ongoing Call with $participantName")
                .setContentText("Tap to return to call")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        } else {
            val n = buildOngoingNotification()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(ONGOING_NOTIFICATION_ID, n)
                }
            } catch (e: Exception) {
                android.util.Log.e("CallListenerService", "Failed to start foreground service (idle)", e)
            }
            return
        }
        manager?.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (TokenManager.isLoggedIn()) {
            CallWebSocketClient.connect(listener)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        CallWebSocketClient.disconnect(listener)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildOngoingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_CALL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FirstApp call service")
            .setContentText("Listening for incoming calls")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun showIncomingCallNotification(event: CallEvent) {
        val callTypeStr = if (event.callType == CallType.VIDEO) "video" else "voice"
        val notificationId = event.sessionId.hashCode()
        val senderIdStr = event.senderId
        val callerName = event.senderName.takeIf { it.isNotBlank() } 
            ?: GlobalCallState.getContactName(senderIdStr)
            ?: GlobalCallState.getContactPhone(senderIdStr) 
            ?: senderIdStr

        val fullScreenActivityIntent = Intent(this, com.example.firstapp.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            event.sessionId.hashCode(),
            fullScreenActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        activeIncomingNotifications[event.sessionId] = notificationId

        fun actionIntent(action: String): PendingIntent {
            val i = Intent(this, CallNotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
                putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                this,
                (event.sessionId + action).hashCode(),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val caller = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(60000) 
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    actionIntent(CallNotificationActionReceiver.ACTION_DECLINE),
                    actionIntent(CallNotificationActionReceiver.ACTION_ACCEPT)
                )
            )
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun dismissIncomingCallNotification(sessionId: String) {
        val id = activeIncomingNotifications.remove(sessionId) ?: return
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(id)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java) ?: return

        val serviceChannel = NotificationChannel(
            CHANNEL_CALL_SERVICE,
            "System Service",
            NotificationManager.IMPORTANCE_MIN
        )
        serviceChannel.description = "Background connectivity"
        serviceChannel.setShowBadge(false)
        manager.createNotificationChannel(serviceChannel)

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incomingChannel = NotificationChannel(
            CHANNEL_INCOMING_CALLS,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        )
        incomingChannel.description = "Alerts for incoming calls"
        incomingChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        incomingChannel.setSound(ringtoneUri, audioAttributes)
        incomingChannel.enableVibration(true)
        incomingChannel.vibrationPattern = longArrayOf(0, 1000, 1000)

        manager.createNotificationChannel(incomingChannel)
    }

    private fun getCurrentUserIdFromToken(): String? {
        return try {
            val token = TokenManager.getToken() ?: return null
            val parts = token.split('.')
            if (parts.size < 2) return null
            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val payload = String(decodedBytes, Charsets.UTF_8)
            val json = JSONObject(payload)
            val id = json.optString("user_id", "")
            if (id.isNotEmpty()) return id
            json.optString("sub", "")
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val CHANNEL_CALL_SERVICE = "call_service"
        private const val CHANNEL_INCOMING_CALLS = "incoming_calls"
        private const val ONGOING_NOTIFICATION_ID = 9001
    }
}
