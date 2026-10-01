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
                let chats = LiveJSON.chats(from: data)
                for chat in chats {
                    let name = chat.consistentName()
                    if !name.isEmpty {
                        chat.otherUserId.map { GlobalCallState.shared.updateContactInfo(id: $0, name: name, phone: chat.phoneNumber) }
                        GlobalCallState.shared.updateContactInfo(id: chat.id, name: name, phone: chat.phoneNumber)
                    }
                }
                self.uiState.chats = .success(chats)
                self.uiState.isChatsFetching = false
            } catch {
                self.uiState.chats = .error(error.localizedDescription)
                self.uiState.isChatsFetching = false
            }
        }
    }

    public func fetchMessages(chatId: String) {
        activeChatId = chatId
        markAsRead(chatId: chatId)
        Task {
            do {
                let data = try await API.data("conversations/\(chatId)/messages")
                let msgs = LiveJSON.messages(from: data, myUserId: SessionStore.shared.userId)
                self.uiState.messages = .success(msgs)
            } catch {
                self.uiState.messages = .success([])
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
            fileName: fileName,
            fileSize: fileSize,
            duration: duration,
            contactName: contactName,
            contactPhone: contactPhone,
            latitude: latitude,
            longitude: longitude,
            replyToId: replyToId,
            replyToText: replyToText,
            replyToSenderName: replyToSenderName
        )

        var current = uiState.messages.data ?? []
        current.append(msg)
        uiState.messages = .success(current)

        Task {
            let clientUUID = msg.id
            let wireType = messageType.replacingOccurrences(of: "-", with: "_").uppercased()
            var bodyObj: [String: Any] = [
                "client_uuid": clientUUID,
                "message_text": body,
                "media_type": wireType,
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
            do {
                _ = try await API.data("conversations/\(chatId)/messages", method: "POST", json: bodyObj)
                if var msgs = self.uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == msg.id }) {
                    msgs[idx].status = .delivered
                    self.uiState.messages = .success(msgs)
                }
            } catch {
                self.uiState.error = error.localizedDescription
                if var msgs = self.uiState.messages.data, let idx = msgs.firstIndex(where: { $0.id == msg.id }) {
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
        Task {
            if let data = try? await API.data("chats/group", method: "POST", json: ["name": name, "member_ids": contactIds]),
               let chat = try? JSONDecoder().decode(ChatMessage.self, from: data) {
                onSuccess(chat)
                fetchChats()
            }
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

    public func connectCallSocket() {
        guard let token = SessionStore.shared.token, !token.isEmpty else { return }
        CallWebSocketClient.shared.connect(listener: CallSocketBridge.shared, serverUrl: AppBackend.wsBaseURL, authToken: token)
    }

    public func startVoiceOrVideoCall(receiverId: String, callType: CallType, name: String) {
        if receiverId.isEmpty { return }
        if !name.isEmpty {
            GlobalCallState.shared.updateContactInfo(id: receiverId, name: name, phone: nil)
        }
        connectCallSocket()
        Task {
            if let data = try? await API.data("calls/ice_servers") {
                WebRtcCallManager.shared.setIceServers(Self.iceServers(from: data))
            }
            do {
                let data = try await API.data("calls/initiate", method: "POST", json: [
                    "receiver_id": receiverId,
                    "call_type": callType.wireValue
                ])
                let sessionId = LiveJSON.string(LiveJSON.object(data)["session_id"])
                guard !sessionId.isEmpty else {
                    self.uiState.error = "Couldn't start the call. No call was placed."
                    return
                }
                GlobalCallState.shared.setCallActive(true, sessionId: sessionId, participantId: receiverId, callType: callType)
                WebRtcCallManager.shared.start(callType: callType)
                CallTonePlayer.shared.playOutgoingRingtone()
                CallWebSocketClient.shared.invite(receiverId: receiverId, sessionId: sessionId, callType: callType)
                CallKitService.shared.startOutgoingCall(sessionId: sessionId, participantId: receiverId, participantName: name, callType: callType)
            } catch {
                GlobalCallState.shared.setCallActive(false, sessionId: nil, participantId: nil, callType: callType)
                self.uiState.error = error.localizedDescription
            }
        }
    }

    private static func iceServers(from data: Data) -> [[String: Any]] {
        let json = try? JSONSerialization.jsonObject(with: data)
        let rows: [Any]
        if let list = json as? [Any] {
            rows = list
        } else if let obj = json as? [String: Any] {
            rows = (obj["ice_servers"] as? [Any]) ?? (obj["iceServers"] as? [Any]) ?? []
        } else {
            rows = []
        }
        return rows.compactMap { $0 as? [String: Any] }
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

final class CallSocketBridge: CallSocketListener {
    static let shared = CallSocketBridge()
    func onOpen() {}
    func onEvent(_ event: CallEvent) {}
    func onError(_ error: String) {}
}
