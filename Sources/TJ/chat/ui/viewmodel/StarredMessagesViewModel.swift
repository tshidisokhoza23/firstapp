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
        starredMessages = .loading([])
        // Simulated fetch
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.starredMessages = .success([])
        }
    }

    public func unstarMessage(messageId: String) {
        if var msgs = starredMessages.data {
            msgs.removeAll { $0.id == messageId }
            starredMessages = .success(msgs)
        }
    }

    public func deleteMessage(messageId: String) {
        unstarMessage(messageId: messageId)
    }
}
