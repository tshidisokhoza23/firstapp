import SwiftUI
import Combine

class ChatViewModel: ObservableObject {
    @Published var chats: [ChatItem] = ChatItem.samples
    @Published var stories: [StoryItem] = StoryItem.samples
    @Published var activeSubTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards
    @Published var activeChat: ChatItem? = nil
    @Published var activeChatMessages: [ChatMessage] = ChatMessage.sampleConversation
    @Published var messageInputText: String = ""
    @Published var isWsConnected: Bool = false

    private var cancellables = Set<AnyCancellable>()

    init() {
        Task { @MainActor in
            let remoteChats = await NetworkClient.shared.fetchChats()
            if !remoteChats.isEmpty {
                self.chats = remoteChats
            }
        }

        WebSocketManager.shared.incomingMessagePublisher
            .sink { [weak self] envelope in
                guard let self = self, let type = envelope.type else { return }
                if type == "chat_message", let payload = envelope.payload, let content = payload["content"] {
                    let sender = payload["sender"] ?? "Friend"
                    let incoming = ChatMessage(
                        id: UUID().uuidString,
                        chatId: self.activeChat?.id ?? "c1",
                        senderName: sender,
                        content: content,
                        timestamp: "Just now",
                        isFromMe: false,
                        isAudio: false,
                        mediaUrl: nil
                    )
                    self.activeChatMessages.append(incoming)
                }
            }
            .store(in: &cancellables)

        WebSocketManager.shared.$isConnected
            .assign(to: &$isWsConnected)
    }

    func sendMessage() {
        guard !messageInputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let text = messageInputText
        let newMessage = ChatMessage(
            id: UUID().uuidString,
            chatId: activeChat?.id ?? "c1",
            senderName: "Alex",
            content: text,
            timestamp: "Just now",
            isFromMe: true,
            isAudio: false,
            mediaUrl: nil
        )
        activeChatMessages.append(newMessage)
        messageInputText = ""

        WebSocketManager.shared.send(type: "chat_message", payload: ["content": text, "chatId": activeChat?.id ?? "c1"])
    }
}
