import Foundation

extension Notification.Name {
    public static let didUpdateChats = Notification.Name("didUpdateChats")
    public static let didUpdateMessages = Notification.Name("didUpdateMessages")
}

// MARK: - Local Entities

public struct ChatEntity: Codable, Sendable, Identifiable {
    public let id: String
    public var type: ConversationType
    public var senderName: String
    public var lastMessage: String
    public var timestamp: Int64
    public var unreadCount: Int
    public var avatarUrl: String?
    public var otherUserId: String?
    public var isOnline: Bool
    public var isMuted: Bool
    public var isPinned: Bool
    public var isTyping: Bool
    public var isAdmin: Bool
    public var isSubscribed: Bool
    public var adminsCanPost: Bool
    public var createdBy: String?
    public var phoneNumber: String?
    public var lastMessageIsFromMe: Bool
    public var lastMessageRead: Bool
    public var pinnedMessageId: String?
    public var lastSeen: Int64
    public var disappearingMessagesDuration: Int
    public var isGroup: Bool
    public var isArchived: Bool
    public var wallpaperUrl: String?
    public var customNotificationTone: String?
    public var mediaVisibility: Bool

    public init(
        id: String,
        type: ConversationType = .direct,
        senderName: String = "",
        lastMessage: String = "",
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        unreadCount: Int = 0,
        avatarUrl: String? = nil,
        otherUserId: String? = nil,
        isOnline: Bool = false,
        isMuted: Bool = false,
        isPinned: Bool = false,
        isTyping: Bool = false,
        isAdmin: Bool = false,
        isSubscribed: Bool = false,
        adminsCanPost: Bool = false,
        createdBy: String? = nil,
        phoneNumber: String? = nil,
        lastMessageIsFromMe: Bool = false,
        lastMessageRead: Bool = false,
        pinnedMessageId: String? = nil,
        lastSeen: Int64 = 0,
        disappearingMessagesDuration: Int = 0,
        isGroup: Bool = false,
        isArchived: Bool = false,
        wallpaperUrl: String? = nil,
        customNotificationTone: String? = nil,
        mediaVisibility: Bool = true
    ) {
        self.id = id
        self.type = type
        self.senderName = senderName
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.avatarUrl = avatarUrl
        self.otherUserId = otherUserId
        self.isOnline = isOnline
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.isTyping = isTyping
        self.isAdmin = isAdmin
        self.isSubscribed = isSubscribed
        self.adminsCanPost = adminsCanPost
        self.createdBy = createdBy
        self.phoneNumber = phoneNumber
        self.lastMessageIsFromMe = lastMessageIsFromMe
        self.lastMessageRead = lastMessageRead
        self.pinnedMessageId = pinnedMessageId
        self.lastSeen = lastSeen
        self.disappearingMessagesDuration = disappearingMessagesDuration
        self.isGroup = isGroup
        self.isArchived = isArchived
        self.wallpaperUrl = wallpaperUrl
        self.customNotificationTone = customNotificationTone
        self.mediaVisibility = mediaVisibility
    }
}

public struct MessageEntity: Codable, Sendable, Identifiable {
    public let id: String
    public var conversationId: String
    public var content: String
    public var senderId: String
    public var timestamp: Int64
    public var isFromMe: Bool
    public var status: MessageStatus
    public var clientId: String?
    public var type: MessageType
    public var mediaUrls: [String]
    public var duration: String?
    public var fileName: String?
    public var fileSize: String?
    public var contactName: String?
    public var contactPhone: String?
    public var latitude: Double?
    public var longitude: Double?
    public var replyToId: String?
    public var replyToText: String?
    public var replyToSenderName: String?
    public var isStarred: Bool
    public var isDelivered: Bool
    public var isRead: Bool
    public var reactions: [String: Int]
    public var scheduledAt: Int64?

    public init(
        id: String,
        conversationId: String,
        content: String,
        senderId: String,
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        isFromMe: Bool = false,
        status: MessageStatus = .sent,
        clientId: String? = nil,
        type: MessageType = .text,
        mediaUrls: [String] = [],
        duration: String? = nil,
        fileName: String? = nil,
        fileSize: String? = nil,
        contactName: String? = nil,
        contactPhone: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        replyToId: String? = nil,
        replyToText: String? = nil,
        replyToSenderName: String? = nil,
        isStarred: Bool = false,
        isDelivered: Bool = false,
        isRead: Bool = false,
        reactions: [String: Int] = [:],
        scheduledAt: Int64? = nil
    ) {
        self.id = id
        self.conversationId = conversationId
        self.content = content
        self.senderId = senderId
        self.timestamp = timestamp
        self.isFromMe = isFromMe
        self.status = status
        self.clientId = clientId
        self.type = type
        self.mediaUrls = mediaUrls
        self.duration = duration
        self.fileName = fileName
        self.fileSize = fileSize
        self.contactName = contactName
        self.contactPhone = contactPhone
        self.latitude = latitude
        self.longitude = longitude
        self.replyToId = replyToId
        self.replyToText = replyToText
        self.replyToSenderName = replyToSenderName
        self.isStarred = isStarred
        self.isDelivered = isDelivered
        self.isRead = isRead
        self.reactions = reactions
        self.scheduledAt = scheduledAt
    }
}

public struct ContactEntity: Codable, Sendable, Identifiable {
    public let id: String
    public var username: String
    public var displayName: String
    public var avatarUrl: String?
    public var phoneNumber: String
    public var isRegistered: Bool

    public init(
        id: String,
        username: String = "",
        displayName: String = "",
        avatarUrl: String? = nil,
        phoneNumber: String = "",
        isRegistered: Bool = false
    ) {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.avatarUrl = avatarUrl
        self.phoneNumber = phoneNumber
        self.isRegistered = isRegistered
    }
}

public struct AdvertEntity: Codable, Sendable, Identifiable {
    public let id: String
    public var title: String
    public var description: String
    public var imageUrl: String?
    public var videoUrl: String?
    public var ctaUrl: String?
    public var isViewed: Bool
    public var likesCount: Int
    public var sharesCount: Int

    public init(
        id: String,
        title: String = "",
        description: String = "",
        imageUrl: String? = nil,
        videoUrl: String? = nil,
        ctaUrl: String? = nil,
        isViewed: Bool = false,
        likesCount: Int = 0,
        sharesCount: Int = 0
    ) {
        self.id = id
        self.title = title
        self.description = description
        self.imageUrl = imageUrl
        self.videoUrl = videoUrl
        self.ctaUrl = ctaUrl
        self.isViewed = isViewed
        self.likesCount = likesCount
        self.sharesCount = sharesCount
    }
}

public struct CallLogEntity: Codable, Sendable, Identifiable {
    public let id: String
    public var sessionId: String?
    public var callerId: String?
    public var calleeId: String?
    public var contactId: String?
    public var contactName: String?
    public var contactAvatar: String?
    public var callType: String?
    public var status: String?
    public var durationSeconds: Int
    public var startedAt: Int64
    public var endedAt: Int64?

    public init(
        id: String,
        sessionId: String? = nil,
        callerId: String? = nil,
        calleeId: String? = nil,
        contactId: String? = nil,
        contactName: String? = nil,
        contactAvatar: String? = nil,
        callType: String? = nil,
        status: String? = nil,
        durationSeconds: Int = 0,
        startedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        endedAt: Int64? = nil
    ) {
        self.id = id
        self.sessionId = sessionId
        self.callerId = callerId
        self.calleeId = calleeId
        self.contactId = contactId
        self.contactName = contactName
        self.contactAvatar = contactAvatar
        self.callType = callType
        self.status = status
        self.durationSeconds = durationSeconds
        self.startedAt = startedAt
        self.endedAt = endedAt
    }
}

public struct SyncCursorEntity: Codable, Sendable, Identifiable {
    public var id: String { entityType }
    public let entityType: String
    public var lastSyncedSequence: Int64
    public var lastSyncedAt: Int64

    public init(entityType: String, lastSyncedSequence: Int64, lastSyncedAt: Int64) {
        self.entityType = entityType
        self.lastSyncedSequence = lastSyncedSequence
        self.lastSyncedAt = lastSyncedAt
    }
}

// MARK: - Converters Namespace

public enum LocalConverters {
    public static func jsonString<T: Encodable>(_ object: T) -> String? {
        guard let data = try? JSONEncoder().encode(object) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    public static func jsonObject<T: Decodable>(_ json: String, as type: T.Type) -> T? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}

// MARK: - Swift Actor Local Database

public actor LocalDatabase {
    public static let shared = LocalDatabase()
    private var chats: [String: ChatEntity] = [:]
    private var messages: [String: MessageEntity] = [:]
    private var contacts: [String: ContactEntity] = [:]
    private var adverts: [String: AdvertEntity] = [:]
    private var callLogs: [String: CallLogEntity] = [:]
    private var syncCursors: [String: SyncCursorEntity] = [:]

    private var currentUserId: String?

    public init(userId: String? = nil) {
        self.currentUserId = userId
        restore()
    }

    public func setUserId(_ userId: String) {
        guard currentUserId != userId else { return }
        currentUserId = userId
        chats.removeAll()
        messages.removeAll()
        contacts.removeAll()
        adverts.removeAll()
        callLogs.removeAll()
        syncCursors.removeAll()
        restore()
    }

    public func clearAll() {
        chats.removeAll()
        messages.removeAll()
        contacts.removeAll()
        adverts.removeAll()
        callLogs.removeAll()
        syncCursors.removeAll()
        AppDiskCache.remove(name: storeName())
    }

    private func storeName() -> String {
        if let currentUserId, !currentUserId.isEmpty {
            return "local-db-\(currentUserId)"
        }
        return "local-db"
    }

    private func restore() {
        guard let snap = AppDiskCache.loadCodable(LocalSnapshot.self, name: storeName()) else { return }
        chats = snap.chats
        messages = snap.messages
        contacts = snap.contacts
        adverts = snap.adverts
        callLogs = snap.callLogs
        syncCursors = snap.syncCursors
    }

    private func persist() {
        if messages.count > 3000 {
            let pending = messages.values.filter { $0.status == .pending || $0.status == .sending }
            let recent = messages.values.sorted { $0.timestamp > $1.timestamp }.prefix(3000)
            var kept: [String: MessageEntity] = [:]
            for message in pending + recent {
                kept[message.id] = message
            }
            messages = kept
        }
        AppDiskCache.saveCodable(LocalSnapshot(
            chats: chats,
            messages: messages,
            contacts: contacts,
            adverts: adverts,
            callLogs: callLogs,
            syncCursors: syncCursors
        ), name: storeName())
    }

    // MARK: - Chats

    public func upsertChats(_ list: [ChatEntity]) {
        for chat in list {
            chats[chat.id] = chat
        }
        persist()
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .didUpdateChats, object: nil)
        }
    }

    public func getChats() -> [ChatEntity] {
        return Array(chats.values).sorted { $0.timestamp > $1.timestamp }
    }

    public func getChat(id: String) -> ChatEntity? {
        return chats[id]
    }

    public func deleteChat(id: String) {
        chats.removeValue(forKey: id)
        persist()
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .didUpdateChats, object: nil)
        }
    }

    public func updateLastMessage(chatId: String, text: String, time: Int64, isFromMe: Bool, isRead: Bool) {
        if var existing = chats[chatId] {
            existing.lastMessage = text
            existing.timestamp = max(existing.timestamp, time)
            existing.lastMessageIsFromMe = isFromMe
            existing.lastMessageRead = isRead
            chats[chatId] = existing
            persist()
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didUpdateChats, object: nil)
            }
        }
    }

    public func incrementUnread(chatId: String) {
        if var existing = chats[chatId] {
            existing.unreadCount += 1
            chats[chatId] = existing
            persist()
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didUpdateChats, object: nil)
            }
        }
    }

    public func clearUnread(chatId: String) {
        if var existing = chats[chatId] {
            existing.unreadCount = 0
            chats[chatId] = existing
            persist()
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didUpdateChats, object: nil)
            }
        }
    }

    // MARK: - Messages

    public func upsertMessages(_ list: [MessageEntity]) {
        var convIds = Set<String>()
        for msg in list {
            messages[msg.id] = msg
            convIds.insert(msg.conversationId)
        }
        persist()
        DispatchQueue.main.async {
            for convId in convIds {
                NotificationCenter.default.post(name: .didUpdateMessages, object: convId)
            }
            NotificationCenter.default.post(name: .didUpdateChats, object: nil)
        }
    }

    public func getMessages(conversationId: String) -> [MessageEntity] {
        return messages.values
            .filter { $0.conversationId == conversationId }
            .sorted { $0.timestamp < $1.timestamp }
    }

    public func getMessage(id: String) -> MessageEntity? {
        return messages[id]
    }

    public func deleteMessage(id: String) {
        messages.removeValue(forKey: id)
        persist()
    }

    public func clearMessages(conversationId: String) {
        let toRemove = messages.values.filter { $0.conversationId == conversationId }.map { $0.id }
        for id in toRemove {
            messages.removeValue(forKey: id)
        }
        persist()
    }

    public func getPendingMessages() -> [MessageEntity] {
        return messages.values.filter { $0.status == .pending || $0.status == .sending }
    }

    // MARK: - Contacts

    public func upsertContacts(_ list: [ContactEntity]) {
        for c in list {
            contacts[c.id] = c
        }
        persist()
    }

    public func getContacts() -> [ContactEntity] {
        return Array(contacts.values).sorted { $0.displayName < $1.displayName }
    }

    public func getContact(id: String) -> ContactEntity? {
        return contacts[id]
    }

    // MARK: - Adverts

    public func upsertAdverts(_ list: [AdvertEntity]) {
        for a in list {
            adverts[a.id] = a
        }
        persist()
    }

    public func getAdverts() -> [AdvertEntity] {
        return Array(adverts.values)
    }

    public func getAdvert(id: String) -> AdvertEntity? {
        return adverts[id]
    }

    public func markAdvertViewed(id: String) {
        if var existing = adverts[id] {
            existing.isViewed = true
            adverts[id] = existing
            persist()
        }
    }

    // MARK: - Cursors

    public func getSyncCursor(type: String) -> SyncCursorEntity? {
        return syncCursors[type]
    }

    public func upsertSyncCursor(_ cursor: SyncCursorEntity) {
        syncCursors[cursor.entityType] = cursor
        persist()
    }
}

private struct LocalSnapshot: Codable {
    var chats: [String: ChatEntity]
    var messages: [String: MessageEntity]
    var contacts: [String: ContactEntity]
    var adverts: [String: AdvertEntity]
    var callLogs: [String: CallLogEntity]
    var syncCursors: [String: SyncCursorEntity]
}
