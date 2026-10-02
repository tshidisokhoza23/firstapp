import Foundation
import Combine

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
    public static let shared = ChatRepositoryImpl(
        chatApi: ChatApi(),
        localDb: LocalDatabase.shared,
        webSocketManager: WebSocketManager.shared,
        currentUserIdProvider: { SessionStore.shared.userId }
    )
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
        try? await webSocketManager.send(type: "subscribe", payload: userTopicPayload)
        try? await webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("presence")])
        try? await webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("chat")])
        try? await webSocketManager.send(type: "subscribe", payload: ["topic": WSAnyCodable("channels")])

        let activeChats = await localDb.getChats()
        for chat in activeChats {
            let topicPayload: [String: WSAnyCodable] = ["topic": WSAnyCodable("chat:\(chat.id)")]
            try? await webSocketManager.send(type: "subscribe", payload: topicPayload)
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

            let mediaType = payload["media_type"]?.stringValue ?? payload["message_type"]?.stringValue ?? "TEXT"
            var mediaUrls = payload["media_urls"]?.arrayValue?.compactMap(\.stringValue) ?? []
            if mediaUrls.isEmpty, let one = payload["media_url"]?.stringValue, !one.isEmpty {
                mediaUrls = [one]
            }
            let msg = Message(
                id: msgId,
                conversationId: convId,
                content: text,
                senderId: senderId,
                timestamp: payload["created_at"]?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000),
                isFromMe: senderId == myUserId,
                type: chatMediaType(mediaType),
                mediaUrls: mediaUrls,
                fileName: payload["file_name"]?.stringValue,
                duration: payload["duration"]?.stringValue
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
            if let userId = payload["user_id"]?.stringValue {
                let statusOnline = (payload["status"]?.stringValue == "online") || (payload["is_online"]?.boolValue == true)
                let chats = await localDb.getChats()
                if var chat = chats.first(where: { $0.otherUserId == userId }) {
                    chat.isOnline = statusOnline
                    await localDb.upsertChats([chat])
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
            type: mType,
            mediaUrls: mediaUrls,
            fileName: fileName,
            fileSize: fileSize,
            duration: duration,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            clientId: clientUuid,
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
        if !mediaUrls.isEmpty {
            payload["media_urls"] = WSAnyCodable(mediaUrls.map { WSAnyCodable($0) })
        }
        if let duration, !duration.isEmpty { payload["duration"] = WSAnyCodable(duration) }
        if let fileName, !fileName.isEmpty { payload["file_name"] = WSAnyCodable(fileName) }
        if let replyToId = replyToId { payload["reply_to_id"] = WSAnyCodable(replyToId) }

        // Voice notes and files must go through REST. The live socket stores
        // media URLs but drops duration, so a socket-only send would not play back.
        let needsRest = !mediaUrls.isEmpty || duration != nil
        var sentLive = false
        if !needsRest {
            sentLive = (try? await webSocketManager.send(type: "send_message", payload: payload)) != nil
        }

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
                return .error(error.localizedDescription, pendingMsg)
            }
        }
    }

    public func syncDelta() async -> Resource<Int64> {
        do {
            let cursor = await localDb.getSyncCursor(type: "messages")
            let since = cursor?.lastSyncedSequence ?? 0
            let response = try await chatApi.syncDelta(since: since)

            let myUserId = currentUserIdProvider() ?? ""
            for apiMsg in response.messages {
                let msg = chatMessage(from: apiMsg, conversationId: apiMsg.conversationId, myUserId: myUserId)
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
                chatMessage(from: api, conversationId: chatId, myUserId: myUserId).toEntity(chatId: chatId)
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
            var entities = res.chats.map { summary -> ChatEntity in
                let rawType = summary.type.uppercased()
                let type = ConversationType(rawValue: rawType) ?? .direct
                let ids = [summary.conversationId, summary.contactId ?? ""]
                let phone = summary.phoneNumber ?? ""
                let local = phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(phone)
                let isRoom = rawType.contains("GROUP") || rawType.contains("CHANNEL")
                let name = isRoom
                    ? realPersonLabel(names: [summary.title], ids: ids)
                    : realPersonLabel(local: local, names: [summary.contactName, summary.title], phone: phone, ids: ids)
                return ChatEntity(
                    id: summary.conversationId,
                    type: type,
                    senderName: name,
                    lastMessage: summary.lastMessage,
                    timestamp: summary.time,
                    unreadCount: summary.unreadCount,
                    avatarUrl: summary.avatarUrl,
                    otherUserId: summary.contactId,
                    isOnline: summary.isOnline,
                    isMuted: summary.isMuted,
                    isPinned: summary.isPinned,
                    isAdmin: summary.isAdmin,
                    isSubscribed: summary.isSubscribed,
                    adminsCanPost: summary.adminsCanPost,
                    createdBy: summary.createdBy,
                    phoneNumber: summary.phoneNumber,
                    lastSeen: summary.lastSeen
                )
            }
            for index in entities.indices {
                let row = entities[index]
                guard row.type == .direct || row.type == .jobChat || row.type == .mingle else { continue }
                guard let userId = row.otherUserId, !userId.isEmpty else { continue }
                let ids = [userId, row.id]
                let current = realPersonLabel(names: [row.senderName], phone: row.phoneNumber, ids: ids)
                if !current.isEmpty && !looksLikePhone(current) { continue }
                guard let profile = await LiveJSON.userProfile(userId) else { continue }
                let phone = profile.phone.isEmpty ? (row.phoneNumber ?? "") : profile.phone
                let local = phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(phone)
                let resolved = realPersonLabel(local: local, names: [profile.displayName, profile.username], phone: phone, ids: ids)
                guard !resolved.isEmpty else { continue }
                entities[index].senderName = resolved
                entities[index].phoneNumber = phone.isEmpty ? row.phoneNumber : phone
            }
            await localDb.upsertChats(entities)
            return .success(true)
        } catch {
            return .error(error.localizedDescription)
        }
    }

    public func markAsRead(chatId: String) async -> Resource<Bool> {
        await localDb.clearUnread(chatId: chatId)
        try? await webSocketManager.send(type: "read_ack", payload: ["conversation_id": WSAnyCodable(chatId)])
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
            return .error(error.localizedDescription, msg)
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

private extension ChatEntity {
    func toModel() -> ChatMessage {
        ChatMessage(
            id: id,
            type: type,
            senderName: senderName,
            lastMessage: lastMessage,
            timestamp: timestamp,
            unreadCount: unreadCount,
            avatarUrl: avatarUrl,
            isOnline: isOnline,
            lastSeen: lastSeen,
            isMuted: isMuted,
            isPinned: isPinned,
            pinnedMessageId: pinnedMessageId,
            isTyping: isTyping,
            isGroup: isGroup,
            lastMessageIsFromMe: lastMessageIsFromMe,
            lastMessageRead: lastMessageRead,
            isArchived: isArchived,
            isAdmin: isAdmin,
            adminsCanPost: adminsCanPost,
            isSubscribed: isSubscribed,
            createdBy: createdBy,
            wallpaperUrl: wallpaperUrl,
            customNotificationTone: customNotificationTone,
            mediaVisibility: mediaVisibility,
            disappearingMessagesDuration: disappearingMessagesDuration,
            phoneNumber: phoneNumber,
            otherUserId: otherUserId
        )
    }
}

private func chatMediaType(_ raw: String) -> MessageType {
    let upper = raw.uppercased()
    if upper == "AUDIO" || upper == "AUDIO_MESSAGE" { return .audioMessage }
    return MessageType(rawValue: upper) ?? .text
}

private func chatMessage(from api: ApiMessage, conversationId: String, myUserId: String) -> Message {
    var urls = api.mediaUrls ?? []
    if urls.isEmpty, let one = api.mediaUrl, !one.isEmpty { urls = [one] }
    return Message(
        id: api.id,
        conversationId: conversationId.isEmpty ? api.conversationId : conversationId,
        content: api.messageText,
        senderId: api.senderId,
        senderName: api.senderName,
        timestamp: api.createdAt,
        isFromMe: api.senderId == myUserId,
        type: chatMediaType(api.mediaType),
        mediaUrls: urls,
        fileName: api.fileName,
        fileSize: api.fileSize,
        duration: api.duration,
        contactName: api.contactName,
        contactPhone: api.contactPhone,
        latitude: api.latitude,
        longitude: api.longitude,
        isRead: api.isRead ?? false,
        isDelivered: api.isDelivered ?? false,
        isStarred: api.isStarred ?? false,
        replyToId: api.replyToId,
        replyToText: api.replyToText,
        replyToSenderName: api.replyToSenderName,
        reactions: api.reactions ?? [:],
        clientId: api.clientUuid,
        audioAmplitudes: api.audioAmplitudes
    )
}

private extension ChatMessage {
    func toEntity() -> ChatEntity {
        ChatEntity(
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
            disappearingMessagesDuration: disappearingMessagesDuration,
            isGroup: isGroup,
            isArchived: isArchived,
            wallpaperUrl: wallpaperUrl,
            customNotificationTone: customNotificationTone,
            mediaVisibility: mediaVisibility
        )
    }
}

private extension MessageEntity {
    func toModel() -> Message {
        Message(
            id: id,
            conversationId: conversationId,
            content: content,
            senderId: senderId,
            timestamp: timestamp,
            isFromMe: isFromMe,
            status: status,
            type: type,
            mediaUrls: mediaUrls,
            fileName: fileName,
            fileSize: fileSize,
            duration: duration,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            isRead: isRead,
            isDelivered: isDelivered,
            isStarred: isStarred,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            reactions: reactions,
            clientId: clientId,
            scheduledAt: scheduledAt
        )
    }
}

private extension Message {
    func toEntity(chatId: String) -> MessageEntity {
        MessageEntity(
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

private extension AdvertEntity {
    func toModel() -> Advert {
        Advert(
            id: id,
            advertiserName: title,
            title: title,
            contentUrl: imageUrl ?? videoUrl,
            contentText: description,
            targetUrl: ctaUrl ?? "",
            isViewed: isViewed
        )
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
    func resetAds() async -> Resource<Bool>
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

    public func resetAds() async -> Resource<Bool> {
        let adverts = await localDb.getAdverts()
        let resetEntities = adverts.map { a in
            var updated = a
            updated.isViewed = false
            return updated
        }
        await localDb.upsertAdverts(resetEntities)
        return .success(true)
    }
}
