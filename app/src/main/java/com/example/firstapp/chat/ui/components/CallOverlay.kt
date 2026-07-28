package com.example.firstapp.chat.ui.components

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
                VideoCallContent(participantName, isConnectionEstablished, isRinging, callManager)
            } else {
                VoiceCallContent(participantName, isConnectionEstablished, isRinging)
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
private fun VoiceCallContent(name: String, isConnected: Boolean, isRinging: Boolean) {
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
                isConnected -> "Voice Call"
                isRinging -> "Ringing..."
                else -> "Calling..."
            },
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun VideoCallContent(name: String, isConnected: Boolean, isRinging: Boolean, callManager: WebRtcCallManager) {
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote Video (Full Screen)
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    callManager.attachRemoteRenderer(this)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { callManager.detachRemoteRenderer() }
        )

        // Local Video (Small Overlay)
        Box(
            modifier = Modifier
                .size(120.dp, 160.dp)
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        callManager.attachLocalRenderer(this)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { callManager.detachLocalRenderer() }
            )
        }

        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRinging) "Ringing $name..." else "Calling $name...",
                    color = Color.White,
                    fontSize = 18.sp
                )
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
    var muted by remember { mutableStateOf(isMuted) }
    var speaker by remember { mutableStateOf(isSpeakerOn) }

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
                if (speaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
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
                onMuteToggle(muted)
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
