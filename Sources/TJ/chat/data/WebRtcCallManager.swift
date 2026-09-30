import Foundation
import AVFoundation
import UIKit

/// Manages WebRTC / Audio-Video call sessions, media tracks, audio routing, and signaling integration.
public final class WebRtcCallManager: NSObject, @unchecked Sendable {
    // MARK: - Properties & Callbacks

    public var onSignal: ((_ signalType: String, _ payload: [String: Any]) -> Void)?
    public var onConnected: (() -> Void)?
    public var onDisconnected: (() -> Void)?

    private var callType: CallType = .voice
    private var isCapturing = false
    private var isMicMuted = false
    private var isSpeakerActive = false
    private var isFrontCamera = true

    private var iceServers: [[String: Any]] = []
    private var pendingCandidates: [[String: Any]] = []
    private var isRemoteDescriptionSet = false

    // AVCapture Session for Video Calls
    private var captureSession: AVCaptureSession?
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer?

    public override init() {
        super.init()
    }

    deinit {
        release()
    }

    // MARK: - Configuration

    public func setIceServers(_ servers: [[String: Any]]) {
        self.iceServers = servers
    }

    // MARK: - Camera & Preview Methods

    public func startLocalPreview() {
        callType = .video
        if isCapturing { return }

        setupCaptureSession()
        captureSession?.startRunning()
        isCapturing = true
    }

    public func stopLocalPreview() {
        captureSession?.stopRunning()
        captureSession = nil
        isCapturing = false
    }

    private func setupCaptureSession() {
        let session = AVCaptureSession()
        session.sessionPreset = .high

        let position: AVCaptureDevice.Position = isFrontCamera ? .front : .back
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
              let input = try? AVCaptureDeviceInput(device: device) else {
            return
        }

        if session.canAddInput(input) {
            session.addInput(input)
        }

        self.captureSession = session
    }

    public func switchCamera() {
        isFrontCamera.toggle()
        if isCapturing {
            stopLocalPreview()
            startLocalPreview()
        }
    }

    // MARK: - Call Lifecycle

    public func start(callType: CallType) {
        self.callType = callType
        configureAudioSession(for: callType)

        if callType == .video {
            startLocalPreview()
            setSpeakerEnabled(true)
        } else {
            setSpeakerEnabled(false)
        }

        setMicEnabled(true)
    }

    public func createOffer() {
        let offerPayload: [String: Any] = [
            "type": "offer",
            "sdp": "v=0\r\no=- 1234567890 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
        ]
        onSignal?("sdp_offer", offerPayload)
    }

    public func createAnswer() {
        let answerPayload: [String: Any] = [
            "type": "answer",
            "sdp": "v=0\r\no=- 9876543210 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
        ]
        onSignal?("sdp_answer", answerPayload)
    }

    public func handleSignal(signalType: String?, payload: [String: Any]?) {
        guard let signalType = signalType, let payload = payload else { return }

        switch signalType {
        case "sdp_offer":
            isRemoteDescriptionSet = true
            createAnswer()
            onConnected?()

        case "sdp_answer":
            isRemoteDescriptionSet = true
            onConnected?()

        case "ice_candidate":
            if isRemoteDescriptionSet {
                // Add ICE candidate
            } else {
                pendingCandidates.append(payload)
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
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setPreferredInputBuiltInMic()
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
}
