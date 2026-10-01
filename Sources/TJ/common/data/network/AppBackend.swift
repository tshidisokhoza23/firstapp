import Foundation
import AuthenticationServices
import UIKit

/// Same hosts as the Android `sit` (default) and `prod` flavors.
public enum AppBackend {
    public static let googleiOSClientID = "303703683011-6ksjg0b8uakesl9a821ri05tokrg41f2.apps.googleusercontent.com"
    public static let googleWebClientID = "303703683011-g2p7atop0uackslr2eq9fnsja5jlfk7u.apps.googleusercontent.com"
    public static let stagingAPI = "https://firstappserver-staging.fly.dev/api/v1"
    public static let productionAPI = "https://firstappserver.fly.dev/api/v1"

    /// Matches Android's default `sit` flavor so both apps hit the same database.
    public static var apiBaseURL: String {
        let stored = UserDefaults.standard.string(forKey: "tj_api_base")?.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if let stored, stored.hasPrefix("http") { return stored }
        return stagingAPI
    }

    public static var wsBaseURL: String {
        apiBaseURL
            .replacingOccurrences(of: "https://", with: "wss://")
            .replacingOccurrences(of: "http://", with: "ws://")
    }

    public static var origin: String {
        if let url = URL(string: apiBaseURL), let host = url.host, let scheme = url.scheme {
            return "\(scheme)://\(host)"
        }
        return "https://firstappserver-staging.fly.dev"
    }
}

public final class SessionStore: @unchecked Sendable {
    public static let shared = SessionStore()
    public static let didChange = Notification.Name("tj.session.didChange")

    private let defaults = UserDefaults.standard
    private let tokenKey = "auth_token"
    private let refreshKey = "refresh_token"
    private let userKey = "current_user_json"

    public var token: String? {
        let value = defaults.string(forKey: tokenKey)
        return (value?.isEmpty == false) ? value : nil
    }

    public var refreshToken: String? {
        defaults.string(forKey: refreshKey)
    }

    public var userId: String? { user?.id }

    public var user: User? {
        guard let data = defaults.data(forKey: userKey) else { return nil }
        return try? JSONDecoder().decode(User.self, from: data)
    }

    public var isLoggedIn: Bool { token != nil }

    public func save(token: String, refreshToken: String?, user: User?) {
        defaults.set(token, forKey: tokenKey)
        if let refreshToken, !refreshToken.isEmpty {
            defaults.set(refreshToken, forKey: refreshKey)
        }
        if let user, let data = try? JSONEncoder().encode(user) {
            defaults.set(data, forKey: userKey)
        }
        NotificationCenter.default.post(name: Self.didChange, object: nil)
    }

    public func updateUser(_ user: User) {
        if let data = try? JSONEncoder().encode(user) {
            defaults.set(data, forKey: userKey)
        }
        NotificationCenter.default.post(name: Self.didChange, object: nil)
    }

    public func clearSession() {
        defaults.removeObject(forKey: tokenKey)
        defaults.removeObject(forKey: refreshKey)
        defaults.removeObject(forKey: userKey)
        NotificationCenter.default.post(name: Self.didChange, object: nil)
    }

    public func clear() {
        clearSession()
    }
}

public enum APIError: Error, LocalizedError {
    case badURL
    case status(Int, String)

    public var errorDescription: String? {
        switch self {
        case .badURL: return "Invalid server URL"
        case .status(let code, let message): return message.isEmpty ? "Server error \(code)" : message
        }
    }
}

public enum API {
    public static func url(_ path: String, query: [String: String] = [:]) -> URL? {
        let base = AppBackend.apiBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let clean = path.hasPrefix("/") ? String(path.dropFirst()) : path
        guard var components = URLComponents(string: "\(base)/\(clean)") else { return nil }
        if !query.isEmpty {
            components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        return components.url
    }

    public static func request(_ path: String, method: String = "GET", query: [String: String] = [:], body: Data? = nil, authorized: Bool = true) -> URLRequest? {
        guard let url = url(path, query: query) else { return nil }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        if authorized, let token = SessionStore.shared.token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        req.httpBody = body
        return req
    }

    public static func data(
        _ path: String,
        method: String = "GET",
        query: [String: String] = [:],
        json: [String: Any]? = nil,
        body: Encodable? = nil,
        authorized: Bool = true
    ) async throws -> Data {
        var encBody: Data? = nil
        if let json = json {
            encBody = try? JSONSerialization.data(withJSONObject: json)
        } else if let body = body {
            if let dataBody = body as? Data {
                encBody = dataBody
            } else {
                encBody = try? JSONEncoder().encode(body)
            }
        }
        guard var req = request(path, method: method, query: query, body: encBody, authorized: authorized) else {
            throw APIError.badURL
        }
        if method == "GET" { req.httpBody = nil }
        let (data, response) = try await URLSession.shared.data(for: req)
        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
        guard (200...299).contains(code) else {
            let message = String(data: data, encoding: .utf8) ?? ""
            throw APIError.status(code, message)
        }
        return data
    }

    public static func getJSON(_ path: String, query: [String: String] = [:]) async throws -> [String: Any] {
        let data = try await data(path, query: query)
        return (try JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }
}

public final class TJAPIProtocol: URLProtocol, @unchecked Sendable {
    public static var isInstalled = false
    private static let handled = "TJAPIProtocolHandled"
    private static let forward: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = []
        return URLSession(configuration: config)
    }()
    private var forwardTask: URLSessionDataTask?

    public static func install() {
        URLProtocol.registerClass(TJAPIProtocol.self)
        isInstalled = true
    }

    public override class func canInit(with request: URLRequest) -> Bool {
        guard URLProtocol.property(forKey: handled, in: request) == nil,
              let host = request.url?.host?.lowercased() else { return false }
        return host.contains("tj.app")
            || host.contains("example.com")
            || host.contains("firstappserver")
    }

    public override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    public override func startLoading() {
        guard let original = request.url, var components = URLComponents(url: original, resolvingAgainstBaseURL: false) else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        let host = components.host?.lowercased() ?? ""
        let fake = host.contains("tj.app") || host.contains("example.com")
        if fake, let base = URL(string: AppBackend.apiBaseURL) {
            components.scheme = base.scheme
            components.host = base.host
            components.port = base.port
        }
        components.path = Self.normalize(path: components.path)
        if components.path == "/api/v1/mingle/feed" {
            components.queryItems = components.queryItems?.map { item in
                item.name == "radius" ? URLQueryItem(name: "radius_km", value: item.value) : item
            }
        }
        if components.path == "/api/v1/search" {
            components.queryItems = components.queryItems?.map { item in
                item.name == "q" ? URLQueryItem(name: "query", value: item.value) : item
            }
        }
        guard let url = components.url else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        let mutable = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
        URLProtocol.setProperty(true, forKey: Self.handled, in: mutable)
        mutable.url = url
        if mutable.value(forHTTPHeaderField: "Authorization") == nil, let token = SessionStore.shared.token {
            mutable.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if mutable.value(forHTTPHeaderField: "Content-Type") == nil {
            mutable.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if mutable.value(forHTTPHeaderField: "Accept") == nil {
            mutable.setValue("application/json", forHTTPHeaderField: "Accept")
        }

        let client = self.client
        forwardTask = Self.forward.dataTask(with: mutable as URLRequest) { data, response, error in
            if let error {
                client?.urlProtocol(self, didFailWithError: error)
                return
            }
            if let response { client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed) }
            if let data { client?.urlProtocol(self, didLoad: data) }
            client?.urlProtocolDidFinishLoading(self)
        }
        forwardTask?.resume()
    }

    public override func stopLoading() {
        forwardTask?.cancel()
    }

    private static func normalize(path: String) -> String {
        var path = path
        if path.hasPrefix("/v1/") { path = String(path.dropFirst(3)) }
        if !path.hasPrefix("/api/v1") {
            if path == "/v1" { path = "" }
            path = "/api/v1" + (path.hasPrefix("/") ? path : "/\(path)")
        }
        if path.hasPrefix("/api/v1/music/") {
            path = "/api/v1/" + path.dropFirst("/api/v1/music/".count)
        }
        if path.hasPrefix("/api/v1/drive/") {
            path = "/api/v1/" + path.dropFirst("/api/v1/drive/".count)
        }
        if path == "/api/v1/stories" || path == "/api/v1/stories/feed" {
            path = "/api/v1/chats/stories"
        } else if path.hasPrefix("/api/v1/stories/") {
            path = "/api/v1/chats/stories/" + path.dropFirst("/api/v1/stories/".count)
        }
        if path == "/api/v1/chat/direct/list" || path == "/api/v1/chat/groups/list" {
            path = "/api/v1/conversations"
        } else if path.hasPrefix("/api/v1/chat/groups/") {
            path = "/api/v1/chats/groups/" + path.dropFirst("/api/v1/chat/groups/".count)
        } else if path.hasPrefix("/api/v1/chat/direct/") {
            path = "/api/v1/chats/" + path.dropFirst("/api/v1/chat/".count)
        }
        if path == "/api/v1/mingle/profile" { path = "/api/v1/mingle/me" }
        if path.hasPrefix("/api/v1/channels/") && path.hasSuffix("/subscribe") {
            let id = path.dropFirst("/api/v1/channels/".count).dropLast("/subscribe".count)
            path = "/api/v1/chats/channels/\(id)/subscribe"
        }
        return path
    }
}

public final class GoogleSignInService: NSObject, ASWebAuthenticationPresentationContextProviding {
    public static let shared = GoogleSignInService()
    private var session: ASWebAuthenticationSession?

    public func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }

    public func signIn() async throws -> String {
        let iosClientID = AppBackend.googleiOSClientID
        let scheme = "com.googleusercontent.apps.303703683011-6ksjg0b8uakesl9a821ri05tokrg41f2"
        let nonce = UUID().uuidString
        var components = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        components.queryItems = [
            URLQueryItem(name: "client_id", value: iosClientID),
            URLQueryItem(name: "redirect_uri", value: "\(scheme):/oauth2redirect"),
            URLQueryItem(name: "response_type", value: "id_token"),
            URLQueryItem(name: "scope", value: "openid email profile"),
            URLQueryItem(name: "nonce", value: nonce),
            URLQueryItem(name: "prompt", value: "select_account")
        ]
        guard let url = components.url else { throw APIError.badURL }
        return try await withCheckedThrowingContinuation { continuation in
            let auth = ASWebAuthenticationSession(url: url, callbackURLScheme: scheme) { callback, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let callback else {
                    continuation.resume(throwing: APIError.status(-1, "Google sign-in returned no token"))
                    return
                }
                let raw = callback.absoluteString
                let fragment = callback.fragment ?? ""
                let query = callback.query ?? ""
                let bag = fragment + "&" + query + "&" + raw
                if let token = Self.value("id_token", in: bag), !token.isEmpty {
                    continuation.resume(returning: token)
                    return
                }
                let message = Self.value("error_description", in: bag) ?? Self.value("error", in: bag) ?? "Google sign-in failed"
                continuation.resume(throwing: APIError.status(-1, message.removingPercentEncoding ?? message))
            }
            auth.presentationContextProvider = self
            auth.prefersEphemeralWebBrowserSession = false
            self.session = auth
            if !auth.start() {
                continuation.resume(throwing: APIError.status(-1, "Unable to open Google sign-in"))
            }
        }
    }

    private static func value(_ key: String, in text: String) -> String? {
        let pattern = "(?:[?&#]|\n|^)" + NSRegularExpression.escapedPattern(for: key) + "=([^&#\n]*)"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let range = NSRange(text.startIndex..., in: text)
        guard let match = regex.firstMatch(in: text, range: range), match.numberOfRanges > 1 else { return nil }
        guard let r = Range(match.range(at: 1), in: text) else { return nil }
        return String(text[r])
    }
}

/// Shared chat socket and repository, same endpoints Android uses.
public enum AppRuntime {
    public static let database = LocalDatabase()
    public static let socket = WebSocketManager(
        tokenProvider: { SessionStore.shared.token },
        wsURLProvider: { AppBackend.wsBaseURL }
    )
    public static let chatApi = URLSessionChatApi(
        baseURLProvider: { AppBackend.apiBaseURL },
        tokenProvider: { SessionStore.shared.token }
    )
    public static let chatRepository = ChatRepositoryImpl(
        chatApi: chatApi,
        localDb: database,
        webSocketManager: socket,
        currentUserIdProvider: { SessionStore.shared.userId }
    )

    public static func connectRealtime() {
        guard SessionStore.shared.token != nil else { return }
        socket.connect()
    }
}
