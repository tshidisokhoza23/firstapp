import Foundation
import SwiftUI

public enum StoryType: String, Codable, Sendable {
    case text = "TEXT"
    case image = "IMAGE"
    case video = "VIDEO"
    case ad = "AD"
}

public struct UserStory: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var userId: String
    public var userName: String
    public var phoneNumber: String?
    public var userAvatarUrl: String?
    public var userAvatarColorHex: String
    public var stories: [StoryItem]
    public var lastUpdated: String
    public var isMuted: Bool

    public var allViewed: Bool {
        !stories.isEmpty && stories.allSatisfy { $0.isViewed }
    }

    public init(
        id: String,
        userId: String = "",
        userName: String,
        phoneNumber: String? = nil,
        userAvatarUrl: String? = nil,
        userAvatarColorHex: String = "#546E7A",
        stories: [StoryItem] = [],
        lastUpdated: String = "",
        isMuted: Bool = false
    ) {
        self.id = id
        self.userId = userId
        self.userName = userName
        self.phoneNumber = phoneNumber
        self.userAvatarUrl = userAvatarUrl
        self.userAvatarColorHex = userAvatarColorHex
        self.stories = stories
        self.lastUpdated = lastUpdated
        self.isMuted = isMuted
    }
}

public struct StoryItem: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var content: String
    public var contentUrl: String?
    public var type: StoryType
    public var timestamp: String
    public var createdAt: Int64
    public var backgroundColorArgb: Int64
    public var isViewed: Bool
    public var durationMs: Int
    public var expiresAt: String
    public var viewsCount: Int

    public var backgroundColor: Color {
        let argb = UInt64(backgroundColorArgb)
        let alpha = Double((argb >> 24) & 0xFF) / 255.0
        let red   = Double((argb >> 16) & 0xFF) / 255.0
        let green = Double((argb >> 8) & 0xFF) / 255.0
        let blue  = Double(argb & 0xFF) / 255.0
        return Color(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }

    public init(
        id: String,
        content: String,
        contentUrl: String? = nil,
        type: StoryType,
        timestamp: String,
        createdAt: Int64 = 0,
        backgroundColorArgb: Int64 = 0xFF546E7A,
        isViewed: Bool = false,
        durationMs: Int = 5000,
        expiresAt: String = "",
        viewsCount: Int = 0
    ) {
        self.id = id
        self.content = content
        self.contentUrl = contentUrl
        self.type = type
        self.timestamp = timestamp
        self.createdAt = createdAt
        self.backgroundColorArgb = backgroundColorArgb
        self.isViewed = isViewed
        self.durationMs = durationMs
        self.expiresAt = expiresAt
        self.viewsCount = viewsCount
    }
}
