package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.realtime.*
import com.example.firstapp.ui.navigation.AppNavigation
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.FirstAppTheme
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.viewmodel.MusicViewModel
import com.example.firstapp.util.GlobalNavigation
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
        val callerName = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME) ?: ""

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
        consumeIncomingCallIntent(intent)

        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }

        MobileAds.initialize(this) {}
        
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw")
        }
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb())
        )

        setContent {
            FirstAppTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()
                val musicViewModel: MusicViewModel = viewModel()
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

                val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route
                
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination,
                    viewModel = viewModel,
                    musicViewModel = musicViewModel,
                    onLogout = {
                        TokenManager.clear()
                        isLoggedIn = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    GlobalNavigation.navigationEvents.collect { route ->
                        navController.navigate(route)
                    }
                }

                // Handle initial chat navigation from intent
                LaunchedEffect(Unit) {
                    val openChatId = intent?.getIntExtra("open_chat_id", -1) ?: -1
                    val senderId = intent?.getIntExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, -1) ?: -1
                    if (isLoggedIn) {
                        if (openChatId > 0) {
                            navController.navigate(Screen.Chat.createRoute(openChatId))
                        } else if (senderId > 0) {
                            navController.navigate(Screen.Chat.createRoute(senderId))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (TokenManager.isLoggedIn()) {
            val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingCallIntent(intent)
        
        // Handle navigation for already running activity
        val senderId = intent.getIntExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, -1)
        if (senderId > 0 && TokenManager.isLoggedIn()) {
            GlobalNavigation.navigateTo(Screen.Chat.createRoute(senderId))
        }
    }
}
