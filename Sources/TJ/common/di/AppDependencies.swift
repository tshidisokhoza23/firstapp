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
    public let userApi: UserApi
    public let chatApi: ChatApi
    public let channelApi: ChannelApi
    public let jobApi: JobApi
    public let socialApi: SocialApi
    public let mediaApi: MediaApi
    public let driveApi: DriveApi
    public let rewardsApi: RewardsApi
    public let mingleApi: MingleApi
    public let systemApi: SystemApi

    public let chatRepository: ChatRepository
    public let jobRepository: JobRepository
    public let globalCallState: GlobalCallState

    public init(
        sessionStore: SessionStore = SessionStore.shared,
        localDb: LocalDatabase = LocalDatabase.shared
    ) {
        self.sessionStore = sessionStore
        self.localDb = localDb

        self.networkClient = NetworkClient.shared
        self.authApi = AuthApi.shared
        self.userApi = UserApi.shared
        self.chatApi = ChatApi.shared
        self.channelApi = ChannelApi.shared
        self.jobApi = JobApi.shared
        self.socialApi = SocialApi.shared
        self.mediaApi = MediaApi.shared
        self.driveApi = DriveApi.shared
        self.rewardsApi = RewardsApi.shared
        self.mingleApi = MingleApi.shared
        self.systemApi = SystemApi.shared

        self.webSocketManager = WebSocketManager.shared

        self.chatRepository = ChatRepositoryImpl.shared
        self.jobRepository = JobRepositoryImpl.shared

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
