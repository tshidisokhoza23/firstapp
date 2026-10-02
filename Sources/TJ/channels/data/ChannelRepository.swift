import Foundation
import Combine

public struct ChannelPost: Identifiable, Codable, Equatable {
    public var id: String
    public var content: String
    public var imageUrl: String?
    public var timestamp: String
    public var isLiked: Bool

    public init(id: String = UUID().uuidString, content: String, imageUrl: String? = nil, timestamp: String = "Just now", isLiked: Bool = false) {
        self.id = id
        self.content = content
        self.imageUrl = imageUrl
        self.timestamp = timestamp
        self.isLiked = isLiked
    }
}

public struct ChannelChatMessage: Identifiable, Codable, Equatable {
    public var id: String
    public var senderName: String
    public var message: String
    public var timestamp: String
    public var mediaUrl: String?

    public init(id: String = UUID().uuidString, senderName: String, message: String, timestamp: String = "Just now", mediaUrl: String? = nil) {
        self.id = id
        self.senderName = senderName
        self.message = message
        self.timestamp = timestamp
        self.mediaUrl = mediaUrl
    }
}

public struct ChannelContact: Identifiable, Codable, Equatable {
    public var id: String
    public var displayName: String
    public var isRegistered: Bool

    public init(id: String, displayName: String, isRegistered: Bool = true) {
        self.id = id
        self.displayName = displayName
        self.isRegistered = isRegistered
    }
}

public protocol ChannelRepositoryProtocol: AnyObject {
    func getChannels() async throws -> [Channel]
    func getChannelById(id: String) async throws -> Channel?
    func getChannel(channelId: String) async -> Channel?
    func subscribeToChannel(id: String) async throws -> Bool
    func subscribeToChannel(channelId: String) async -> Bool
    func unsubscribeFromChannel(id: String) async throws -> Bool
    func muteChannel(id: String, isMuted: Bool) async throws -> Bool
    func muteChannel(channelId: String, isMuted: Bool) async -> Bool
    func createChannel(name: String, description: String, category: String) async throws -> Channel
    func createChannel(name: String, description: String, avatarUrl: String?) async -> Channel
    func addAdmin(channelId: String, userId: String) async -> Bool
    func removeAdmin(channelId: String, userId: String) async -> Bool
    func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Bool
}

@MainActor
public final class ChannelRepository: ObservableObject, ChannelRepositoryProtocol {
    public static let shared = ChannelRepository()

    @Published public private(set) var channels: [Channel] = []
    private let channelApi: ChannelApiService

    public init(channelApi: ChannelApiService = ChannelApi.shared) {
        self.channelApi = channelApi
    }

    public func getChannels() async throws -> [Channel] {
        var list: [Channel] = []
        if let data = try? await API.data("chats/channels") {
            list = ChannelJSON.list(from: data)
        }
        if list.isEmpty, let data = try? await API.data("channels") {
            list = ChannelJSON.list(from: data)
        }
        if list.isEmpty {
            let res = try await channelApi.getChannels()
            list = res.channels.map { api in
                Channel(
                    id: api.id,
                    name: api.name,
                    description: api.description ?? "",
                    followers: api.subscriberCount,
                    avatarUrl: api.avatarUrl,
                    isSubscribed: api.isSubscribed,
                    isAdmin: api.isAdmin
                )
            }
        }
        self.channels = list
        return list
    }

    public func getChannelById(id: String) async throws -> Channel? {
        if let local = channels.first(where: { $0.id == id }) {
            return local
        }
        let api = try await channelApi.getChannel(channelId: id)
        return Channel(
            id: api.id,
            name: api.name,
            description: api.description ?? "",
            followers: api.subscriberCount,
            avatarUrl: api.avatarUrl,
            isSubscribed: api.isSubscribed,
            isAdmin: api.isAdmin
        )
    }

    public func getChannel(channelId: String) async -> Channel? {
        return (try? await getChannelById(id: channelId)) ?? channels.first(where: { $0.id == channelId })
    }

    public func subscribeToChannel(id: String) async throws -> Bool {
        _ = try await channelApi.subscribeToChannel(channelId: id)
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed.toggle()
        }
        return true
    }

    public func subscribeToChannel(channelId: String) async -> Bool {
        return (try? await subscribeToChannel(id: channelId)) ?? false
    }

    public func unsubscribeFromChannel(id: String) async throws -> Bool {
        _ = try await channelApi.subscribeToChannel(channelId: id)
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed = false
            channels[index].followers = max(0, channels[index].followers - 1)
        }
        return true
    }

    public func muteChannel(id: String, isMuted: Bool) async throws -> Bool {
        _ = try await channelApi.muteChannel(channelId: id, request: MuteRequest(isMuted: isMuted))
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isMuted = isMuted
        }
        return true
    }

    public func muteChannel(channelId: String, isMuted: Bool) async -> Bool {
        (try? await muteChannel(id: channelId, isMuted: isMuted)) ?? false
    }

    public func createChannel(name: String, description: String, category: String) async throws -> Channel {
        let api = try await channelApi.createChannel(request: CreateChannelRequest(name: name, description: description))
        let created = Channel(id: api.id, name: api.name, description: api.description ?? "", followers: api.subscriberCount, avatarUrl: api.avatarUrl, isSubscribed: api.isSubscribed, isAdmin: api.isAdmin)
        channels.append(created)
        return created
    }

    public func createChannel(name: String, description: String, avatarUrl: String?) async -> Channel {
        if let created = try? await channelApi.createChannel(request: CreateChannelRequest(name: name, description: description, avatarUrl: avatarUrl)) {
            let model = Channel(id: created.id, name: created.name, description: created.description ?? "", followers: created.subscriberCount, avatarUrl: created.avatarUrl, isSubscribed: created.isSubscribed, isAdmin: created.isAdmin)
            channels.append(model)
            return model
        }
        return Channel(id: "", name: name, description: description, avatarUrl: avatarUrl)
    }

    public func addAdmin(channelId: String, userId: String) async -> Bool {
        (try? await channelApi.addChannelAdmin(channelId: channelId, request: UserIdRequest(userId: userId))) != nil
    }

    public func removeAdmin(channelId: String, userId: String) async -> Bool {
        (try? await channelApi.removeChannelAdmin(channelId: channelId, request: UserIdRequest(userId: userId))) != nil
    }

    public func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Bool {
        (try? await channelApi.toggleAdminPosting(channelId: channelId, request: ToggleAdminPostingRequest(adminsCanPost: allowed))) != nil
    }
}

public struct ChannelUser: Identifiable, Codable {
    public var id: String
    public var displayName: String
    public var isRegistered: Bool

    public init(id: String, displayName: String, isRegistered: Bool = true) {
        self.id = id
        self.displayName = displayName
        self.isRegistered = isRegistered
    }
}
