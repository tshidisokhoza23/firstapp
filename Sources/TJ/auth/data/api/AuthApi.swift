import Foundation

public protocol AuthApiService {
    func register(request: RegisterRequest) async throws -> AuthResponse
    func login(request: LoginRequest) async throws -> AuthResponse
    func refresh(request: TokenRefreshRequest) async throws -> AuthResponse
    func logout(request: TokenRefreshRequest) async throws -> StatusResponse
    func forgotPassword(request: ForgotPasswordRequest) async throws -> StatusResponse
    func requestOTP(body: [String: String]) async throws -> StatusResponse
    func verifyOTP(body: [String: String]) async throws -> StatusResponse
    func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool]
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

    public func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool] {
        var queryItems: [URLQueryItem] = []
        if let username = username { queryItems.append(URLQueryItem(name: "username", value: username)) }
        if let email = email { queryItems.append(URLQueryItem(name: "email", value: email)) }
        if let phone = phone { queryItems.append(URLQueryItem(name: "phone", value: phone)) }

        return try await client.request(path: "auth/availability", method: .get, queryItems: queryItems)
    }
}
