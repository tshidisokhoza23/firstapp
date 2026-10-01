import Foundation

public final class SessionStore: ObservableObject {
    public static let shared = SessionStore()

    private let tokenKey = "auth_token"
    private let refreshTokenKey = "auth_refresh_token"
    private let userKey = "auth_user"

    @Published public private(set) var token: String?
    @Published public private(set) var refreshToken: String?
    @Published public private(set) var user: User?

    public var isLoggedIn: Bool {
        guard let token, !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        return true
    }

    private init() {
        self.token = UserDefaults.standard.string(forKey: tokenKey)
        self.refreshToken = UserDefaults.standard.string(forKey: refreshTokenKey)
        if let data = UserDefaults.standard.data(forKey: userKey),
           let savedUser = try? JSONDecoder().decode(User.self, from: data) {
            self.user = savedUser
        }
    }

    public func save(token: String, refreshToken: String? = nil, user: User? = nil) {
        let clean = token.trimmingCharacters(in: .whitespacesAndNewlines)
        self.token = clean
        UserDefaults.standard.set(clean, forKey: tokenKey)
        if let refreshToken {
            self.refreshToken = refreshToken
            UserDefaults.standard.set(refreshToken, forKey: refreshTokenKey)
        }
        if let user {
            updateUser(user)
        }
    }

    public func saveSession(token: String, user: User? = nil) {
        save(token: token, refreshToken: nil, user: user)
    }

    public func updateUser(_ user: User) {
        self.user = user
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: userKey)
        }
    }

    public func clearSession() {
        self.token = nil
        self.refreshToken = nil
        self.user = nil
        UserDefaults.standard.removeObject(forKey: tokenKey)
        UserDefaults.standard.removeObject(forKey: refreshTokenKey)
        UserDefaults.standard.removeObject(forKey: userKey)
    }
}
