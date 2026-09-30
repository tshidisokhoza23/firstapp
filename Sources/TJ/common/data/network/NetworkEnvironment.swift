import Foundation

public enum NetworkMode: String, Codable, Sendable, CaseIterable {
    case forceActive = "FORCE_ACTIVE"
    case forcePassive = "FORCE_PASSIVE"
}

public struct NetworkConstants {
    public static var defaultBaseURL: String = AppBackend.stagingAPI
    public static var defaultWsURL: String = AppBackend.stagingAPI.replacingOccurrences(of: "https://", with: "wss://")

    public static func formatMediaUrl(_ url: String?) -> String? {
        guard let url = url, !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        if url.hasPrefix("http://") || url.hasPrefix("https://") {
            return url
        }
        let base = defaultBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return "\(base)/\(url.trimmingCharacters(in: CharacterSet(charactersIn: "/")))"
    }
}

public final class NetworkEnvironmentManager: @unchecked Sendable {
    private let userDefaults: UserDefaults
    private let modeKey = "network_mode"

    public var activeUrl: String
    public var passiveUrl: String
    public var activeWsUrl: String
    public var passiveWsUrl: String

    public init(
        userDefaults: UserDefaults = .standard,
        activeUrl: String = AppBackend.stagingAPI,
        passiveUrl: String = AppBackend.productionAPI,
        activeWsUrl: String = AppBackend.stagingAPI.replacingOccurrences(of: "https://", with: "wss://"),
        passiveWsUrl: String = AppBackend.productionAPI.replacingOccurrences(of: "https://", with: "wss://")
    ) {
        self.userDefaults = userDefaults
        self.activeUrl = activeUrl
        self.passiveUrl = passiveUrl
        self.activeWsUrl = activeWsUrl
        self.passiveWsUrl = passiveWsUrl
    }

    public var mode: NetworkMode {
        get {
            guard let raw = userDefaults.string(forKey: modeKey),
                  let m = NetworkMode(rawValue: raw) else {
                return .forceActive
            }
            return m
        }
        set {
            userDefaults.set(newValue.rawValue, forKey: modeKey)
        }
    }

    public var currentUrl: String {
        switch mode {
        case .forceActive:
            return activeUrl
        case .forcePassive:
            return passiveUrl
        }
    }

    public var currentWsUrl: String {
        switch mode {
        case .forceActive:
            return activeWsUrl
        case .forcePassive:
            return passiveWsUrl
        }
    }
}

public struct NetworkRequestInterceptor: Sendable {
    private let environmentManager: NetworkEnvironmentManager

    public init(environmentManager: NetworkEnvironmentManager) {
        self.environmentManager = environmentManager
    }

    public func prepareRequest(_ original: URLRequest, authToken: String? = nil) -> URLRequest {
        var request = original

        if let originalURL = original.url,
           let envURL = URL(string: environmentManager.currentUrl),
           let scheme = envURL.scheme,
           let host = envURL.host {
            var components = URLComponents(url: originalURL, resolvingAgainstBaseURL: false)
            components?.scheme = scheme
            components?.host = host
            if let port = envURL.port {
                components?.port = port
            }
            if let newURL = components?.url {
                request.url = newURL
            }
        }

        if request.value(forHTTPHeaderField: "Content-Type") == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        if let token = authToken, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        return request
    }
}
