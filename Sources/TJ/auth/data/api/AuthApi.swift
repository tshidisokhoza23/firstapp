import Foundation

public protocol AuthApiService {
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

public final class AuthApi: AuthApiService {
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
