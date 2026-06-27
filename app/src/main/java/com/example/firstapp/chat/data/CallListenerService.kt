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

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallListenerService : Service() {

    @Inject lateinit var webRtcCallManager: WebRtcCallManager
    @Inject lateinit var callTonePlayer: CallTonePlayer

    private val activeIncomingNotifications = ConcurrentHashMap<String, Int>()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
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
            
            // Ignore events where I am the sender, except for certain cases if needed.
            // In most signaling protocols, you only handle what the OTHER side sends you.
            if (event.senderId == currentUserId) {
                return
            }

            when (event.type) {
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
                    
                    // If we are the caller (we have an active session we started), 
                    // we should now create the offer.
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
            // Keep foreground service alive; websocket client handles reconnect.
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Call startForeground ASAP to avoid ForegroundServiceDidNotStartInTimeException
        createChannels()
        val ongoingNotification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification, type)
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification)
        }

        TokenManager.initialize(applicationContext)
        
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        
        val webRtcManager = webRtcCallManager
        webRtcManager.onSignal = { signalType, payload ->
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
                    activeIncomingNotifications.forEach { (_, id) ->
                        manager.cancel(id)
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
            val participantId = GlobalCallState.activeParticipantId.value ?: -1
            val callType = GlobalCallState.activeCallType.value ?: CallType.VOICE
            val callTypeStr = if (callType == CallType.VIDEO) "video" else "voice"

            val participantName = GlobalCallState.getContactName(participantId)?.takeIf { !it.startsWith("User ") }
                ?: GlobalCallState.getContactPhone(participantId)
                ?: "$participantId"

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (sessionId != null) putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
                if (participantId > 0) putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, participantId)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                9002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // When call is active, we upgrade the foreground service type if on Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                
                // Only add microphone/camera types if permissions are granted, 
                // otherwise startForeground will throw SecurityException on Android 14+
                val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                // On Android 14+, we can only use MICROPHONE/CAMERA types if the app is in the foreground
                // or if we have a very specific set of conditions. 
                // We'll try to add them but catch the exception if the OS denies it.
                if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (hasCam) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                
                // We use startForeground again to update the type
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
                    android.util.Log.e("CallListenerService", "Denined FGS types, falling back", e)
                    // Fallback to just phoneCall if it still fails
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } else {
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
            // When call ends, we downgrade back to just PHONE_CALL type
            val n = buildOngoingNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, n)
            }
            return
        }
        manager.notify(ONGOING_NOTIFICATION_ID, notification)
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
        if (TokenManager.isLoggedIn()) {
            scheduleRestart()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (TokenManager.isLoggedIn()) {
            scheduleRestart()
        }
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
        val callerName = event.senderName.takeIf { it.isNotBlank() && !it.startsWith("User ") } 
            ?: GlobalCallState.getContactName(event.senderId)
            ?: GlobalCallState.getContactPhone(event.senderId) 
            ?: "${event.senderId}"

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
            .setTimeoutAfter(60000) // Auto-dismiss after 1 minute to prevent infinite ringing
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
        manager.notify(notificationId, notification)
    }

    private fun dismissIncomingCallNotification(sessionId: String) {
        val id = activeIncomingNotifications.remove(sessionId) ?: return
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(id)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            CHANNEL_CALL_SERVICE,
            "Call listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps incoming call listener active"
            setSound(null, null)
            enableVibration(false)
        }

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incomingChannel = NotificationChannel(
            CHANNEL_INCOMING_CALLS,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for incoming calls"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(ringtoneUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(incomingChannel)
    }

    private fun getCurrentUserIdFromToken(): Int? {
        return try {
            val token = TokenManager.getToken() ?: return null
            val parts = token.split('.')
            if (parts.size < 2) return null
            // Use NO_PADDING for robustness
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val json = JSONObject(payload)
            // Some systems use "user_id", others might use "sub"
            val id = json.optInt("user_id", -1)
            if (id > 0) return id
            json.optString("sub").toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, CallListenerService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            9020,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + 1500L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    companion object {
        private const val CHANNEL_CALL_SERVICE = "call_service"
        private const val CHANNEL_INCOMING_CALLS = "incoming_calls"
        private const val ONGOING_NOTIFICATION_ID = 9001
    }
}
