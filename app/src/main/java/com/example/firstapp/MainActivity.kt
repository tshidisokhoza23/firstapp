package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.realtime.CallEvent
import com.example.firstapp.data.realtime.CallNotificationActionReceiver
import com.example.firstapp.data.realtime.CallType
import com.example.firstapp.data.realtime.CallListenerService
import com.example.firstapp.data.realtime.GlobalCallState
import com.example.firstapp.data.realtime.PushTokenRegistrar
import com.example.firstapp.ui.screens.LoginScreen
import com.example.firstapp.ui.screens.MainScreen
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.FirstAppTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {

    private fun consumeIncomingCallIntent(intent: Intent?) {
        intent ?: return
        val sessionId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID) ?: return
        val senderId = intent.getIntExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, -1)
        if (senderId <= 0) return
        val callType = if (intent.getStringExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE) == "video") {
            CallType.VIDEO
        } else {
            CallType.VOICE
        }
        val callerName = intent.getStringExtra("caller_name") ?: ""

        GlobalCallState.onCallInvite(
            CallEvent(
                type = "call_invite",
                senderId = senderId,
                receiverId = 0,
                sessionId = sessionId,
                callType = callType,
                senderName = callerName
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.initialize(applicationContext)
        consumeIncomingCallIntent(intent)

        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}
        
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw")
        }
        
        // Ensure system bars match our AppBackground
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb())
        )

        setContent {
            FirstAppTheme {
                var isLoggedIn by remember { mutableStateOf(TokenManager.isLoggedIn()) }

                  LaunchedEffect(isLoggedIn) {
                      val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
                      if (isLoggedIn) {
                          ContextCompat.startForegroundService(applicationContext, serviceIntent)
                          PushTokenRegistrar.syncCurrentToken(applicationContext)
                      } else {
                          applicationContext.stopService(serviceIntent)
                      }
                  }

                if (isLoggedIn) {
                    MainScreen(onLogout = {
                        TokenManager.clearToken()
                        isLoggedIn = false
                    })
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingCallIntent(intent)
    }
}
