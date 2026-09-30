package com.tpsk.tj

import android.app.NotificationManager
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tpsk.tj.chat.data.*
import com.tpsk.tj.common.ui.theme.TJTheme


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

        if (openCallFromIntent(intent)) return

        setContent {
            TJTheme {

                val incomingCall by GlobalCallState.incomingCall.collectAsState()
                val senderId = incomingCall?.senderId.orEmpty()
                val sessionId = incomingCall?.sessionId
                val callType = incomingCall?.callType ?: CallType.VOICE
                val callTypeStr = if (callType == CallType.VIDEO) "video" else "voice"
                val notifId = intent.getIntExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, 0)
                val callerName = remember(incomingCall, senderId) {
                    incomingCall?.senderName?.takeIf { it.isNotBlank() && !it.startsWith("User ") && it.length < 30 }
                        ?: GlobalCallState.getContactName(senderId)
                        ?: GlobalCallState.getContactPhone(senderId)
                        ?: ""
                }
                val callLabel = if (callType == CallType.VIDEO) "Incoming video call" else "Incoming voice call"
                var cameraGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@IncomingCallActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val cameraPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> cameraGranted = granted }

                // If the call was cancelled while this screen was open, close ourselves
                LaunchedEffect(incomingCall) {
                    if (incomingCall == null && !GlobalCallState.isCallActive.value) finish()
                }
                LaunchedEffect(callType) {
                    if (callType == CallType.VIDEO && !cameraGranted) {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }
                DisposableEffect(callType, cameraGranted) {
                    if (callType == CallType.VIDEO && cameraGranted) {
                        webRtcCallManager.startLocalPreview()
                    }
                    onDispose {
                        if (!GlobalCallState.isCallActive.value) {
                            webRtcCallManager.stopLocalPreview()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D))
                ) {
                    if (callType == CallType.VIDEO && cameraGranted) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                CallVideoRenderer(context).also { renderer ->
                                    renderer.init(webRtcCallManager.eglContext())
                                    webRtcCallManager.attachLocalRenderer(renderer)
                                }
                            },
                            onRelease = { renderer ->
                                webRtcCallManager.detachLocalRenderer()
                                renderer.releaseRenderer()
                            }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = callerName,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = callLabel,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 48.dp),
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
                                            putExtra("is_ongoing_call_return", true)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (openCallFromIntent(intent)) return
    }

    private fun openCallFromIntent(intent: Intent?): Boolean {
        val sessionId = intent?.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID) ?: return false
        val senderId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID) ?: return false
        val callTypeStr = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE) ?: "voice"
        val callType = if (callTypeStr == "video") CallType.VIDEO else CallType.VOICE
        val callerName = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME)
            ?: intent.getStringExtra("caller_name")
            ?: ""

        GlobalCallState.onCallInvite(
            CallEvent(
                type = "call_invite",
                senderId = senderId,
                receiverId = "",
                sessionId = sessionId,
                callType = callType,
                senderName = callerName
            )
        )

        if (!intent.getBooleanExtra("auto_accept", false)) return false

        GlobalCallState.setAutoAccept(sessionId)
        try {
            GlobalCallState.acceptCall(
                CallEvent(
                    type = "call_invite",
                    senderId = senderId,
                    receiverId = "",
                    sessionId = sessionId,
                    callType = callType,
                    senderName = callerName
                ),
                webRtcCallManager,
                callWebSocketClient
            )
        } catch (t: Throwable) {
            android.util.Log.e("IncomingCallActivity", "Accept failed", t)
        }
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("is_ongoing_call_return", true)
        })
        finish()
        return true
    }
}
