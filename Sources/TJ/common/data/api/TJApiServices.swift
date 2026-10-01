import Foundation

// MARK: - Auth API Services & DTOs

public protocol AuthApiService: Sendable {
    func register(request: RegisterRequest) async throws -> AuthResponse
    func login(request: LoginRequest) async throws -> AuthResponse
    func refresh(request: TokenRefreshRequest) async throws -> AuthResponse
    func logout(request: TokenRefreshRequest) async throws -> StatusResponse
    func forgotPassword(request: ForgotPasswordRequest) async throws -> StatusResponse
    func requestOTP(body: [String: String]) async throws -> StatusResponse
    func verifyOTP(body: [String: String]) async throws -> StatusResponse
    func googleSignIn(idToken: String) async throws -> AuthResponse
    func completeProfile(displayName: String?, username: String?, phoneNumber: String?, categories: [String]?) async throws -> User
    func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool]
}

public struct GoogleSignInBody: Codable, Sendable {
    public let idToken: String
    enum CodingKeys: String, CodingKey { case idToken = "id_token" }
    public init(idToken: String) { self.idToken = idToken }
}

public struct CompleteProfileBody: Codable, Sendable {
    public let displayName: String?
    public let username: String?
    public let phoneNumber: String?
    public let categories: [String]?
    enum CodingKeys: String, CodingKey {
        case displayName = "display_name"
        case username
        case phoneNumber = "phone_number"
        case categories
    }
}

public struct UserEnvelope: Codable, Sendable {
    public let user: User
}

public final class AuthApi: AuthApiService, @unchecked Sendable {
    public static let shared = AuthApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func register(request: RegisterRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/register", method: .post, body: request)
    }

    public func login(request: LoginRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/login", method: .post, body: request)
    }

    public func refresh(request: TokenRefreshRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/refresh", method: .post, body: request)
    }

    public func logout(request: TokenRefreshRequest) async throws -> StatusResponse {
        return try await client.request(path: "auth/logout", method: .post, body: request)
    }

    public func forgotPassword(request: ForgotPasswordRequest) async throws -> StatusResponse {
        return try await client.request(path: "auth/forgot-password", method: .post, body: request)
    }

    public func requestOTP(body: [String: String]) async throws -> StatusResponse {
        return try await client.request(path: "auth/request-otp", method: .post, body: body)
    }

    public func verifyOTP(body: [String: String]) async throws -> StatusResponse {
        return try await client.request(path: "auth/verify-otp", method: .post, body: body)
    }

    public func googleSignIn(idToken: String) async throws -> AuthResponse {
        return try await client.request(path: "auth/google", method: .post, body: GoogleSignInBody(idToken: idToken))
    }

    public func completeProfile(displayName: String?, username: String?, phoneNumber: String?, categories: [String]?) async throws -> User {
        let body = CompleteProfileBody(displayName: displayName, username: username, phoneNumber: phoneNumber, categories: categories)
        let envelope: UserEnvelope = try await client.request(path: "users/complete-profile", method: .post, body: body)
        return envelope.user
    }

    public func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool] {
        var queryItems: [URLQueryItem] = []
        if let username = username { queryItems.append(URLQueryItem(name: "username", value: username)) }
        if let email = email { queryItems.append(URLQueryItem(name: "email", value: email)) }
        if let phone = phone { queryItems.append(URLQueryItem(name: "phone", value: phone)) }

        return try await client.request(path: "auth/availability", method: .get, queryItems: queryItems)
    }
}

// MARK: - Channel API Services & DTOs

public struct ChannelsResponse: Codable, Sendable {
    public let channels: [ApiChannel]
}

public struct ApiChannel: Codable, Identifiable, Sendable {
    public let id: String
    public let name: String
    public let description: String?
    public let avatarUrl: String?
    public let subscriberCount: Int
    public let isSubscribed: Bool
    public let isAdmin: Bool

    enum CodingKeys: String, CodingKey {
        case id, name, description
        case avatarUrl = "avatar_url"
        case subscriberCount = "subscriber_count"
        case isSubscribed = "is_subscribed"
        case isAdmin = "is_admin"
    }
}

public struct CreateChannelRequest: Codable, Sendable {
    public let name: String
    public let description: String?
    public let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case name, description
        case avatarUrl = "avatar_url"
    }

    public init(name: String, description: String? = nil, avatarUrl: String? = nil) {
        self.name = name
        self.description = description
        self.avatarUrl = avatarUrl
    }
}

public protocol ChannelApiService: Sendable {
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

public final class ChannelApi: ChannelApiService, @unchecked Sendable {
    public static let shared = ChannelApi()
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
