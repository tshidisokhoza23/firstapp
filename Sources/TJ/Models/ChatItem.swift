import Foundation

public struct ChatItem: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var title: String
    public var subtitle: String
    public var timestamp: String
    public var unreadCount: Int
    public var avatarUrl: String?
    public var isOnline: Bool
    public var isMuted: Bool
    public var isPinned: Bool

    public init(
        id: String,
        title: String,
        subtitle: String = "",
        timestamp: String = "",
        unreadCount: Int = 0,
        avatarUrl: String? = nil,
        isOnline: Bool = false,
        isMuted: Bool = false,
        isPinned: Bool = false
    ) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.avatarUrl = avatarUrl
        self.isOnline = isOnline
        self.isMuted = isMuted
        self.isPinned = isPinned
    }
}
