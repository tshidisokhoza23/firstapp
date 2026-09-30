import Foundation
import SwiftUI

public enum ConversationType: String, Codable, Sendable {
    case direct = "DIRECT"
    case group = "GROUP"
    case channel = "CHANNEL"
    case jobChat = "JOB_CHAT"
    case mingle = "MINGLE"
}

public struct Conversation: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var type: ConversationType
    public var title: String
    public var description: String
    public var lastMessage: String
    public var timestamp: Int64
    public var unreadCount: Int
    public var lastSeen: Int64
    public var avatarUrl: String?
    public var avatarColorHex: String
    public var isOnline: Bool
    public var isMuted: Bool
    public var isPinned: Bool
    public var isArchived: Bool
    public var isAdmin: Bool
    public var isVerified: Bool
    public var isSubscribed: Bool
    public var createdBy: String?
    public var followers: String
    public var category: String
    public var lastMessageIsFromMe: Bool
    public var lastMessageRead: Bool
    public var publicKey: String?
    public var messages: [Message]

    public init(
        id: String,
        type: ConversationType,
        title: String,
        description: String = "",
        lastMessage: String = "",
        timestamp: Int64,
        unreadCount: Int = 0,
        lastSeen: Int64 = 0,
        avatarUrl: String? = nil,
        avatarColorHex: String = "#808080",
        isOnline: Bool = false,
        isMuted: Bool = false,
        isPinned: Bool = false,
        isArchived: Bool = false,
        isAdmin: Bool = false,
        isVerified: Bool = false,
        isSubscribed: Bool = false,
        createdBy: String? = nil,
        followers: String = "0",
        category: String = "Community",
        lastMessageIsFromMe: Bool = false,
        lastMessageRead: Bool = false,
        publicKey: String? = nil,
        messages: [Message] = []
    ) {
        self.id = id
        self.type = type
        self.title = title
        self.description = description
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.lastSeen = lastSeen
        self.avatarUrl = avatarUrl
        self.avatarColorHex = avatarColorHex
        self.isOnline = isOnline
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.isArchived = isArchived
        self.isAdmin = isAdmin
        self.isVerified = isVerified
        self.isSubscribed = isSubscribed
        self.createdBy = createdBy
        self.followers = followers
        self.category = category
        self.lastMessageIsFromMe = lastMessageIsFromMe
        self.lastMessageRead = lastMessageRead
        self.publicKey = publicKey
        self.messages = messages
    }

    public func canPost(myUserId: String?) -> Bool {
        switch type {
        case .direct, .jobChat, .mingle, .group:
            return true
        case .channel:
            return isAdmin || (createdBy != nil && createdBy == myUserId)
        }
    }
}
