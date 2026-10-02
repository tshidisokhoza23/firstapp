import Foundation
import Combine
import SwiftUI

@MainActor
public class StarredMessagesViewModel: ObservableObject {
    @Published public var starredMessages: ResourceState<[Message]> = .loading([])

    public init() {
        fetchStarredMessages()
    }

    public func fetchStarredMessages() {
        starredMessages = .loading(starredMessages.data)
        Task {
            do {
                let data = try await API.data("messages/starred")
                let messages = LiveJSON.messages(from: data, myUserId: SessionStore.shared.userId).map { message in
                    var copy = message
                    copy.senderName = Self.visibleName(message.senderName)
                    copy.isStarred = true
                    return copy
                }
                starredMessages = .success(messages)
            } catch {
                starredMessages = .error("Could not load starred messages", starredMessages.data ?? [])
            }
        }
    }

    public func unstarMessage(messageId: String) {
        if var msgs = starredMessages.data {
            msgs.removeAll { $0.id == messageId }
            starredMessages = .success(msgs)
        }
        Task {
            _ = try? await API.data("messages/\(messageId)/star", method: "DELETE")
            fetchStarredMessages()
        }
    }

    public func deleteMessage(messageId: String) {
        let conversationId = starredMessages.data?.first(where: { $0.id == messageId })?.conversationId ?? ""
        if var msgs = starredMessages.data {
            msgs.removeAll { $0.id == messageId }
            starredMessages = .success(msgs)
        }
        Task {
            if !conversationId.isEmpty {
                _ = try? await API.data("conversations/\(conversationId)/messages/\(messageId)", method: "DELETE")
            }
            fetchStarredMessages()
        }
    }

    private static func visibleName(_ raw: String) -> String {
        let cleaned = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let lower = cleaned.lowercased()
        let blocked = ["user", "someone", "direct", "chat", "mingle user", "mingle contact", "unknown caller"]
        if cleaned.isEmpty || blocked.contains(lower) || LiveJSON.isOpaqueId(cleaned) {
            return ""
        }
        return cleaned
    }
}
