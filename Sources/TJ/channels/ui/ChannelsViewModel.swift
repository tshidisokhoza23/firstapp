import Foundation
import Combine
import SwiftUI

@MainActor
public final class ChannelsViewModel: ObservableObject {
    @Published public var channels: Resource<[Channel]> = .loading()
    @Published public var toastMessage: String? = nil
    @Published public var createdChannelId: String? = nil

    private let repository: ChannelRepository

    public init(repository: ChannelRepository? = nil) {
        self.repository = repository ?? ChannelRepository.shared
        fetchChannels()
    }

    public func fetchChannels() {
        Task {
            do {
                let list = try await repository.getChannels()
                self.channels = .success(list)
            } catch {
                self.channels = .error(error.localizedDescription)
            }
        }
    }

    public func clearToast() {
        toastMessage = nil
    }

    public func subscribeToChannel(channelId: String) {
        Task {
            _ = await repository.subscribeToChannel(channelId: channelId)
            fetchChannels()
        }
    }

    public func muteChannel(channelId: String, isMuted: Bool) {
        Task {
            _ = await repository.muteChannel(channelId: channelId, isMuted: isMuted)
            fetchChannels()
        }
    }

    public func createChannel(name: String, description: String, avatarUrl: String? = nil) {
        Task {
            let channel = await repository.createChannel(name: name, description: description, avatarUrl: avatarUrl)
            createdChannelId = channel.conversationId ?? channel.id
            toastMessage = "Channel created successfully"
            fetchChannels()
        }
    }

    public func addAdmin(channelId: String, userId: String) {
        Task {
            _ = await repository.addAdmin(channelId: channelId, userId: userId)
            toastMessage = "Admin added"
            fetchChannels()
        }
    }

    public func removeAdmin(channelId: String, userId: String) {
        Task {
            _ = await repository.removeAdmin(channelId: channelId, userId: userId)
            toastMessage = "Admin removed"
            fetchChannels()
        }
    }

    public func toggleAdminPosting(channelId: String, allowed: Bool) {
        Task {
            _ = await repository.updateAdminPostingPermission(channelId: channelId, allowed: allowed)
            toastMessage = "Posting permission updated"
            fetchChannels()
        }
    }
}
