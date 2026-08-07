@file:Suppress("DEPRECATION")

package com.example.firstapp.chat.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WebRtcCallManager(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private val eglBase = EglBase.create()

    private val observationScope = CoroutineScope(Dispatchers.Main)

    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var iceServers: List<PeerConnection.IceServer>? = null
    private var audioDeviceModule: org.webrtc.audio.JavaAudioDeviceModule? = null

    private var callType: CallType = CallType.VOICE
    private val pendingIceCandidates = CopyOnWriteArrayList<IceCandidate>()
    private var remoteDescriptionSet = false
    private var isCapturing = false

    var onSignal: ((signalType: String, payload: JsonObject) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun setIceServers(servers: List<PeerConnection.IceServer>) {
        this.iceServers = servers
    }

    companion object {
        @Volatile
        private var isInitialized = false
        private val initLock = Any()

        private fun ensureFactoryInitialized(context: Context) {
            if (isInitialized) return
            synchronized(initLock) {
                if (isInitialized) return
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                isInitialized = true
            }
        }
    }

    private val factory: PeerConnectionFactory by lazy {
        ensureFactoryInitialized(appContext)

        val adm = org.webrtc.audio.JavaAudioDeviceModule.builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .createAudioDeviceModule()
        
        this.audioDeviceModule = adm

        PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    @Synchronized
    fun start(callType: CallType) {
        this.callType = callType
        releasePeerConnectionOnly()

        requestAudioFocus()
        if (callType == CallType.VOICE) {
            enableProximitySensor()
        }
        
        val defaultIceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers ?: defaultIceServers).apply {
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 2 
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        // Re-assert the audio route now that media is actually flowing. Some
                        // OEM firmware (observed on Samsung) resets/ignores the communication
                        // device chosen at call setup time, leaving audio routed to the quiet
                        // earpiece even during a video call.
                        setSpeakerEnabled(callType == CallType.VIDEO)
                        onConnected?.invoke()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        // Network might have switched, try ICE restart if not recovered soon
                        observationScope.launch {
                            delay(3000)
                            if (peerConnection?.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) {
                                restartIce()
                            }
                        }
                    }
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> onDisconnected?.invoke()
                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) {
                val payload = JsonObject().apply {
                    addProperty("sdpMid", candidate.sdpMid)
                    addProperty("sdpMLineIndex", candidate.sdpMLineIndex)
                    addProperty("candidate", candidate.sdp)
                }
                onSignal?.invoke("ice_candidate", payload)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) {
                val track = stream.videoTracks.firstOrNull() ?: return
                if (remoteVideoTrack == track) return
                remoteVideoTrack?.setEnabled(false)
                remoteVideoTrack = track
                track.setEnabled(true)
                remoteRenderer?.let { renderer ->
                    renderer.setMirror(false)
                    track.addSink(renderer)
                }
            }
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) = Unit

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    if (remoteVideoTrack == track) return
                    remoteVideoTrack?.setEnabled(false)
                    remoteVideoTrack = track
                    track.setEnabled(true)
                    remoteRenderer?.let { renderer ->
                        renderer.setMirror(false)
                        track.addSink(renderer)
                    }
                } else if (track is AudioTrack) {
                    if (remoteAudioTrack == track) return
                    remoteAudioTrack?.setEnabled(false)
                    remoteAudioTrack = track
                    track.setEnabled(true)
                }
            }
        })

        val streamId = "stream-${System.currentTimeMillis()}"
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("audio0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf(streamId))

        if (callType == CallType.VIDEO) {
            videoCapturer = createFrontCameraCapturer()
            if (videoCapturer != null) {
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                videoSource = factory.createVideoSource(videoCapturer?.isScreencast == true)
                videoCapturer?.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)
                try {
                    videoCapturer?.startCapture(640, 480, 24)
                    isCapturing = true
                } catch (_: Exception) {
                    isCapturing = false
                }
                localVideoTrack = factory.createVideoTrack("video0", videoSource)
                peerConnection?.addTrack(localVideoTrack, listOf(streamId))
                localRenderer?.let { renderer ->
                    renderer.setMirror(true)
                    localVideoTrack?.addSink(renderer)
                }
            }
        }

        setSpeakerEnabled(callType == CallType.VIDEO)
        setMicEnabled(true)
    }

    fun createOffer() {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", (callType == CallType.VIDEO).toString()))
        }
        pc.createOffer(object : BaseSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(BaseSdpObserver(), desc)
                val payload = JsonObject().apply {
                    addProperty("type", desc.type.canonicalForm())
                    addProperty("sdp", desc.description)
                }
                onSignal?.invoke("sdp_offer", payload)
            }
        }, constraints)
    }

    fun handleSignal(signalType: String?, payload: JsonElement?) {
        if (signalType.isNullOrBlank() || payload == null || !payload.isJsonObject) {
            return
        }

        val obj = payload.asJsonObject
        when (signalType) {
            "sdp_offer" -> {
                val sdp = obj.get("sdp")?.asString ?: return
                setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp)) {
                    createAnswer()
                }
            }
            "sdp_answer" -> {
                val sdp = obj.get("sdp")?.asString ?: return
                setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp), null)
            }
            "ice_candidate" -> {
                val sdpMid = obj.get("sdpMid")?.asString
                val sdpMLineIndex = obj.get("sdpMLineIndex")?.asInt ?: return
                val candidate = obj.get("candidate")?.asString ?: return
                addRemoteIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
            }
        }
    }

    fun restartIce() {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", (callType == CallType.VIDEO).toString()))
        }
        pc.createOffer(object : BaseSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(BaseSdpObserver(), desc)
                val payload = JsonObject().apply {
                    addProperty("type", desc.type.canonicalForm())
                    addProperty("sdp", desc.description)
                }
                onSignal?.invoke("sdp_offer", payload)
            }
        }, constraints)
    }

    private fun createAnswer() {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", (callType == CallType.VIDEO).toString()))
        }
        pc.createAnswer(object : BaseSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(BaseSdpObserver(), desc)
                val payload = JsonObject().apply {
                    addProperty("type", desc.type.canonicalForm())
                    addProperty("sdp", desc.description)
                }
                onSignal?.invoke("sdp_answer", payload)
            }
        }, constraints)
    }

    private fun setRemoteDescription(desc: SessionDescription, onDone: (() -> Unit)?) {
        val pc = peerConnection ?: return
        pc.setRemoteDescription(object : BaseSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                flushPendingIceCandidates()
                onDone?.invoke()
            }
        }, desc)
    }

    private fun addRemoteIceCandidate(candidate: IceCandidate) {
        if (!remoteDescriptionSet) {
            pendingIceCandidates.add(candidate)
            return
        }
        peerConnection?.addIceCandidate(candidate)
    }

    private fun flushPendingIceCandidates() {
        if (!remoteDescriptionSet) return
        pendingIceCandidates.forEach { candidate ->
            peerConnection?.addIceCandidate(candidate)
        }
        pendingIceCandidates.clear()
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        localAudioTrack?.setVolume(if (enabled) 1.0 else 0.0)
        // Set hardware mute on the Audio Device Module (Robust for Huawei/Samsung)
        audioDeviceModule?.setMicrophoneMute(!enabled)
        // Fallback system mute
        audioManager.isMicrophoneMute = !enabled
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        var routedViaCommunicationDevice = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            val targetType = if (enabled) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            val device = devices.firstOrNull { it.type == targetType }
            if (device != null) {
                routedViaCommunicationDevice = audioManager.setCommunicationDevice(device)
            }
        }
        // Always also set the legacy speakerphone flag as a fallback: some OEM builds
        // (observed on Samsung devices) silently fail setCommunicationDevice() or reset
        // the route shortly after, leaving the call routed to the earpiece (very quiet)
        // even during a video call. Setting this too costs nothing and guarantees the
        // expected route is applied.
        if (!routedViaCommunicationDevice || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = enabled
        }
        ensureAudibleCallVolume()
    }

    /**
     * Boosts the in-call (STREAM_VOICE_CALL) volume to an audible level when a call starts.
     * Without this, calls can sound almost silent if the stream's volume index was left low
     * from a previous call/media session, even though the app itself plays audio at full gain.
     */
    private fun ensureAudibleCallVolume() {
        try {
            val stream = AudioManager.STREAM_VOICE_CALL
            val max = audioManager.getStreamMaxVolume(stream)
            val current = audioManager.getStreamVolume(stream)
            val minAudible = (max * 0.75f).toInt().coerceAtLeast(1)
            if (current < minAudible) {
                audioManager.setStreamVolume(stream, minAudible, 0)
            }
        } catch (_: Exception) {
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        if (!enabled) {
            if (isCapturing) {
                videoCapturer?.stopCaptureSafely()
                isCapturing = false
            }
        } else if (callType == CallType.VIDEO && !isCapturing) {
            try {
                videoCapturer?.startCapture(640, 480, 24)
                isCapturing = true
            } catch (_: Exception) {
                isCapturing = false
            }
        }
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer?.let { localVideoTrack?.removeSink(it) }
        localRenderer = renderer
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(true)
        } catch (_: Exception) {}
        localVideoTrack?.addSink(renderer)
    }

    fun detachLocalRenderer() {
        localRenderer?.let { localVideoTrack?.removeSink(it) }
        localRenderer = null
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
        remoteRenderer = renderer
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(false)
        } catch (_: Exception) {}
        remoteVideoTrack?.addSink(renderer)
    }

    fun detachRemoteRenderer() {
        remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
        remoteRenderer = null
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun enableProximitySensor() {
        if (proximityWakeLock == null) {
            try {
                proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "TJ:ProximityLock")
            } catch (_: Exception) {}
        }
        proximityWakeLock?.let {
            if (!it.isHeld) it.acquire(10 * 60 * 1000L) // 10 min fallback
        }
    }

    private fun disableProximitySensor() {
        proximityWakeLock?.let {
            if (it.isHeld) it.release()
        }
        proximityWakeLock = null
    }

    @Synchronized
    fun end() {
        releasePeerConnectionOnly()
        abandonAudioFocus()
        disableProximitySensor()
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
        audioManager.isMicrophoneMute = false
    }

    fun release() {
        end()
        localRenderer = null
        remoteRenderer = null
        eglBase.release()
    }

    @Synchronized
    private fun releasePeerConnectionOnly() {
        remoteDescriptionSet = false
        pendingIceCandidates.clear()
        remoteVideoTrack?.let { track ->
            remoteRenderer?.let { renderer -> track.removeSink(renderer) }
            track.setEnabled(false)
        }
        remoteVideoTrack = null

        localVideoTrack?.let { track ->
            localRenderer?.let { renderer -> track.removeSink(renderer) }
            track.setEnabled(false)
        }
        localVideoTrack = null

        localAudioTrack?.setEnabled(false)
        localAudioTrack = null
        remoteAudioTrack?.let { track ->
            track.setEnabled(false)
        }
        remoteAudioTrack = null


        if (isCapturing) {
            videoCapturer?.stopCaptureSafely()
            isCapturing = false
        }
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoSource?.dispose()
        videoSource = null

        audioSource?.dispose()
        audioSource = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    private fun createFrontCameraCapturer(): CameraVideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(appContext)) {
            Camera2Enumerator(appContext)
        } else {
            Camera1Enumerator(false)
        }
        val frontName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val backName = enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        val selected = frontName ?: backName ?: return null
        return enumerator.createCapturer(selected, null)
    }

    private open class BaseSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String) = Unit
        override fun onSetFailure(error: String) = Unit
    }
}
private fun CameraVideoCapturer.stopCaptureSafely() {
    try {
        stopCapture()
    } catch (_: Exception) {
        // No-op: camera may already be stopped.
    }
}
