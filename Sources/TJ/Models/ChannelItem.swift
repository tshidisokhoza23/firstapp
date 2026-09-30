import Foundation

public struct ChannelItem: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var name: String
    public var description: String
    public var followers: String
    public var avatarUrl: String?
    public var isVerified: Bool
    public var isSubscribed: Bool

    public init(
        id: String,
        name: String,
        description: String = "",
        followers: String = "0",
        avatarUrl: String? = nil,
        isVerified: Bool = false,
        isSubscribed: Bool = false
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.followers = followers
        self.avatarUrl = avatarUrl
        self.isVerified = isVerified
        self.isSubscribed = isSubscribed
    }
}
