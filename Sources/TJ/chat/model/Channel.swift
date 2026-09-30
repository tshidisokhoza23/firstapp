import Foundation
import SwiftUI

public struct Channel: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var name: String
    public var description: String
    public var followers: String
    public var avatarUrl: String?
    public var avatarColorHex: String
    public var isVerified: Bool
    public var isSubscribed: Bool
    public var isMuted: Bool
    public var isAdmin: Bool
    public var adminsCanPost: Bool
    public var category: String
    public var createdBy: String?
    public var lastPostText: String
    public var lastPostImageUrl: String?
    public var lastPostImageColorHex: String
    public var lastPostTime: String
    public var conversationId: String?
    public var isTopInCountry: Bool
    public var countryCode: String

    public init(
        id: String,
        name: String,
        description: String = "",
        followers: String,
        avatarUrl: String? = nil,
        avatarColorHex: String = "#808080",
        isVerified: Bool = false,
        isSubscribed: Bool = false,
        isMuted: Bool = false,
        isAdmin: Bool = false,
        adminsCanPost: Bool = false,
        category: String = "Community",
        createdBy: String? = nil,
        lastPostText: String = "",
        lastPostImageUrl: String? = nil,
        lastPostImageColorHex: String = "#808080",
        lastPostTime: String = "",
        conversationId: String? = nil,
        isTopInCountry: Bool = false,
        countryCode: String = ""
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.followers = followers
        self.avatarUrl = avatarUrl
        self.avatarColorHex = avatarColorHex
        self.isVerified = isVerified
        self.isSubscribed = isSubscribed
        self.isMuted = isMuted
        self.isAdmin = isAdmin
        self.adminsCanPost = adminsCanPost
        self.category = category
        self.createdBy = createdBy
        self.lastPostText = lastPostText
        self.lastPostImageUrl = lastPostImageUrl
        self.lastPostImageColorHex = lastPostImageColorHex
        self.lastPostTime = lastPostTime
        self.conversationId = conversationId
        self.isTopInCountry = isTopInCountry
        self.countryCode = countryCode
    }
}
