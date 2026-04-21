@file:Suppress("DEPRECATION")

package com.example.firstapp.data.realtime

import android.content.Context
import android.media.AudioManager
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

class WebRtcCallManager(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val eglBase = EglBase.create()

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

    private var callType: CallType = CallType.VOICE
    private val pendingIceCandidates = CopyOnWriteArrayList<IceCandidate>()
    private var remoteDescriptionSet = false
    private var isCapturing = false

    var onSignal: ((signalType: String, payload: JsonObject) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

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
        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    @Synchronized
    fun start(callType: CallType) {
        this.callType = callType
        releasePeerConnectionOnly()

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        )
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> onConnected?.invoke()
                    PeerConnection.IceConnectionState.DISCONNECTED,
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
                remoteVideoTrack = track
                remoteRenderer?.let { renderer ->
                    renderer.setMirror(false)
                    remoteVideoTrack?.addSink(renderer)
                }
            }
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) = Unit

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    remoteVideoTrack?.setEnabled(false)
                    remoteVideoTrack = track
                    remoteRenderer?.let { renderer ->
                        renderer.setMirror(false)
                        remoteVideoTrack?.addSink(renderer)
                    }
                } else if (track is AudioTrack) {
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
        audioManager.isMicrophoneMute = !enabled
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
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

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(true)
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(false)
        remoteVideoTrack?.addSink(renderer)
    }

    @Synchronized
    fun end() {
        releasePeerConnectionOnly()
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
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
