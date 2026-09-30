import SwiftUI
import Combine

class ChatViewModel: ObservableObject {
    @Published var chats: [ChatItem] = ChatItem.samples
    @Published var stories: [StoryItem] = StoryItem.samples
    @Published var activeSubTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards
    @Published var activeChat: ChatItem? = nil
    @Published var activeChatMessages: [ChatMessage] = ChatMessage.sampleConversation
    @Published var messageInputText: String = ""

    init() {
        Task { @MainActor in
            let remoteChats = await NetworkClient.shared.fetchChats()
            if !remoteChats.isEmpty {
                self.chats = remoteChats
            }
        }
    }

    func sendMessage() {
        guard !messageInputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let newMessage = ChatMessage(
            id: UUID().uuidString,
            chatId: activeChat?.id ?? "c1",
            senderName: "Alex",
            content: messageInputText,
            timestamp: "Just now",
            isFromMe: true,
            isAudio: false,
            mediaUrl: nil
        )
        activeChatMessages.append(newMessage)
        messageInputText = ""
    }
}
