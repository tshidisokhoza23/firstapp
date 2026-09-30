import SwiftUI

// MARK: - App Routes

public enum AppRoute: Hashable, Sendable {
    case main(tab: String? = nil, subTab: String? = nil)
    case chatDetail(conversationId: String, participantName: String?)
    case profile(userId: String)
    case editProfile
    case settings
    case accountSettings
    case privacySettings
    case notificationSettings
    case storageSettings
    case helpSettings
    case wallet
    case files
    case calendar
}

// MARK: - App Navigation Router View

public struct AppNavigation: View {
    @StateObject private var router = NavigationRouter()
    @StateObject private var globalCallState = GlobalCallState.shared

    public init() {}

    public var body: some View {
        NavigationStack(path: $router.path) {
            MainTabView(router: router)
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .main(let tab, let subTab):
                        MainTabView(router: router, initialTab: tab, initialSubTab: subTab)

                    case .chatDetail(let conversationId, let participantName):
                        ChatDetailPlaceholderView(conversationId: conversationId, name: participantName)

                    case .profile(let userId):
                        ProfileView(userId: userId, router: router)

                    case .editProfile:
                        EditProfilePlaceholderView()

                    case .settings:
                        SettingsView(router: router)

                    case .accountSettings:
                        SettingsSubDetailView(title: "Account Settings")

                    case .privacySettings:
                        SettingsSubDetailView(title: "Privacy & Security")

                    case .notificationSettings:
                        SettingsSubDetailView(title: "Notifications & Sound")

                    case .storageSettings:
                        SettingsSubDetailView(title: "Storage & Data")

                    case .helpSettings:
                        SettingsSubDetailView(title: "Help & Support")

                    case .wallet:
                        GenericPlaceholderScreen(title: "Wallet")

                    case .files:
                        GenericPlaceholderScreen(title: "Files Manager")

                    case .calendar:
                        GenericPlaceholderScreen(title: "Calendar")
                    }
                }
        }
        .environmentObject(router)
    }
}

// MARK: - Navigation Router Class

public final class NavigationRouter: ObservableObject {
    @Published public var path = NavigationPath()

    public init() {}

    public func navigate(to route: AppRoute) {
        path.append(route)
    }

    public func pop() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    public func popToRoot() {
        path.removeLast(path.count)
    }
}

// MARK: - Navigation Placeholder Subviews

struct ChatDetailPlaceholderView: View {
    let conversationId: String
    let name: String?

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "bubble.left.and.bubble.right.fill")
                .font(.system(size: 48))
                .foregroundColor(.blue)
            Text(name ?? "Chat Session")
                .font(.title2)
                .fontWeight(.bold)
            Text("Conversation ID: \(conversationId)")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .navigationTitle(name ?? "Chat")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct EditProfilePlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 48))
                .foregroundColor(.purple)
            Text("Edit Profile")
                .font(.title2)
                .fontWeight(.bold)
            Text("Update avatar, display name, bio, and location.")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SettingsSubDetailView: View {
    let title: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "gearshape.2.fill")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct GenericPlaceholderScreen: View {
    let title: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "folder.fill")
                .font(.system(size: 48))
                .foregroundColor(.green)
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
