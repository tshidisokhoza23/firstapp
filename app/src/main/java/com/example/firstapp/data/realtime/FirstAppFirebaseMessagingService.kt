package com.example.firstapp.data.realtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.firstapp.MainActivity
import com.example.firstapp.R
import com.example.firstapp.IncomingCallActivity
import com.example.firstapp.data.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirstAppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TokenManager.initialize(applicationContext)
        PushTokenRegistrar.uploadToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: ""
        if (type != "call_invite") {
            return
        }

        val callType = data["call_type"] ?: "voice"
        val callerName = data["caller_name"] ?: "Incoming call"
        val sessionId = data["session_id"] ?: return
        val senderId = data["caller_id"]?.toIntOrNull() ?: return
        val receiverId = data["receiver_id"]?.toIntOrNull() ?: 0
        val typeEnum = if (callType == "video") CallType.VIDEO else CallType.VOICE
        GlobalCallState.onCallInvite(
            CallEvent(
                type = "call_invite",
                senderId = senderId,
                receiverId = receiverId,
                sessionId = sessionId,
                callType = typeEnum,
                senderName = callerName
            )
        )
        val title = if (callType == "video") "Incoming video call" else "Incoming voice call"
        val body = "$callerName is calling"

        createIncomingCallsChannel()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra("caller_name", callerName)
        }
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra("caller_name", callerName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            sessionId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            (sessionId + "_full").hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String): PendingIntent {
            val i = Intent(this, CallNotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callType)
                putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, sessionId.hashCode())
            }
            return PendingIntent.getBroadcast(
                this,
                (sessionId + action).hashCode(),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", actionIntent(CallNotificationActionReceiver.ACTION_DECLINE))
            .addAction(android.R.drawable.ic_menu_call, "Accept", actionIntent(CallNotificationActionReceiver.ACTION_ACCEPT))
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(sessionId.hashCode(), notification)
    }

    private fun createIncomingCallsChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
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

        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_INCOMING_CALLS = "incoming_calls"
    }
}
