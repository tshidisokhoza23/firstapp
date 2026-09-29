package com.tpsk.tj.chat.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.tpsk.tj.chat.data.api.IceServerConfig
import com.tpsk.tj.MainActivity
import com.tpsk.tj.R
import com.tpsk.tj.common.domain.auth.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import android.net.ConnectivityManager
import android.net.Network
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallListenerService : Service() {

    @Inject lateinit var webRtcCallManager: WebRtcCallManager
    @Inject lateinit var callTonePlayer: CallTonePlayer
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var callWebSocketClient: CallWebSocketClient
    @Inject lateinit var repository: com.tpsk.tj.domain.repository.ChatRepository

    private val activeIncomingNotifications = ConcurrentHashMap<String, Int>()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var callTimeoutJob: Job? = null
    private var callDurationJob: Job? = null
    private var incomingTimeoutJob: Job? = null

    private val telephonyManager by lazy { getSystemService(android.telephony.TelephonyManager::class.java) }
    
    private val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : android.telephony.TelephonyCallback(), android.telephony.TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleTelephonyState(state)
            }
        }
    } else null

    private fun handleTelephonyState(state: Int) {
        if (state != android.telephony.TelephonyManager.CALL_STATE_IDLE) {
            // GSM call active or ringing, abort WebRTC
            if (GlobalCallState.isCallActive.value) {
                Log.i("CallListenerService", "GSM call detected, ending WebRTC call")
                val sessionId = GlobalCallState.activeSessionId.value
                val participantId = GlobalCallState.activeParticipantId.value
                val callType = GlobalCallState.activeCallType.value
                if (sessionId != null && participantId != null && callType != null) {
                    callWebSocketClient.end(participantId, sessionId, callType, 0, "gsm_interruption")
                    GlobalCallState.onCallEnded(sessionId)
                    webRtcCallManager.end()
                    callTonePlayer.stop()
                }
            }
        }
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (sessionManager.isLoggedIn()) {
                Log.d("CallListenerService", "Network available, connecting socket")
                callWebSocketClient.connect(listener)
            }
        }

        override fun onLost(network: Network) {
            Log.d("CallListenerService", "Network lost, disconnecting socket")
            callWebSocketClient.disconnect(listener)
        }
    }

    private val listener = object : CallSocketListener {
        override fun onOpen() {
            // No-op
        }

        override fun onEvent(event: CallEvent) {
            val currentUserId = sessionManager.getUserId() ?: return
            
            if (event.senderId == currentUserId) {
                return
            }

            val type = event.type ?: return
            when (type) {
                "call_invite" -> {
                    if (event.receiverId == currentUserId) {
                        if (GlobalCallState.isCallActive.value) {
                            val activeSessionId = GlobalCallState.activeSessionId.value
                            if (activeSessionId == event.sessionId) {
                                Log.d("CallListenerService", "Duplicate invite for active session $activeSessionId, ignoring")
                                return
                            }
                            
                            val activePid = GlobalCallState.activeParticipantId.value
                            if (!GlobalCallState.isConnectionEstablished.value && activePid == event.senderId) {
                                // Glare detected: both users called each other simultaneously.
                                // Logic: smaller user ID wins (becomes the offerer).
                                if (currentUserId < event.senderId) {
                                    Log.d("CallListenerService", "Glare detected: I win tie-breaker. Rejecting incoming.")
                                    callWebSocketClient.reject(event.senderId, event.sessionId, event.callType, "glare")
                                    return
                                } else {
                                    Log.d("CallListenerService", "Glare detected: I lose tie-breaker. Aborting my offer.")
                                    val mySessionId = GlobalCallState.activeSessionId.value
                                    if (mySessionId != null) GlobalCallState.onCallEnded(mySessionId)
                                    webRtcCallManager.end()
                                    // Fall through to accept their invite
                                }
                            } else {
                                callWebSocketClient.reject(
                                    receiverId = event.senderId,
                                    sessionId = event.sessionId,
                                    callType = event.callType,
                                    reason = "busy"
                                )
                                return
                            }
                        }

                        // Fetch ICE servers for professional TURN support
                        serviceScope.launch {
                            val iceResource = repository.getICEServers()
                            if (iceResource is com.tpsk.tj.common.util.Resource.Success) {
                                val servers = iceResource.data?.mapNotNull { config: IceServerConfig ->
                                    val urls = config.resolvedUrls()
                                    if (urls.isEmpty()) return@mapNotNull null
                                    val builder = org.webrtc.PeerConnection.IceServer.builder(urls)
                                    if (config.username != null) builder.setUsername(config.username)
                                    if (config.credential != null) builder.setPassword(config.credential)
                                    builder.createIceServer()
                                }
                                if (servers != null) {
                                    webRtcCallManager.setIceServers(servers)
                                }
                            }
                        }

                        GlobalCallState.onCallInvite(event)
                        showIncomingCallNotification(event)
                        callWebSocketClient.ringing(event.senderId, event.sessionId, event.callType)
                        openIncomingCallScreen(event)
                    }
                }
                "call_ringing" -> {
                    if (GlobalCallState.activeSessionId.value == event.sessionId) {
                        GlobalCallState.setRinging(true)
                    }
                }
                "call_reject", "call_end" -> {
                    val isActiveSession = GlobalCallState.activeSessionId.value == event.sessionId
                    GlobalCallState.onCallEnded(event.sessionId)
                    dismissIncomingCallNotification(event.sessionId)
                    
                    if (event.endReason == "busy" && isActiveSession) {
                        callTonePlayer.playBusyTone()
                    } else {
                        callTonePlayer.stop()
                    }
                    
                    if (isActiveSession) {
                        webRtcCallManager.end()
                    }
                }
                "call_accept" -> {
                    callTonePlayer.stop()
                    GlobalCallState.setRinging(false)
                    if (GlobalCallState.activeSessionId.value == event.sessionId &&
                        !GlobalCallState.isConnectionEstablished.value
                    ) {
                        webRtcCallManager.createOffer()
                    }
                }
                "call_signal" -> {
                    if (GlobalCallState.activeSessionId.value == event.sessionId) {
                        webRtcCallManager.handleSignal(event.signalType, event.signalPayload)
                    }
                }
            }
        }

        override fun onError(error: String) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        createChannels()

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        
        // Register Telephony Callback for GSM interruption handling
        try {
            val hasPhoneStatePermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasPhoneStatePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    telephonyCallback?.let { telephonyManager?.registerTelephonyCallback(mainExecutor, it) }
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(object : android.telephony.PhoneStateListener() {
                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleTelephonyState(state)
                        }
                    }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
                }
            } else {
                Log.w("CallListenerService", "READ_PHONE_STATE permission not granted, GSM interruption handling disabled")
            }
        } catch (e: SecurityException) {
            Log.e("CallListenerService", "SecurityException while registering telephony callback", e)
        } catch (e: Exception) {
            Log.e("CallListenerService", "Error registering telephony callback", e)
        }

        webRtcCallManager.onSignal = { signalType, payload ->
            val participantId = GlobalCallState.activeParticipantId.value
            val sessionId = GlobalCallState.activeSessionId.value
            val callType = GlobalCallState.activeCallType.value
            if (participantId != null && sessionId != null && callType != null) {
                callWebSocketClient.signal(
                    receiverId = participantId,
                    sessionId = sessionId,
                    callType = callType,
                    signalType = signalType,
                    signalPayload = payload
                )
            }
        }

        webRtcCallManager.onDisconnected = {
            serviceScope.launch {
                val sessionId = GlobalCallState.activeSessionId.value
                val participantId = GlobalCallState.activeParticipantId.value
                val callType = GlobalCallState.activeCallType.value
                if (sessionId != null && participantId != null && callType != null) {
                    Log.w("CallListenerService", "WebRTC Disconnected, ending session")
                    callWebSocketClient.end(participantId, sessionId, callType, 0, "webrtc_disconnected")
                    GlobalCallState.onCallEnded(sessionId)
                    webRtcCallManager.end()
                    callTonePlayer.stop()
                }
            }
        }

        webRtcCallManager.onConnected = {
            GlobalCallState.setConnectionEstablished(true)
            callTonePlayer.stop()
        }

        serviceScope.launch {
            GlobalCallState.incomingCall.collect { call ->
                incomingTimeoutJob?.cancel()
                if (call == null) {
                    val manager = getSystemService(NotificationManager::class.java)
                    activeIncomingNotifications.values.forEach { id ->
                        manager?.cancel(id)
                    }
                    activeIncomingNotifications.clear()
                } else {
                    val sessionId = call.sessionId
                    incomingTimeoutJob = serviceScope.launch {
                        delay(45_000)
                        val still = GlobalCallState.incomingCall.value
                        if (still?.sessionId == sessionId && !GlobalCallState.isCallActive.value) {
                            callWebSocketClient.reject(still.senderId, sessionId, still.callType, "timeout")
                            GlobalCallState.onCallEnded(sessionId)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            GlobalCallState.isCallActive.collect { active ->
                updateServiceNotification(active)
                
                if (active) {
                    // Start timeout if call not established yet
                    if (!GlobalCallState.isConnectionEstablished.value) {
                        startCallTimeout()
                    }
                } else {
                    cancelCallTimeout()
                }
            }
        }

        serviceScope.launch {
            GlobalCallState.isConnectionEstablished.collect { established ->
                if (established) {
                    cancelCallTimeout()
                    startCallDurationTimer()
                } else {
                    stopCallDurationTimer()
                }
            }
        }
    }

    private fun startCallDurationTimer() {
        callDurationJob?.cancel()
        callDurationJob = serviceScope.launch {
            var seconds = 0
            while (isActive) {
                delay(1000)
                seconds++
                GlobalCallState.setCallDuration(seconds)
            }
        }
    }

    private fun stopCallDurationTimer() {
        callDurationJob?.cancel()
        callDurationJob = null
    }

    private fun startCallTimeout() {
        callTimeoutJob?.cancel()
        callTimeoutJob = serviceScope.launch {
            delay(30000) // 30 seconds
            val sessionId = GlobalCallState.activeSessionId.value
            val participantId = GlobalCallState.activeParticipantId.value
            val callType = GlobalCallState.activeCallType.value
            if (sessionId != null && participantId != null && callType != null) {
                Log.w("CallListenerService", "Call timed out after 30s")
                callWebSocketClient.end(participantId, sessionId, callType, 0, "timeout")
                GlobalCallState.onCallEnded(sessionId)
                webRtcCallManager.end()
                callTonePlayer.stop()
            }
        }
    }

    private fun cancelCallTimeout() {
        callTimeoutJob?.cancel()
        callTimeoutJob = null
    }

    private fun updateServiceNotification(isCallActive: Boolean) {
        if (isCallActive) {
            val sessionId = GlobalCallState.activeSessionId.value
            val participantId = GlobalCallState.activeParticipantId.value ?: ""
            val callType = GlobalCallState.activeCallType.value ?: CallType.VOICE
            val callTypeStr = if (callType == CallType.VIDEO) "video" else "voice"

            val participantName = GlobalCallState.getContactName(participantId)?.takeIf { it.isNotBlank() }
                ?: GlobalCallState.getContactPhone(participantId)
                ?: participantId

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (sessionId != null) putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, sessionId)
                if (participantId.isNotBlank()) putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, participantId)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
                putExtra("is_ongoing_call_return", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                9002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // FIX: Use CHANNEL_ONGOING_CALLS (Low Importance) for ACTIVE calls
            // Using a high importance channel with a ringtone (like CHANNEL_INCOMING_CALLS) 
            // causes the device to keep ringing even after answering.
            val channelId = CHANNEL_ONGOING_CALLS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                
                val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (hasCam) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                
                val n = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Ongoing Call with $participantName")
                    .setContentText("Tap to return to call")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build()
                
                try {
                    startForeground(ONGOING_NOTIFICATION_ID, n, type)
                } catch (e: Exception) {
                    Log.e("CallListenerService", "Failed to start ongoing call foreground: ${e.message}")
                    try {
                        startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } catch (_: Exception) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            try {
                                startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
                            } catch (_: Exception) {
                                stopSelf()
                            }
                        } else {
                            try {
                                startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                            } catch (_: Exception) {
                                stopSelf()
                            }
                        }
                    }
                }
            } else {
                val n = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Ongoing Call with $participantName")
                    .setContentText("Tap to return to call")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        startForeground(ONGOING_NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } catch (_: Exception) {
                        stopSelf()
                    }
                } else {
                    try {
                        startForeground(ONGOING_NOTIFICATION_ID, n)
                    } catch (_: Exception) {
                        stopSelf()
                    }
                }
            }
        } else if (GlobalCallState.incomingCall.value != null) {
            val caller = GlobalCallState.incomingCall.value?.senderName?.takeIf { it.isNotBlank() } ?: "Incoming call"
            startTransientCallForeground(caller)
        } else {
            releaseIdleForeground()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!sessionManager.isLoggedIn()) {
            releaseIdleForeground()
            stopSelf()
            return START_NOT_STICKY
        }
        callWebSocketClient.connect(listener)
        val incoming = intent?.getBooleanExtra(EXTRA_INCOMING_CALL, false) == true
        when {
            GlobalCallState.isCallActive.value -> updateServiceNotification(true)
            incoming || GlobalCallState.incomingCall.value != null -> startTransientCallForeground(
                intent?.getStringExtra(EXTRA_CALLER_NAME)?.takeIf { it.isNotBlank() }
                    ?: GlobalCallState.incomingCall.value?.senderName?.takeIf { it.isNotBlank() }
                    ?: "Incoming call"
            )
            else -> releaseIdleForeground()
        }
        return START_NOT_STICKY
    }

    private fun releaseIdleForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
        getSystemService(NotificationManager::class.java)?.cancel(ONGOING_NOTIFICATION_ID)
    }

    private fun startTransientCallForeground(callerName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText("Incoming call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()
        startCallForeground(notification)
    }

    private fun startCallForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    ONGOING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } catch (e: Exception) {
                Log.w("CallListenerService", "Failed to start foreground with REMOTE_MESSAGING, trying PHONE_CALL", e)
                startForeground(
                    ONGOING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun showIncomingCallNotification(event: CallEvent) {
        val callTypeStr = if (event.callType == CallType.VIDEO) "video" else "voice"
        val notificationId = event.sessionId.hashCode()
        val senderIdStr = event.senderId
        val callerName = event.senderName.takeIf { it.isNotBlank() && it != senderIdStr } 
            ?: GlobalCallState.getContactName(senderIdStr)
            ?: GlobalCallState.getContactPhone(senderIdStr) 
            ?: ""

        val fullScreenActivityIntent = Intent(this, com.tpsk.tj.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            event.sessionId.hashCode(),
            fullScreenActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        activeIncomingNotifications[event.sessionId] = notificationId

        fun actionIntent(action: String): PendingIntent {
            val i = Intent(this, CallNotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
                putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
                putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
                putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                this,
                (event.sessionId + action).hashCode(),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val caller = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(60000) 
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    actionIntent(CallNotificationActionReceiver.ACTION_DECLINE),
                    acceptActivityIntent(event, callerName, callTypeStr, notificationId)
                )
            )
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun openIncomingCallScreen(event: CallEvent) {
        val callTypeStr = if (event.callType == CallType.VIDEO) "video" else "voice"
        val callerName = event.senderName.takeIf { it.isNotBlank() && it != event.senderId }
            ?: GlobalCallState.getContactName(event.senderId)
            ?: ""
        val intent = Intent(this, com.tpsk.tj.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, event.sessionId.hashCode())
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.d("CallListenerService", "Incoming screen deferred to full-screen intent: ${e.message}")
        }
    }

    private fun acceptActivityIntent(
        event: CallEvent,
        callerName: String,
        callTypeStr: String,
        notificationId: Int
    ): PendingIntent {
        val accept = Intent(this, com.tpsk.tj.IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationActionReceiver.EXTRA_SESSION_ID, event.sessionId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_ID, event.senderId)
            putExtra(CallNotificationActionReceiver.EXTRA_SENDER_NAME, callerName)
            putExtra(CallNotificationActionReceiver.EXTRA_CALL_TYPE, callTypeStr)
            putExtra(CallNotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
            putExtra("auto_accept", true)
        }
        return PendingIntent.getActivity(
            this,
            (event.sessionId + "_accept").hashCode(),
            accept,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissIncomingCallNotification(sessionId: String) {
        val id = activeIncomingNotifications.remove(sessionId) ?: return
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(id)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Quiet channel keeps the listener alive without a shade alert.
        // A new id is required because Android will not lower an existing channel.
        val quietChannel = NotificationChannel(
            CHANNEL_CALL_SERVICE_QUIET,
            "Call listener",
            NotificationManager.IMPORTANCE_MIN
        )
        quietChannel.description = "Keeps incoming calls connected"
        quietChannel.setShowBadge(false)
        quietChannel.setSound(null, null)
        quietChannel.enableVibration(false)
        quietChannel.enableLights(false)
        quietChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        manager.createNotificationChannel(quietChannel)

        // Fallback only. Some vendor OSes reject a min-importance foreground service.
        val serviceChannel = NotificationChannel(
            CHANNEL_CALL_SERVICE,
            "System Service",
            NotificationManager.IMPORTANCE_LOW
        )
        serviceChannel.description = "Background connectivity"
        serviceChannel.setShowBadge(false)
        serviceChannel.setSound(null, null)
        serviceChannel.enableVibration(false)
        manager.createNotificationChannel(serviceChannel)

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incomingChannel = NotificationChannel(
            CHANNEL_INCOMING_CALLS,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        )
        incomingChannel.description = "Alerts for incoming calls"
        incomingChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        incomingChannel.setSound(ringtoneUri, audioAttributes)
        incomingChannel.enableVibration(true)
        incomingChannel.vibrationPattern = longArrayOf(0, 1000, 1000)

        manager.createNotificationChannel(incomingChannel)

        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING_CALLS,
            "Active calls",
            NotificationManager.IMPORTANCE_LOW
        )
        ongoingChannel.description = "Ongoing call status"
        ongoingChannel.setShowBadge(false)
        manager.createNotificationChannel(ongoingChannel)
    }

    companion object {
        private const val CHANNEL_CALL_SERVICE = "call_service"
        private const val CHANNEL_CALL_SERVICE_QUIET = "call_service_quiet"
        private const val CHANNEL_INCOMING_CALLS = "incoming_calls"
        private const val CHANNEL_ONGOING_CALLS = "ongoing_calls"
        private const val ONGOING_NOTIFICATION_ID = 9001
        const val EXTRA_INCOMING_CALL = "incoming_call"
        const val EXTRA_CALLER_NAME = "caller_name"
    }
}
