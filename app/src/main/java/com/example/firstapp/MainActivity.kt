package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.ui.navigation.AppNavigation
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.FirstAppTheme
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.ui.components.CallOverlay
import com.example.firstapp.common.util.GlobalNavigation
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.Places

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.firstapp.common.util.BiometricHelper
import com.example.firstapp.common.util.RootUtils
import android.widget.Toast

 @dagger.hilt.android.AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private fun consumeIncomingCallIntent(intent: Intent?) {
        intent ?: return
        
        val sessionId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID) ?: return
        val senderId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID) ?: return
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
                receiverId = "",
                sessionId = sessionId,
                callType = callType,
                senderName = callerName,
            ),
        )
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: android.net.Uri? = intent?.data
        if (data != null && (data.scheme == "firstapp")) {
            when (data.host) {
                "chat" -> {
                    val id = data.lastPathSegment
                    id?.let { GlobalNavigation.navigateTo(Screen.Chat.createRoute(it)) }
                }
                "post" -> {
                    // Navigate to single post view if available, or media feed
                    GlobalNavigation.navigateTo(Screen.Main.route + "?tab=media")
                }
                "song" -> {
                    // Navigate to music search or player
                    GlobalNavigation.navigateTo(Screen.Main.route + "?tab=music")
                }
                "advert" -> {
                    val id = data.lastPathSegment
                    id?.let { GlobalNavigation.navigateTo(Screen.AdvertDetail.createRoute(it)) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (RootUtils.isDeviceRooted()) {
            Toast.makeText(this, "Security Warning: Device appears to be rooted. App integrity cannot be guaranteed.", Toast.LENGTH_LONG).show()
        }

        // Prevent screenshots and screen recording for high security
        // window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)

        consumeIncomingCallIntent(intent)
        handleDeepLink(intent)

        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }

        // Request calling permissions early if possible
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 2002)
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
            FirstAppTheme(darkTheme = true) {
                val navController = rememberNavController()
                val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val musicViewModel: MusicViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val isLoggedIn by TokenManager.authState.collectAsState()
                var isAppLocked by remember(isLoggedIn) { mutableStateOf(isLoggedIn) }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn && BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isAppLocked = false },
                            onError = { _, _ -> /* Handle error */ },
                            onFailed = { /* Handle failure */ }
                        )
                    } else {
                        isAppLocked = false
                    }
                }

                LaunchedEffect(isLoggedIn) {
                    android.util.Log.d("MainActivity", "isLoggedIn: $isLoggedIn")
                    if (!isLoggedIn) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
                    if (isLoggedIn) {
                        try {
                            ContextCompat.startForegroundService(applicationContext, serviceIntent)
                            PushTokenRegistrar.syncCurrentToken(applicationContext)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to start CallListenerService", e)
                        }
                    } else {
                        applicationContext.stopService(serviceIntent)
                    }
                }

                val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route
                
                Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
                    if (!isAppLocked) {
                        AppNavigation(
                            navController = navController,
                            startDestination = startDestination,
                            viewModel = viewModel,
                            musicViewModel = musicViewModel,
                            onLogout = {
                                TokenManager.clear()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        // Show a placeholder or lock screen
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("App Locked", color = androidx.compose.ui.graphics.Color.White, fontSize = 24.sp)
                            Button(
                                onClick = {
                                    if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = this@MainActivity,
                                            onSuccess = { isAppLocked = false },
                                            onError = { _, _ -> },
                                            onFailed = { }
                                        )
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Unlock")
                            }
                        }
                    }

                    CallOverlay()
                }

                LaunchedEffect(Unit) {
                    GlobalNavigation.navigationEvents.collect { route ->
                        navController.navigate(route)
                    }
                }

                // Handle initial chat navigation from intent
                LaunchedEffect(Unit) {
                    val openChatId = intent?.getStringExtra("open_chat_id")
                    val senderId = intent?.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID)
                    if (isLoggedIn) {
                        openChatId?.let { 
                            navController.navigate(Screen.Chat.createRoute(it)) 
                        } ?: senderId?.let { 
                            navController.navigate(Screen.Chat.createRoute(it))
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
            try {
                ContextCompat.startForegroundService(applicationContext, serviceIntent)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start service on resume", e)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingCallIntent(intent)
        handleDeepLink(intent)
        
        // Handle notification-specific navigation
        val senderId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID)
        if (senderId != null && TokenManager.isLoggedIn()) {
            GlobalNavigation.navigateTo(Screen.Chat.createRoute(senderId))
        }
    }
}
