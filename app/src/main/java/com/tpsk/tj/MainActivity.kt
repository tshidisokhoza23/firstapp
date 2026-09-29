package com.tpsk.tj

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.ui.navigation.AppNavigation
import com.tpsk.tj.ui.navigation.Screen
import com.tpsk.tj.common.ui.theme.*

import com.tpsk.tj.home.ui.viewmodel.MainViewModel
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.chat.data.*
import com.tpsk.tj.chat.ui.components.CallOverlay
import com.tpsk.tj.common.util.GlobalNavigation
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.chat.rewards.data.RewardsManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.places.api.Places

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tpsk.tj.common.util.BiometricHelper
import com.tpsk.tj.common.util.RootUtils
import android.widget.Toast
import com.tpsk.tj.common.util.ContactsObserver
import com.tpsk.tj.common.util.DisplayNameResolver
import com.tpsk.tj.common.util.UserScopedStorage
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.ArrayList

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import com.tpsk.tj.common.model.User as AppUser

val LocalCurrentUser = staticCompositionLocalOf<AppUser> {
    error("No current user provided")
}

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var displayNameResolver: DisplayNameResolver
    @Inject lateinit var systemRepository: com.tpsk.tj.common.domain.repository.SystemRepository
    @Inject lateinit var chatRepository: com.tpsk.tj.domain.repository.ChatRepository
    @Inject lateinit var adManager: com.tpsk.tj.common.data.ads.AdManager
    @Inject lateinit var sessionManager: com.tpsk.tj.common.domain.auth.SessionManager
    @Inject lateinit var userRepository: com.tpsk.tj.vocab.domain.repository.UserRepository
    @Inject lateinit var settingsRepository: com.tpsk.tj.domain.repository.SettingsRepository
    private var contactsObserver: ContactsObserver? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("chat_settings", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("selected_language", "English") ?: "English"
        super.attachBaseContext(com.tpsk.tj.common.util.LocaleHelper.applyLocale(newBase, lang))
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        if (contactsGranted) {
            setupContactsObserver()
            lifecycleScope.launch {
                try {
                    val phoneNumbers = com.tpsk.tj.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
                    chatRepository.syncContacts(phoneNumbers).collect {}
                    chatRepository.refreshLocalChatNames()
                } catch (_: Exception) {}
            }
        }
        
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
        
        android.util.Log.d("MainActivity", "Permissions result: contacts=$contactsGranted, notifications=$notificationsGranted")
    }

    private fun printSigningHash() {
        try {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                packageManager.getPackageInfo(packageName, @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION") info.signatures
            }

            signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA-1")
                md.update(signature.toByteArray())
                val hash = StringBuilder()
                for (b in md.digest()) {
                    hash.append(String.format("%02X:", b))
                }
                if (hash.length > 0) hash.setLength(hash.length - 1)
                android.util.Log.i("SIGNING_HASH", "SHA-1: $hash")

                val md256 = java.security.MessageDigest.getInstance("SHA-256")
                md256.update(signature.toByteArray())
                val hash256 = StringBuilder()
                for (b in md256.digest()) {
                    hash256.append(String.format("%02X:", b))
                }
                if (hash256.length > 0) hash256.setLength(hash256.length - 1)
                android.util.Log.i("SIGNING_HASH", "SHA-256: $hash256")
            }
        } catch (e: Exception) {
            android.util.Log.e("SIGNING_HASH", "Error getting hash", e)
        }
    }

    private fun consumeIncomingCallIntent(intent: Intent?) {
        intent ?: return
        
        val sessionId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID) ?: return
        val senderId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID) ?: return
        if (GlobalCallState.activeSessionId.value == sessionId || GlobalCallState.autoAcceptSessionId.value == sessionId) {
            return
        }
        val callType = if (intent.getStringExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE) == "video") {
            CallType.VIDEO
        } else {
            CallType.VOICE
        }
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
                senderName = callerName,
            ),
        )
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: android.net.Uri? = intent?.data ?: return
        
        // 1. Legacy Scheme (firstapp://)
        if (data?.scheme == "firstapp") {
            when (data.host) {
                "chat" -> {
                    val id = data.lastPathSegment
                    if (id != null) { GlobalNavigation.navigateTo(Screen.Chat.createRoute(id)) }
                }
                "post" -> {
                    val id = data.lastPathSegment
                    if (id != null) { GlobalNavigation.navigateTo(Screen.PostDetail.createRoute(id)) }
                }
                "song" -> {
                    GlobalNavigation.navigateTo(Screen.Main.route + "?tab=stream")
                }
                "advert" -> {
                    val id = data.lastPathSegment
                    if (id != null) { GlobalNavigation.navigateTo(Screen.AdvertDetail.createRoute(id)) }
                }
                "channel" -> {
                    val id = data.lastPathSegment
                    if (id != null) { GlobalNavigation.navigateTo(Screen.ChannelDetail.createRoute(id)) }
                }
                "group" -> {
                    val segments = data.pathSegments
                    if (segments.size >= 2 && segments[0] == "invite") {
                        GlobalNavigation.navigateTo(Screen.JoinGroup.createRoute(segments[1]))
                    } else {
                        val id = data.lastPathSegment
                        if (id != null) { GlobalNavigation.navigateTo(Screen.GroupDetail.createRoute(id)) }
                    }
                }
            }
        }
        
        // 2. Universal Links (https://firstapp.com/post/...)
        if (data?.scheme == "https" && data.host == "firstapp.com") {
            val path = data.path ?: ""
            if (path.startsWith("/post/")) {
                val id = data.lastPathSegment
                if (id != null) { GlobalNavigation.navigateTo(Screen.PostDetail.createRoute(id)) }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf<String>()
        
        // 1. Notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // 2. Contacts permission (Crucial for Chat Hub)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }
        
        // 3. Audio & Camera (For Calling features)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            android.util.Log.d("MainActivity", "Requesting permissions: $permissions")
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (RootUtils.isDeviceRooted()) {
            Toast.makeText(this, "Security Warning: Device appears to be rooted. App integrity cannot be guaranteed.", Toast.LENGTH_LONG).show()
        }

        consumeIncomingCallIntent(intent)
        handleDeepLink(intent)
        printSigningHash()
        if (intent?.getBooleanExtra("is_ongoing_call_return", false) == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
        }

        // Initialize AdManager (Rule 1: Always init AdMob/MAX before usage)
        adManager.initialize(this)

        if (!Places.isInitialized() && isGooglePlayServicesReady()) {
            Places.initialize(applicationContext, "AIzaSyDvArA2Ykp3Q6EsQMVSre4piq9SvARF7H4")
        }
        
        val isSystemDark = android.content.res.Configuration.UI_MODE_NIGHT_YES == (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
        
        enableEdgeToEdge(
            statusBarStyle = if (isSystemDark) SystemBarStyle.dark(AppBackgroundDarkTop.toArgb()) else SystemBarStyle.light(Color.White.toArgb(), Color.Black.toArgb()),
            navigationBarStyle = if (isSystemDark) SystemBarStyle.dark(AppBackgroundDarkTop.toArgb()) else SystemBarStyle.light(Color.White.toArgb(), Color.Black.toArgb())
        )

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val autoNightMode by viewModel.autoNightMode.collectAsState()
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val selectedLanguage by viewModel.selectedLanguage.collectAsState()

            LaunchedEffect(selectedLanguage) {
                val localeCode = when (selectedLanguage) {
                    "Afrikaans" -> "af"
                    "isiZulu" -> "zu"
                    "isiXhosa" -> "xh"
                    "Sepedi" -> "nso"
                    "Setswana" -> "tn"
                    "Sesotho" -> "st"
                    "Xitsonga" -> "ts"
                    "siSwati" -> "ss"
                    "Tshivenda" -> "ve"
                    "isiNdebele" -> "nr"
                    else -> "en"
                }
                val appLocales = LocaleListCompat.forLanguageTags(localeCode)
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != localeCode) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = if (autoNightMode) systemInDark else isDarkModePref

            val pinLockEnabled by viewModel.pinLockEnabled.collectAsState()
            val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
            val appPin by viewModel.appPin.collectAsState()
            var isAppUnlocked by rememberSaveable { mutableStateOf(!pinLockEnabled && !biometricLockEnabled) }

            // Handle Biometric Prompt on Startup
            LaunchedEffect(biometricLockEnabled, isAppUnlocked) {
                if (biometricLockEnabled && !isAppUnlocked) {
                    com.tpsk.tj.common.util.BiometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        onSuccess = { isAppUnlocked = true },
                        onError = { _, _ -> /* Fallback to PIN if needed or show error */ },
                        onFailed = { /* Handle failure */ }
                    )
                }
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme }
                )
                onDispose { }
            }

            TJTheme(darkTheme = darkTheme) {
                if (!isAppUnlocked && pinLockEnabled && appPin != null) {
                    PinLockScreen(
                        correctPin = appPin!!,
                        onUnlocked = { isAppUnlocked = true }
                    )
                } else {
                    val navController = rememberNavController()
                    val musicViewModel: MusicViewModel = hiltViewModel()
                    val isLoggedIn by sessionManager.authState.collectAsState()
                    val currentUser by sessionManager.currentUser.collectAsState()
                    var isAppLocked by remember(isLoggedIn) { mutableStateOf(false) }
                    var wasLoggedIn by remember { mutableStateOf(isLoggedIn) }

                    LaunchedEffect(isLoggedIn, currentUser) {
                        if (isLoggedIn && currentUser == null) {
                            val fetchJob = lifecycleScope.launch {
                                try {
                                    userRepository.getCurrentUser().collect { res ->
                                        if (res.data != null) {
                                            // Expert Fix: Update session manager with cached data immediately
                                            sessionManager.updateCurrentUser(res.data!!)
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                            
                            // Expert Fix: Safety timeout to break splash deadlock if server is down
                            delay(5000)
                            if (fetchJob.isActive && sessionManager.getToken() != null) {
                                android.util.Log.w("MainActivity", "User fetch taking too long, proceeding with available token")
                            }
                        }

                        if (!isLoggedIn || sessionManager.getToken().isNullOrEmpty()) {
                            val isAtAuthScreen = navController.currentDestination?.route == Screen.Login.route
                            if (!isAtAuthScreen) {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } else if (currentUser != null || sessionManager.getToken() != null) {
                            val isAtAuthScreen = navController.currentDestination?.route == Screen.Login.route
                            val isAtCompleteProfile = navController.currentDestination?.route == Screen.CompleteProfile.route
                            
                            if (currentUser != null && !currentUser!!.isProfileComplete) {
                                if (!isAtCompleteProfile) {
                                    navController.navigate(Screen.CompleteProfile.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else if (isAtAuthScreen || isAtCompleteProfile || !wasLoggedIn) {
                                 navController.navigate(Screen.Main.createRoute()) {
                                    popUpTo(0) { inclusive = true }
                                 }
                            }
                        }
                        
                        if (isLoggedIn) {
                            requestAppPermissions()
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                setupContactsObserver()
                            }
                            isAppLocked = false
                            try {
                                RewardsManager.refreshBalance()
                            } catch (e: Exception) {}
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    launch { chatRepository.syncChats().collect {} }
                                    ContactsObserver.triggerSyncWorker(this@MainActivity)
                                    val phoneNumbers = com.tpsk.tj.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
                                    chatRepository.syncContacts(phoneNumbers).collect {}
                                    chatRepository.refreshLocalChatNames()
                                } catch (e: Exception) {}
                            }
                            try {
                                val serviceIntent = Intent(applicationContext, CallListenerService::class.java)
                                ContextCompat.startForegroundService(applicationContext, serviceIntent)
                                PushTokenRegistrar.syncCurrentToken(applicationContext, sessionManager)
                            } catch (e: Exception) {}
                        } else {
                            isAppLocked = false
                            applicationContext.stopService(Intent(applicationContext, CallListenerService::class.java))
                        }
                        wasLoggedIn = isLoggedIn
                    }

                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        val hasToken = remember(isLoggedIn) { sessionManager.getToken() != null }
                        if (isLoggedIn && currentUser == null && !hasToken) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF2196F3))
                            }
                        } else if (isLoggedIn && currentUser == null) {
                            // If we have a token but no user yet, show loading but allow a "Try Offline" button after a few seconds
                            var showOfflineButton by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(8000)
                                showOfflineButton = true
                            }
                            
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF2196F3))
                                    if (showOfflineButton) {
                                        Spacer(Modifier.height(24.dp))
                                        Text("Connecting to server...", color = Color.Gray, fontSize = 14.sp)
                                        TextButton(onClick = { 
                                            navController.navigate(Screen.Main.createRoute()) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }) {
                                            Text("Continue Offline")
                                        }
                                    }
                                }
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
                                            val userId = sessionManager.getUserId() ?: ""
                                            try { userRepository.logout().collect {} } catch (e: Exception) {}
                                            try { systemRepository.clearAllData() } catch (e: Exception) {}
                                            sessionManager.clearSession()
                                            if (userId.isNotEmpty()) {
                                                UserScopedStorage(applicationContext, userId).clearUserData()
                                            }
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
                            val currentRoute = navController.currentDestination?.route
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        val openChatId = intent?.getStringExtra("open_chat_id")
                        val openMingleRequests = intent?.getBooleanExtra("open_mingle_requests", false) ?: false
                        val openMingleNotif = intent?.getBooleanExtra("open_mingle_notif", false) ?: false
                        val openSocialNotif = intent?.getBooleanExtra("open_social_notif", false) ?: false
                        val senderId = intent?.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID)
                        val callSessionId = intent?.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID)
                        val isOngoingCallReturn = intent?.getBooleanExtra("is_ongoing_call_return", false) ?: false
                        if (isLoggedIn) {
                            if (openMingleRequests) {
                                navController.navigate(Screen.MingleMessageRequests.route) {
                                    launchSingleTop = true
                                }
                            } else if (openMingleNotif) {
                                navController.navigate(Screen.MingleNotification.route) {
                                    launchSingleTop = true
                                }
                            } else if (openSocialNotif) {
                                navController.navigate(Screen.SocialNotification.route) {
                                    launchSingleTop = true
                                }if (callSessionId == null && !isOngoingCallReturn) senderId else null
                            } else {
                                val targetId = openChatId ?: senderId
                                if (targetId != null) {
                                    val openType = intent?.getStringExtra("open_chat_type").orEmpty()
                                    val targetRoute = if (openType.equals("channel", ignoreCase = true)) {
                                        Screen.ChannelDetail.createRoute(targetId)
                                    } else {
                                        Screen.Chat.createRoute(targetId, type = openType.ifBlank { "direct" })
                                    }
                                    val currentRoute = navController.currentDestination?.route
                                    if (currentRoute != targetRoute) {
                                        navController.navigate(targetRoute) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
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
            } catch (e: Exception) {}
            lifecycleScope.launch {
                try {
                    val phoneNumbers = com.tpsk.tj.common.util.ContactUtils.getPhoneContacts(this@MainActivity)
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
        printSigningHash()
        if (intent.getBooleanExtra("is_ongoing_call_return", false)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
        }
        
        val senderId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID)
        val isOngoingCallReturn = intent.getBooleanExtra("is_ongoing_call_return", false)
        val openChatId = intent.getStringExtra("open_chat_id")
        if (openChatId != null && sessionManager.isLoggedIn()) {
            val openType = intent.getStringExtra("open_chat_type").orEmpty()
            val route = if (openType.equals("channel", ignoreCase = true)) {
                Screen.ChannelDetail.createRoute(openChatId)
            } else {
                Screen.Chat.createRoute(openChatId, type = openType.ifBlank { "direct" })
            }
            GlobalNavigation.navigateTo(route)
        }

        val callSessionId = intent.getStringExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID)
        if (senderId != null && callSessionId == null && sessionManager.isLoggedIn() && !isOngoingCallReturn) {
            GlobalNavigation.navigateTo(Screen.Chat.createRoute(senderId, type = "initiate"))
        }
    }

    private fun setupContactsObserver() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            contactsObserver?.unregister()
            contactsObserver = ContactsObserver(this) {
                displayNameResolver.clearCache()
                if (sessionManager.isLoggedIn()) {
                    lifecycleScope.launch {
                        try {
                            chatRepository.refreshLocalChatNames()
                        } catch (_: Exception) {}
                    }
                }
            }
            contactsObserver?.register()
        } catch (e: Exception) {}
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

@Composable
fun PinLockScreen(correctPin: String, onUnlocked: () -> Unit) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Enter PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val filled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
            }
        }
        
        if (error) {
            Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }
        
        Spacer(Modifier.height(48.dp))
        
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(280.dp)
        ) {
            items(numbers.size) { index ->
                val label = numbers[index]
                Surface(
                    onClick = {
                        when (label) {
                            "C" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                            "OK" -> {
                                if (enteredPin == correctPin) onUnlocked()
                                else {
                                    error = true
                                    enteredPin = ""
                                }
                            }
                            else -> if (enteredPin.length < 4) {
                                enteredPin += label
                                error = false
                            }
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
