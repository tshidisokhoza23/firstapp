package com.example.firstapp.chat.data

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
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.util.sanitizeDisplayName
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@AndroidEntryPoint
class FirstAppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var chatRepository: com.example.firstapp.domain.repository.ChatRepository
    @Inject lateinit var storyRepository: com.example.firstapp.stories.domain.StoryRepository
    @Inject lateinit var settingsRepository: com.example.firstapp.domain.repository.SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenRegistrar.uploadToken(applicationContext, token, sessionManager)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: ""
        
        serviceScope.launch {
            when (type) {
                "call_invite" -> handleIncomingCall(data)
                "new_message" -> {
                    val rawType = data["chat_type"] ?: "direct"
                    val isEnabled = when (rawType.lowercase()) {
                        "group" -> settingsRepository.groupsEnabled.first()
                        "channel" -> settingsRepository.channelsEnabled.first()
                        else -> settingsRepository.privateChatsEnabled.first()
                    }
                    if (isEnabled) handleNewMessage(data)
                }
                "new_story" -> {
                    storyRepository.syncStories()
                }
                "new_chat" -> {
                    if (settingsRepository.privateChatsEnabled.first()) handleNewChat(data)
                }
                "mingle_like", "mingle_match", "mingle" -> {
                    handleMingleNotification(data)
                }
            }
        }
    }

    private fun handleMingleNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Mingle Notification"
        val body = data["body"] ?: "Someone interacted with your Mingle profile"

        createMessagesChannel()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_mingle_notif", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            ("mingle_" + System.currentTimeMillis()).hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(("mingle_" + System.currentTimeMillis()).hashCode(), notification)
    }

    private fun handleNewChat(data: Map<String, String>) {
        val conversationId = data["conversation_id"] ?: return
        
        // Trigger background sync
        val scope = kotlinx.coroutines.MainScope()
        scope.launch {
            chatRepository.syncChats().collect {}
        }

        val title = data["title"] ?: "New Group"
        val body = data["body"] ?: "You were added to a new chat"
        
        createMessagesChannel()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(conversationId.hashCode(), notification)
    }

    private fun handleIncomingCall(data: Map<String, String>) {
        val callType = data["call_type"] ?: "voice"
        val callerName = data["caller_name"] ?: "Incoming call"
        val sessionId = data["session_id"] ?: return
        val senderId = data["caller_id"] ?: data["sender_id"] ?: return
        val receiverId = data["receiver_id"] ?: "0"
        val typeEnum = if (callType == "video") CallType.VIDEO else CallType.VOICE

        // Start CallListenerService to ensure WebSocket connection for signaling
        val serviceIntent = Intent(this, CallListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
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

    private suspend fun handleNewMessage(data: Map<String, String>) {
        val chatId = data["contact_id"] ?: "0"
        val rawSenderName = data["sender_name"].orEmpty()
        val senderPhone = data["sender_phone"] ?: data["contact_phone"]
        val senderName = sanitizeDisplayName(rawSenderName, chatId, senderPhone).ifEmpty { "New Message" }
        val body = data["body"] ?: ""
        val messageId = data["message_id"] ?: ""
        
        // Respect settings
        val previewsEnabled = settingsRepository.previewsEnabled.first()
        val soundsEnabled = settingsRepository.soundsEnabled.first()
        val vibrateEnabled = settingsRepository.vibrateEnabled.first()

        // Expert Fix: Trigger background sync on new message FCM
        chatRepository.syncDelta().collect {}
        chatRepository.syncChats().collect {}
        
        // Emit Delivery ACK via repository
        if (messageId.isNotBlank() && chatId != "0") {
            chatRepository.sendDeliveryAck(chatId, messageId)
        }

        createMessagesChannel(soundsEnabled, vibrateEnabled)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(if (previewsEnabled) body else "New Message")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        if (!soundsEnabled) notification.setSound(null)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(chatId.hashCode(), notification.build())
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

    private fun createMessagesChannel(soundsEnabled: Boolean = true, vibrateEnabled: Boolean = true) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New message alerts"
            enableVibration(vibrateEnabled)
            if (!soundsEnabled) setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_INCOMING_CALLS = "incoming_calls"
        private const val CHANNEL_MESSAGES = "messages"
    }
}
