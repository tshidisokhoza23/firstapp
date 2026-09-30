import Foundation

// MARK: - Channel DTO Models
public struct ChannelsResponse: Codable {
    public let channels: [ApiChannel]
}

public struct ApiChannel: Codable, Identifiable {
    public let id: String
    public let name: String
    public let description: String?
    public let avatarUrl: String?
    public let subscriberCount: Int
    public let isSubscribed: Bool
    public let isAdmin: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case description
        case avatarUrl = "avatar_url"
        case subscriberCount = "subscriber_count"
        case isSubscribed = "is_subscribed"
        case isAdmin = "is_admin"
    }
}

public struct CreateChannelRequest: Codable {
    public let name: String
    public let description: String?
    public let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case name
        case description
        case avatarUrl = "avatar_url"
    }

    public init(name: String, description: String? = nil, avatarUrl: String? = nil) {
        self.name = name
        self.description = description
        self.avatarUrl = avatarUrl
    }
}

public struct AvatarUrlRequest: Codable {
    public let avatarUrl: String

    enum CodingKeys: String, CodingKey {
        case avatarUrl = "avatar_url"
    }

    public init(avatarUrl: String) {
        self.avatarUrl = avatarUrl
    }
}

public struct ToggleAdminPostingRequest: Codable {
    public let adminOnlyPosting: Bool

    enum CodingKeys: String, CodingKey {
        case adminOnlyPosting = "admin_only_posting"
    }

    public init(adminOnlyPosting: Bool) {
        self.adminOnlyPosting = adminOnlyPosting
    }
}

public struct UserIdRequest: Codable {
    public let userId: String

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
    }

    public init(userId: String) {
        self.userId = userId
    }
}

public protocol ChannelApiService {
    func getChannels() async throws -> ChannelsResponse
    func getChatsChannels() async throws -> ChannelsResponse
    func searchChannels(query: String) async throws -> ChannelsResponse
    func getChannel(channelId: String) async throws -> ApiChannel
    func createChannel(request: CreateChannelRequest) async throws -> ApiChannel
    func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse
    func subscribeToChannel(channelId: String) async throws -> StatusResponse
    func muteChannel(channelId: String, request: MuteRequest) async throws -> StatusResponse
    func addChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse
    func toggleAdminPosting(channelId: String, request: ToggleAdminPostingRequest) async throws -> StatusResponse
    func removeChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse
}

public final class ChannelApi: ChannelApiService {
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getChannels() async throws -> ChannelsResponse {
        return try await client.request(path: "channels", method: .get)
    }

    public func getChatsChannels() async throws -> ChannelsResponse {
        return try await client.request(path: "chats/channels", method: .get)
    }

    public func searchChannels(query: String) async throws -> ChannelsResponse {
        let queryItems = [URLQueryItem(name: "query", value: query)]
        return try await client.request(path: "chats/channels/search", method: .get, queryItems: queryItems)
    }

    public func getChannel(channelId: String) async throws -> ApiChannel {
        return try await client.request(path: "chats/channels/\(channelId)", method: .get)
    }

    public func createChannel(request: CreateChannelRequest) async throws -> ApiChannel {
        return try await client.request(path: "chats/channels", method: .post, body: request)
    }

    public func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/avatar", method: .patch, body: request)
    }

    public func subscribeToChannel(channelId: String) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/subscribe", method: .post)
    }

    public func muteChannel(channelId: String, request: MuteRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/mute", method: .post, body: request)
    }

    public func addChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admins", method: .post, body: request)
    }

    public func toggleAdminPosting(channelId: String, request: ToggleAdminPostingRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admin-posting", method: .post, body: request)
    }

    public func removeChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admins", method: .delete, body: request)
    }
}
