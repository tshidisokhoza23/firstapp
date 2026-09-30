import Foundation

public struct AppUser: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var username: String
    public var displayName: String
    public var email: String
    public var phoneNumber: String
    public var avatarUrl: String?
    public var bio: String?
    public var isVerified: Bool

    public init(
        id: String,
        username: String = "",
        displayName: String = "",
        email: String = "",
        phoneNumber: String = "",
        avatarUrl: String? = nil,
        bio: String? = nil,
        isVerified: Bool = false
    ) {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.email = email
        self.phoneNumber = phoneNumber
        self.avatarUrl = avatarUrl
        self.bio = bio
        self.isVerified = isVerified
    }
}
