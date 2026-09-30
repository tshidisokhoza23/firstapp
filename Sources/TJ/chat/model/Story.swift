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

    public static func decodeFeed(_ data: Data) -> [UserStory] {
        if let decoded = try? JSONDecoder().decode(StoryFeedResponse.self, from: data) {
            return decoded.stories
        }
        guard let json = try? JSONSerialization.jsonObject(with: data) else { return [] }
        let raw: [Any]
        if let obj = json as? [String: Any], let list = obj["stories"] as? [Any] {
            raw = list
        } else if let list = json as? [Any] {
            raw = list
        } else {
            return []
        }
        return raw.compactMap { ($0 as? [String: Any]).flatMap(UserStory.init(json:)) }
    }

    init?(json: [String: Any]) {
        let userId = StoryJSON.string(json["user_id"]) ?? StoryJSON.string(json["userId"]) ?? ""
        guard !userId.isEmpty || json["stories"] != nil else { return nil }
        self.id = StoryJSON.string(json["id"]) ?? userId
        self.userId = userId.isEmpty ? self.id : userId
        self.userName = StoryJSON.string(json["user_name"]) ?? StoryJSON.string(json["userName"]) ?? "Story"
        self.phoneNumber = StoryJSON.string(json["phone_number"]) ?? StoryJSON.string(json["phoneNumber"])
        self.userAvatarUrl = StoryJSON.string(json["avatar_url"]) ?? StoryJSON.string(json["userAvatarUrl"])
        self.userAvatarColorHex = StoryJSON.string(json["userAvatarColorHex"]) ?? "#546E7A"
        self.isMuted = StoryJSON.bool(json["is_muted"]) ?? StoryJSON.bool(json["isMuted"]) ?? false
        self.lastUpdated = StoryJSON.string(json["latest_at"]) ?? StoryJSON.string(json["lastUpdated"]) ?? ""
        let items = (json["stories"] as? [Any]) ?? []
        self.stories = items.compactMap { ($0 as? [String: Any]).flatMap(StoryItem.init(json:)) }
    }

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

    init?(json: [String: Any]) {
        guard let id = StoryJSON.string(json["id"]), !id.isEmpty else { return nil }
        self.id = id
        self.content = StoryJSON.string(json["content"]) ?? ""
        self.contentUrl = StoryJSON.string(json["content_url"]) ?? StoryJSON.string(json["contentUrl"])
        let rawType = StoryJSON.string(json["story_type"]) ?? StoryJSON.string(json["type"]) ?? "text"
        self.type = StoryType.parse(rawType)
        self.createdAt = StoryJSON.int64(json["created_at"]) ?? StoryJSON.int64(json["createdAt"]) ?? 0
        self.timestamp = StoryJSON.string(json["timestamp"]) ?? (createdAt > 0 ? String(createdAt) : "")
        self.backgroundColorArgb = StoryJSON.int64(json["backgroundColorArgb"]) ?? 0xFF546E7A
        self.isViewed = StoryJSON.bool(json["is_viewed"]) ?? StoryJSON.bool(json["isViewed"]) ?? false
        self.durationMs = StoryJSON.int(json["duration_ms"]) ?? StoryJSON.int(json["durationMs"]) ?? 5000
        self.expiresAt = StoryJSON.string(json["expires_at"]) ?? StoryJSON.string(json["expiresAt"]) ?? ""
        self.viewsCount = StoryJSON.int(json["views_count"]) ?? StoryJSON.int(json["viewsCount"]) ?? 0
    }
}

private struct StoryFeedResponse: Decodable {
    let stories: [UserStory]
}

private enum StoryJSON {
    static func string(_ value: Any?) -> String? {
        if let value = value as? String, !value.isEmpty { return value }
        if let value = value as? NSNumber { return value.stringValue }
        return nil
    }

    static func bool(_ value: Any?) -> Bool? {
        if let value = value as? Bool { return value }
        if let value = value as? NSNumber { return value.boolValue }
        return nil
    }

    static func int(_ value: Any?) -> Int? {
        if let value = value as? Int { return value }
        if let value = value as? NSNumber { return value.intValue }
        if let value = value as? String { return Int(value) }
        return nil
    }

    static func int64(_ value: Any?) -> Int64? {
        if let value = value as? Int64 { return value }
        if let value = value as? NSNumber { return value.int64Value }
        if let value = value as? String { return Int64(value) }
        return nil
    }
}

extension StoryType {
    static func parse(_ raw: String) -> StoryType {
        switch raw.lowercased() {
        case "image", "photo": return .image
        case "video": return .video
        case "ad": return .ad
        case "text": return .text
        default: return StoryType(rawValue: raw.uppercased()) ?? .text
        }
    }
}
