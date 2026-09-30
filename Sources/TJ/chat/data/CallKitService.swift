import Foundation
import CallKit
import AVFoundation

public enum CallType: String, Codable {
    case voice = "voice"
    case video = "video"
}

public struct CallSessionInfo {
    public let uuid: UUID
    public let sessionId: String
    public let participantId: String
    public let participantName: String
    public let callType: CallType
    public var isAnswered: Bool
    public var isConnected: Bool

    public init(
        uuid: UUID = UUID(),
        sessionId: String,
        participantId: String,
        participantName: String,
        callType: CallType,
        isAnswered: Bool = false,
        isConnected: Bool = false
    ) {
        self.uuid = uuid
        self.sessionId = sessionId
        self.participantId = participantId
        self.participantName = participantName
        self.callType = callType
        self.isAnswered = isAnswered
        self.isConnected = isConnected
    }
}

public protocol CallKitServiceDelegate: AnyObject {
    func callKitDidAnswerCall(session: CallSessionInfo)
    func callKitDidEndCall(session: CallSessionInfo, reason: String)
    func callKitDidStartCall(session: CallSessionInfo)
    func callKitSetMuted(_ isMuted: Bool, session: CallSessionInfo)
}

public final class CallKitService: NSObject {

    public static let shared = CallKitService()

    public weak var delegate: CallKitServiceDelegate?

    private let provider: CXProvider
    private let callController = CXCallController()

    private var activeSessions: [UUID: CallSessionInfo] = [:]
    private var sessionByStringId: [String: UUID] = [:]

    private var durationTimer: Timer?
    private(set) public var callDurationSeconds: Int = 0

    private override init() {
        let configuration = CXProviderConfiguration(localizedName: "TJ")
        configuration.supportsVideo = true
        configuration.maximumCallGroups = 1
        configuration.maximumCallsPerCallGroup = 1
        configuration.supportedHandleTypes = [.generic, .phoneNumber]
        if let iconImage = UIImage(named: "AppIcon") {
            configuration.iconTemplateImageData = iconImage.pngData()
        }

        self.provider = CXProvider(configuration: configuration)
        super.init()
        self.provider.setDelegate(self, queue: .main)
    }

    // MARK: - Incoming Calls

    public func reportIncomingCall(
        sessionId: String,
        participantId: String,
        participantName: String,
        callType: CallType,
        completion: ((Error?) -> Void)? = nil
    ) {
        let uuid = UUID()
        let session = CallSessionInfo(
            uuid: uuid,
            sessionId: sessionId,
            participantId: participantId,
            participantName: participantName,
            callType: callType
        )

        activeSessions[uuid] = session
        sessionByStringId[sessionId] = uuid

        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: participantId)
        update.localizedCallerName = participantName.isEmpty ? participantId : participantName
        update.hasVideo = (callType == .video)
        update.supportsHolding = false
        update.supportsGrouping = false
        update.supportsUngrouping = false
        update.supportsDTMF = false

        provider.reportNewIncomingCall(with: uuid, update: update) { [weak self] error in
            if let error = error {
                self?.activeSessions.removeValue(forKey: uuid)
                self?.sessionByStringId.removeValue(forKey: sessionId)
                completion?(error)
            } else {
                completion?(nil)
            }
        }
    }

    // MARK: - Outgoing Calls

    public func startOutgoingCall(
        sessionId: String,
        participantId: String,
        participantName: String,
        callType: CallType
    ) {
        let uuid = UUID()
        let session = CallSessionInfo(
            uuid: uuid,
            sessionId: sessionId,
            participantId: participantId,
            participantName: participantName,
            callType: callType
        )

        activeSessions[uuid] = session
        sessionByStringId[sessionId] = uuid

        let handle = CXHandle(type: .generic, value: participantId)
        let startCallAction = CXStartCallAction(call: uuid, handle: handle)
        startCallAction.isVideo = (callType == .video)
        startCallAction.contactIdentifier = participantName

        let transaction = CXTransaction(action: startCallAction)
        callController.request(transaction) { error in
            if let error = error {
                print("[CallKitService] Failed to start call transaction: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - Call State Updates

    public func reportConnected(sessionId: String) {
        guard let uuid = sessionByStringId[sessionId], var session = activeSessions[uuid] else { return }
        session.isConnected = true
        activeSessions[uuid] = session
        provider.reportOutgoingCall(with: uuid, connectedAt: Date())
        startDurationTimer()
    }

    public func reportRinging(sessionId: String) {
        guard let uuid = sessionByStringId[sessionId] else { return }
        provider.reportOutgoingCall(with: uuid, startedConnectingAt: Date())
    }

    public func endCall(sessionId: String, reason: String = "ended") {
        guard let uuid = sessionByStringId[sessionId] else { return }
        let endAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction(action: endAction)

        callController.request(transaction) { [weak self] error in
            if let error = error {
                print("[CallKitService] Error requesting end call: \(error.localizedDescription)")
                self?.provider.reportCall(with: uuid, endedAt: Date(), reason: .remoteEnded)
                self?.cleanupSession(uuid: uuid)
            }
        }
        stopDurationTimer()
    }

    // MARK: - Timer

    private func startDurationTimer() {
        stopDurationTimer()
        callDurationSeconds = 0
        durationTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.callDurationSeconds += 1
        }
    }

    private func stopDurationTimer() {
        durationTimer?.invalidate()
        durationTimer = nil
    }

    private func cleanupSession(uuid: UUID) {
        if let session = activeSessions[uuid] {
            sessionByStringId.removeValue(forKey: session.sessionId)
        }
        activeSessions.removeValue(forKey: uuid)
        if activeSessions.isEmpty {
            stopDurationTimer()
        }
    }
}

// MARK: - CXProviderDelegate

extension CallKitService: CXProviderDelegate {

    public func providerDidReset(_ provider: CXProvider) {
        stopDurationTimer()
        activeSessions.clear()
        sessionByStringId.clear()
    }

    public func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        guard let session = activeSessions[action.callUUID] else {
            action.fail()
            return
        }
        configureAudioSession()
        delegate?.callKitDidStartCall(session: session)
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        guard var session = activeSessions[action.callUUID] else {
            action.fail()
            return
        }
        session.isAnswered = true
        activeSessions[action.callUUID] = session
        configureAudioSession()
        delegate?.callKitDidAnswerCall(session: session)
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        guard let session = activeSessions[action.callUUID] else {
            action.fail()
            return
        }
        delegate?.callKitDidEndCall(session: session, reason: "user_ended")
        cleanupSession(uuid: action.callUUID)
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        guard let session = activeSessions[action.callUUID] else {
            action.fail()
            return
        }
        delegate?.callKitSetMuted(action.isMuted, session: session)
        action.fulfill()
    }

    private func configureAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .allowBluetoothA2DP])
            try audioSession.setActive(true)
        } catch {
            print("[CallKitService] Audio session error: \(error.localizedDescription)")
        }
    }
}

private extension Dictionary {
    mutating func clear() {
        self.removeAll()
    }
}
