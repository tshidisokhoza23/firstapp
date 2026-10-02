import Foundation
import SwiftUI
import Combine
import AVFoundation

// MARK: - Chat UI State & Intent Models

public enum ResourceState<T> {
    case loading(T? = nil)
    case success(T)
    case error(String, T? = nil)

    public var data: T? {
        switch self {
        case .loading(let data), .error(_, let data):
            return data
        case .success(let data):
            return data
        }
    }

    public var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }
}

public struct ChatUiState {
    public var currentUser: User? = nil
    public var chats: ResourceState<[ChatMessage]> = .loading(nil)
    public var archivedChats: ResourceState<[ChatMessage]> = .loading(nil)
    public var messages: ResourceState<[Message]> = .loading(nil)
    public var isChatsFetching: Bool = false
    public var starredMessages: ResourceState<[Message]> = .loading(nil)
    public var chatSearchResults: [Message] = []
    public var selectedContactsForGroup: [Contact] = []
    public var closedConversationIds: Set<String> = []
    public var typingStatus: [String: Bool] = [:]
    public var groupInfo: ResourceState<GroupInfoResponse> = .loading(nil)
    public var isLoading: Bool = false
    public var error: String? = nil

    public init() {}
}

public enum ChatIntent {
    case fetchChats
    case fetchMessages(chatId: String)
    case sendMessage(
        chatId: String,
        body: String,
        messageType: String = "text",
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
        replyToSenderName: String? = nil
    )
    case markAsRead(chatId: String)
    case deleteMessage(messageId: String, deleteForEveryone: Bool = false)
    case editMessage(messageId: String, newBody: String)
    case reactToMessage(messageId: String, emoji: String)
    case searchInChat(chatId: String, query: String)
    case setSelectedContactsForGroup(contacts: [Contact])
    case createGroup(name: String, contactIds: [String], onSuccess: (ChatMessage) -> Void)
    case archiveChat(chatId: String, isArchived: Bool)
    case pinChat(chatId: String, isPinned: Bool)
    case muteChat(chatId: String, isMuted: Bool)
    case clearChat(chatId: String)
    case starMessage(messageId: String, isStarred: Bool)
    case clearAllChats
}

// MARK: - ChatViewModel

@MainActor
public class ChatViewModel: ObservableObject {
    @Published public var uiState: ChatUiState = ChatUiState()
    @Published public var activeChatId: String? = nil
    @Published public var activeSubTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards, 4: DM
    @Published public var currentlyPlayingAudio: String? = nil
    private var audioPlayer: AVAudioPlayer?
    private var livePoll: Task<Void, Never>?
    @Published public var blockedUserIds: Set<String> = []
    @Published public var contactUser: User? = nil

    // Settings
    @Published public var fontSize: CGFloat = 16.0
    @Published public var enterIsSend: Bool = false
    @Published public var animationsEnabled: Bool = true
    @Published public var showReadReceipts: Bool = true

    private var cancellables = Set<AnyCancellable>()
    private var groupNameCache: [String: String] = [:]

    public init() {
        fontSize = ChatPrefs.fontSize()
        enterIsSend = ChatPrefs.enterIsSend()
        fetchCurrentUser()
        fetchChats()
        refreshBlockedUsers()
        observeRealtimeUpdates()
    }

    private func observeRealtimeUpdates() {
        NotificationCenter.default.publisher(for: .didUpdateChats)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.fetchChats()
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: TJEvents.clearLocalChats)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.uiState.chats = .success([])
                self?.uiState.messages = .success([])
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: .didUpdateMessages)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] notification in
                guard let self = self else { return }
                if let convId = notification.object as? String {
                    if self.activeChatId == convId {
                        self.fetchMessages(chatId: convId)
                    }
                } else if let activeId = self.activeChatId {
                    self.fetchMessages(chatId: activeId)
                }
                self.fetchChats()
            }
            .store(in: &cancellables)

        WebSocketManager.shared.connect()
        Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                self?.fetchChats()
            }
        }
        _ = AppRuntime.chatRepository
        WebSocketManager.shared.eventsSubject
            .receive(on: DispatchQueue.main)
            .sink { [weak self] event in
                self?.applyRealtime(event)
            }
            .store(in: &cancellables)
    }

    private func applyRealtime(_ event: WSEnvelope) {
        let type = event.type ?? ""
        guard type == "message_new" || type == "message_ack" || type == "new_message" || type == "INCOMING_MSG" else { return }
        let payload = event.payload ?? [:]
        func text(_ key: String) -> String { payload[key]?.stringValue ?? "" }
        let conversationId = text("conversation_id")
        guard !conversationId.isEmpty else { return }
        let clientId = text("client_msg_id").isEmpty ? text("client_uuid") : text("client_msg_id")
        let messageId = text("message_id").isEmpty ? text("id") : text("message_id")
        let body = text("message_text").isEmpty ? text("content") : text("message_text")
        let senderId = text("sender_id")
        let created = payload["created_at"]?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000)
        if activeChatId == conversationId {
            if var msgs = uiState.messages.data,
               let idx = msgs.firstIndex(where: { (!clientId.isEmpty && ($0.clientId == clientId || $0.id == clientId)) || (!messageId.isEmpty && $0.id == messageId) }) {
                if !messageId.isEmpty { msgs[idx].id = messageId }
                if !body.isEmpty { msgs[idx].content = body }
                msgs[idx].status = .delivered
                uiState.messages = .success(msgs)
            } else if type != "message_ack", !body.isEmpty || !messageId.isEmpty {
                var msgs = uiState.messages.data ?? []
                let mine = !senderId.isEmpty && senderId == SessionStore.shared.userId
                msgs.append(Message(
                    id: messageId.isEmpty ? UUID().uuidString : messageId,
                    conversationId: conversationId,
                    content: body,
                    senderId: senderId,
                    senderName: text("sender_name"),
                    timestamp: created,
                    isFromMe: mine,
                    status: .delivered,
                    type: MessageType(rawValue: text("media_type").uppercased()) ?? .text,
                    clientId: clientId.isEmpty ? nil : clientId
                ))
                uiState.messages = .success(msgs)
            }
            fetchMessages(chatId: conversationId)
        }
        fetchChats()
    }

    public func setActiveSubTab(_ tabIndex: Int) {
        activeSubTab = tabIndex
    }

    public func handleIntent(_ intent: ChatIntent) {
        switch intent {
        case .fetchChats:
            fetchChats()
        case .fetchMessages(let chatId):
            fetchMessages(chatId: chatId)
        case .sendMessage(let chatId, let body, let type, let media, let duration, let fileName, let fileSize, let contactName, let contactPhone, let lat, let lng, let replyId, let replyText, let replySender):
            sendMessage(chatId: chatId, body: body, messageType: type, mediaUrls: media, duration: duration, fileName: fileName, fileSize: fileSize, contactName: contactName, contactPhone: contactPhone, latitude: lat, longitude: lng, replyToId: replyId, replyToText: replyText, replyToSenderName: replySender)
        case .markAsRead(let chatId):
            markAsRead(chatId: chatId)
        case .deleteMessage(let messageId, let forEveryone):
            deleteMessage(messageId: messageId, deleteForEveryone: forEveryone)
        case .editMessage(let messageId, let newBody):
            editMessage(messageId: messageId, newBody: newBody)
        case .reactToMessage(let messageId, let emoji):
            reactToMessage(messageId: messageId, emoji: emoji)
        case .searchInChat(let chatId, let query):
            searchInChat(chatId: chatId, query: query)
        case .setSelectedContactsForGroup(let contacts):
            uiState.selectedContactsForGroup = contacts
        case .createGroup(let name, let contactIds, let onSuccess):
            createGroup(name: name, contactIds: contactIds, onSuccess: onSuccess)
        case .archiveChat(let chatId, let isArchived):
            archiveChat(chatId: chatId, isArchived: isArchived)
        case .pinChat(let chatId, let isPinned):
            pinChat(chatId: chatId, isPinned: isPinned)
        case .muteChat(let chatId, let isMuted):
            muteChat(chatId: chatId, isMuted: isMuted)
        case .clearChat(let chatId):
            clearChat(chatId: chatId)
        case .starMessage(let messageId, let isStarred):
            starMessage(messageId: messageId, isStarred: isStarred)
        case .clearAllChats:
            clearAllChats()
        }
    }

    public func fetchCurrentUser() {
        Task {
            if let user = SessionStore.shared.user {
                uiState.currentUser = user
            } else if let data = try? await API.data("users/me"),
                      let env = try? JSONDecoder().decode(UserEnvelope.self, from: data) {
                uiState.currentUser = env.user
                SessionStore.shared.updateUser(env.user)
            }
        }
    }

    public func fetchChats() {
        uiState.isChatsFetching = true
        Task {
            do {
                let data = try await API.data("conversations")
                let chats = await LiveJSON.enrichMissingProfiles(LiveJSON.chats(from: data))
                self.uiState.chats = .success(chats)
                self.uiState.isChatsFetching = false
            } catch {
                self.uiState.isChatsFetching = false
                if self.uiState.chats.data == nil {
                    self.uiState.error = "Could not load chats"
                }
            }
        }
    }

    public func fetchMessages(chatId: String) {
        activeChatId = chatId
        markAsRead(chatId: chatId)
        livePoll?.cancel()
        livePoll = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                if self.activeChatId != chatId { return }
                await self.loadMessages(chatId)
                try? await Task.sleep(nanoseconds: 2_000_000_000)
            }
        }
    }

    private func loadMessages(_ chatId: String) async {
        do {
            let data = try await API.data("conversations/\(chatId)/messages")
            let server = LiveJSON.messages(from: data, myUserId: SessionStore.shared.userId)
            let local = uiState.messages.data ?? []
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            var merged = server
            for msg in local {
                let exists = merged.contains { $0.id == msg.id || ($0.clientId != nil && $0.clientId == msg.clientId) }
                if exists { continue }
                if msg.status == .sending || msg.status == .failed || now - msg.timestamp < 120_000 {
                    merged.append(msg)
                }
            }
            merged.sort { $0.timestamp < $1.timestamp }
            uiState.messages = .success(merged)
        } catch {
            uiState.error = "Could not load messages"
            if uiState.messages.data == nil {
                uiState.messages = .error("Could not load messages", [])
            }
        }
    }

    public func clearActiveChat() {
        activeChatId = nil
        uiState.messages = .success([])
    }

    public func sendMessage(
        chatId: String,
        body: String,
        messageType: String = "text",
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
        audioAmplitudes: [Int]? = nil,
        scheduledAt: Int64? = nil
    ) {
        let outgoingId = clientId ?? UUID().uuidString
        let msg = Message(
            id: outgoingId,
            conversationId: chatId,
            content: body,
            senderId: uiState.currentUser?.id ?? "me",
            senderName: uiState.currentUser?.displayName ?? "You",
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            isFromMe: true,
            status: .sending,
            type: MessageType(rawValue: messageType.uppercased()) ?? .text,
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
            clientId: outgoingId
        )

        var current = uiState.messages.data ?? []
        current.append(msg)
        uiState.messages = .success(current)

        Task {
            let clientId = msg.id
            let wireBody = replyToId?.hasPrefix("story_") == true
                ? MessageMarkers.storyWire(text: body, replyToId: replyToId, replyToText: replyToText, author: replyToSenderName)
                : body
            var bodyObj: [String: Any] = [
                "client_uuid": clientId,
                "message_text": wireBody,
                "media_type": messageType.uppercased(),
                "media_urls": mediaUrls
            ]
            if let duration { bodyObj["duration"] = duration }
            if let fileName { bodyObj["file_name"] = fileName }
            if let fileSize { bodyObj["file_size"] = fileSize }
            if let contactName { bodyObj["contact_name"] = contactName }
            if let contactPhone { bodyObj["contact_phone"] = contactPhone }
            if let latitude { bodyObj["latitude"] = latitude }
            if let longitude { bodyObj["longitude"] = longitude }
            if let replyToId { bodyObj["reply_to_id"] = replyToId }
            if let replyToText { bodyObj["reply_to_text"] = replyToText }
            if let replyToSenderName { bodyObj["reply_to_sender_name"] = replyToSenderName }
            WebSocketManager.shared.connect()
            do {
                _ = try await API.data("conversations/\(chatId)/messages", method: "POST", json: bodyObj)
                if var msgs = self.uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == clientId || $0.clientId == clientId }) {
                    msgs[idx].status = .delivered
                    self.uiState.messages = .success(msgs)
                }
            } catch {
                self.uiState.error = "Message was not sent"
                if var msgs = self.uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == clientId }) {
                    msgs[idx].status = .failed
                    self.uiState.messages = .success(msgs)
                }
            }
        }
    }

    public func markAsRead(chatId: String) {
        Task {
            _ = try? await API.data("conversations/\(chatId)/read", method: "POST", json: [:])
        }
    }

    public func deleteMessage(messageId: String, deleteForEveryone: Bool = false) {
        if var msgs = uiState.messages.data {
            msgs.removeAll { $0.id == messageId }
            uiState.messages = .success(msgs)
        }
    }

    public func editMessage(messageId: String, newBody: String) {
        if var msgs = uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == messageId }) {
            msgs[idx].content = newBody
            uiState.messages = .success(msgs)
        }
    }

    public func reactToMessage(messageId: String, emoji: String) {
        if var msgs = uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == messageId }) {
            msgs[idx].reactions[emoji, default: 0] += 1
            uiState.messages = .success(msgs)
        }
    }

    public func searchInChat(chatId: String, query: String) {
        guard !query.isEmpty, let msgs = uiState.messages.data else {
            uiState.chatSearchResults = []
            return
        }
        uiState.chatSearchResults = msgs.filter { $0.content.localizedCaseInsensitiveContains(query) }
    }

    public func createGroup(name: String, contactIds: [String], onSuccess: @escaping (ChatMessage) -> Void) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            guard let data = try? await API.data("chats/group", method: "POST", json: ["name": trimmed, "member_ids": contactIds]),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
            let id = LiveJSON.string(json["conversation_id"]).nilIfEmpty ?? LiveJSON.string(json["id"])
            guard !id.isEmpty else { return }
            let title = LiveJSON.string(json["title"]).nilIfEmpty ?? trimmed
            onSuccess(ChatMessage(id: id, type: .group, senderName: title, isGroup: true))
            fetchChats()
        }
    }

    public func archiveChat(chatId: String, isArchived: Bool) {
        if var list = uiState.chats.data, let idx = list.firstIndex(where: { $0.id == chatId }) {
            list[idx].isArchived = isArchived
            uiState.chats = .success(list)
        }
    }

    public func pinChat(chatId: String, isPinned: Bool) {
        if var list = uiState.chats.data, let idx = list.firstIndex(where: { $0.id == chatId }) {
            list[idx].isPinned = isPinned
            uiState.chats = .success(list)
        }
    }

    public func muteChat(chatId: String, isMuted: Bool) {
        if var list = uiState.chats.data, let idx = list.firstIndex(where: { $0.id == chatId }) {
            list[idx].isMuted = isMuted
            uiState.chats = .success(list)
        }
    }

    public func clearChat(chatId: String) {
        uiState.messages = .success([])
    }

    public func startVoiceOrVideoCall(receiverId: String, callType: CallType, name: String) {
        guard !receiverId.isEmpty else {
            uiState.error = "This chat has no person to call"
            return
        }
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            Task { @MainActor in
                guard granted else {
                    self.uiState.error = "Microphone permission is required to call"
                    return
                }
                if callType == .video {
                    let camera = await AVCaptureDevice.requestAccess(for: .video)
                    guard camera else {
                        self.uiState.error = "Microphone and Camera permissions are required for calling"
                        return
                    }
                }
                await self.placeCall(receiverId: receiverId, callType: callType, name: name)
            }
        }
    }

    private func resolvedCalleeId(_ receiverId: String) -> String {
        if let chat = uiState.chats.data?.first(where: { $0.id == receiverId }),
           let other = chat.otherUserId, !other.isEmpty {
            return other
        }
        return receiverId
    }

    private func placeCall(receiverId: String, callType: CallType, name: String) async {
        let token = SessionStore.shared.token ?? ""
        guard !token.isEmpty else {
            uiState.error = "Sign in again before calling"
            return
        }
        let calleeId = resolvedCalleeId(receiverId)
        CallSocketBridge.shared.bind(receiverId: calleeId, sessionId: "", callType: callType)
        CallWebSocketClient.shared.connect(listener: CallSocketBridge.shared, serverUrl: AppBackend.wsBaseURL, authToken: token)
        await WebRtcCallManager.shared.prepareIce()
        do {
            let data = try await API.data("calls/initiate", method: "POST", json: [
                "receiver_id": calleeId,
                "call_type": callType.wireValue
            ])
            let sessionId = LiveJSON.string(LiveJSON.object(data)["session_id"])
            guard !sessionId.isEmpty else {
                uiState.error = "Call could not start"
                return
            }
            let targetId = resolvedCalleeId(receiverId)
            GlobalCallState.shared.updateContactInfo(id: targetId, name: name, phone: nil)
            GlobalCallState.shared.setCallActive(true, sessionId: sessionId, participantId: targetId, callType: callType)
            CallSocketBridge.shared.bind(receiverId: targetId, sessionId: sessionId, callType: callType)
            WebRtcCallManager.shared.start(callType: callType)
            CallWebSocketClient.shared.invite(receiverId: targetId, sessionId: sessionId, callType: callType)
        } catch {
            uiState.error = "Call could not start"
        }
    }

    public func starMessage(messageId: String, isStarred: Bool) {
        if var msgs = uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == messageId }) {
            msgs[idx].isStarred = isStarred
            uiState.messages = .success(msgs)
        }
    }

    public func clearAllChats() {
        uiState.chats = .success([])
    }

    public func refreshBlockedUsers() {
        blockedUserIds = []
    }

    public func blockUser(userId: String) {
        blockedUserIds.insert(userId)
        Task {
            _ = try? await API.data("users/\(userId)/block", method: "POST")
        }
    }

    public func reportUser(userId: String, reason: String) {
        Task {
            _ = try? await API.data("users/\(userId)/report", method: "POST", json: ["reason": reason])
        }
    }

    public func setPlayingAudio(_ uri: String?) {
        currentlyPlayingAudio = uri
    }
}
