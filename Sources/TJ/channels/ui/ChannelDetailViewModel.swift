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
        return "user1"
    }

    private let repository: ChannelRepository

    public init(repository: ChannelRepository = ChannelRepository.shared) {
        self.repository = repository
    }

    public func loadChannel(channelId: String) {
        Task {
            if let ch = await repository.getChannel(channelId: channelId) {
                self.channel = .success(ch)
            } else {
                self.channel = .error("Channel not found")
            }
        }
    }

    public func loadMessages(conversationId: String) {}

    public func subscribe(channelId: String) {
        Task {
            let isSubbed = await repository.subscribeToChannel(channelId: channelId)
            self.toast = isSubbed ? "Channel followed" : "Unfollowed channel"
            self.loadChannel(channelId: channelId)
        }
    }

    public func mute(channelId: String, isMuted: Bool) {
        Task {
            _ = await repository.muteChannel(channelId: channelId, isMuted: isMuted)
            self.loadChannel(channelId: channelId)
        }
    }

    public func addAdmin(channelId: String, userId: String) {
        Task {
            _ = await repository.addAdmin(channelId: channelId, userId: userId)
            self.loadChannel(channelId: channelId)
        }
    }

    public func removeAdmin(channelId: String, userId: String) {
        Task {
            _ = await repository.removeAdmin(channelId: channelId, userId: userId)
            self.loadChannel(channelId: channelId)
        }
    }

    public func toggleAdminPosting(channelId: String, allowed: Bool) {
        Task {
            _ = await repository.updateAdminPostingPermission(channelId: channelId, allowed: allowed)
            self.loadChannel(channelId: channelId)
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
            senderName: currentUserId ?? "user1",
            message: content
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
