import Foundation
import AVFoundation
import WebRTC

/// Real WebRTC peer connection. Signaling matches Android: sdp_offer, sdp_answer, ice_candidate.
public final class WebRtcCallManager: NSObject, RTCPeerConnectionDelegate, @unchecked Sendable {
    // MARK: - Properties & Callbacks

    public var onSignal: ((_ signalType: String, _ payload: [String: Any]) -> Void)?
    public var onConnected: (() -> Void)?
    public var onDisconnected: (() -> Void)?

    private var callType: CallType = .voice
    private var isCapturing = false
    private var isMicMuted = false
    private var isSpeakerActive = false
    private var isFrontCamera = true

    private var iceServers: [RTCIceServer] = []
    private var factory: RTCPeerConnectionFactory?
    private var peerConnection: RTCPeerConnection?
    private var localVideoSource: RTCVideoSource?
    private var videoCapturer: RTCCameraVideoCapturer?
    private var pendingCandidates: [RTCIceCandidate] = []
    private var isRemoteDescriptionSet = false

    public static let shared = WebRtcCallManager()

    public override init() {
        super.init()
    }

    deinit {
        release()
    }

    // MARK: - Configuration

    public func setIceServers(_ servers: [[String: Any]]) {
        let parsed = servers.compactMap { row -> RTCIceServer? in
            var urls: [String] = []
            if let one = row["urls"] as? String, !one.isEmpty { urls = [one] }
            if let many = row["urls"] as? [String] { urls = many.filter { !$0.isEmpty } }
            if let many = row["urls"] as? [Any] { urls = many.compactMap { $0 as? String }.filter { !$0.isEmpty } }
            guard !urls.isEmpty else { return nil }
            return RTCIceServer(urlStrings: urls, username: row["username"] as? String, credential: row["credential"] as? String)
        }
        iceServers = parsed.isEmpty
            ? [RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"])]
            : parsed
    }

    // MARK: - Camera & Preview Methods

    public func startLocalPreview() {
        callType = .video
        ensurePeer()
        startCamera()
        isCapturing = true
    }

    public func stopLocalPreview() {
        videoCapturer?.stopCapture()
        isCapturing = false
    }

    public func switchCamera() {
        isFrontCamera.toggle()
        if isCapturing { startCamera() }
    }

    // MARK: - Call Lifecycle

    public func start(callType: CallType) {
        self.callType = callType
        configureAudioSession(for: callType)
        ensurePeer()
        if callType == .video {
            startLocalPreview()
            setSpeakerEnabled(true)
        } else {
            setSpeakerEnabled(false)
        }
        setMicEnabled(true)
    }

    public func createOffer() {
        ensurePeer()
        peerConnection?.offer(for: mediaConstraints()) { [weak self] sdp, error in
            guard let self, let sdp, error == nil else { return }
            self.peerConnection?.setLocalDescription(sdp) { _ in
                self.emit("sdp_offer", ["type": "offer", "sdp": sdp.sdp])
            }
        }
    }

    public func createAnswer() {
        ensurePeer()
        peerConnection?.answer(for: mediaConstraints()) { [weak self] sdp, error in
            guard let self, let sdp, error == nil else { return }
            self.peerConnection?.setLocalDescription(sdp) { _ in
                self.emit("sdp_answer", ["type": "answer", "sdp": sdp.sdp])
            }
        }
    }

    public func handleSignal(signalType: String?, payload: [String: Any]?) {
        guard let signalType, let payload else { return }
        switch signalType {
        case "sdp_offer", "sdp_answer":
            let sdp = payload["sdp"] as? String ?? ""
            guard !sdp.isEmpty else { return }
            ensurePeer()
            let type: RTCSdpType = signalType == "sdp_offer" ? .offer : .answer
            peerConnection?.setRemoteDescription(RTCSessionDescription(type: type, sdp: sdp)) { [weak self] error in
                guard let self, error == nil else { return }
                self.isRemoteDescriptionSet = true
                self.pendingCandidates.forEach { self.peerConnection?.add($0) }
                self.pendingCandidates.removeAll()
                if type == .offer { self.createAnswer() }
                DispatchQueue.main.async { self.onConnected?() }
            }
        case "ice_candidate":
            let candidate = payload["candidate"] as? String ?? ""
            guard !candidate.isEmpty else { return }
            let index: Int32
            if let number = payload["sdpMLineIndex"] as? NSNumber {
                index = number.int32Value
            } else {
                index = Int32(payload["sdpMLineIndex"] as? String ?? "0") ?? 0
            }
            let ice = RTCIceCandidate(sdp: candidate, sdpMLineIndex: index, sdpMid: payload["sdpMid"] as? String)
            if isRemoteDescriptionSet {
                peerConnection?.add(ice)
            } else {
                pendingCandidates.append(ice)
            }
        default:
            break
        }
    }

    public func restartIce() {
        createOffer()
    }

    // MARK: - Media Control

    public func setMicEnabled(_ enabled: Bool) {
        isMicMuted = !enabled
        peerConnection?.senders.forEach { sender in
            if sender.track?.kind == "audio" {
                sender.track?.isEnabled = enabled
            }
        }
        let session = AVAudioSession.sharedInstance()
        do {
            if let builtInMic = session.availableInputs?.first(where: { $0.portType == .builtInMic }) {
                try session.setPreferredInput(builtInMic)
            }
        } catch {
            print("[WebRtcCallManager] Mic toggle error: \(error.localizedDescription)")
        }
    }

    public func setSpeakerEnabled(_ enabled: Bool) {
        isSpeakerActive = enabled
        let session = AVAudioSession.sharedInstance()
        do {
            try session.overrideOutputAudioPort(enabled ? .speaker : .none)
        } catch {
            print("[WebRtcCallManager] Speaker toggle error: \(error.localizedDescription)")
        }
    }

    public func setCameraEnabled(_ enabled: Bool) {
        if enabled {
            startLocalPreview()
        } else {
            stopLocalPreview()
        }
    }

    // MARK: - Audio Session Config

    private func configureAudioSession(for callType: CallType) {
        let session = AVAudioSession.sharedInstance()
        do {
            let categoryOptions: AVAudioSession.CategoryOptions = [.allowBluetooth, .defaultToSpeaker]
            try session.setCategory(.playAndRecord, mode: callType == .video ? .videoChat : .voiceChat, options: categoryOptions)
            try session.setActive(true)
        } catch {
            print("[WebRtcCallManager] Audio session setup error: \(error.localizedDescription)")
        }
    }

    // MARK: - Teardown

    public func end() {
        stopLocalPreview()
        peerConnection?.close()
        peerConnection = nil
        isRemoteDescriptionSet = false
        pendingCandidates.removeAll()

        let session = AVAudioSession.sharedInstance()
        do {
            try session.setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("[WebRtcCallManager] Audio session teardown error: \(error.localizedDescription)")
        }

        onDisconnected?()
    }

    public func release() {
        end()
        onSignal = nil
        onConnected = nil
        onDisconnected = nil
    }

    public func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        stream.audioTracks.forEach { $0.isEnabled = true }
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
    public func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        if newState == .connected || newState == .completed {
            DispatchQueue.main.async { self.onConnected?() }
        }
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        emit("ice_candidate", [
            "sdpMid": candidate.sdpMid ?? "",
            "sdpMLineIndex": Int(candidate.sdpMLineIndex),
            "candidate": candidate.sdp
        ])
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {}

    private func ensurePeer() {
        if factory == nil {
            RTCInitializeSSL()
            factory = RTCPeerConnectionFactory()
        }
        guard peerConnection == nil, let factory else { return }
        if iceServers.isEmpty { setIceServers([]) }
        let config = RTCConfiguration()
        config.iceServers = iceServers
        config.sdpSemantics = .unifiedPlan
        let constraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: ["DtlsSrtpKeyAgreement": "true"])
        let connection = factory.peerConnection(with: config, constraints: constraints, delegate: self)
        let audioSource = factory.audioSource(with: RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: nil))
        connection?.add(factory.audioTrack(with: audioSource, trackId: "audio0"), streamIds: ["stream0"])
        if callType == .video {
            let source = factory.videoSource()
            localVideoSource = source
            connection?.add(factory.videoTrack(with: source, trackId: "video0"), streamIds: ["stream0"])
        }
        peerConnection = connection
    }

    private func startCamera() {
        guard let factory else { return }
        if localVideoSource == nil { localVideoSource = factory.videoSource() }
        guard let source = localVideoSource else { return }
        let capturer = videoCapturer ?? RTCCameraVideoCapturer(delegate: source)
        videoCapturer = capturer
        let position: AVCaptureDevice.Position = isFrontCamera ? .front : .back
        guard let device = RTCCameraVideoCapturer.captureDevices().first(where: { $0.position == position })
                ?? RTCCameraVideoCapturer.captureDevices().first,
              let format = RTCCameraVideoCapturer.supportedFormats(for: device).last else { return }
        capturer.startCapture(with: device, format: format, fps: 30)
    }

    private func mediaConstraints() -> RTCMediaConstraints {
        RTCMediaConstraints(
            mandatoryConstraints: [
                "OfferToReceiveAudio": "true",
                "OfferToReceiveVideo": callType == .video ? "true" : "false"
            ],
            optionalConstraints: nil
        )
    }

    private func emit(_ type: String, _ payload: [String: Any]) {
        DispatchQueue.main.async { self.onSignal?(type, payload) }
    }
}

public final class CallSocketBridge: NSObject, CallSocketListener {
    public static let shared = CallSocketBridge()
    private var receiverId = ""
    private var sessionId = ""
    private var callType: CallType = .voice

    public func bind(receiverId: String, sessionId: String, callType: CallType) {
        self.receiverId = receiverId
        if !sessionId.isEmpty { self.sessionId = sessionId }
        self.callType = callType
        WebRtcCallManager.shared.onSignal = { [weak self] type, payload in
            guard let self, !self.receiverId.isEmpty, !self.sessionId.isEmpty else { return }
            CallWebSocketClient.shared.signalObject(
                receiverId: self.receiverId,
                sessionId: self.sessionId,
                callType: self.callType,
                signalType: type,
                signalPayload: payload
            )
        }
        WebRtcCallManager.shared.onConnected = {
            GlobalCallState.shared.isConnectionEstablished = true
        }
    }

    public func acceptIncoming() {
        guard let info = GlobalCallState.shared.incomingCall else { return }
        GlobalCallState.shared.incomingCall = nil
        GlobalCallState.shared.setCallActive(true, sessionId: info.id, participantId: info.senderId, callType: info.callType)
        GlobalCallState.shared.isAnswered = true
        bind(receiverId: info.senderId, sessionId: info.id, callType: info.callType)
        WebRtcCallManager.shared.start(callType: info.callType)
        CallWebSocketClient.shared.accept(receiverId: info.senderId, sessionId: info.id, callType: info.callType)
    }

    public func declineIncoming() {
        guard let info = GlobalCallState.shared.incomingCall else { return }
        GlobalCallState.shared.incomingCall = nil
        CallWebSocketClient.shared.reject(receiverId: info.senderId, sessionId: info.id, callType: info.callType, reason: "rejected")
    }

    public func endActive(reason: String) {
        let state = GlobalCallState.shared
        if let info = state.incomingCall {
            CallWebSocketClient.shared.reject(receiverId: info.senderId, sessionId: info.id, callType: info.callType, reason: reason)
            state.incomingCall = nil
        }
        if let sessionId = state.activeSessionId, let participantId = state.activeParticipantId, let callType = state.activeCallType {
            CallWebSocketClient.shared.end(
                receiverId: participantId,
                sessionId: sessionId,
                callType: callType,
                durationSeconds: state.callDurationSeconds,
                reason: reason
            )
        }
        WebRtcCallManager.shared.end()
        state.onCallEnded(sessionId: state.activeSessionId ?? "force_all")
    }

    public func onOpen() {}
    public func onError(_ error: String) {}

    public func onEvent(_ event: CallEvent) {
        DispatchQueue.main.async {
            let mine = SessionStore.shared.userId ?? ""
            if !mine.isEmpty && event.senderId == mine { return }
            let state = GlobalCallState.shared
            switch event.type ?? "" {
            case "call_invite":
                guard event.receiverId == mine else { return }
                if state.isCallActive {
                    CallWebSocketClient.shared.reject(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType, reason: "busy")
                    return
                }
                self.bind(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType)
                CallWebSocketClient.shared.ringing(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType)
                self.presentIncoming(event)
            case "call_accept":
                guard state.activeSessionId == event.sessionId else { return }
                state.isAnswered = true
                state.isRinging = false
                WebRtcCallManager.shared.createOffer()
            case "call_ringing":
                if state.activeSessionId == event.sessionId { state.isRinging = true }
            case "call_end", "call_reject":
                WebRtcCallManager.shared.end()
                state.onCallEnded(sessionId: event.sessionId)
            case "call_signal":
                guard state.activeSessionId == event.sessionId else { return }
                var payload: [String: Any] = [:]
                event.signalPayload?.forEach { payload[$0.key] = $0.value }
                WebRtcCallManager.shared.handleSignal(signalType: event.signalType, payload: payload)
            default:
                break
            }
        }
    }

    private func presentIncoming(_ event: CallEvent) {
        let cached = GlobalCallState.shared.contactNames[event.senderId] ?? ""
        let known = realPersonLabel(names: [cached, event.senderName], phone: GlobalCallState.shared.contactPhones[event.senderId], ids: [event.senderId])
        GlobalCallState.shared.incomingCall = IncomingCallInfo(id: event.sessionId, senderId: event.senderId, callerName: known, callType: event.callType)
        guard known.isEmpty else { return }
        Task {
            guard let profile = await LiveJSON.userProfile(event.senderId) else { return }
            let local = profile.phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(profile.phone)
            let name = realPersonLabel(local: local, names: [profile.displayName, profile.username], phone: profile.phone, ids: [event.senderId])
            await MainActor.run {
                GlobalCallState.shared.updateContactInfo(id: event.senderId, name: name, phone: profile.phone)
                if GlobalCallState.shared.incomingCall?.id == event.sessionId {
                    GlobalCallState.shared.incomingCall = IncomingCallInfo(id: event.sessionId, senderId: event.senderId, callerName: name, callType: event.callType)
                }
            }
        }
    }
}


