import Foundation
import Combine

// MARK: - Resource Enum

public enum Resource<T: Sendable>: Sendable {
    case success(T)
    case error(String, data: T? = nil)
    case loading(data: T? = nil)

    public var data: T? {
        switch self {
        case .success(let data): return data
        case .error(_, let data): return data
        case .loading(let data): return data
        }
    }

    public var isSuccess: Bool {
        if case .success = self { return true }
        return false
    }
}

// MARK: - Domain Models

public struct ChatMessage: Codable, Sendable, Identifiable {
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
    public var isGroup: Bool
    public var isArchived: Bool
    public var lastMessageStatus: MessageStatus?

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
        isGroup: Bool = false,
        isArchived: Bool = false,
        lastMessageStatus: MessageStatus? = nil
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
        self.isGroup = isGroup
        self.isArchived = isArchived
        self.lastMessageStatus = lastMessageStatus
    }

    public func toEntity() -> ChatEntity {
        return ChatEntity(
            id: id,
            type: type,
            senderName: senderName,
            lastMessage: lastMessage,
            timestamp: timestamp,
            unreadCount: unreadCount,
            avatarUrl: avatarUrl,
            otherUserId: otherUserId,
            isOnline: isOnline,
            isMuted: isMuted,
            isPinned: isPinned,
            isTyping: isTyping,
            isAdmin: isAdmin,
            isSubscribed: isSubscribed,
            adminsCanPost: adminsCanPost,
            createdBy: createdBy,
            phoneNumber: phoneNumber,
            lastMessageIsFromMe: lastMessageIsFromMe,
            lastMessageRead: lastMessageRead,
            pinnedMessageId: pinnedMessageId,
            lastSeen: lastSeen,
            isGroup: isGroup,
            isArchived: isArchived
        )
    }
}

public extension ChatEntity {
    func toModel() -> ChatMessage {
        return ChatMessage(
            id: id,
            type: type,
            senderName: senderName,
            lastMessage: lastMessage,
            timestamp: timestamp,
            unreadCount: unreadCount,
            avatarUrl: avatarUrl,
            otherUserId: otherUserId,
            isOnline: isOnline,
            isMuted: isMuted,
            isPinned: isPinned,
            isTyping: isTyping,
            isAdmin: isAdmin,
            isSubscribed: isSubscribed,
            adminsCanPost: adminsCanPost,
            createdBy: createdBy,
            phoneNumber: phoneNumber,
            lastMessageIsFromMe: lastMessageIsFromMe,
            lastMessageRead: lastMessageRead,
            pinnedMessageId: pinnedMessageId,
            lastSeen: lastSeen,
            isGroup: isGroup,
            isArchived: isArchived
        )
    }
}

public struct Message: Codable, Sendable, Identifiable {
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

    public func toEntity(chatId: String) -> MessageEntity {
        return MessageEntity(
            id: id,
            conversationId: chatId,
            content: content,
            senderId: senderId,
            timestamp: timestamp,
            isFromMe: isFromMe,
            status: status,
            clientId: clientId,
            type: type,
            mediaUrls: mediaUrls,
            duration: duration,
            fileName: fileName,
            fileSize: fileSize,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            isStarred: isStarred,
            isDelivered: isDelivered,
            isRead: isRead,
            reactions: reactions,
            scheduledAt: scheduledAt
        )
    }
}

public extension MessageEntity {
    func toModel() -> Message {
        return Message(
            id: id,
            conversationId: conversationId,
            content: content,
            senderId: senderId,
            timestamp: timestamp,
            isFromMe: isFromMe,
            status: status,
            clientId: clientId,
            type: type,
            mediaUrls: mediaUrls,
            duration: duration,
            fileName: fileName,
            fileSize: fileSize,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            isStarred: isStarred,
            isDelivered: isDelivered,
            isRead: isRead,
            reactions: reactions,
            scheduledAt: scheduledAt
        )
    }
}

public struct Contact: Codable, Sendable, Identifiable {
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

    public func toEntity() -> ContactEntity {
        return ContactEntity(
            id: id,
            username: username,
            displayName: displayName,
            avatarUrl: avatarUrl,
            phoneNumber: phoneNumber,
            isRegistered: isRegistered
        )
    }
}

public extension ContactEntity {
    func toModel() -> Contact {
        return Contact(
            id: id,
            username: username,
            displayName: displayName,
            avatarUrl: avatarUrl,
            phoneNumber: phoneNumber,
            isRegistered: isRegistered
        )
    }
}

public struct Advert: Codable, Sendable, Identifiable {
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

    public func toEntity() -> AdvertEntity {
        return AdvertEntity(
            id: id,
            title: title,
            description: description,
            imageUrl: imageUrl,
            videoUrl: videoUrl,
            ctaUrl: ctaUrl,
            isViewed: isViewed,
            likesCount: likesCount,
            sharesCount: sharesCount
        )
    }
}

public extension AdvertEntity {
    func toModel() -> Advert {
        return Advert(
            id: id,
            title: title,
            description: description,
            imageUrl: imageUrl,
            videoUrl: videoUrl,
            ctaUrl: ctaUrl,
            isViewed: isViewed,
            likesCount: likesCount,
            sharesCount: sharesCount
        )
    }
}

// MARK: - ChatRepository Protocol

public protocol ChatRepository: Sendable {
    func sendMessage(
        chatId: String,
        body: String,
        messageType: String,
        mediaUrls: [String],
        duration: String?,
        fileName: String?,
        fileSize: String?,
        contactName: String?,
        contactPhone: String?,
        latitude: Double?,
        longitude: Double?,
        replyToId: String?,
        replyToText: String?,
        replyToSenderName: String?,
        clientId: String?,
        scheduledAt: Int64?
    ) async -> Resource<Message>

    func syncDelta() async -> Resource<Int64>
    func getChats() async -> Resource<[ChatMessage]>
    func getChatById(chatId: String) async -> Resource<ChatMessage>
    func getMessages(chatId: String) async -> Resource<[Message]>
    func fetchMessages(chatId: String) async -> Resource<Bool>
    func syncChats() async -> Resource<Bool>
    func markAsRead(chatId: String) async -> Resource<Bool>
    func deleteChat(chatId: String) async -> Resource<Bool>
    func initiateDM(contactId: String) async -> Resource<String>
    func createGroup(name: String, contactIds: [String]) async -> Resource<ChatMessage>
    func deleteMessage(messageId: String, deleteForEveryone: Bool) async -> Resource<Bool>
    func editMessage(messageId: String, newBody: String) async -> Resource<Message>
    func reactToMessage(messageId: String, emoji: String) async -> Resource<Bool>
    func pinMessage(chatId: String, messageId: String?) async -> Resource<Bool>
    func pinChat(chatId: String, isPinned: Bool) async -> Resource<Bool>
    func muteChat(chatId: String, isMuted: Bool) async -> Resource<Bool>
    func archiveChat(chatId: String, isArchived: Bool) async -> Resource<Bool>
    func starMessage(messageId: String, isStarred: Bool) async -> Resource<Bool>
    func subscribeToChannel(channelId: String) async -> Resource<Bool>
    func getICEServers() async -> Resource<[IceServerConfig]>
}

// MARK: - ChatRepositoryImpl

public final class ChatRepositoryImpl: ChatRepository, @unchecked Sendable {
    private let chatApi: ChatApi
    private let localDb: LocalDatabase
    private let webSocketManager: WebSocketManager
    private let currentUserIdProvider: @Sendable () -> String?

    private var cancellables = Set<AnyCancellable>()

    public init(
        chatApi: ChatApi,
        localDb: LocalDatabase,
        webSocketManager: WebSocketManager,
        currentUserIdProvider: @escaping @Sendable () -> String?
    ) {
        self.chatApi = chatApi
        self.localDb = localDb
        self.webSocketManager = webSocketManager
        self.currentUserIdProvider = currentUserIdProvider

        observeWebSocketEvents()
        observeConnectionState()
    }

    private func observeWebSocketEvents() {
        webSocketManager.eventsSubject
            .sink { [weak self] envelope in
                guard let self = self else { return }
                Task {
                    await self.handleIncomingEvent(envelope)
                }
            }
            .store(in: &cancellables)
    }

    private func observeConnectionState() {
        webSocketManager.connectionStateSubject
            .sink { [weak self] state in
                guard let self = self else { return }
                if state == .connected {
                    Task {
                        await self.subscribeToTopics()
                        _ = await self.syncDelta()
                    }
                }
            }
            .store(in: &cancellables)
    }

    private func subscribeToTopics() async {
        guard let myUserId = currentUserIdProvider() else { return }

        let userTopicPayload: [String: WSAnyCodable] = ["topic": WSAnyCodable("user:\(myUserId)")]
        webSocketManager.send(type: "subscribe", payload: userTopicPayload)
        webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("presence")])
        webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("chat")])
        webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("channels")])

        let activeChats = await localDb.getChats()
        for chat in activeChats {
            let topicPayload: [String: WSAnyCodable] = ["topic": WSAnyCodable("chat:\(chat.id)")]
            webSocketManager.send(type: "subscribe", payload: topicPayload)
        }
    }

    private func handleIncomingEvent(_ envelope: WSEnvelope) async {
        guard let event = envelope.type else { return }
        let payload = envelope.payload ?? [:]

        switch event {
        case "message_new":
            let convId = payload["conversation_id"]?.stringValue ?? ""
            let msgId = payload["id"]?.stringValue ?? payload["message_id"]?.stringValue ?? UUID().uuidString
            let text = payload["message_text"]?.stringValue ?? payload["content"]?.stringValue ?? ""
            let senderId = payload["sender_id"]?.stringValue ?? ""
            let myUserId = currentUserIdProvider() ?? ""

            guard !convId.isEmpty else { return }

            let msg = Message(
                id: msgId,
                conversationId: convId,
                content: text,
                senderId: senderId,
                timestamp: Int64(Date().timeIntervalSince1970 * 1000),
                isFromMe: senderId == myUserId
            )

            await localDb.upsertMessages([msg.toEntity(chatId: convId)])
            await localDb.updateLastMessage(chatId: convId, text: text, time: msg.timestamp, isFromMe: msg.isFromMe, isRead: false)
            if !msg.isFromMe {
                await localDb.incrementUnread(chatId: convId)
            }

        case "read_receipt", "read_ack":
            if let convId = payload["conversation_id"]?.stringValue {
                await localDb.clearUnread(chatId: convId)
            }

        case "presence":
            if let userId = payload["user_id"]?.stringValue,
               let isOnline = payload["status"]?.stringValue == "online" || payload["is_online"]?.boolValue == true {
                let chats = await localDb.getChats()
                if var chat = chats.first(where: { $0.otherUserId == userId }) {
                    chat.isOnline = isOnline
                    await localDb.upsertChats([chat.toEntity()])
                }
            }

        default:
            break
        }
    }

    public func sendMessage(
        chatId: String,
        body: String,
        messageType: String,
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
        clientId: String? = nil,
        scheduledAt: Int64? = nil
    ) async -> Resource<Message> {
        let clientUuid = clientId ?? UUID().uuidString
        let myUserId = currentUserIdProvider() ?? "0"
        let mType = MessageType(rawValue: messageType.uppercased()) ?? .text

        let pendingMsg = Message(
            id: clientUuid,
            conversationId: chatId,
            content: body,
            senderId: myUserId,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            isFromMe: true,
            status: .pending,
            clientId: clientUuid,
            type: mType,
            mediaUrls: mediaUrls,
            duration: duration,
            fileName: fileName,
            fileSize: fileSize,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            scheduledAt: scheduledAt
        )

        await localDb.upsertMessages([pendingMsg.toEntity(chatId: chatId)])
        await localDb.updateLastMessage(chatId: chatId, text: body, time: pendingMsg.timestamp, isFromMe: true, isRead: false)

        var payload: [String: WSAnyCodable] = [
            "client_msg_id": WSAnyCodable(clientUuid),
            "conversation_id": WSAnyCodable(chatId),
            "content": WSAnyCodable(body),
            "media_type": WSAnyCodable(messageType.uppercased())
        ]
        if let replyToId = replyToId { payload["reply_to_id"] = WSAnyCodable(replyToId) }

        let sentLive = webSocketManager.send(type: "send_message", payload: payload)

        if sentLive {
            var sentMsg = pendingMsg
            sentMsg.status = .sending
            await localDb.upsertMessages([sentMsg.toEntity(chatId: chatId)])
            return .success(sentMsg)
        } else {
            // Fallback REST call
            do {
                let req = SendMessageRequest(
                    clientUuid: clientUuid,
                    messageText: body,
                    mediaType: messageType.uppercased(),
                    mediaUrls: mediaUrls,
                    duration: duration,
                    fileName: fileName,
                    fileSize: fileSize,
                    contactName: contactName,
                    contactPhone: contactPhone,
                    latitude: latitude,
                    longitude: longitude,
                    replyToId: replyToId,
                    replyToText: replyToText,
                    replyToSenderName: replyToSenderName,
                    scheduledAt: scheduledAt
                )
                let response = try await chatApi.sendMessage(conversationID: chatId, request: req)
                var confirmedMsg = pendingMsg
                confirmedMsg.id = response.message.id
                confirmedMsg.status = .sent
                await localDb.upsertMessages([confirmedMsg.toEntity(chatId: chatId)])
                return .success(confirmedMsg)
            } catch {
                return .error(error.localizedDescription, data: pendingMsg)
            }
        }
    }

    public func syncDelta() async -> Resource<Int64> {
        do {
            let cursor = await localDb.getSyncCursor(type: "messages")
            let since = cursor?.lastSyncedSequence ?? 0
            let response = try await chatApi.syncDelta(since: since)

            for apiMsg in response.messages {
                let msg = Message(
                    id: apiMsg.id,
                    conversationId: apiMsg.conversationId,
                    content: apiMsg.messageText,
                    senderId: apiMsg.senderId,
                    timestamp: apiMsg.createdAt,
                    isFromMe: apiMsg.senderId == currentUserIdProvider()
                )
                await localDb.upsertMessages([msg.toEntity(chatId: apiMsg.conversationId)])
            }

            let newCursor = SyncCursorEntity(
                entityType: "messages",
                lastSyncedSequence: response.latestSequence,
                lastSyncedAt: Int64(Date().timeIntervalSince1970 * 1000)
            )
            await localDb.upsertSyncCursor(newCursor)
            return .success(response.latestSequence)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func getChats() async -> Resource<[ChatMessage]> {
        let entities = await localDb.getChats()
        let models = entities.map { $0.toModel() }
        return .success(models)
    }

    public func getChatById(chatId: String) async -> Resource<ChatMessage> {
        if let chat = await localDb.getChat(id: chatId) {
            return .success(chat.toModel())
        }
        return .error("Chat not found")
    }

    public func getMessages(chatId: String) async -> Resource<[Message]> {
        let entities = await localDb.getMessages(conversationId: chatId)
        return .success(entities.map { $0.toModel() })
    }

    public func fetchMessages(chatId: String) async -> Resource<Bool> {
        do {
            let res = try await chatApi.getMessages(conversationID: chatId, cursor: nil, limit: 50)
            let myUserId = currentUserIdProvider() ?? ""
            let entities = res.messages.map { api -> MessageEntity in
                let msg = Message(
                    id: api.id,
                    conversationId: chatId,
                    content: api.messageText,
                    senderId: api.senderId,
                    timestamp: api.createdAt,
                    isFromMe: api.senderId == myUserId
                )
                return msg.toEntity(chatId: chatId)
            }
            await localDb.upsertMessages(entities)
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func syncChats() async -> Resource<Bool> {
        do {
            let res = try await chatApi.getChats()
            let entities = res.chats.map { summary -> ChatEntity in
                return ChatEntity(
                    id: summary.conversationId,
                    type: ConversationType(rawValue: summary.type.uppercased()) ?? .direct,
                    senderName: summary.title,
                    lastMessage: summary.lastMessage,
                    timestamp: summary.time,
                    unreadCount: summary.unreadCount,
                    avatarUrl: summary.avatarUrl,
                    otherUserId: summary.contactId,
                    isOnline: summary.isOnline,
                    isMuted: summary.isMuted,
                    isPinned: summary.isPinned,
                    isSubscribed: summary.isSubscribed,
                    isAdmin: summary.isAdmin,
                    adminsCanPost: summary.adminsCanPost,
                    createdBy: summary.createdBy,
                    phoneNumber: summary.phoneNumber,
                    lastSeen: summary.lastSeen
                )
            }
            await localDb.upsertChats(entities)
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func markAsRead(chatId: String) async -> Resource<Bool> {
        await localDb.clearUnread(chatId: chatId)
        webSocketManager.send(type: "read_ack", payload: ["conversation_id": WSAnyCodable(chatId)])
        do {
            _ = try await chatApi.markAsRead(conversationID: chatId, request: ReadReceiptRequest(lastReadMessageId: ""))
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func deleteChat(chatId: String) async -> Resource<Bool> {
        await localDb.deleteChat(id: chatId)
        await localDb.clearMessages(conversationId: chatId)
        do {
            _ = try await chatApi.deleteConversation(conversationID: chatId)
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func initiateDM(contactId: String) async -> Resource<String> {
        do {
            let res = try await chatApi.initiateDM(request: InitiateDMRequest(contactId: contactId))
            let chat = ChatEntity(id: res.conversationId, type: .direct, otherUserId: contactId)
            await localDb.upsertChats([chat])
            return .success(res.conversationId)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func createGroup(name: String, contactIds: [String]) async -> Resource<ChatMessage> {
        do {
            let res = try await chatApi.createGroup(request: CreateGroupRequest(title: name, memberIds: contactIds))
            let group = ChatMessage(id: res.conversationId, type: .group, senderName: name, isGroup: true)
            await localDb.upsertChats([group.toEntity()])
            return .success(group)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func deleteMessage(messageId: String, deleteForEveryone: Bool) async -> Resource<Bool> {
        if let msg = await localDb.getMessage(id: messageId) {
            await localDb.deleteMessage(id: messageId)
            if deleteForEveryone {
                _ = try? await chatApi.deleteMessage(conversationID: msg.conversationId, messageID: messageId)
            }
            return .success(true)
        }
        return .error("Message not found")
    }

    public func editMessage(messageId: String, newBody: String) async -> Resource<Message> {
        guard var msg = (await localDb.getMessage(id: messageId))?.toModel() else {
            return .error("Message not found")
        }
        msg.content = newBody
        await localDb.upsertMessages([msg.toEntity(chatId: msg.conversationId)])
        do {
            let res = try await chatApi.editMessage(
                conversationID: msg.conversationId,
                messageID: messageId,
                request: EditMessageRequest(messageText: newBody)
            )
            msg.content = res.message.messageText
            return .success(msg)
        } catch {
            return .error(error.localizedDescription, data: msg)
        }
    }

    public func reactToMessage(messageId: String, emoji: String) async -> Resource<Bool> {
        guard var msgEntity = await localDb.getMessage(id: messageId) else {
            return .error("Message not found")
        }
        msgEntity.reactions[emoji] = (msgEntity.reactions[emoji] ?? 0) + 1
        await localDb.upsertMessages([msgEntity])

        do {
            let res = try await chatApi.reactToMessage(
                conversationID: msgEntity.conversationId,
                messageID: messageId,
                request: ReactMessageRequest(emoji: emoji)
            )
            msgEntity.reactions = res.reactions
            await localDb.upsertMessages([msgEntity])
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func pinMessage(chatId: String, messageId: String?) async -> Resource<Bool> {
        if var chat = await localDb.getChat(id: chatId) {
            chat.pinnedMessageId = messageId
            await localDb.upsertChats([chat])
        }
        do {
            _ = try await chatApi.pinMessage(conversationID: chatId, request: PinMessageRequest(messageId: messageId))
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func pinChat(chatId: String, isPinned: Bool) async -> Resource<Bool> {
        if var chat = await localDb.getChat(id: chatId) {
            chat.isPinned = isPinned
            await localDb.upsertChats([chat])
        }
        do {
            _ = try await chatApi.updateConversationPrefs(conversationID: chatId, request: ConversationPrefsRequest(isPinned: isPinned))
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func muteChat(chatId: String, isMuted: Bool) async -> Resource<Bool> {
        if var chat = await localDb.getChat(id: chatId) {
            chat.isMuted = isMuted
            await localDb.upsertChats([chat])
        }
        do {
            _ = try await chatApi.updateConversationPrefs(conversationID: chatId, request: ConversationPrefsRequest(isMuted: isMuted))
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func archiveChat(chatId: String, isArchived: Bool) async -> Resource<Bool> {
        if var chat = await localDb.getChat(id: chatId) {
            chat.isArchived = isArchived
            await localDb.upsertChats([chat])
        }
        do {
            _ = try await chatApi.updateConversationPrefs(conversationID: chatId, request: ConversationPrefsRequest(isArchived: isArchived))
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func starMessage(messageId: String, isStarred: Bool) async -> Resource<Bool> {
        if var msg = await localDb.getMessage(id: messageId) {
            msg.isStarred = isStarred
            await localDb.upsertMessages([msg])
        }
        do {
            if isStarred {
                _ = try await chatApi.starMessage(messageID: messageId, request: StarMessageRequest(starred: true))
            } else {
                _ = try await chatApi.unstarMessage(messageID: messageId)
            }
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func subscribeToChannel(channelId: String) async -> Resource<Bool> {
        do {
            _ = try await chatApi.subscribeToChannel(channelId: channelId)
            if var chat = await localDb.getChat(id: channelId) {
                chat.isSubscribed.toggle()
                await localDb.upsertChats([chat])
            }
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func getICEServers() async -> Resource<[IceServerConfig]> {
        do {
            let res = try await chatApi.getICEServers()
            return .success(res.iceServers)
        } catch {
            return .error(error.localizedDescription)
        }
    }
}

// MARK: - AdRepository Protocol & Implementation

public protocol AdRepository: Sendable {
    func getAdverts() async -> Resource<[Advert]>
    func getViewedAdverts() async -> Resource<[Advert]>
    func markAdvertViewed(advertId: String) async -> Resource<Bool>
    func trackAdvertClick(advertId: String) async -> Resource<Bool>
    func trackAdvertShare(advertId: String) async -> Resource<Bool>
    func toggleLikeAdvert(advertId: String) async -> Resource<Bool>
    func resetAds() async -> Resource<Void>
}

public final class AdRepositoryImpl: AdRepository, @unchecked Sendable {
    private let localDb: LocalDatabase

    public init(localDb: LocalDatabase) {
        self.localDb = localDb
    }

    public func getAdverts() async -> Resource<[Advert]> {
        let entities = await localDb.getAdverts()
        return .success(entities.map { $0.toModel() })
    }

    public func getViewedAdverts() async -> Resource<[Advert]> {
        let entities = await localDb.getAdverts().filter { $0.isViewed }
        return .success(entities.map { $0.toModel() })
    }

    public func markAdvertViewed(advertId: String) async -> Resource<Bool> {
        guard !advertId.isEmpty else {
            return .error("Invalid advert ID")
        }
        await localDb.markAdvertViewed(id: advertId)
        return .success(true)
    }

    public func trackAdvertClick(advertId: String) async -> Resource<Bool> {
        return .success(true)
    }

    public func trackAdvertShare(advertId: String) async -> Resource<Bool> {
        return .success(true)
    }

    public func toggleLikeAdvert(advertId: String) async -> Resource<Bool> {
        if var advert = await localDb.getAdvert(id: advertId) {
            advert.likesCount += 1
            await localDb.upsertAdverts([advert])
        }
        return .success(true)
    }

    public func resetAds() async -> Resource<Void> {
        let adverts = await localDb.getAdverts()
        let resetEntities = adverts.map { a in
            var updated = a
            updated.isViewed = false
            return updated
        }
        await localDb.upsertAdverts(resetEntities)
        return .success(())
    }
}
