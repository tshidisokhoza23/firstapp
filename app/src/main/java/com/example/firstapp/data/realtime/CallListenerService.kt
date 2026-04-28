package com.example.firstapp.data.realtime

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
import com.example.firstapp.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

class CallListenerService : Service() {

    private val activeIncomingNotifications = ConcurrentHashMap<String, Int>()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val listener = object : CallSocketListener {
        override fun onOpen() {
            // No-op
        }

        override fun onEvent(event: CallEvent) {
            when (event.type) {
                "call_invite" -> {
                    val currentUserId = getCurrentUserIdFromToken() ?: return
                    if (event.receiverId == currentUserId && event.senderId != currentUserId) {
                        if (GlobalCallState.isCallActive.value) {
                            // Automatically reject if busy
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
                    }
                }
                "call_reject", "call_end" -> {
                    val isActiveSession = GlobalCallState.activeSessionId.value == event.sessionId
                    GlobalCallState.onCallEnded(event.sessionId)
                    dismissIncomingCallNotification(event.sessionId)
                    com.example.firstapp.AppContainer.callTonePlayer.stop()
                    if (isActiveSession) {
                        com.example.firstapp.AppContainer.webRtcCallManager.end()
                    }
                }
                "call_accept" -> {
                    com.example.firstapp.AppContainer.callTonePlayer.stop()
                    GlobalCallState.setConnectionEstablished(true)
                }
                "call_signal" -> {
                    if (GlobalCallState.activeSessionId.value == event.sessionId) {
                        com.example.firstapp.AppContainer.webRtcCallManager.handleSignal(event.signalType, event.signalPayload)
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
        TokenManager.initialize(applicationContext)
        createChannels()
        
        val webRtcManager = com.example.firstapp.AppContainer.webRtcCallManager
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

        val ongoingNotification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification, type)
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification)
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

            val participantName = GlobalCallState.getContactName(participantId) ?: "User $participantId"

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
            buildOngoingNotification()
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
        val callerName = event.senderName.takeIf { it.isNotBlank() } ?: GlobalCallState.getContactName(event.senderId) ?: "User ${event.senderId}"

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
