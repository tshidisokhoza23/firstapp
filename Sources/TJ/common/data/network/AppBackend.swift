import Foundation
import UIKit
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

/// Same hosts as the Android `sit` and `prod` flavors.
public enum AppBackend {
    public static let googleiOSClientID = "303703683011-6ksjg0b8uakesl9a821ri05tokrg41f2.apps.googleusercontent.com"
    public static let googleWebClientID = "303703683011-g2p7atop0uackslr2eq9fnsja5jlfk7u.apps.googleusercontent.com"
    public static let stagingAPI = "https://firstappserver-staging.fly.dev/api/v1"
    public static let productionAPI = "https://firstappserver.fly.dev/api/v1"

    /// Default to Production backend (https://firstappserver.fly.dev/api/v1) matching Android prod build.
    public static var apiBaseURL: String {
        let stored = UserDefaults.standard.string(forKey: "tj_api_base")?.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if let stored, stored.hasPrefix("http") { return stored }
        return productionAPI
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
        return "https://firstappserver.fly.dev"
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

    public func saveSession(token: String, user: User? = nil) {
        save(token: token, refreshToken: nil, user: user)
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

/// Same auth attachment as Android's OkHttp interceptor: bearer header on every
/// non-auth call, plus the query token the server already accepts when a
/// redirect or URL protocol drops the header.
enum AuthSession {
    private static let refreshLock = NSLock()
    private static var refreshTask: Task<Bool, Never>?

    static func isAuthPath(_ path: String) -> Bool {
        let value = path.lowercased()
        return value.contains("/auth/login")
            || value.contains("/auth/register")
            || value.contains("/auth/refresh")
            || value.contains("/auth/logout")
            || value.contains("/auth/google")
            || value.contains("/auth/request-otp")
            || value.contains("/auth/verify-otp")
            || value.contains("/auth/forgot-password")
            || value.contains("/auth/availability")
    }

    static func bearer() -> String? {
        let token = SessionStore.shared.token?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let token, !token.isEmpty else { return nil }
        return token
    }

    static func apply(to components: inout URLComponents, path: String) {
        guard !isAuthPath(path), let token = bearer() else { return }
        var items = components.queryItems ?? []
        if !items.contains(where: { $0.name == "token" }) {
            items.append(URLQueryItem(name: "token", value: token))
        }
        components.queryItems = items
    }

    static func apply(to request: inout URLRequest) {
        guard let path = request.url?.path, !isAuthPath(path), let token = bearer() else { return }
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    }

    static func refresh() async -> Bool {
        refreshLock.lock()
        if let refreshTask {
            refreshLock.unlock()
            return await refreshTask.value
        }
        let task = Task { await performRefresh() }
        refreshTask = task
        refreshLock.unlock()
        let ok = await task.value
        refreshLock.lock()
        refreshTask = nil
        refreshLock.unlock()
        return ok
    }

    private static func performRefresh() async -> Bool {
        guard let refresh = SessionStore.shared.refreshToken?.trimmingCharacters(in: .whitespacesAndNewlines), !refresh.isEmpty else {
            return false
        }
        let base = AppBackend.apiBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: "\(base)/auth/refresh") else { return false }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["refresh_token": refresh])
        guard let (data, response) = try? await URLSession.shared.data(for: req),
              let http = response as? HTTPURLResponse else { return false }
        if http.statusCode == 400 || http.statusCode == 401 || http.statusCode == 403 {
            SessionStore.shared.clearSession()
            return false
        }
        guard (200...299).contains(http.statusCode),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let token = json["token"] as? String, !token.isEmpty else { return false }
        let nextRefresh = (json["refresh_token"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? refresh
        SessionStore.shared.save(token: token, refreshToken: nextRefresh, user: SessionStore.shared.user)
        return true
    }
}

public final class TJAPIProtocol: URLProtocol {
    private static let handled = "TJAPIProtocolHandled"
    private static let forwardSession: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = nil
        return URLSession(configuration: config)
    }()

    private var forwardTask: URLSessionDataTask?

    public static func install() {
        URLProtocol.registerClass(TJAPIProtocol.self)
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
        AuthSession.apply(to: &components, path: components.path)
        guard let url = components.url else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        let mutable = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
        URLProtocol.setProperty(true, forKey: Self.handled, in: mutable)
        mutable.url = url
        if !AuthSession.isAuthPath(components.path), let token = AuthSession.bearer() {
            mutable.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if mutable.value(forHTTPHeaderField: "Content-Type") == nil && request.httpMethod != "GET" {
            mutable.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if mutable.value(forHTTPHeaderField: "Accept") == nil {
            mutable.setValue("application/json", forHTTPHeaderField: "Accept")
        }

        let client = self.client
        forwardTask = Self.forwardSession.dataTask(with: mutable as URLRequest) { data, response, error in
            if let error {
                client?.urlProtocol(self, didFailWithError: error)
                return
            }
            if let response {
                let mime = (response as? HTTPURLResponse)?.mimeType?.lowercased() ?? ""
                let path = url.path.lowercased()
                let method = mutable.httpMethod?.uppercased() ?? "GET"
                let media = method == "GET" && (
                    mime.hasPrefix("image/") || mime.hasPrefix("video/") || mime.hasPrefix("audio/")
                    || path.contains("/uploads/") || path.contains("/storage/")
                )
                client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: media ? .allowed : .notAllowed)
            }
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

public enum AppRuntime {
    @MainActor
    public static let chatRepository = ChatRepositoryImpl.shared

    @MainActor
    public static func connectRealtime() {
        PushTokenRegistrar.shared.uploadPending()
        WebSocketManager.shared.reconnect()
        _ = chatRepository
        let token = SessionStore.shared.token ?? ""
        if !token.isEmpty {
            CallWebSocketClient.shared.connect(listener: CallSocketBridge.shared, serverUrl: AppBackend.wsBaseURL, authToken: token)
        }
    }
}
