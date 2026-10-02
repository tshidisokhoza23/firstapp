import SwiftUI
import AVFoundation
import AudioToolbox

public struct IncomingCallInfo: Identifiable, Equatable {
    public let id: String // sessionId
    public let senderId: String
    public let callerName: String
    public let avatarUrl: String?
    public let callType: CallType

    public init(id: String, senderId: String, callerName: String, avatarUrl: String? = nil, callType: CallType = .voice) {
        self.id = id
        self.senderId = senderId
        self.callerName = callerName
        self.avatarUrl = avatarUrl
        self.callType = callType
    }
}

/// Full screen incoming call view with caller avatar, accept/decline actions, ringtone player, and haptic vibration.
public struct IncomingCallView: View {
    public let callInfo: IncomingCallInfo
    public var onAccept: (IncomingCallInfo) -> Void
    public var onDecline: (IncomingCallInfo) -> Void

    @State private var audioPlayer: AVAudioPlayer?
    @State private var vibrationTimer: Timer?
    @State private var isCameraGranted: Bool = false

    public init(
        callInfo: IncomingCallInfo,
        onAccept: @escaping (IncomingCallInfo) -> Void,
        onDecline: @escaping (IncomingCallInfo) -> Void
    ) {
        self.callInfo = callInfo
        self.onAccept = onAccept
        self.onDecline = onDecline
    }

    public var body: some View {
        ZStack {
            Color(red: 0.05, green: 0.05, blue: 0.05)
                .ignoresSafeArea()

            // Optional Local Camera Preview for Video Call
            if callInfo.callType == .video {
                CallVideoSurface(track: CallMediaState.shared.localVideo)
                    .ignoresSafeArea()
            }

            VStack {
                Spacer()
                    .frame(height: 60)

                // Caller Info & Avatar
                VStack(spacing: 16) {
                    if let avatarUrl = callInfo.avatarUrl, let url = URL(string: avatarUrl) {
                        AsyncImage(url: url) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Circle().fill(Color.gray.opacity(0.4))
                        }
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                    } else {
                        Image(systemName: "person.circle.fill")
                            .resizable()
                            .frame(width: 100, height: 100)
                            .foregroundColor(.gray)
                    }

                    Text(callInfo.callerName)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundColor(.white)

                    Text(callInfo.callType == .video ? "Incoming Video Call" : "Incoming Voice Call")
                        .font(.system(size: 16))
                        .foregroundColor(.white.opacity(0.8))
                }

                Spacer()

                // Action Buttons: Decline / Accept
                HStack(spacing: 64) {
                    // Decline
                    VStack(spacing: 8) {
                        Button(action: {
                            stopRingtoneAndVibration()
                            onDecline(callInfo)
                        }) {
                            Image(systemName: "phone.down.fill")
                                .font(.system(size: 32))
                                .foregroundColor(.white)
                                .frame(width: 72, height: 72)
                                .background(Color.red)
                                .clipShape(Circle())
                        }
                        Text("Decline")
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                    }

                    // Accept
                    VStack(spacing: 8) {
                        Button(action: {
                            stopRingtoneAndVibration()
                            onAccept(callInfo)
                        }) {
                            Image(systemName: "phone.fill")
                                .font(.system(size: 32))
                                .foregroundColor(.white)
                                .frame(width: 72, height: 72)
                                .background(Color.green)
                                .clipShape(Circle())
                        }
                        Text("Accept")
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                    }
                }
                .padding(.bottom, 60)
            }
        }
        .onAppear {
            startRingtoneAndVibration()
            checkCameraPermission()
        }
        .onDisappear {
            stopRingtoneAndVibration()
        }
    }

    // MARK: - Ringtone & Vibration Management
    private func startRingtoneAndVibration() {
        // Play system vibration
        vibrationTimer = Timer.scheduledTimer(withTimeInterval: 1.2, repeats: true) { _ in
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
        if callInfo.callType == .video {
            AudioServicesPlaySystemSound(1005)
            return
        }

        // Play Ringtone
        if let soundURL = Bundle.main.url(forResource: "ringtone", withExtension: "mp3") {
            do {
                audioPlayer = try AVAudioPlayer(contentsOf: soundURL)
                audioPlayer?.numberOfLoops = -1
                audioPlayer?.play()
            } catch {
                print("[IncomingCallView] Error playing ringtone: \(error.localizedDescription)")
            }
        } else {
            // Fallback system sound call
            AudioServicesPlaySystemSound(1005)
        }
    }

    private func stopRingtoneAndVibration() {
        vibrationTimer?.invalidate()
        vibrationTimer = nil
        audioPlayer?.stop()
        audioPlayer = nil
    }

    private func checkCameraPermission() {
        guard callInfo.callType == .video else { return }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            isCameraGranted = true
            WebRtcCallManager.shared.startLocalPreview()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    isCameraGranted = granted
                    if granted { WebRtcCallManager.shared.startLocalPreview() }
                }
            }
        default:
            isCameraGranted = false
        }
    }
}
