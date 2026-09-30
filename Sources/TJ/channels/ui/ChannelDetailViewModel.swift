import Foundation
import Combine
import SwiftUI

@MainActor
public final class ChannelDetailViewModel: ObservableObject {
    @Published public var channel: Resource<Channel> = .loading()
    @Published public var messages: [ChannelChatMessage] = []
    @Published public var contacts: [ChannelContact] = []
    @Published public var members: [ApiUser] = []
    @Published public var toast: String? = nil

    public var currentUserId: String? {
        return "user1" // Current session user ID
    }

    private let repository: ChannelRepository
    private var cancellables = Set<AnyCancellable>()

    public init(repository: ChannelRepository = ChannelRepositoryImpl.shared) {
        self.repository = repository
    }

    public func loadChannel(channelId: String) {
        Task {
            let res = await repository.getChannel(channelId: channelId)
            self.channel = res
            if case .success(let ch) = res {
                let convId = ch.conversationId ?? ch.id
                self.loadMessages(conversationId: convId)
            }
        }
    }

    public func loadMessages(conversationId: String) {
        // Load messages for channel conversation
    }

    public func subscribe(channelId: String) {
        Task {
            let res = await repository.subscribeToChannel(channelId: channelId)
            if case .success(let isSubbed) = res {
                self.toast = isSubbed ? "Channel followed" : "Unfollowed channel"
                self.loadChannel(channelId: channelId)
            } else if case .error(let msg, _) = res {
                self.toast = msg
            }
        }
    }

    public func mute(channelId: String, isMuted: Bool) {
        Task {
            let res = await repository.muteChannel(channelId: channelId, isMuted: isMuted)
            if case .success = res {
                self.loadChannel(channelId: channelId)
            }
        }
    }

    public func addAdmin(channelId: String, userId: String) {
        Task {
            let res = await repository.addAdmin(channelId: channelId, userId: userId)
            if case .success = res {
                self.loadChannel(channelId: channelId)
            }
        }
    }

    public func removeAdmin(channelId: String, userId: String) {
        Task {
            let res = await repository.removeAdmin(channelId: channelId, userId: userId)
            if case .success = res {
                self.loadChannel(channelId: channelId)
            }
        }
    }

    public func toggleAdminPosting(channelId: String, allowed: Bool) {
        Task {
            let res = await repository.updateAdminPostingPermission(channelId: channelId, allowed: allowed)
            if case .success = res {
                self.loadChannel(channelId: channelId)
            }
        }
    }

    public func clearToast() {
        toast = nil
    }

    public func sendMessage(
        channelId: String,
        content: String,
        mediaUrls: [String] = [],
        messageType: String = "text",
        fileName: String? = nil
    ) {
        let newMsg = ChannelChatMessage(
            conversationId: channelId,
            content: content,
            senderId: currentUserId ?? "user1",
            type: messageType,
            mediaUrls: mediaUrls
        )
        messages.append(newMsg)
        toast = "Update sent"
    }

    public func sendMediaPost(
        channelId: String,
        content: String,
        mediaUrls: [String],
        messageType: String = "text",
        fileName: String? = nil
    ) {
        sendMessage(
            channelId: channelId,
            content: content,
            mediaUrls: mediaUrls,
            messageType: messageType,
            fileName: fileName
        )
    }

    public func createPost(channelId: String, content: String, mediaUrls: [String]) {
        sendMessage(channelId: channelId, content: content, mediaUrls: mediaUrls)
    }
}
