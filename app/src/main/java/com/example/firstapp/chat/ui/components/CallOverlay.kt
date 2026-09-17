package com.example.firstapp.chat.ui.components

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.chat.data.*
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

import com.example.firstapp.chat.ui.viewmodel.CallViewModel

@Composable
fun CallOverlay(
    viewModel: CallViewModel = hiltViewModel()
) {
    val callManager = viewModel.webRtcCallManager
    val tonePlayer = viewModel.callTonePlayer
    val isCallActive by GlobalCallState.isCallActive.collectAsState()
    val activeCallType by GlobalCallState.activeCallType.collectAsState()
    val participantId by GlobalCallState.activeParticipantId.collectAsState()
    val sessionId by GlobalCallState.activeSessionId.collectAsState()
    val isConnectionEstablished by GlobalCallState.isConnectionEstablished.collectAsState()
    val isRinging by GlobalCallState.isRinging.collectAsState()
    val durationSeconds by GlobalCallState.callDurationSeconds.collectAsState()

    AnimatedVisibility(
        visible = isCallActive,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val participantName = remember(participantId) {
            participantId?.let { 
                GlobalCallState.getContactName(it) ?: GlobalCallState.getContactPhone(it) ?: it
            } ?: ""
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            if (activeCallType == CallType.VIDEO) {
                VideoCallContent(participantName, isConnectionEstablished, isRinging, durationSeconds, callManager)
            } else {
                VoiceCallContent(participantName, isConnectionEstablished, isRinging, durationSeconds)
            }

            // Banner Ad (only before connection is established)
            if (!isConnectionEstablished) {
                com.example.firstapp.common.ui.components.BannerAdView(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                )
            }

            // Call Controls
            CallControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                isMuted = false, // TODO: track state
                isSpeakerOn = activeCallType == CallType.VIDEO,
                isVideo = activeCallType == CallType.VIDEO,
                onMuteToggle = { callManager.setMicEnabled(it) },
                onSpeakerToggle = { callManager.setSpeakerEnabled(it) },
                onEndCall = {
                    sessionId?.let { sid ->
                        participantId?.let { pid ->
                            viewModel.callWebSocketClient.end(pid, sid, activeCallType ?: CallType.VOICE, 0)
                        }
                        GlobalCallState.onCallEnded(sid)
                    }
                    callManager.end()
                    tonePlayer.stop()
                },
                onSwitchCamera = { callManager.switchCamera() }
            )
        }
    }
}

@Composable
private fun VoiceCallContent(name: String, isConnected: Boolean, isRinging: Boolean, durationSeconds: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color.DarkGray
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                isConnected -> formatDuration(durationSeconds)
                isRinging -> "Ringing..."
                else -> "Calling..."
            },
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun VideoCallContent(name: String, isConnected: Boolean, isRinging: Boolean, durationSeconds: Int, callManager: WebRtcCallManager) {
    var isLocalFullScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isConnected) {
            // Main View (Remote by default)
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        try {
                            init(callManager.eglBaseContext, null)
                        } catch (_: Exception) {}
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                    }
                },
                update = { view ->
                    if (isLocalFullScreen) {
                        callManager.attachLocalRenderer(view)
                    } else {
                        callManager.attachRemoteRenderer(view)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = {
                    callManager.detachLocalRenderer()
                    callManager.detachRemoteRenderer()
                }
            )

            // PiP View (Local Video Box)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 120.dp, height = 180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clickable { isLocalFullScreen = !isLocalFullScreen }
            ) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            setZOrderOnTop(true)
                            setZOrderMediaOverlay(true)
                            try {
                                init(callManager.eglBaseContext, null)
                            } catch (_: Exception) {}
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                        }
                    },
                    update = { view ->
                        if (isLocalFullScreen) {
                            callManager.attachRemoteRenderer(view)
                        } else {
                            callManager.attachLocalRenderer(view)
                        }
                        view.post {
                            view.invalidate()
                            view.requestLayout()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Duration Overlay for Video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = formatDuration(durationSeconds),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Local Video (Full Screen while waiting/ringing)
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        try {
                            init(callManager.eglBaseContext, null)
                        } catch (_: Exception) {}
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                    }
                },
                update = { view ->
                    callManager.attachLocalRenderer(view)
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { callManager.detachLocalRenderer() }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRinging) "Ringing $name..." else "Calling $name...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Video will start when answered", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CallControls(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideo: Boolean,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onEndCall: () -> Unit,
    onSwitchCamera: () -> Unit = {}
) {
    // Sync with hardware state if provided
    var muted by remember(isMuted) { mutableStateOf(isMuted) }
    var speaker by remember(isSpeakerOn) { mutableStateOf(isSpeakerOn) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speaker
        IconButton(
            onClick = { 
                speaker = !speaker
                onSpeakerToggle(speaker)
            },
            modifier = Modifier
                .size(56.dp)
                .background(if (speaker) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                if (speaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Speaker",
                tint = if (speaker) Color.Black else Color.White
            )
        }

        if (isVideo) {
            // Switch Camera
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
            }
        }

        IconButton(
            onClick = { 
                muted = !muted
                onMuteToggle(!muted) // If muted is true (UI shows mic-off), call setMicEnabled(false)
            },
            modifier = Modifier
                .size(56.dp)
                .background(if (muted) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute",
                tint = if (muted) Color.Black else Color.White
            )
        }

        // End Call
        FloatingActionButton(
            onClick = onEndCall,
            containerColor = Color.Red,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(32.dp))
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(java.util.Locale.US, mins, secs)
}

