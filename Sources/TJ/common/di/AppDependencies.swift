import Foundation
import Combine
import SwiftUI

/// Centralized Dependency Injection Container for TJ App.
/// Manages SessionStore, NetworkClient, Repositories, APIs, and Services in one unified place.
public final class AppDependencies: ObservableObject, @unchecked Sendable {
    public static let shared = AppDependencies()

    public let sessionStore: SessionStore
    public let networkClient: NetworkClient
    public let localDb: LocalDatabase
    public let webSocketManager: WebSocketManager
    public let authApi: AuthApi
    public let chatApi: ChatApi
    public let jobApi: JobApi
    public let chatRepository: ChatRepository
    public let jobRepository: JobRepository
    public let globalCallState: GlobalCallState

    public init(
        sessionStore: SessionStore = SessionStore.shared,
        localDb: LocalDatabase = LocalDatabase.shared
    ) {
        self.sessionStore = sessionStore
        self.localDb = localDb

        self.networkClient = NetworkClient(
            baseURL: URL(string: AppBackend.apiBaseURL)!,
            sessionStore: sessionStore
        )
        self.authApi = AuthApi(client: networkClient)
        self.chatApi = ChatApi(client: networkClient)
        self.jobApi = JobApi(
            baseURL: URL(string: AppBackend.apiBaseURL + "/")!,
            sessionStore: sessionStore
        )

        self.webSocketManager = WebSocketManager(
            sessionStore: sessionStore,
            wsURLProvider: { AppBackend.wsBaseURL }
        )

        self.chatRepository = ChatRepositoryImpl(
            chatApi: chatApi,
            localDb: localDb,
            webSocketManager: webSocketManager,
            sessionStore: sessionStore
        )

        self.jobRepository = JobRepositoryImpl(
            jobApi: jobApi,
            localStore: JobLocalStore()
        )

        self.globalCallState = GlobalCallState.shared
    }
}

// MARK: - SwiftUI Environment Key

private struct AppDependenciesKey: EnvironmentKey {
    static let defaultValue: AppDependencies = AppDependencies.shared
}

public extension EnvironmentValues {
    var appDependencies: AppDependencies {
        get { self[AppDependenciesKey.self] }
        set { self[AppDependenciesKey.self] = newValue }
    }
}
