import Foundation

public struct CallLog: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var sessionId: String
    public var callerId: String
    public var calleeId: String
    public var contactId: String
    public var contactName: String
    public var contactAvatar: String?
    public var callType: String // "voice" | "video"
    public var status: String // "missed" | "incoming" | "outgoing"
    public var durationSeconds: Int
    public var timestamp: Int64
    public var endedAt: String?

    public init(
        id: String,
        sessionId: String,
        callerId: String,
        calleeId: String,
        contactId: String,
        contactName: String,
        contactAvatar: String? = nil,
        callType: String,
        status: String,
        durationSeconds: Int,
        timestamp: Int64,
        endedAt: String? = nil
    ) {
        self.id = id
        self.sessionId = sessionId
        self.callerId = callerId
        self.calleeId = calleeId
        self.contactId = contactId
        self.contactName = contactName
        self.contactAvatar = contactAvatar
        self.callType = callType
        self.status = status
        self.durationSeconds = durationSeconds
        self.timestamp = timestamp
        self.endedAt = endedAt
    }
}
