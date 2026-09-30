import Foundation
import SwiftUI
import Combine

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

    public var isLoading: Boolean {
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
        fetchCurrentUser()
        fetchChats()
        refreshBlockedUsers()
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
        // Fetch current user logic
    }

    public func fetchChats() {
        uiState.isChatsFetching = true
        // Simulated network sync
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.uiState.isChatsFetching = false
        }
    }

    public func fetchMessages(chatId: String) {
        activeChatId = chatId
        markAsRead(chatId: chatId)
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
        let msg = Message(
            id: clientId ?? UUID().uuidString,
            conversationId: chatId,
            content: body,
            senderId: uiState.currentUser?.id ?? "me",
            senderName: uiState.currentUser?.displayName ?? "You",
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            isFromMe: true,
            status: .sending,
            type: MessageType(rawValue: messageType.uppercased()) ?? .text,
            mediaUrls: mediaUrls,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName,
            duration: duration,
            fileName: fileName,
            fileSize: fileSize,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            audioAmplitudes: audioAmplitudes
        )

        var currentMsgs = uiState.messages.data ?? []
        currentMsgs.append(msg)
        uiState.messages = .success(currentMsgs)
    }

    public func startVoiceOrVideoCall(receiverId: String, callType: CallType, name: String? = nil) {
        guard !receiverId.isEmpty else { return }
        GlobalCallState.shared.setCallActive(true, sessionId: UUID().uuidString, participantId: receiverId, callType: callType)
        if let name = name {
            GlobalCallState.shared.updateContactInfo(id: receiverId, name: name, phone: nil)
        }
    }

    public func markAsRead(chatId: String) {
        // Mark conversation messages as read
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
            msgs[idx].reactions[uiState.currentUser?.id ?? "me"] = emoji
            uiState.messages = .success(msgs)
        }
    }

    public func starMessage(messageId: String, isStarred: Bool) {
        if var msgs = uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == messageId }) {
            msgs[idx].isStarred = isStarred
            uiState.messages = .success(msgs)
        }
    }

    public func searchInChat(chatId: String, query: String) {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            uiState.chatSearchResults = []
            return
        }
        let msgs = uiState.messages.data ?? []
        uiState.chatSearchResults = msgs.filter { $0.content.localizedCaseInsensitiveContains(query) }
    }

    public func createGroup(name: String, contactIds: [String], onSuccess: @escaping (ChatMessage) -> Void) {
        let groupChat = ChatMessage(
            id: UUID().uuidString,
            senderName: name,
            lastMessage: "Group created",
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            type: .group,
            isGroup: true
        )
        onSuccess(groupChat)
        fetchChats()
    }

    public func pinChat(chatId: String, isPinned: Bool) { fetchChats() }
    public func muteChat(chatId: String, isMuted: Bool) { fetchChats() }
    public func archiveChat(chatId: String, isArchived: Bool) { fetchChats() }
    public func clearChat(chatId: String) { uiState.messages = .success([]) }
    public func clearAllChats() { fetchChats() }

    public func refreshBlockedUsers() {}
    public func blockUser(userId: String, onDone: @escaping (Bool) -> Void = { _ in }) {
        blockedUserIds.insert(userId)
        onDone(true)
    }
    public func unblockUser(userId: String, onDone: @escaping (Bool) -> Void = { _ in }) {
        blockedUserIds.remove(userId)
        onDone(true)
    }
    public func reportUser(userId: String, reason: String, onDone: @escaping (Bool) -> Void = { _ in }) {
        onDone(true)
    }

    public func sendTyping(_ isTyping: Bool) {
        guard let chatId = activeChatId else { return }
        uiState.typingStatus[chatId] = isTyping
    }

    public func setPlayingAudio(_ uri: String?) {
        currentlyPlayingAudio = uri
    }

    public func sendMediaMessage(chatId: String, fileUrl: URL, type: String, name: String? = nil, duration: String? = nil) {
        sendMessage(
            chatId: chatId,
            body: name ?? type.capitalized,
            messageType: type,
            mediaUrls: [fileUrl.absoluteString],
            duration: duration,
            fileName: fileUrl.lastPathComponent
        )
    }
}
