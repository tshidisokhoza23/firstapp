import Foundation
import Combine
import SwiftUI

@MainActor
public final class ChannelsViewModel: ObservableObject {
    @Published public var channels: Resource<[Channel]> = .loading()
    @Published public var toastMessage: String? = nil
    @Published public var createdChannelId: String? = nil

    private let repository: ChannelRepository
    private var cancellables = Set<AnyCancellable>()

    public init(repository: ChannelRepository = ChannelRepositoryImpl.shared) {
        self.repository = repository
        fetchChannels()
    }

    public func fetchChannels() {
        repository.getChannels()
            .receive(on: DispatchQueue.main)
            .sink { [weak self] resource in
                self?.channels = resource
            }
            .store(in: &cancellables)
    }

    public func clearToast() {
        toastMessage = nil
    }

    public func subscribeToChannel(channelId: String) {
        Task {
            let res = await repository.subscribeToChannel(channelId: channelId)
            if case .success = res {
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }

    public func muteChannel(channelId: String, isMuted: Bool) {
        Task {
            let res = await repository.muteChannel(channelId: channelId, isMuted: isMuted)
            if case .success = res {
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }

    public func createChannel(name: String, description: String, avatarUrl: String? = nil) {
        Task {
            let res = await repository.createChannel(name: name, description: description, avatarUrl: avatarUrl)
            if case .success(let channel) = res {
                createdChannelId = channel.conversationId ?? channel.id
                toastMessage = "Channel created successfully"
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }

    public func addAdmin(channelId: String, userId: String) {
        Task {
            let res = await repository.addAdmin(channelId: channelId, userId: userId)
            if case .success = res {
                toastMessage = "Admin added"
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }

    public func removeAdmin(channelId: String, userId: String) {
        Task {
            let res = await repository.removeAdmin(channelId: channelId, userId: userId)
            if case .success = res {
                toastMessage = "Admin removed"
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }

    public func toggleAdminPosting(channelId: String, allowed: Bool) {
        Task {
            let res = await repository.updateAdminPostingPermission(channelId: channelId, allowed: allowed)
            if case .success = res {
                toastMessage = "Posting permission updated"
                fetchChannels()
            } else if case .error(let msg) = res {
                toastMessage = msg
            }
        }
    }
}
