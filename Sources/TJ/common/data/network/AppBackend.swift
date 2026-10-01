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

public enum APIError: Error, LocalizedError {
    case badURL
    case status(Int, String)

    public var errorDescription: String? {
        switch self {
        case .badURL: return "Invalid server URL"
        case .status(_, let message): return message
        }
    }
}

public enum API {
    public static func data(_ endpoint: String, method: String = "GET", body: Encodable? = nil) async throws -> Data {
        let full = AppBackend.apiBaseURL + (endpoint.hasPrefix("/") ? endpoint : "/\(endpoint)")
        guard let url = URL(string: full) else { throw APIError.badURL }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = SessionStore.shared.token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body {
            if let raw = body as? Data {
                request.httpBody = raw
            } else {
                request.httpBody = try JSONEncoder().encode(body)
            }
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
        guard (200...299).contains(code) else {
            let text = String(data: data, encoding: .utf8) ?? "HTTP \(code)"
            throw APIError.status(code, text)
        }
        return data
    }
}

public final class TJAPIProtocol: URLProtocol, @unchecked Sendable {
    public static var isInstalled = false
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
        guard let url = request.url, let host = url.host?.lowercased() else { return false }
        if host == "api.tj.app" || host == "api.example.com" { return true }
        return false
    }

    public override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    public override func startLoading() {
        guard let original = request.url else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        var path = original.path
        if let query = original.query, !query.isEmpty { path += "?\(query)" }
        let targetPath = Self.normalize(path: path)

        let targetURLString = AppBackend.apiBaseURL + (targetPath.hasPrefix("/") ? targetPath : "/\(targetPath)")
        guard let targetURL = URL(string: targetURLString) else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        var mutable = request
        mutable.url = targetURL
        if let token = SessionStore.shared.token, !token.isEmpty {
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
            if let data { client?.urlProtocolDidFinishLoading(self) }
            client?.urlProtocolDidFinishLoading(self)
        }
        forwardTask?.resume()
    }

    public override func stopLoading() {
        forwardTask?.cancel()
    }

    private static func normalize(path: String) -> String {
        var path = path
        if path.hasPrefix("/api/v1") {
            path = String(path.dropFirst("/api/v1".count))
        }
        if path.hasPrefix("/channels") {
            path = "/chats" + path
        }
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
            auth.start()
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
