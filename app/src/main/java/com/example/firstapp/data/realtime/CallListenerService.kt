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
import com.example.firstapp.MainActivity
import com.example.firstapp.R
import com.example.firstapp.data.TokenManager
import org.json.JSONObject

class CallListenerService : Service() {

    private val activeIncomingNotifications = mutableMapOf<String, Int>()

    private val listener = object : CallSocketListener {
        override fun onOpen() {
            // No-op
        }

        override fun onEvent(event: CallEvent) {
            when (event.type) {
                "call_invite" -> {
                    val currentUserId = getCurrentUserIdFromToken() ?: return
                    if (event.receiverId == currentUserId && event.senderId != currentUserId) {
                        GlobalCallState.onCallInvite(event)
                        showIncomingCallNotification(event)
                    }
                }
                "call_reject", "call_end" -> {
                    GlobalCallState.onCallEnded(event.sessionId)
                    dismissIncomingCallNotification(event.sessionId)
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
        val ongoingNotification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                ongoingNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification)
        }
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

        val fullScreenActivityIntent = Intent(this, com.example.firstapp.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            event.sessionId.hashCode(),
            fullScreenActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (event.callType == CallType.VIDEO) "Incoming video call" else "Incoming voice call"
        val callerName = event.senderName.takeIf { it.isNotBlank() } ?: "User ${event.senderId}"
        val body = "$callerName is calling"
        activeIncomingNotifications[event.sessionId] = notificationId

        fun actionIntent(action: String): PendingIntent {
            val i = Intent(this, CallNotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
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

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", actionIntent(CallNotificationActionReceiver.ACTION_DECLINE))
            .addAction(android.R.drawable.ic_menu_call, "Accept", actionIntent(CallNotificationActionReceiver.ACTION_ACCEPT))
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
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(payload).optInt("user_id", -1).takeIf { it > 0 }
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
