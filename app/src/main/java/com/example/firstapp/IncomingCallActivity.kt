package com.example.firstapp

import android.app.NotificationManager
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.chat.data.*
import com.example.firstapp.common.ui.theme.FirstAppTheme

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    @Inject lateinit var webRtcCallManager: WebRtcCallManager
    @Inject lateinit var callWebSocketClient: CallWebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and wake the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val sessionId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID)
        val senderId  = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID) ?: ""
        val callTypeStr = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE) ?: "voice"
        val notifId   = intent.getIntExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, 0)
        val callType  = if (callTypeStr == "video") CallType.VIDEO else CallType.VOICE

        setContent {
            FirstAppTheme {
                val incomingCall by GlobalCallState.incomingCall.collectAsState()
                val callerName = incomingCall?.senderName?.takeIf { it.isNotBlank() && !it.startsWith("User ") }
                    ?: GlobalCallState.getContactName(senderId)
                    ?: GlobalCallState.getContactPhone(senderId)
                    ?: if (senderId != "") senderId else "Unknown"
                val callLabel = if (callType == CallType.VIDEO) "Incoming video call" else "Incoming voice call"

                // If the call was cancelled while this screen was open, close ourselves
                LaunchedEffect(incomingCall) {
                    if (incomingCall == null) finish()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = callerName,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = callLabel,
                            color = Color(0xFFAAAAAA),
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(64.dp)
                        ) {
                            // Decline
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = {
                                        if (sessionId != null && senderId != "") {
                                            callWebSocketClient.reject(
                                                receiverId = senderId,
                                                sessionId = sessionId,
                                                callType = callType
                                            )
                                            GlobalCallState.onCallEnded(sessionId)
                                        }
                                        if (notifId != 0) {
                                            val manager = getSystemService(NotificationManager::class.java)
                                            manager.cancel(notifId)
                                        }
                                        finish()
                                    },
                                    containerColor = Color(0xFFE53935),
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CallEnd,
                                        contentDescription = "Decline",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Decline", color = Color.White, fontSize = 14.sp)
                            }

                            // Accept
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = {
                                        if (sessionId != null) {
                                            GlobalCallState.setAutoAccept(sessionId)
                                            GlobalCallState.acceptCall(
                                                CallEvent(
                                                    type = "call_invite",
                                                    senderId = senderId,
                                                    receiverId = "",
                                                    sessionId = sessionId,
                                                    callType = callType
                                                ),
                                                webRtcCallManager,
                                                callWebSocketClient
                                            )
                                        }
                                        if (notifId != 0) {
                                            val manager = getSystemService(NotificationManager::class.java)
                                            manager.cancel(notifId)
                                        }
                                        val mainIntent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            if (sessionId != null) putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
                                            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, senderId)
                                            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
                                        }
                                        startActivity(mainIntent)
                                        finish()
                                    },
                                    containerColor = Color(0xFF43A047),
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Accept", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
