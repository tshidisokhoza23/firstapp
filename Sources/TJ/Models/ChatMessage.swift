import Foundation
import SwiftUI

public struct ChatMessage: Identifiable, Codable, Equatable, Hashable, Sendable {
    public var id: String
    public var type: ConversationType
    public var senderName: String
    public var localContactName: String?
    public var serverProfileName: String?
    public var lastMessage: String
    public var timestamp: Int64
    public var unreadCount: Int
    public var avatarUrl: String?
    public var avatarColorHex: String
    public var isOnline: Bool
    public var lastSeen: Int64
    public var isMuted: Bool
    public var isPinned: Bool
    public var pinnedMessageId: String?
    public var isTyping: Bool
    public var isGroup: Bool
    public var lastMessageIsFromMe: Bool
    public var lastMessageRead: Bool
    public var lastMessageStatus: MessageStatus
    public var isArchived: Bool
    public var isAdmin: Bool
    public var adminsCanPost: Bool
    public var isVerified: Bool
    public var isSubscribed: Bool
    public var createdBy: String?
    public var followers: String
    public var category: String
    public var wallpaperUrl: String?
    public var customNotificationTone: String?
    public var mediaVisibility: Bool
    public var disappearingMessagesDuration: Int
    public var publicKey: String?
    public var phoneNumber: String?
    public var otherUserId: String?
    public var isMingle: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case type
        case senderName = "sender_name"
        case localContactName = "local_contact_name"
        case serverProfileName = "server_profile_name"
        case lastMessage = "last_message"
        case timestamp = "updated_at"
        case unreadCount = "unread_count"
        case avatarUrl = "avatar_url"
        case avatarColorHex = "avatar_color_hex"
        case isOnline = "is_online"
        case lastSeen = "last_seen"
        case isMuted = "is_muted"
        case isPinned = "is_pinned"
        case pinnedMessageId = "pinned_message_id"
        case isTyping = "is_typing"
        case isGroup = "is_group"
        case lastMessageIsFromMe = "last_message_is_from_me"
        case lastMessageRead = "last_message_read"
        case lastMessageStatus = "last_message_status"
        case isArchived = "is_archived"
        case isAdmin = "is_admin"
        case adminsCanPost = "admins_can_post"
        case isVerified = "is_verified"
        case isSubscribed = "is_subscribed"
        case createdBy = "created_by"
        case followers
        case category
        case wallpaperUrl = "wallpaper_url"
        case customNotificationTone = "custom_notification_tone"
        case mediaVisibility = "media_visibility"
        case disappearingMessagesDuration = "disappearing_messages_duration"
        case publicKey = "public_key"
        case phoneNumber = "phone_number"
        case otherUserId = "other_user_id"
        case isMingle = "is_mingle"
    }

    public init(
        id: String,
        type: ConversationType = .direct,
        senderName: String = "",
        localContactName: String? = nil,
        serverProfileName: String? = nil,
        lastMessage: String = "",
        timestamp: Int64 = 0,
        unreadCount: Int = 0,
        avatarUrl: String? = nil,
        avatarColorHex: String = "#808080",
        isOnline: Bool = false,
        lastSeen: Int64 = 0,
        isMuted: Bool = false,
        isPinned: Bool = false,
        pinnedMessageId: String? = nil,
        isTyping: Bool = false,
        isGroup: Bool = false,
        lastMessageIsFromMe: Bool = false,
        lastMessageRead: Bool = false,
        lastMessageStatus: MessageStatus = .sent,
        isArchived: Bool = false,
        isAdmin: Bool = false,
        adminsCanPost: Bool = false,
        isVerified: Bool = false,
        isSubscribed: Bool = false,
        createdBy: String? = nil,
        followers: String = "0",
        category: String = "Community",
        wallpaperUrl: String? = nil,
        customNotificationTone: String? = nil,
        mediaVisibility: Bool = true,
        disappearingMessagesDuration: Int = 0,
        publicKey: String? = nil,
        phoneNumber: String? = nil,
        otherUserId: String? = nil,
        isMingle: Bool = false
    ) {
        self.id = id
        self.type = type
        self.senderName = senderName
        self.localContactName = localContactName
        self.serverProfileName = serverProfileName
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.avatarUrl = avatarUrl
        self.avatarColorHex = avatarColorHex
        self.isOnline = isOnline
        self.lastSeen = lastSeen
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.pinnedMessageId = pinnedMessageId
        self.isTyping = isTyping
        self.isGroup = isGroup
        self.lastMessageIsFromMe = lastMessageIsFromMe
        self.lastMessageRead = lastMessageRead
        self.lastMessageStatus = lastMessageStatus
        self.isArchived = isArchived
        self.isAdmin = isAdmin
        self.adminsCanPost = adminsCanPost
        self.isVerified = isVerified
        self.isSubscribed = isSubscribed
        self.createdBy = createdBy
        self.followers = followers
        self.category = category
        self.wallpaperUrl = wallpaperUrl
        self.customNotificationTone = customNotificationTone
        self.mediaVisibility = mediaVisibility
        self.disappearingMessagesDuration = disappearingMessagesDuration
        self.publicKey = publicKey
        self.phoneNumber = phoneNumber
        self.otherUserId = otherUserId
        self.isMingle = isMingle
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = (try? container.decode(String.self, forKey: .id)) ?? UUID().uuidString
        let typeStr = (try? container.decode(String.self, forKey: .type)) ?? "DIRECT"
        type = ConversationType(rawValue: typeStr.uppercased()) ?? .direct

        senderName = (try? container.decode(String.self, forKey: .senderName)) ?? ""
        localContactName = try container.decodeIfPresent(String.self, forKey: .localContactName)
        serverProfileName = try container.decodeIfPresent(String.self, forKey: .serverProfileName)
        lastMessage = (try? container.decode(String.self, forKey: .lastMessage)) ?? ""

        if let ts = try? container.decode(Int64.self, forKey: .timestamp) {
            timestamp = ts
        } else if let ts = try? container.decode(Double.self, forKey: .timestamp) {
            timestamp = Int64(ts)
        } else {
            timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        }

        unreadCount = (try? container.decode(Int.self, forKey: .unreadCount)) ?? 0
        avatarUrl = try container.decodeIfPresent(String.self, forKey: .avatarUrl)
        avatarColorHex = (try? container.decode(String.self, forKey: .avatarColorHex)) ?? "#808080"
        isOnline = (try? container.decode(Bool.self, forKey: .isOnline)) ?? false
        lastSeen = (try? container.decode(Int64.self, forKey: .lastSeen)) ?? 0
        isMuted = (try? container.decode(Bool.self, forKey: .isMuted)) ?? false
        isPinned = (try? container.decode(Bool.self, forKey: .isPinned)) ?? false
        pinnedMessageId = try container.decodeIfPresent(String.self, forKey: .pinnedMessageId)
        isTyping = (try? container.decode(Bool.self, forKey: .isTyping)) ?? false
        isGroup = (try? container.decode(Bool.self, forKey: .isGroup)) ?? (type == .group)
        lastMessageIsFromMe = (try? container.decode(Bool.self, forKey: .lastMessageIsFromMe)) ?? false
        lastMessageRead = (try? container.decode(Bool.self, forKey: .lastMessageRead)) ?? false

        let statusStr = (try? container.decode(String.self, forKey: .lastMessageStatus)) ?? "SENT"
        lastMessageStatus = MessageStatus(rawValue: statusStr.uppercased()) ?? .sent

        isArchived = (try? container.decode(Bool.self, forKey: .isArchived)) ?? false
        isAdmin = (try? container.decode(Bool.self, forKey: .isAdmin)) ?? false
        adminsCanPost = (try? container.decode(Bool.self, forKey: .adminsCanPost)) ?? false
        isVerified = (try? container.decode(Bool.self, forKey: .isVerified)) ?? false
        isSubscribed = (try? container.decode(Bool.self, forKey: .isSubscribed)) ?? false
        createdBy = try container.decodeIfPresent(String.self, forKey: .createdBy)
        followers = (try? container.decode(String.self, forKey: .followers)) ?? "0"
        category = (try? container.decode(String.self, forKey: .category)) ?? "Community"
        wallpaperUrl = try container.decodeIfPresent(String.self, forKey: .wallpaperUrl)
        customNotificationTone = try container.decodeIfPresent(String.self, forKey: .customNotificationTone)
        mediaVisibility = (try? container.decode(Bool.self, forKey: .mediaVisibility)) ?? true
        disappearingMessagesDuration = (try? container.decode(Int.self, forKey: .disappearingMessagesDuration)) ?? 0
        publicKey = try container.decodeIfPresent(String.self, forKey: .publicKey)
        phoneNumber = try container.decodeIfPresent(String.self, forKey: .phoneNumber)
        otherUserId = try container.decodeIfPresent(String.self, forKey: .otherUserId)
        isMingle = (try? container.decode(Bool.self, forKey: .isMingle)) ?? false
    }

    public var isSavedContact: Bool {
        let key = otherUserId ?? id
        if let local = localContactName, !local.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return true
        }
        if !senderName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isIdString(senderName, key: key) {
            return true
        }
        if let server = serverProfileName, !server.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isIdString(server, key: key) {
            return true
        }
        return false
    }

    public var displayName: String {
        let key = otherUserId ?? id
        if let local = localContactName?.trimmingCharacters(in: .whitespacesAndNewlines), !local.isEmpty {
            return local
        }
        let profile = serverProfileName?.takeIfValidName(key: key) ?? senderName.takeIfValidName(key: key)
        if let profile = profile, !profile.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return profile.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if let phone = phoneNumber, !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return CountryUtils.formatDisplayPhone(phone)
        }
        return ""
    }

    public var profileHint: String? {
        if isSavedContact { return nil }
        let key = otherUserId ?? id
        let profile = serverProfileName?.takeIfValidName(key: key) ?? senderName.takeIfValidName(key: key)
        if let profile = profile, !profile.isEmpty && profile != displayName {
            return "~\(profile)"
        }
        return nil
    }

    public func consistentName() -> String {
        let current = displayName
        let key = otherUserId ?? id
        if !current.isEmpty && !isIdString(current, key: key) {
            return current
        }
        return phoneNumber ?? ""
    }
}

private extension String {
    func takeIfValidName(key: String) -> String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        let blocked = ["chat", "direct", "mingle", "mingle user", "unknown", "group", "channel", "tj"]
        if trimmed.isEmpty || isIdString(trimmed, key: key) || blocked.contains(trimmed.lowercased()) { return nil }
        return trimmed
    }
}

private func isIdString(_ text: String, key: String) -> Bool {
    return text == key || text.lowercased() == key.lowercased()
}
