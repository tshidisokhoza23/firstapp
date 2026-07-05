package com.example.firstapp.chat.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.firstapp.MainActivity

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var webRtcCallManager: WebRtcCallManager

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val senderId = intent.getStringExtra(EXTRA_SENDER_ID) ?: return
        val callTypeStr = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "voice"
        val callType = if (callTypeStr == "video") CallType.VIDEO else CallType.VOICE
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notifId)

        when (intent.action) {
            ACTION_DECLINE -> {
                CallWebSocketClient.reject(
                    receiverId = senderId,
                    sessionId = sessionId,
                    callType = callType
                )
                GlobalCallState.onCallEnded(sessionId)
            }
            ACTION_ACCEPT -> {
                GlobalCallState.setAutoAccept(sessionId)
                
                GlobalCallState.acceptCall(
                    CallEvent(
                        type = "call_invite",
                        senderId = senderId,
                        receiverId = "",
                        sessionId = sessionId,
                        callType = callType
                    ),
                    webRtcCallManager
                )

                // Bring the app to foreground
                val launch = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_SESSION_ID, sessionId)
                    putExtra(EXTRA_SENDER_ID, senderId)
                    putExtra(EXTRA_CALL_TYPE, callTypeStr)
                }
                context.startActivity(launch)
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT  = "com.example.firstapp.CALL_ACCEPT"
        const val ACTION_DECLINE = "com.example.firstapp.CALL_DECLINE"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SENDER_ID  = "sender_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_CALL_TYPE  = "call_type"
        const val EXTRA_NOTIF_ID   = "notif_id"
    }
}
