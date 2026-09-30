import Foundation
import Combine

/// Core service managing real-time chat data, active conversations, and message states.
public final class ChatService: ObservableObject, @unchecked Sendable {
    public static let shared = ChatService()

    // MARK: - Published State
    @Published public private(set) var activeChats: [ChatMessage] = []
    @Published public private(set) var unreadCount: Int = 0
    @Published public private(set) var isLoading: Bool = false
    @Published public var errorMessage: String? = nil

    private var cancellables = Set<AnyCancellable>()

    private init() {
        // Initialization for chat listeners and repository synchronization
    }

    // MARK: - Public Methods

    public func fetchChats() {
        isLoading = true
        errorMessage = nil

        // Fetch real-time chat threads from repository
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.isLoading = false
        }
    }

    public func sendMessage(_ text: String, to recipientId: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        let message = ChatMessage(
            id: UUID().uuidString,
            senderId: "currentUser",
            receiverId: recipientId,
            content: text,
            timestamp: Date(),
            isRead: false
        )

        DispatchQueue.main.async { [weak self] in
            self?.activeChats.append(message)
        }
    }

    public func markAsRead(chatId: String) {
        DispatchQueue.main.async { [weak self] in
            if let index = self?.activeChats.firstIndex(where: { $0.id == chatId }) {
                self?.activeChats[index].isRead = true
                self?.recalculateUnreadCount()
            }
        }
    }

    public func clearChat(chatId: String) {
        DispatchQueue.main.async { [weak self] in
            self?.activeChats.removeAll(where: { $0.id == chatId })
            self?.recalculateUnreadCount()
        }
    }

    // MARK: - Helper Methods

    private func recalculateUnreadCount() {
        unreadCount = activeChats.filter { !$0.isRead && $0.receiverId == "currentUser" }.count
    }
}

// MARK: - Supporting Data Models

public struct ChatMessage: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let senderId: String
    public let receiverId: String
    public let content: String
    public let timestamp: Date
    public var isRead: Bool

    public init(id: String = UUID().uuidString, senderId: String, receiverId: String, content: String, timestamp: Date = Date(), isRead: Bool = false) {
        self.id = id
        self.senderId = senderId
        self.receiverId = receiverId
        self.content = content
        self.timestamp = timestamp
        self.isRead = isRead
    }
}
