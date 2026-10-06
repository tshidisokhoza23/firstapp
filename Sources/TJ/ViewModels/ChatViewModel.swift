import SwiftUI
import Combine

class ChatViewModel: ObservableObject {
    @Published var chats: [ChatItem] = []
    @Published var stories: [StoryItem] = []
    @Published var activeSubTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards
    @Published var activeChat: ChatItem? = nil
    @Published var activeChatMessages: [ChatMessage] = []
    @Published var messageInputText: String = ""
    @Published var isWsConnected: Bool = false

    private var cancellables = Set<AnyCancellable>()

    init() {
        Task { @MainActor in
            let remoteChats = await NetworkClient.shared.fetchChats()
            self.chats = remoteChats
        }

        WebSocketManager.shared.incomingMessagePublisher
            .sink { [weak self] envelope in
                guard let self = self, let type = envelope.type?.lowercased() else { return }

                if type == "message_new" || type == "chat_message" || type == "new_message" || type == "message" {
                    let payload = envelope.payload
                    let content = payload?["content"]?.stringValue ?? payload?["text"]?.stringValue ?? payload?["body"]?.stringValue ?? "New Message"
                    let sender = payload?["sender_name"]?.stringValue ?? payload?["sender"]?.stringValue ?? payload?["user_name"]?.stringValue ?? "Friend"
                    let chatId = payload?["chat_id"]?.stringValue ?? payload?["conversation_id"]?.stringValue ?? self.activeChat?.id ?? "c1"

                    let incoming = ChatMessage(
                        id: UUID().uuidString,
                        chatId: chatId,
                        senderName: sender,
                        content: content,
                        timestamp: "Just now",
                        isFromMe: false,
                        isAudio: false,
                        mediaUrl: nil
                    )

                    DispatchQueue.main.async {
                        self.activeChatMessages.append(incoming)
                        if let index = self.chats.firstIndex(where: { $0.id == chatId }) {
                            let oldChat = self.chats[index]
                            let updatedChat = ChatItem(
                                id: oldChat.id,
                                name: oldChat.name,
                                lastMessage: content,
                                timestamp: "Just now",
                                unreadCount: oldChat.unreadCount + 1,
                                avatarUrl: oldChat.avatarUrl,
                                isOnline: true
                            )
                            self.chats.remove(at: index)
                            self.chats.insert(updatedChat, at: 0)
                        } else {
                            let newChatCard = ChatItem(
                                id: chatId,
                                name: sender,
                                lastMessage: content,
                                timestamp: "Just now",
                                unreadCount: 1,
                                avatarUrl: "https://i.pravatar.cc/150?u=\(chatId)",
                                isOnline: true
                            )
                            self.chats.insert(newChatCard, at: 0)
                        }
                    }
                }
            }
            .store(in: &cancellables)

        Timer.publish(every: 5.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                Task { @MainActor in
                    let remoteChats = await NetworkClient.shared.fetchChats()
                    if !remoteChats.isEmpty {
                        self?.chats = remoteChats
                    }
                }
            }
            .store(in: &cancellables)

        WebSocketManager.shared.$isConnected
            .assign(to: &$isWsConnected)
    }

    func sendMessage() {
        guard !messageInputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let text = messageInputText
        let targetChatId = activeChat?.id ?? UUID().uuidString
        let targetChatName = activeChat?.name ?? "Conversation"

        let newMessage = ChatMessage(
            id: UUID().uuidString,
            chatId: targetChatId,
            senderName: "Alex",
            content: text,
            timestamp: "Just now",
            isFromMe: true,
            isAudio: false,
            mediaUrl: nil
        )
        activeChatMessages.append(newMessage)
        messageInputText = ""

        DispatchQueue.main.async {
            if let index = self.chats.firstIndex(where: { $0.id == targetChatId }) {
                let oldChat = self.chats[index]
                let updated = ChatItem(
                    id: oldChat.id,
                    name: oldChat.name,
                    lastMessage: text,
                    timestamp: "Just now",
                    unreadCount: oldChat.unreadCount,
                    avatarUrl: oldChat.avatarUrl,
                    isOnline: oldChat.isOnline
                )
                self.chats.remove(at: index)
                self.chats.insert(updated, at: 0)
            } else {
                let newChatCard = ChatItem(
                    id: targetChatId,
                    name: targetChatName,
                    lastMessage: text,
                    timestamp: "Just now",
                    unreadCount: 0,
                    avatarUrl: "https://i.pravatar.cc/150?u=\(targetChatId)",
                    isOnline: true
                )
                self.chats.insert(newChatCard, at: 0)
            }
        }

        WebSocketManager.shared.send(type: "chat_message", payload: ["content": text, "chatId": targetChatId, "conversation_id": targetChatId])
    }
}
