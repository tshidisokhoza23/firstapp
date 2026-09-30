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
        switch type {
        case .group:
            return "Group"
        case .channel:
            return "Channel"
        default:
            return ""
        }
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
        if trimmed.isEmpty || isIdString(trimmed, key: key) { return nil }
        return trimmed
    }
}

private func isIdString(_ text: String, key: String) -> Bool {
    return text == key || text.lowercased() == key.lowercased()
}
