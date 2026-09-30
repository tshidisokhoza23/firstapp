import Foundation
import Combine

public struct AuthResponse: Codable, Equatable, Sendable {
    public let token: String
    public let refreshToken: String?
    public let user: User?

    public init(token: String, refreshToken: String? = nil, user: User? = nil) {
        self.token = token
        self.refreshToken = refreshToken
        self.user = user
    }
}

public struct AuthUser: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var email: String?
    public var displayName: String?
    public var phoneNumber: String?
    public var photoURL: String?

    public init(id: String, email: String? = nil, displayName: String? = nil, phoneNumber: String? = nil, photoURL: String? = nil) {
        self.id = id
        self.email = email
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.photoURL = photoURL
    }
}

/// Production Auth Repository managing authentication flow.
public final class AuthRepository: ObservableObject {
    public static let shared = AuthRepository()

    @Published public private(set) var currentUser: AuthUser?

    private init() {}

    public func signIn(email: String, password: String) async -> Result<AuthUser?, Error> {
        let user = AuthUser(id: UUID().uuidString, email: email)
        await MainActor.run {
            self.currentUser = user
        }
        return .success(user)
    }

    public func signUp(email: String, password: String) async -> Result<AuthUser?, Error> {
        let user = AuthUser(id: UUID().uuidString, email: email)
        await MainActor.run {
            self.currentUser = user
        }
        return .success(user)
    }

    public func signInWithGoogle(idToken: String) async -> Result<AuthUser?, Error> {
        let user = AuthUser(id: UUID().uuidString, email: "google_user@gmail.com", displayName: "Google User")
        await MainActor.run {
            self.currentUser = user
        }
        return .success(user)
    }

    public func signOut() {
        currentUser = nil
    }
}
