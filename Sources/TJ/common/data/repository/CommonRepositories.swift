import Foundation

public struct DevResetResponse: Codable, Equatable, Sendable {
    public let status: String
    public let username: String
    public let password: String

    public init(status: String = "ok", username: String = "", password: String = "") {
        self.status = status
        self.username = username
        self.password = password
    }
}

// MARK: - System Repository Protocol
public protocol SystemRepository {
    func clearCache() async
    func clearAllData() async
    func devResetAndSeed(contacts: [ContactInfo]) async -> AsyncStream<Resource<DevResetResponse>>
}

// MARK: - System Repository Implementation
public final class SystemRepositoryImpl: SystemRepository {
    private let systemApi: SystemApiService

    public init(systemApi: SystemApiService = SystemApi()) {
        self.systemApi = systemApi
    }

    public func clearCache() async {
        print("[SystemRepositoryImpl] Clearing cache database and local stores...")
        URLCache.shared.removeAllCachedResponses()
    }

    public func clearAllData() async {
        await clearCache()
        print("[SystemRepositoryImpl] Clearing all persistent user data...")
        UserDefaults.standard.removeObject(forKey: "auth_token")
    }

    public func devResetAndSeed(contacts: [ContactInfo]) async -> AsyncStream<Resource<DevResetResponse>> {
        return AsyncStream { continuation in
            Task {
                continuation.yield(.loading())
                do {
                    let response = try await systemApi.devResetAndSeed(contacts: contacts)
                    await clearAllData()
                    continuation.yield(.success(response))
                } catch {
                    continuation.yield(.error(error.localizedDescription))
                }
                continuation.finish()
            }
        }
    }
}
