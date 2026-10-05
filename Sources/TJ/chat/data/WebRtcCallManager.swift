import Foundation
import AVFoundation
import AudioToolbox
import UIKit
import UserNotifications
import WebRTC

/// Real WebRTC peer connection. Signaling matches Android: sdp_offer, sdp_answer, ice_candidate.
final class CallMediaState: ObservableObject {
    static let shared = CallMediaState()
    @Published var remoteVideo: RTCVideoTrack?
    @Published var localVideo: RTCVideoTrack?
}

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
    private var localAudioTrack: RTCAudioTrack?
    private var localVideoTrack: RTCVideoTrack?
    private var videoCapturer: RTCCameraVideoCapturer?
    private var pendingCandidates: [RTCIceCandidate] = []
    private var isRemoteDescriptionSet = false
    private var recoveryTask: Task<Void, Never>?
    public private(set) var speakerIsOn = false

    public static let shared = WebRtcCallManager()
    private static let fallbackIce = [
        RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302", "stun:137.66.50.206:3478"]),
        RTCIceServer(urlStrings: ["turn:137.66.50.206:3478", "turn:137.66.50.206:3478?transport=tcp"], username: "turnuser", credential: "turnpassword123")
    ]

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
            if urls.isEmpty, let one = row["url"] as? String, !one.isEmpty { urls = [one] }
            guard !urls.isEmpty else { return nil }
            let username = (row["username"] as? String)?.nilIfEmpty
            let credential = (row["credential"] as? String)?.nilIfEmpty
            return RTCIceServer(urlStrings: urls, username: username, credential: credential)
        }
        iceServers = parsed.isEmpty ? Self.fallbackIce : parsed
    }

    public func prepareIce() async {
        var rows: [[String: Any]] = []
        if let data = try? await API.data("calls/ice_servers") {
            rows = LiveJSON.list(data, keys: ["ice_servers"])
        }
        setIceServers(rows)
    }

    // MARK: - Camera & Preview Methods

    public func startLocalPreview() {
        callType = .video
        configureAudioSession(for: .video)
        ensureFactory()
        startCamera()
        isCapturing = localVideoTrack != nil
        let track = localVideoTrack
        DispatchQueue.main.async { CallMediaState.shared.localVideo = track }
    }

    public func stopLocalPreview() {
        guard peerConnection == nil else { return }
        releaseLocalVideo()
    }

    public func switchCamera() {
        isFrontCamera.toggle()
        videoCapturer?.stopCapture()
        isCapturing = false
        startCamera()
    }

    // MARK: - Call Lifecycle

    public func start(callType: CallType) {
        let reusePreview = callType == .video && isCapturing && localVideoTrack != nil
        self.callType = callType
        if !reusePreview && peerConnection != nil {
            releasePeerOnly(keepLocalVideo: false)
        }
        configureAudioSession(for: callType)
        ensurePeer()
        if callType == .video {
            startCamera()
            setSpeakerEnabled(true)
        } else {
            setSpeakerEnabled(false)
        }
        setMicEnabled(true)
    }

    public func createOffer() {
        ensurePeer()
        ensureSendRecv()
        peerConnection?.offer(for: mediaConstraints(iceRestart: false)) { [weak self] sdp, error in
            guard let self, let sdp, error == nil else { return }
            self.peerConnection?.setLocalDescription(sdp) { _ in
                self.emit("sdp_offer", ["type": "offer", "sdp": sdp.sdp])
            }
        }
    }

    public func createAnswer() {
        ensurePeer()
        ensureSendRecv()
        peerConnection?.answer(for: mediaConstraints(iceRestart: false)) { [weak self] sdp, error in
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
            let sdp = Self.sdpText(payload)
            guard !sdp.isEmpty else { return }
            ensurePeer()
            let type: RTCSdpType = signalType == "sdp_offer" ? .offer : .answer
            peerConnection?.setRemoteDescription(RTCSessionDescription(type: type, sdp: sdp)) { [weak self] error in
                guard let self, error == nil else { return }
                self.isRemoteDescriptionSet = true
                let queued = self.pendingCandidates
                self.pendingCandidates.removeAll()
                queued.forEach { self.peerConnection?.add($0) }
                if let connection = self.peerConnection { self.attachReceivers(connection) }
                if type == .offer { self.createAnswer() }
            }
        case "ice_candidate":
            let candidate = payload["candidate"] as? String ?? ""
            guard !candidate.isEmpty else { return }
            let index: Int32
            if let number = payload["sdpMLineIndex"] as? NSNumber {
                index = number.int32Value
            } else if let text = payload["sdpMLineIndex"] as? String, let parsed = Int32(text) {
                index = parsed
            } else {
                return
            }
            let mid = (payload["sdpMid"] as? String)?.nilIfEmpty
            let ice = RTCIceCandidate(sdp: candidate, sdpMLineIndex: index, sdpMid: mid)
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
        ensurePeer()
        ensureSendRecv()
        peerConnection?.restartIce()
        peerConnection?.offer(for: mediaConstraints(iceRestart: true)) { [weak self] sdp, error in
            guard let self, let sdp, error == nil else { return }
            self.peerConnection?.setLocalDescription(sdp) { _ in
                self.emit("sdp_offer", ["type": "offer", "sdp": sdp.sdp])
            }
        }
    }

    public func setMicEnabled(_ enabled: Bool) {
        isMicMuted = !enabled
        localAudioTrack?.isEnabled = enabled
        peerConnection?.senders.forEach { sender in
            if sender.track?.kind == "audio" {
                sender.track?.isEnabled = enabled
            }
        }
    }

    public func setSpeakerEnabled(_ enabled: Bool) {
        isSpeakerActive = enabled
        speakerIsOn = enabled
        let rtc = RTCAudioSession.sharedInstance()
        rtc.lockForConfiguration()
        do {
            try rtc.overrideOutputAudioPort(enabled ? .speaker : .none)
        } catch {
            print("[WebRtcCallManager] Speaker toggle error: \(error.localizedDescription)")
        }
        rtc.unlockForConfiguration()
    }

    public func setCameraEnabled(_ enabled: Bool) {
        localVideoTrack?.isEnabled = enabled
        if enabled {
            startCamera()
        } else {
            videoCapturer?.stopCapture()
            isCapturing = false
        }
    }

    private func configureAudioSession(for callType: CallType) {
        let rtc = RTCAudioSession.sharedInstance()
        rtc.useManualAudio = false
        rtc.isAudioEnabled = true
        let config = RTCAudioSessionConfiguration.webRTC()
        config.category = AVAudioSession.Category.playAndRecord.rawValue
        config.mode = (callType == .video ? AVAudioSession.Mode.videoChat : AVAudioSession.Mode.voiceChat).rawValue
        var options: AVAudioSession.CategoryOptions = [.allowBluetooth, .allowBluetoothA2DP]
        if callType == .video { options.insert(.defaultToSpeaker) }
        config.categoryOptions = options
        RTCAudioSessionConfiguration.setWebRTC(config)
        rtc.lockForConfiguration()
        do {
            try rtc.setCategory(AVAudioSession.Category.playAndRecord, mode: callType == .video ? .videoChat : .voiceChat, options: options)
            try rtc.setActive(true)
        } catch {
            print("[WebRtcCallManager] Audio session setup error: \(error.localizedDescription)")
        }
        rtc.unlockForConfiguration()
        rtc.isAudioEnabled = true
    }

    public func end() {
        recoveryTask?.cancel()
        recoveryTask = nil
        releasePeerOnly(keepLocalVideo: false)
        let rtc = RTCAudioSession.sharedInstance()
        rtc.lockForConfiguration()
        try? rtc.setActive(false)
        rtc.unlockForConfiguration()
        rtc.isAudioEnabled = false
    }

    public func release() {
        end()
        onSignal = nil
        onConnected = nil
        onDisconnected = nil
    }

    public func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        attachRemote(audio: stream.audioTracks, video: stream.videoTracks)
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didAdd rtpReceiver: RTCRtpReceiver, streams: [RTCMediaStream]) {
        let audio = ([rtpReceiver.track].compactMap { $0 as? RTCAudioTrack }) + streams.flatMap(\.audioTracks)
        let video = ([rtpReceiver.track].compactMap { $0 as? RTCVideoTrack }) + streams.flatMap(\.videoTracks)
        attachRemote(audio: audio, video: video)
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
    public func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}
    public func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        if newState == .connected || newState == .completed {
            recoveryTask?.cancel()
            recoveryTask = nil
            attachReceivers(peerConnection)
            setSpeakerEnabled(callType == .video)
            DispatchQueue.main.async { self.onConnected?() }
        } else if newState == .disconnected {
            recoveryTask?.cancel()
            recoveryTask = Task { [weak self] in
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                guard let self, !Task.isCancelled else { return }
                if self.peerConnection?.iceConnectionState == .disconnected {
                    self.restartIce()
                }
                try? await Task.sleep(nanoseconds: 12_000_000_000)
                guard !Task.isCancelled, let state = self.peerConnection?.iceConnectionState else { return }
                if state == .disconnected || state == .failed {
                    DispatchQueue.main.async { self.onDisconnected?() }
                }
            }
        } else if newState == .failed || newState == .closed {
            DispatchQueue.main.async { self.onDisconnected?() }
        }
    }
    public func peerConnection(_ peerConnection: RTCPeerConnection, didStartReceivingOn transceiver: RTCRtpTransceiver) {
        attachReceivers(peerConnection)
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

    private func attachRemote(audio: [RTCAudioTrack], video: [RTCVideoTrack]) {
        audio.forEach { track in
            track.isEnabled = true
            track.source.volume = 10
        }
        guard let track = video.first else { return }
        track.isEnabled = true
        DispatchQueue.main.async { CallMediaState.shared.remoteVideo = track }
    }

    private func attachReceivers(_ connection: RTCPeerConnection) {
        let audio = connection.transceivers.compactMap { $0.receiver.track as? RTCAudioTrack }
        let video = connection.transceivers.compactMap { $0.receiver.track as? RTCVideoTrack }
        attachRemote(audio: audio, video: video)
    }

    private func ensureFactory() {
        if factory == nil {
            RTCInitializeSSL()
            factory = RTCPeerConnectionFactory()
        }
    }

    private func ensurePeer() {
        ensureFactory()
        guard peerConnection == nil, let factory else { return }
        if iceServers.isEmpty { setIceServers([]) }
        configureAudioSession(for: callType)
        let config = RTCConfiguration()
        config.iceServers = iceServers
        config.sdpSemantics = .unifiedPlan
        config.bundlePolicy = .maxBundle
        config.rtcpMuxPolicy = .require
        config.continualGatheringPolicy = .gatherContinually
        config.iceCandidatePoolSize = 2
        config.iceTransportPolicy = .all
        let constraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: ["DtlsSrtpKeyAgreement": "true"])
        let connection = factory.peerConnection(with: config, constraints: constraints, delegate: self)
        let audioConstraints = RTCMediaConstraints(
            mandatoryConstraints: [
                "googEchoCancellation": "true",
                "googNoiseSuppression": "true",
                "googAutoGainControl": "true"
            ],
            optionalConstraints: nil
        )
        let audioSource = factory.audioSource(with: audioConstraints)
        let audio = factory.audioTrack(with: audioSource, trackId: "audio0")
        audio.isEnabled = !isMicMuted
        localAudioTrack = audio
        connection?.add(audio, streamIds: ["stream0"])
        if callType == .video {
            if localVideoTrack == nil { startCamera() }
            if let video = localVideoTrack {
                video.isEnabled = true
                connection?.add(video, streamIds: ["stream0"])
            }
        }
        peerConnection = connection
        ensureSendRecv()
    }

    private func ensureSendRecv() {
        peerConnection?.transceivers.forEach { transceiver in
            if transceiver.mediaType == .video && callType != .video { return }
            transceiver.setDirection(.sendRecv, error: nil)
        }
    }

    private func startCamera() {
        ensureFactory()
        guard let factory else { return }
        if localVideoSource == nil { localVideoSource = factory.videoSource() }
        guard let source = localVideoSource else { return }
        if localVideoTrack == nil {
            let track = factory.videoTrack(with: source, trackId: "video0")
            track.isEnabled = true
            localVideoTrack = track
            DispatchQueue.main.async { CallMediaState.shared.localVideo = track }
        }
        if isCapturing { return }
        let capturer = videoCapturer ?? RTCCameraVideoCapturer(delegate: source)
        videoCapturer = capturer
        let position: AVCaptureDevice.Position = isFrontCamera ? .front : .back
        guard let device = RTCCameraVideoCapturer.captureDevices().first(where: { $0.position == position })
                ?? RTCCameraVideoCapturer.captureDevices().first else { return }
        let formats = RTCCameraVideoCapturer.supportedFormats(for: device)
        let format = formats.first { item in
            let size = CMVideoFormatDescriptionGetDimensions(item.formatDescription)
            return size.width >= 640 && size.width <= 1280
        } ?? formats.last
        guard let format else { return }
        capturer.startCapture(with: device, format: format, fps: 24)
        isCapturing = true
        localVideoTrack?.isEnabled = true
    }

    private func releaseLocalVideo() {
        videoCapturer?.stopCapture()
        videoCapturer = nil
        isCapturing = false
        localVideoTrack?.isEnabled = false
        localVideoTrack = nil
        localVideoSource = nil
        DispatchQueue.main.async {
            CallMediaState.shared.localVideo = nil
            CallMediaState.shared.remoteVideo = nil
        }
    }

    private func releasePeerOnly(keepLocalVideo: Bool) {
        isRemoteDescriptionSet = false
        pendingCandidates.removeAll()
        localAudioTrack = nil
        peerConnection?.close()
        peerConnection = nil
        if !keepLocalVideo { releaseLocalVideo() }
    }

    private func mediaConstraints(iceRestart: Bool) -> RTCMediaConstraints {
        var mandatory = [
            "OfferToReceiveAudio": "true",
            "OfferToReceiveVideo": callType == .video ? "true" : "false"
        ]
        if iceRestart { mandatory["IceRestart"] = "true" }
        return RTCMediaConstraints(mandatoryConstraints: mandatory, optionalConstraints: nil)
    }

    private static func sdpText(_ payload: [String: Any]) -> String {
        if let text = payload["sdp"] as? String, !text.isEmpty { return text }
        if let nested = payload["sdp"] as? [String: Any], let text = nested["sdp"] as? String { return text }
        return ""
    }

    private func emit(_ type: String, _ payload: [String: Any]) {
        DispatchQueue.main.async { self.onSignal?(type, payload) }
    }
}

public final class CallSocketBridge: NSObject, CallSocketListener, CallKitServiceDelegate {
    public static let shared = CallSocketBridge()
    private var receiverId = ""
    private var sessionId = ""
    private var callType: CallType = .voice
    private var ignoringCallKitEnd = false

    private override init() {
        super.init()
        CallKitService.shared.delegate = self
        CallActionHandler.shared.onCallAccepted = { sessionId, senderId, callType in
            DispatchQueue.main.async {
                if GlobalCallState.shared.incomingCall == nil {
                    GlobalCallState.shared.incomingCall = IncomingCallInfo(id: sessionId, senderId: senderId, callerName: "", callType: callType)
                }
                CallSocketBridge.shared.acceptIncoming()
            }
        }
        CallActionHandler.shared.onCallDeclined = { _, _ in
            CallSocketBridge.shared.declineIncoming()
        }
    }

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
            DispatchQueue.main.async {
                GlobalCallState.shared.isConnectionEstablished = true
            }
        }
        WebRtcCallManager.shared.onDisconnected = { [weak self] in
            self?.endActive(reason: "webrtc_disconnected")
        }
    }

    public func acceptIncoming() {
        DispatchQueue.main.async {
            guard let info = GlobalCallState.shared.incomingCall else { return }
            GlobalCallState.shared.incomingCall = nil
            GlobalCallState.shared.setCallActive(true, sessionId: info.id, participantId: info.senderId, callType: info.callType)
            GlobalCallState.shared.isAnswered = true
            GlobalCallState.shared.isConnectionEstablished = false
            GlobalCallState.shared.isRinging = false
            self.bind(receiverId: info.senderId, sessionId: info.id, callType: info.callType)
            Task {
                await WebRtcCallManager.shared.prepareIce()
                await MainActor.run {
                    WebRtcCallManager.shared.start(callType: info.callType)
                    CallWebSocketClient.shared.accept(receiverId: info.senderId, sessionId: info.id, callType: info.callType)
                }
            }
        }
    }

    public func declineIncoming() {
        DispatchQueue.main.async {
            guard let info = GlobalCallState.shared.incomingCall else { return }
            GlobalCallState.shared.incomingCall = nil
            WebRtcCallManager.shared.stopLocalPreview()
            CallWebSocketClient.shared.reject(receiverId: info.senderId, sessionId: info.id, callType: info.callType, reason: "rejected")
        }
    }

    public func endActive(reason: String) {
        DispatchQueue.main.async {
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
                    if state.activeSessionId == event.sessionId { return }
                    if !state.isConnectionEstablished && state.activeParticipantId == event.senderId {
                        if mine < event.senderId {
                            CallWebSocketClient.shared.reject(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType, reason: "glare")
                            return
                        }
                        WebRtcCallManager.shared.end()
                        state.onCallEnded(sessionId: state.activeSessionId ?? event.sessionId)
                    } else {
                        CallWebSocketClient.shared.reject(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType, reason: "busy")
                        return
                    }
                }
                Task { await WebRtcCallManager.shared.prepareIce() }
                self.bind(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType)
                CallWebSocketClient.shared.ringing(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType)
                self.presentIncoming(event)
                if event.callType == .video {
                    AVCaptureDevice.requestAccess(for: .video) { granted in
                        guard granted else { return }
                        DispatchQueue.main.async {
                            WebRtcCallManager.shared.startLocalPreview()
                        }
                    }
                }
            case "call_accept":
                guard state.activeSessionId == event.sessionId else { return }
                state.isAnswered = true
                state.isRinging = false
                WebRtcCallManager.shared.createOffer()
            case "call_ringing":
                if state.activeSessionId == event.sessionId { state.isRinging = true }
            case "call_end", "call_reject":
                let matches = state.activeSessionId == event.sessionId || state.incomingCall?.id == event.sessionId
                guard matches else { return }
                if event.endReason == "busy" && state.activeSessionId == event.sessionId {
                    AudioServicesPlaySystemSound(1070)
                }
                WebRtcCallManager.shared.end()
                state.onCallEnded(sessionId: event.sessionId)
                self.ignoringCallKitEnd = true
                CallKitService.shared.endCall(sessionId: event.sessionId, reason: event.type ?? "ended")
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
        DispatchQueue.main.async {
            let cached = GlobalCallState.shared.contactNames[event.senderId] ?? ""
            let known = realPersonLabel(names: [cached, event.senderName], phone: GlobalCallState.shared.contactPhones[event.senderId], ids: [event.senderId])
            GlobalCallState.shared.incomingCall = IncomingCallInfo(id: event.sessionId, senderId: event.senderId, callerName: known, callType: event.callType)
            self.reportLockScreenCall(event, name: known)
            guard known.isEmpty else { return }
        }
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

    private func reportLockScreenCall(_ event: CallEvent, name: String) {
        guard UIApplication.shared.applicationState != .active else { return }
        let shown = name.isEmpty ? event.senderId : name
        CallKitService.shared.reportIncomingCall(
            sessionId: event.sessionId,
            participantId: event.senderId,
            participantName: shown,
            callType: event.callType
        ) { error in
            if error != nil {
                self.postIncomingCallNotification(event, name: shown)
            }
        }
        CallBackgroundKeepAlive.begin()
    }

    private func postIncomingCallNotification(_ event: CallEvent, name: String) {
        let content = UNMutableNotificationContent()
        content.title = event.callType == .video ? "Incoming video call" : "Incoming voice call"
        content.body = "\(name) is calling"
        content.sound = .default
        content.categoryIdentifier = PushNotificationHandler.categoryIncomingCall
        content.userInfo = [
            "type": "call_invite",
            "session_id": event.sessionId,
            "sender_id": event.senderId,
            "caller_name": name,
            "call_type": event.callType.rawValue
        ]
        let request = UNNotificationRequest(identifier: "call_\(event.sessionId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    public func callKitDidAnswerCall(session: CallSessionInfo) {
        DispatchQueue.main.async {
            if GlobalCallState.shared.incomingCall == nil {
                GlobalCallState.shared.incomingCall = IncomingCallInfo(
                    id: session.sessionId,
                    senderId: session.participantId,
                    callerName: session.participantName,
                    callType: session.callType
                )
            }
            self.acceptIncoming()
        }
    }

    public func callKitDidEndCall(session: CallSessionInfo, reason: String) {
        if ignoringCallKitEnd {
            ignoringCallKitEnd = false
            return
        }
        DispatchQueue.main.async {
            let active = GlobalCallState.shared.isCallActive
            if session.isAnswered || active {
                self.endActive(reason: "user_ended")
            } else {
                self.declineIncoming()
            }
        }
    }

    public func callKitDidStartCall(session: CallSessionInfo) {}

    public func callKitSetMuted(_ isMuted: Bool, session: CallSessionInfo) {
        WebRtcCallManager.shared.setMicEnabled(!isMuted)
    }
}

enum CallBackgroundKeepAlive {
    private static var taskId: UIBackgroundTaskIdentifier = .invalid

    static func begin() {
        guard taskId == .invalid else { return }
        taskId = UIApplication.shared.beginBackgroundTask(withName: "tj.call") {
            end()
        }
        DispatchQueue.main.async {
            AppRuntime.connectRealtime()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 25) {
            end()
        }
    }

    static func end() {
        guard taskId != .invalid else { return }
        UIApplication.shared.endBackgroundTask(taskId)
        taskId = .invalid
    }
}


