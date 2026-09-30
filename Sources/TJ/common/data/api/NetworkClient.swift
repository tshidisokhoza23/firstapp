import Foundation

// MARK: - HTTP Methods
public enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

// MARK: - Network Error
public enum NetworkError: LocalizedError {
    case invalidURL
    case serverError(statusCode: Int, message: String?)
    case decodingError(Error)
    case unauthorized
    case unknown(Error)

    public var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid endpoint URL."
        case .serverError(let statusCode, let message):
            return "Server error (\(statusCode)): \(message ?? "No message")"
        case .decodingError(let err):
            return "Failed to decode response: \(err.localizedDescription)"
        case .unauthorized:
            return "Unauthorized session. Please log in again."
        case .unknown(let err):
            return err.localizedDescription
        }
    }
}

// MARK: - Shared Network Client
public final class NetworkClient {
    public static let shared = NetworkClient()

    public var baseURL: URL = URL(string: "https://api.firstapp.com/v1/")!
    public var authToken: String? {
        get { UserDefaults.standard.string(forKey: "auth_token") }
        set { UserDefaults.standard.set(newValue, forKey: "auth_token") }
    }

    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    public init(session: URLSession = .shared) {
        self.session = session

        self.decoder = JSONDecoder()
        self.decoder.keyDecodingStrategy = .convertFromSnakeCase

        self.encoder = JSONEncoder()
        self.encoder.keyEncodingStrategy = .convertToSnakeCase
    }

    public func request<T: Decodable, B: Encodable>(
        path: String,
        method: HTTPMethod = .get,
        queryItems: [URLQueryItem]? = nil,
        body: B? = nil
    ) async throws -> T {
        guard var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: true) else {
            throw NetworkError.invalidURL
        }
        if let queryItems = queryItems, !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else { throw NetworkError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            request.httpBody = try encoder.encode(body)
        }

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.serverError(statusCode: -1, message: "Non-HTTP response")
        }

        if httpResponse.statusCode == 401 {
            throw NetworkError.unauthorized
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let errorMsg = String(data: data, encoding: .utf8)
            throw NetworkError.serverError(statusCode: httpResponse.statusCode, message: errorMsg)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw NetworkError.decodingError(error)
        }
    }

    // Overload for empty body requests
    public func request<T: Decodable>(
        path: String,
        method: HTTPMethod = .get,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> T {
        let emptyBody: String? = nil
        return try await request(path: path, method: method, queryItems: queryItems, body: emptyBody)
    }
}

// MARK: - Common API Data Models
public struct StatusResponse: Codable {
    public let status: String
    public let code: String?
}

public struct MuteRequest: Codable {
    public let isMuted: Bool
    enum CodingKeys: String, CodingKey { case isMuted = "is_muted" }
    public init(isMuted: Bool) { self.isMuted = isMuted }
}

public struct PinRequest: Codable {
    public let isPinned: Bool
    enum CodingKeys: String, CodingKey { case isPinned = "is_pinned" }
    public init(isPinned: Bool) { self.isPinned = isPinned }
}

public struct ArchiveRequest: Codable {
    public let isArchived: Bool
    enum CodingKeys: String, CodingKey { case isArchived = "is_archived" }
    public init(isArchived: Bool) { self.isArchived = isArchived }
}

public struct ReportRequest: Codable {
    public let reason: String
    public init(reason: String) { self.reason = reason }
}

public struct ContactInfo: Codable {
    public let name: String
    public let phoneNumber: String
    enum CodingKeys: String, CodingKey {
        case name
        case phoneNumber = "phone_number"
    }
}

public struct DevResetRequest: Codable {
    public let contacts: [ContactInfo]
    public init(contacts: [ContactInfo]) { self.contacts = contacts }
}

public struct DevResetResponse: Codable {
    public let status: String
    public let username: String
    public let password: String
}

public struct RegisterRequest: Codable {
    public let username: String
    public let email: String
    public let displayName: String
    public let phoneNumber: String
    public let password: String
    public let countryCode: String?
    public let location: String?

    enum CodingKeys: String, CodingKey {
        case username, email, password, location
        case displayName = "display_name"
        case phoneNumber = "phone_number"
        case countryCode = "country_code"
    }

    public init(username: String, email: String, displayName: String, phoneNumber: String, password: String, countryCode: String? = nil, location: String? = nil) {
        self.username = username
        self.email = email
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.password = password
        self.countryCode = countryCode
        self.location = location
    }
}

public struct LoginRequest: Codable {
    public let username: String
    public let password: String
    public init(username: String, password: String) {
        self.username = username
        self.password = password
    }
}

public struct User: Codable, Identifiable {
    public let id: String
    public let username: String
    public let displayName: String
    public let email: String
    public let phoneNumber: String?
    public let avatarUrl: String?
    public let bio: String?
    public let isProfileComplete: Bool

    enum CodingKeys: String, CodingKey {
        case id, username, email, bio
        case displayName = "display_name"
        case phoneNumber = "phone_number"
        case avatarUrl = "avatar_url"
        case isProfileComplete = "is_profile_complete"
    }
}

public struct AuthResponse: Codable {
    public let token: String
    public let refreshToken: String?
    public let user: User?

    enum CodingKeys: String, CodingKey {
        case token, user
        case refreshToken = "refresh_token"
    }
}

public struct TokenRefreshRequest: Codable {
    public let refreshToken: String
    enum CodingKeys: String, CodingKey { case refreshToken = "refresh_token" }
    public init(refreshToken: String) { self.refreshToken = refreshToken }
}

public struct PushTokenRequest: Codable {
    public let token: String
    public let deviceId: String
    public let platform: String

    enum CodingKeys: String, CodingKey {
        case token, platform
        case deviceId = "device_id"
    }

    public init(token: String, deviceId: String, platform: String = "ios") {
        self.token = token
        self.deviceId = deviceId
        self.platform = platform
    }
}

public struct ForgotPasswordRequest: Codable {
    public let emailOrUsername: String
    enum CodingKeys: String, CodingKey { case emailOrUsername = "email_or_username" }
    public init(emailOrUsername: String) { self.emailOrUsername = emailOrUsername }
}

public struct UserResponse: Codable {
    public let user: User
}

public struct ApiUser: Codable, Identifiable {
    public let id: String
    public let username: String
    public let displayName: String
    public let email: String
    public let phoneNumber: String?
    public let avatarUrl: String?
    public let isVerified: Bool
    public let isFollowing: Bool

    enum CodingKeys: String, CodingKey {
        case id, username, email
        case displayName = "display_name"
        case phoneNumber = "phone_number"
        case avatarUrl = "avatar_url"
        case isVerified = "is_verified"
        case isFollowing = "is_following"
    }
}

// MARK: - User API Service
public protocol UserApiService {
    func getCurrentUser() async throws -> UserResponse
    func getUserById(userId: String) async throws -> UserResponse
    func deleteAccount() async throws -> StatusResponse
    func upsertPushToken(request: PushTokenRequest) async throws -> StatusResponse
    func blockUser(userId: String) async throws -> StatusResponse
    func unblockUser(userId: String) async throws -> StatusResponse
    func followUser(userId: String) async throws -> StatusResponse
    func unfollowUser(userId: String) async throws -> StatusResponse
}

public final class UserApi: UserApiService {
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getCurrentUser() async throws -> UserResponse {
        return try await client.request(path: "users/me", method: .get)
    }

    public func getUserById(userId: String) async throws -> UserResponse {
        return try await client.request(path: "users/\(userId)", method: .get)
    }

    public func deleteAccount() async throws -> StatusResponse {
        return try await client.request(path: "users/me", method: .delete)
    }

    public func upsertPushToken(request: PushTokenRequest) async throws -> StatusResponse {
        return try await client.request(path: "devices/push-token", method: .post, body: request)
    }

    public func blockUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/block", method: .post)
    }

    public func unblockUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/unblock", method: .post)
    }

    public func followUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/follow", method: .post)
    }

    public func unfollowUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/unfollow", method: .post)
    }
}

// MARK: - System API Service
public protocol SystemApiService {
    func devResetAndSeed(contacts: [ContactInfo]) async throws -> DevResetResponse
}

public final class SystemApi: SystemApiService {
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func devResetAndSeed(contacts: [ContactInfo]) async throws -> DevResetResponse {
        let req = DevResetRequest(contacts: contacts)
        return try await client.request(path: "dev/reset-and-seed", method: .post, body: req)
    }
}
