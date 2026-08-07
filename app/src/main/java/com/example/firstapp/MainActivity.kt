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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.firstapp.ui.navigation.AppNavigation
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.ui.theme.*

import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.ui.components.CallOverlay
import com.example.firstapp.common.util.GlobalNavigation
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.places.api.Places

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.firstapp.common.util.BiometricHelper
import com.example.firstapp.common.util.RootUtils
import android.widget.Toast
import com.example.firstapp.common.util.ContactsObserver
import com.example.firstapp.common.util.DisplayNameResolver
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.ArrayList

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.firstapp.common.model.User as AppUser

val LocalCurrentUser = staticCompositionLocalOf<AppUser> {
    error("No current user provided")
}

 @dagger.hilt.android.AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var displayNameResolver: DisplayNameResolver
    @Inject lateinit var systemRepository: com.example.firstapp.common.domain.repository.SystemRepository
    @Inject lateinit var chatRepository: com.example.firstapp.domain.repository.ChatRepository
    @Inject lateinit var adManager: com.example.firstapp.common.data.ads.AdManager
    @Inject lateinit var sessionManager: com.example.firstapp.common.domain.auth.SessionManager
    private var contactsObserver: ContactsObserver? = null

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
                    if (id != null) { GlobalNavigation.navigateTo(Screen.Chat.createRoute(id)) }
                }
                "post" -> {
                    val shortId = data.lastPathSegment
                    val path = data.path ?: ""
                    if (path.indexOf("short") != -1) {
                        GlobalNavigation.navigateTo(Screen.Main.createRoute(tab = "media", shortId = shortId))
                    } else {
                        GlobalNavigation.navigateTo(Screen.Main.route + "?tab=media")
                    }
                }
                "song" -> {
                    // Navigate to music search or player
                    GlobalNavigation.navigateTo(Screen.Main.route + "?tab=music")
                }
                "advert" -> {
                    val id = data.lastPathSegment
                    if (id != null) { GlobalNavigation.navigateTo(Screen.AdvertDetail.createRoute(id)) }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }

        // Request calling permissions early if possible
        val permissionsToRequest = ArrayList<String>()
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
            requestPermissions(permissionsToRequest.toArray(arrayOfNulls<String>(0)), 2002)
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

        adManager.initialize(this)

        if (!Places.isInitialized() && isGooglePlayServicesReady()) {
            Places.initialize(applicationContext, "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw")
        } else if (!isGooglePlayServicesReady()) {
            android.util.Log.w("MainActivity", "Skipping Places init: Google Play services unavailable")
        }
        
        val isSystemDark = android.content.res.Configuration.UI_MODE_NIGHT_YES == (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
        
        enableEdgeToEdge(
            statusBarStyle = if (isSystemDark) SystemBarStyle.dark(AppBackgroundDarkTop.toArgb()) else SystemBarStyle.light(Color.White.toArgb(), Color.Black.toArgb()),
            navigationBarStyle = if (isSystemDark) SystemBarStyle.dark(AppBackgroundDarkTop.toArgb()) else SystemBarStyle.light(Color.White.toArgb(), Color.Black.toArgb())
        )

        setContent {
            val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val autoNightMode by viewModel.autoNightMode.collectAsState()
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = if (autoNightMode) isSystemDark else isDarkModePref

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme }
                )
                onDispose { }
            }

            TJTheme(darkTheme = darkTheme) {

                val navController = rememberNavController()
                val musicViewModel: MusicViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val isLoggedIn by sessionManager.authState.collectAsState()
                val currentUser by sessionManager.currentUser.collectAsState()
                var isAppLocked by remember(isLoggedIn) { mutableStateOf(isLoggedIn) }
                var wasLoggedIn by remember { mutableStateOf(isLoggedIn) }

                LaunchedEffect(isLoggedIn) {
                    android.util.Log.d("MainActivity", "isLoggedIn: $isLoggedIn, wasLoggedIn: $wasLoggedIn")
                    
                    // 1. Handle Navigation Transitions (Login/Logout)
                    if (wasLoggedIn && !isLoggedIn) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (!wasLoggedIn && isLoggedIn) {
                        navController.navigate(Screen.Main.createRoute()) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    
                    // 2. Handle Biometric Lock
                    if (isLoggedIn) {
                        requestAppPermissions()
                        setupContactsObserver()

                        if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                            BiometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = { isAppLocked = false },
                                onError = { _, _ -> isAppLocked = false },
                                onFailed = { /* Handle failure, let them retry manually via button */ }
                            )
                        } else {
                            isAppLocked = false
                        }

                        // 3. Global Sync on Login/Resume
                        try {
                            RewardsManager.refreshBalance()
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to refresh balance", e)
                        }
                        lifecycleScope.launch {
                            try {
                                val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
                                chatRepository.syncContacts(phoneNumbers).collect {}
                                chatRepository.refreshLocalChatNames()
                                
                                // Populate GlobalCallState names from local chats
                                chatRepository.getChats().collect { res ->
                                    if (res is Resource.Success) {
                                        res.data?.forEach { chat ->
                                            if (chat.senderName.isNotBlank() && chat.senderName.lowercase() != "unknown") {
                                                GlobalCallState.updateContactInfo(chat.id, chat.senderName, chat.phoneNumber)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to sync contacts", e)
                            }
                        }
                        
                        try {
                            val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
                            ContextCompat.startForegroundService(applicationContext, serviceIntent)
                            PushTokenRegistrar.syncCurrentToken(applicationContext, sessionManager)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to start services", e)
                        }
                    } else {
                        isAppLocked = false
                        applicationContext.stopService(Intent(applicationContext, CallListenerService::class.java))
                    }
                    
                    wasLoggedIn = isLoggedIn
                }

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (isLoggedIn && currentUser == null) {
                        // Splash/Loading while session user is being restored from storage
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF2196F3))
                        }
                    } else {
                        val content = @Composable {
                            AppNavigation(
                                navController = navController,
                                startDestination = if (isLoggedIn) Screen.Main.createRoute() else Screen.Login.route,
                                viewModel = viewModel,
                                musicViewModel = musicViewModel,
                                adManager = adManager,
                                onLogout = {
                                    lifecycleScope.launch {
                                        systemRepository.clearAllData()
                                        sessionManager.clearSession()
                                    }
                                }
                            )
                        }

                        if (isLoggedIn && currentUser != null) {
                            CompositionLocalProvider(LocalCurrentUser provides currentUser!!) {
                                content()
                            }
                        } else {
                            content()
                        }
                    }
                    
                    if (isAppLocked) {
                        // Show a placeholder or lock screen overlay
                        Column(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("App Locked", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp)
                            Button(
                                onClick = {
                                    if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = this@MainActivity,
                                            onSuccess = { isAppLocked = false },
                                            onError = { _, _ -> isAppLocked = false },
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

                LaunchedEffect(Unit) {
                    val openChatId = intent?.getStringExtra("open_chat_id")
                    val senderId = intent?.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID)
                    if (isLoggedIn) {
                        if (openChatId != null) { 
                            navController.navigate(Screen.Chat.createRoute(openChatId)) 
                        } else if (senderId != null) { 
                            navController.navigate(Screen.Chat.createRoute(senderId))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
            try {
                ContextCompat.startForegroundService(applicationContext, serviceIntent)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start service on resume", e)
            }
            
            // Sync contacts when app comes to foreground
            lifecycleScope.launch {
                try {
                    val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
                    chatRepository.syncContacts(phoneNumbers).collect {}
                } catch (_: Exception) {}
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
        if (senderId != null && sessionManager.isLoggedIn()) {
            GlobalNavigation.navigateTo(Screen.Chat.createRoute(senderId))
        }
    }

    private fun setupContactsObserver() {
        contactsObserver = ContactsObserver(this) {
            displayNameResolver.clearCache()
            if (sessionManager.isLoggedIn()) {
                lifecycleScope.launch {
                    try {
                        chatRepository.refreshLocalChatNames()
                        val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
                        chatRepository.syncContacts(phoneNumbers).collect {}
                    } catch (_: Exception) {}
                }
            }
        }
        contactsObserver?.register()
    }

    private fun isGooglePlayServicesReady(): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
    }

    override fun onDestroy() {
        super.onDestroy()
        contactsObserver?.unregister()
    }
}
