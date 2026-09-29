package com.tpsk.tj.chat.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tpsk.tj.MainActivity
import com.tpsk.tj.R
import com.tpsk.tj.IncomingCallActivity
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.chat.model.consistentName
import com.tpsk.tj.common.util.sanitizeDisplayName
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@AndroidEntryPoint
class FirstAppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var chatRepository: com.tpsk.tj.domain.repository.ChatRepository
    @Inject lateinit var storyRepository: com.tpsk.tj.stories.domain.StoryRepository
    @Inject lateinit var settingsRepository: com.tpsk.tj.domain.repository.SettingsRepository

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
                "call_end", "call_reject" -> handleCallEnded(data)
                "new_message" -> {
                    val rawType = data["chat_type"] ?: "direct"
                    val isEnabled = when (rawType.lowercase()) {
                        "group" -> settingsRepository.groupsEnabled.first()
                        "channel" -> settingsRepository.channelsEnabled.first()
                        else -> settingsRepository.privateChatsEnabled.first()
                    }
                    if (isEnabled && !isAppInForeground()) handleNewMessage(data)
                }
                "new_story" -> {
                    storyRepository.syncStories()
                    if (!isAppInForeground()) handleStoryNotification(data)
                }
                "new_chat" -> {
                    if (settingsRepository.privateChatsEnabled.first() && !isAppInForeground()) handleNewChat(data)
                }
                "mingle_like", "mingle_match", "mingle", "mingle_intro" -> {
                    if (!isAppInForeground()) handleMingleNotification(data)
                }
                "social_like", "social_comment", "social_follow", "social_notification", "social" -> {
                    if (!isAppInForeground()) handleSocialNotification(data)
                }
            }
        }
    }

    private fun handleSocialNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Social Notification"
        val body = data["body"] ?: "New activity on your post"

        createMessagesChannel()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_social_notif", true)
            data["post_id"]?.let { putExtra("open_post_id", it) }
            data["actor_id"]?.let { putExtra("open_actor_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            ("social_" + System.currentTimeMillis()).hashCode(),
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
        manager.notify(("social_" + System.currentTimeMillis()).hashCode(), notification)
    }

    private fun handleMingleNotification(data: Map<String, String>) {
        val isRequest = data["type"] == "mingle_intro"
        val title = data["title"] ?: if (isRequest) "Mingle" else "Mingle Notification"
        val body = data["body"] ?: if (isRequest) {
            "You received a new message request on Mingle"
        } else {
            "Someone interacted with your Mingle profile"
        }

        createMessagesChannel()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isRequest) {
                putExtra("open_mingle_requests", true)
            }
            data["actor_id"]?.let { putExtra("open_actor_id", it) } else {
                putExtra("open_mingle_notif", true)
            }
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
            putExtra("open_chat_type", data["chat_type"] ?: "direct")
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

    private fun handleStoryNotification(data: Map<String, String>) {
        val userId = data["user_id"] ?: data["actor_id"] ?: return
        val storyId = data["story_id"]
        createMessagesChannel()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_story_id", storyId)
            putExtra("open_user_id", userId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            userId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(data["title"] ?: "New story")
            .setContentText(data["body"] ?: "Someone posted a story")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(userId.hashCode(), notification)
    }

    private fun isAppInForeground(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val processes = manager.runningAppProcesses ?: return false
        return processes.any {
            it.processName == packageName &&
                it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {.apply {
            putExtra(CallListenerService.EXTRA_INCOMING_CALL, true)
            putExtra(CallListenerService.EXTRA_CALLER_NAME, callerName)
        }
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.w("FirebaseMessaging", "Failed to start CallListenerService: ${e.message}")
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

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, sessionId.hashCode())
            putExtra("caller_name", callerName)
        }
        try {
            startActivity(fullScreenIntent)
        } catch (_: Exception) {
            // Android blocks background activity starts. The full-screen intent below still opens the call screen.
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            sessionId.hashCode(),
            fullScreenIntent,
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
            .addAction(android.R.drawable.ic_menu_call, "Accept", acceptActivityIntent(sessionId, senderId, callerName, callType))
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(sessionId.hashCode(), notification)
    }

    private fun acceptActivityIntent(sessionId: String, senderId: String, callerName: String, callType: String): PendingIntent {
        val accept = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, sessionId.hashCode())
            putExtra("auto_accept", true)
        }
        return PendingIntent.getActivity(
            this,
            (sessionId + "_accept").hashCode(),
            accept,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleCallEnded(data: Map<String, String>) {
        val sessionId = data["session_id"] ?: return
        getSystemService(NotificationManager::class.java)?.cancel(sessionId.hashCode())
        GlobalCallState.onCallEnded(sessionId)
    }

    private suspend fun handleNewMessage(data: Map<String, String>) {
        val conversationId = data["conversation_id"]?.takeIf { it.isNotBlank() }
            ?: data["contact_id"]?.takeIf { it.isNotBlank() }
            ?: "0"
        val senderId = data["sender_id"].orEmpty()
        val rawSenderName = data["sender_name"].orEmpty()
        val senderPhone = data["sender_phone"] ?: data["contact_phone"]
        val body = data["body"] ?: ""
        val messageId = data["message_id"] ?: ""
        
        // Respect settings
        val previewsEnabled = settingsRepository.previewsEnabled.first()
        val soundsEnabled = settingsRepository.soundsEnabled.first()
        val vibrateEnabled = settingsRepository.vibrateEnabled.first()

        // Trigger background sync so the notification title matches the chat list.
        chatRepository.syncDelta().collect {}
        chatRepository.syncChats().collect {}
        val cachedName = runCatching {
            kotlinx.coroutines.withTimeoutOrNull(1500) {
                chatRepository.getChats().first { it !is com.tpsk.tj.common.util.Resource.Loading }.data
                    ?.firstOrNull { it.id == conversationId || (senderId.isNotBlank() && it.otherUserId == senderId) }
                    ?.consistentName()
            }
        }.getOrNull()
        val senderName = cachedName?.takeIf { it.isNotBlank() }
            ?: sanitizeDisplayName(rawSenderName, senderId.ifBlank { conversationId }, senderPhone).ifEmpty { "New Message" }
        
        // Emit Delivery ACK via repository using the conversation, never the user id.
        if (messageId.isNotBlank() && conversationId != "0") {
            chatRepository.sendDeliveryAck(conversationId, messageId)
        }

        createMessagesChannel(soundsEnabled, vibrateEnabled)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", conversationId)
            putExtra("open_chat_type", data["chat_type"] ?: "direct")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
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
        manager.notify(conversationId.hashCode(), notification.build())
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
