package com.tpsk.tj.chat.ui.components

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tpsk.tj.chat.data.*

import com.tpsk.tj.chat.ui.viewmodel.CallViewModel

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
                com.tpsk.tj.common.ui.components.BannerAdView(
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
            CallVideoView(
                modifier = Modifier.fillMaxSize(),
                callManager = callManager,
                onView = { view ->
                    if (isLocalFullScreen) callManager.attachLocalRenderer(view)
                    else callManager.attachRemoteRenderer(view)
                }
            )

            DraggableCallPreview(
                callManager = callManager,
                onSwap = { isLocalFullScreen = !isLocalFullScreen },
                onView = { view ->
                    if (isLocalFullScreen) callManager.attachRemoteRenderer(view)
                    else callManager.attachLocalRenderer(view)
                }
            )
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
            CallVideoView(
                modifier = Modifier.fillMaxSize(),
                callManager = callManager,
                onView = { view -> callManager.attachLocalRenderer(view) }
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
                    Text(
                        text = if (isRinging) "Camera is on. Waiting for them to answer" else "Connecting video...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableCallPreview(
    callManager: WebRtcCallManager,
    onSwap: () -> Unit,
    onView: (CallVideoRenderer) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        val density = LocalDensity.current
        val pipWidth = with(density) { 120.dp.toPx() }
        val pipHeight = with(density) { 180.dp.toPx() }
        val margin = with(density) { 12.dp.toPx() }
        val parentWidth = constraints.maxWidth.toFloat()
        val parentHeight = constraints.maxHeight.toFloat()
        var offset by remember(parentWidth, parentHeight) {
            mutableStateOf(
                Offset(
                    (parentWidth - pipWidth - margin).coerceAtLeast(margin),
                    margin
                )
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(width = 120.dp, height = 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .background(Color.Black)
                .pointerInput(parentWidth, parentHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            if (!moved && delta.getDistance() > 8f) moved = true
                            if (moved) {
                                change.consume()
                                val maxX = (parentWidth - pipWidth - margin).coerceAtLeast(margin)
                                val maxY = (parentHeight - pipHeight - margin).coerceAtLeast(margin)
                                offset = Offset(
                                    (offset.x + delta.x).coerceIn(margin, maxX),
                                    (offset.y + delta.y).coerceIn(margin, maxY)
                                )
                            }
                        }
                        if (!moved) onSwap()
                    }
                }
        ) {
            CallVideoView(
                modifier = Modifier.fillMaxSize(),
                callManager = callManager,
                onView = onView
            )
        }
    }
}

@Composable
private fun CallVideoView(
    modifier: Modifier,
    callManager: WebRtcCallManager,
    onView: (CallVideoRenderer) -> Unit
) {
    val viewRef = remember { mutableStateOf<CallVideoRenderer?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CallVideoRenderer(context).also {
                it.isClickable = false
                viewRef.value = it
            }
        },
        update = { view -> onView(view) },
        onRelease = {
            viewRef.value?.let {
                callManager.releaseRenderer(it)
                it.releaseRenderer()
            }
            viewRef.value = null
        }
    )
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
    var muted by remember(isMuted) { mutableStateOf(isMuted) }
    var speaker by remember(isSpeakerOn) { mutableStateOf(isSpeakerOn) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

