import Foundation

public struct Contact: Identifiable, Codable, Equatable, Hashable, Sendable {
    public var id: String
    public var username: String
    public var displayName: String
    public var avatarUrl: String?
    public var phoneNumber: String
    public var isRegistered: Bool
    public var isAuthenticated: Bool
    public var isVerified: Bool
    public var bio: String?
    public var publicKey: String?
    public var isFollowing: Bool

    public init(
        id: String,
        username: String,
        displayName: String,
        avatarUrl: String? = nil,
        phoneNumber: String = "",
        isRegistered: Bool = false,
        isAuthenticated: Bool = true,
        isVerified: Bool = false,
        bio: String? = nil,
        publicKey: String? = nil,
        isFollowing: Bool = false
    ) {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.avatarUrl = avatarUrl
        self.phoneNumber = phoneNumber
        self.isRegistered = isRegistered
        self.isAuthenticated = isAuthenticated
        self.isVerified = isVerified
        self.bio = bio
        self.publicKey = publicKey
        self.isFollowing = isFollowing
    }
}
