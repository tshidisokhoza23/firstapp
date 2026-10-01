import Foundation
import Combine
import SwiftUI

@MainActor
public class GlobalCallState: ObservableObject {
    public static let shared = GlobalCallState()

    @Published public var isCallActive: Bool = false
    @Published public var activeSessionId: String? = nil
    @Published public var activeParticipantId: String? = nil
    @Published public var activeCallType: CallType? = .voice
    @Published public var isConnectionEstablished: Bool = false
    @Published public var isRinging: Bool = false
    @Published public var isAnswered: Bool = false
    @Published public var callDurationSeconds: Int = 0
    @Published public var contactNames: [String: String] = [:]
    @Published public var contactPhones: [String: String] = [:]

    private var timer: Timer?

    public init() {}

    public func setCallActive(_ active: Bool, sessionId: String?, participantId: String?, callType: CallType?) {
        self.isCallActive = active
        self.activeSessionId = sessionId
        self.activeParticipantId = participantId
        self.activeCallType = callType
        if active {
            startTimer()
        } else {
            stopTimer()
        }
    }

    public func startCall(receiverId: String, isVideo: Bool) {
        let sid = UUID().uuidString
        let type: CallType = isVideo ? .video : .voice
        setCallActive(true, sessionId: sid, participantId: receiverId, callType: type)
    }

    public func updateContactInfo(id: String, name: String, phone: String?) {
        contactNames[id] = name
        if let phone = phone {
            contactPhones[id] = phone
        }
    }

    public func onCallEnded(sessionId: String) {
        setCallActive(false, sessionId: nil, participantId: nil, callType: nil)
        callDurationSeconds = 0
    }

    private func startTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.callDurationSeconds += 1
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

@MainActor
public class CallViewModel: ObservableObject {
    @Published public var isMicMuted: Bool = false
    @Published public var isSpeakerOn: Bool = false

    public init() {}

    public func toggleMic() {
        isMicMuted.toggle()
    }

    public func toggleSpeaker() {
        isSpeakerOn.toggle()
    }

    public func endCall() {
        if let sid = GlobalCallState.shared.activeSessionId {
            GlobalCallState.shared.onCallEnded(sessionId: sid)
        }
    }
}
