import Foundation
import Combine

/// App-wide observable state for active, pending, or incoming audio/video calls.
public final class GlobalCallState: ObservableObject, @unchecked Sendable {
    public static let shared = GlobalCallState()

    // MARK: - Published Properties
    @Published public private(set) var incomingCall: CallEvent?
    @Published public private(set) var isCallActive: Bool = false
    @Published public private(set) var activeSessionId: String?
    @Published public private(set) var activeParticipantId: String?
    @Published public private(set) var activeCallType: CallType?
    @Published public private(set) var isConnectionEstablished: Bool = false
    @Published public private(set) var isRinging: Bool = false
    @Published public private(set) var isAnswered: Bool = false
    @Published public private(set) var callDurationSeconds: Int = 0
    @Published public private(set) var autoAcceptSessionId: String?

    // MARK: - Contact Info Cache
    private var contactNames: [String: String] = [:]
    private var contactPhones: [String: String] = [:]
    private let lock = NSLock()

    private init() {}

    // MARK: - State Modifiers

    public func setCallActive(_ active: Bool, sessionId: String? = nil, participantId: String? = nil, callType: CallType? = nil) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.isCallActive = active
            self.activeSessionId = sessionId
            self.activeParticipantId = participantId
            self.activeCallType = callType

            if !active {
                self.isConnectionEstablished = false
                self.isRinging = false
                self.isAnswered = false
                self.callDurationSeconds = 0
            }
        }
    }

    public func setAnswered(_ answered: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.isAnswered = answered
            if answered { self?.isRinging = false }
        }
    }

    public func setCallDuration(_ seconds: Int) {
        DispatchQueue.main.async { [weak self] in
            self?.callDurationSeconds = seconds
        }
    }

    public func setConnectionEstablished(_ established: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.isConnectionEstablished = established
            if established { self?.isRinging = false }
        }
    }

    public func setRinging(_ ringing: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.isRinging = ringing
        }
    }

    // MARK: - Contact Management

    public func updateContactInfo(userId: String, name: String, phone: String?) {
        lock.lock()
        defer { lock.unlock() }

        if !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            contactNames[userId] = name
        }
        if let phone = phone, !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            contactPhones[userId] = phone
        }
    }

    public func getContactName(id: String) -> String? {
        lock.lock()
        defer { lock.unlock() }
        return contactNames[id]
    }

    public func getContactPhone(userId: String) -> String? {
        lock.lock()
        defer { lock.unlock() }
        return contactPhones[userId]
    }

    // MARK: - Call Event Handlers

    public func onCallInvite(event: CallEvent) {
        lock.lock()
        let phone = contactPhones[event.senderId]
        let name = contactNames[event.senderId]
        lock.unlock()

        let finalName = !event.senderName.isEmpty ? event.senderName : (name ?? phone ?? "Unknown")
        var updatedEvent = event
        updatedEvent.senderName = finalName

        DispatchQueue.main.async { [weak self] in
            self?.incomingCall = updatedEvent
        }
    }

    public func clearIncomingCall() {
        DispatchQueue.main.async { [weak self] in
            self?.incomingCall = nil
        }
    }

    public func acceptCall(event: CallEvent, webRtcCallManager: WebRtcCallManager, callWebSocketClient: CallWebSocketClient) {
        setCallActive(true, sessionId: event.sessionId, participantId: event.senderId, callType: event.callType)
        setAnswered(true)
        setConnectionEstablished(false)
        setRinging(false)
        clearIncomingCall()

        webRtcCallManager.start(callType: event.callType)
        callWebSocketClient.accept(receiverId: event.senderId, sessionId: event.sessionId, callType: event.callType)
    }

    public func onCallEnded(sessionId: String) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            if self.incomingCall?.sessionId == sessionId || sessionId == "force_all" {
                self.incomingCall = nil
            }
            if self.autoAcceptSessionId == sessionId || sessionId == "force_all" {
                self.autoAcceptSessionId = nil
            }
            if self.activeSessionId == sessionId || sessionId == "force_all" {
                self.isCallActive = false
                self.activeSessionId = nil
                self.activeParticipantId = nil
                self.activeCallType = nil
                self.isAnswered = false
                self.isConnectionEstablished = false
                self.callDurationSeconds = 0
            }
        }
    }

    public func setAutoAccept(sessionId: String) {
        DispatchQueue.main.async { [weak self] in
            self?.autoAcceptSessionId = sessionId
        }
    }

    public func clearAutoAccept() {
        DispatchQueue.main.async { [weak self] in
            self?.autoAcceptSessionId = nil
        }
    }

    public func clear() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.incomingCall = nil
            self.autoAcceptSessionId = nil
            self.isCallActive = false
            self.activeSessionId = nil
            self.activeParticipantId = nil
            self.activeCallType = nil
            self.isAnswered = false
            self.isConnectionEstablished = false
            self.callDurationSeconds = 0
        }
    }
}
