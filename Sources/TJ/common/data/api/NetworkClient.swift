import Foundation
#if canImport(UIKit)
import UIKit
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

public enum HTTPMethod: String, Codable, Sendable {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

public enum APIError: Error, LocalizedError {
    case badURL
    case status(Int, String)

    public var errorDescription: String? {
        switch self {
        case .badURL: return "Bad URL"
        case .status(let code, let msg): return "HTTP \(code): \(msg)"
        }
    }
}

public enum API {
    public static func request(_ path: String, method: String = "GET", body: Data? = nil) -> URLRequest? {
        var base = AppBackend.apiBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let fullPath = path.hasPrefix("/") ? path : "/\(path)"
        guard let url = URL(string: "\(base)\(fullPath)") else { return nil }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.httpBody = body
        AuthSession.apply(to: &req)
        if req.value(forHTTPHeaderField: "Content-Type") == nil {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        return req
    }

    public static func data(_ path: String, method: String = "GET", query: [String: String]? = nil, json: [String: Any]? = nil, retry: Bool = true) async throws -> Data {
        var base = AppBackend.apiBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let fullPath = path.hasPrefix("/") ? path : "/\(path)"
        guard var components = URLComponents(string: "\(base)\(fullPath)") else {
            throw APIError.badURL
        }
        if let query = query, !query.isEmpty {
            components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        AuthSession.apply(to: &components, path: fullPath)
        guard let url = components.url else {
            throw APIError.badURL
        }
        var req = URLRequest(url: url)
        req.httpMethod = method
        if let json = json {
            req.httpBody = try? JSONSerialization.data(withJSONObject: json)
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        AuthSession.apply(to: &req)

        let (responseData, response) = try await URLSession.shared.data(for: req)
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 500
        if retry, statusCode == 401, !AuthSession.isAuthPath(fullPath), await AuthSession.refresh() {
            return try await data(path, method: method, query: query, json: json, retry: false)
        }
        guard (200...299).contains(statusCode) else {
            let msg = String(data: responseData, encoding: .utf8) ?? "Server error"
            throw APIError.status(statusCode, msg)
        }
        return responseData
    }
}

@MainActor
public final class GoogleSignInService {
    public static let shared = GoogleSignInService()
    private var inFlight = false
    private init() {}

    public func signIn() async throws -> String {
        #if canImport(GoogleSignIn)
        if inFlight {
            throw NSError(domain: "GoogleSignIn", code: -4, userInfo: [NSLocalizedDescriptionKey: "Google sign-in is already in progress"])
        }
        inFlight = true
        defer { inFlight = false }

        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene ?? scenes.first as? UIWindowScene
        guard let rootVC = windowScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController ?? windowScene?.windows.first?.rootViewController else {
            throw NSError(domain: "GoogleSignIn", code: -1, userInfo: [NSLocalizedDescriptionKey: "Could not find root view controller for Google Sign-In presentation"])
        }
        let presenter = Self.topViewController(from: rootVC)

        let clientID = AppBackend.googleiOSClientID
        try Self.assertCallbackScheme(for: clientID)
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID, serverClientID: AppBackend.googleWebClientID)

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        guard let idToken = result.user.idToken?.tokenString else {
            throw NSError(domain: "GoogleSignIn", code: -2, userInfo: [NSLocalizedDescriptionKey: "Missing ID token from Google response"])
        }
        return idToken
        #else
        throw NSError(domain: "GoogleSignIn", code: -3, userInfo: [NSLocalizedDescriptionKey: "GoogleSignIn framework unavailable on device"])
        #endif
    }

    /// GIDSignIn raises an uncaught NSException when this scheme is missing.
    private static func assertCallbackScheme(for clientID: String) throws {
        let scheme = clientID
            .split(separator: ".")
            .reversed()
            .joined(separator: ".")
            .lowercased()
        let types = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        let registered = types.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] }.map { $0.lowercased() }
        guard registered.contains(scheme) else {
            throw NSError(
                domain: "GoogleSignIn",
                code: -5,
                userInfo: [NSLocalizedDescriptionKey: "Google sign-in cannot start because the app is missing URL scheme \(scheme)."]
            )
        }
    }

    private static func topViewController(from root: UIViewController) -> UIViewController {
        if let presented = root.presentedViewController, !presented.isBeingDismissed {
            return topViewController(from: presented)
        }
        if let navigation = root as? UINavigationController, let visible = navigation.visibleViewController {
            return topViewController(from: visible)
        }
        if let tab = root as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(from: selected)
        }
        return root
    }
}

public final class NetworkClient: @unchecked Sendable {
    public static let shared = NetworkClient()

    private let session: URLSession
    private let baseURL: URL

    public init(
        baseURL: URL = URL(string: AppBackend.apiBaseURL)!,
        session: URLSession = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
    }

    public func request<T: Decodable>(
        endpoint: String,
        method: HTTPMethod = .get,
        headers: [String: String] = [:],
        body: Data? = nil
    ) async throws -> T {
        return try await request(path: endpoint, method: method, queryItems: nil, headers: headers, body: body)
    }

    public func request<T: Decodable, B: Encodable>(
        path: String,
        method: HTTPMethod = .get,
        queryItems: [URLQueryItem]? = nil,
        headers: [String: String] = [:],
        body: B? = nil
    ) async throws -> T {
        var encBody: Data? = nil
        if let body = body {
            if let dataBody = body as? Data {
                encBody = dataBody
            } else {
                let encoder = JSONEncoder()
                encBody = try encoder.encode(body)
            }
        }
        return try await request(path: path, method: method, queryItems: queryItems, headers: headers, body: encBody)
    }

    public func request<T: Decodable>(
        path: String,
        method: HTTPMethod = .get,
        queryItems: [URLQueryItem]? = nil,
        headers: [String: String] = [:],
        body: Data? = nil
    ) async throws -> T {
        var base = baseURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if !base.hasPrefix("http://") && !base.hasPrefix("https://") {
            base = "https://" + base
        }
        let fullPath = path.hasPrefix("/") ? path : "/\(path)"
        guard var components = URLComponents(string: "\(base)\(fullPath)") else {
            throw URLError(.badURL)
        }
        if let queryItems = queryItems, !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        AuthSession.apply(to: &components, path: fullPath)
        guard let url = components.url else {
            throw URLError(.badURL)
        }

        var req = URLRequest(url: url)
        req.httpMethod = method.rawValue
        req.httpBody = body

        for (key, value) in headers {
            req.setValue(value, forHTTPHeaderField: key)
        }

        if req.value(forHTTPHeaderField: "Content-Type") == nil {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        AuthSession.apply(to: &req)

        let (data, response) = try await session.data(for: req)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if httpResponse.statusCode == 401, !AuthSession.isAuthPath(path) {
            let message = String(data: data, encoding: .utf8) ?? "Unauthorized"
            throw NSError(domain: "NetworkClient", code: 401, userInfo: [NSLocalizedDescriptionKey: message])
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "HTTP Error \(httpResponse.statusCode)"
            throw NSError(domain: "NetworkClient", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
        }

        let decoder = JSONDecoder()
        return try decoder.decode(T.self, from: data)
    }
}

// MARK: - Common Network DTOs

public struct StatusResponse: Codable, Equatable, Sendable {
    public let status: String
    public let message: String?

    public init(status: String = "ok", message: String? = nil) {
        self.status = status
        self.message = message
    }
}

public struct ApiUser: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let username: String
    public let displayName: String
    public let avatarUrl: String?
    public let isOnline: Bool?

    enum CodingKeys: String, CodingKey {
        case id, username
        case displayName = "display_name"
        case avatarUrl = "avatar_url"
        case isOnline = "is_online"
    }

    public init(id: String, username: String, displayName: String = "", avatarUrl: String? = nil, isOnline: Bool? = nil) {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.avatarUrl = avatarUrl
        self.isOnline = isOnline
    }
}

public struct LoginRequest: Codable, Sendable {
    public let username: String
    public let password: String

    public init(username: String, password: String) {
        self.username = username
        self.password = password
    }
}

public struct AuthResponse: Codable, Equatable, Sendable {
    public let token: String
    public let refreshToken: String?
    public let user: User?

    enum CodingKeys: String, CodingKey {
        case token, user
        case refreshToken = "refresh_token"
    }

    public init(token: String, refreshToken: String? = nil, user: User? = nil) {
        self.token = token
        self.refreshToken = refreshToken
        self.user = user
    }
}

public struct TokenRefreshRequest: Codable, Sendable {
    public let refreshToken: String

    enum CodingKeys: String, CodingKey {
        case refreshToken = "refresh_token"
    }

    public init(refreshToken: String) {
        self.refreshToken = refreshToken
    }
}

public struct RegisterRequest: Codable, Sendable {
    public let username: String
    public let email: String
    public let displayName: String
    public let phoneNumber: String
    public let password: String
    public let countryCode: String?

    enum CodingKeys: String, CodingKey {
        case username, email, password
        case displayName = "display_name"
        case phoneNumber = "phone_number"
        case countryCode = "country_code"
    }

    public init(username: String, email: String, displayName: String, phoneNumber: String, password: String, countryCode: String? = nil) {
        self.username = username
        self.email = email
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.password = password
        self.countryCode = countryCode
    }
}

public struct ForgotPasswordRequest: Codable, Sendable {
    public let emailOrUsername: String

    enum CodingKeys: String, CodingKey {
        case emailOrUsername = "email_or_username"
    }

    public init(emailOrUsername: String) {
        self.emailOrUsername = emailOrUsername
    }
}

public struct MuteRequest: Codable, Sendable {
    public let isMuted: Bool

    enum CodingKeys: String, CodingKey {
        case isMuted = "is_muted"
    }

    public init(isMuted: Bool) {
        self.isMuted = isMuted
    }
}

public struct AvatarUrlRequest: Codable, Sendable {
    public let avatarUrl: String

    enum CodingKeys: String, CodingKey {
        case avatarUrl = "avatar_url"
    }

    public init(avatarUrl: String) {
        self.avatarUrl = avatarUrl
    }
}

public struct UserIdRequest: Codable, Sendable {
    public let userId: String

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
    }

    public init(userId: String) {
        self.userId = userId
    }
}

public struct ToggleAdminPostingRequest: Codable, Sendable {
    public let adminsCanPost: Bool

    enum CodingKeys: String, CodingKey {
        case adminsCanPost = "admins_can_post"
    }

    public init(adminsCanPost: Bool) {
        self.adminsCanPost = adminsCanPost
    }
}
