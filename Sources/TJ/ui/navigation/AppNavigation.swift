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
                        LiveChatScreen(conversationId: conversationId, name: participantName)

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
                        WalletView()

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

struct LiveChatScreen: View {
    let conversationId: String
    let name: String?
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel = ChatViewModel()
    @State private var chat: ChatMessage?

    var body: some View {
        Group {
            if let chat {
                ChatView(
                    chat: chat,
                    viewModel: viewModel,
                    onBack: { router.pop() },
                    onShowProfile: { userId, _ in
                        let id = userId.isEmpty ? (chat.otherUserId ?? "") : userId
                        if !id.isEmpty { router.navigate(to: .profile(userId: id)) }
                    },
                    onAvatarClick: { userId in
                        if !userId.isEmpty { router.navigate(to: .profile(userId: userId)) }
                    }
                )
            } else {
                ProgressView("Opening chat...")
            }
        }
        .task {
            viewModel.fetchCurrentUser()
            viewModel.fetchChats()
            let loaded = await AppRuntime.chatRepository.getChats().data ?? []
            if let match = loaded.first(where: { $0.id == conversationId || $0.otherUserId == conversationId }) {
                chat = match
                viewModel.fetchMessages(chatId: match.id)
                return
            }
            if let data = try? await API.data("chats/dm", method: "POST", json: ["contact_id": conversationId]),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let convID = json["conversation_id"] as? String, !convID.isEmpty {
                chat = ChatMessage(id: convID, senderName: resolvedRouteName(name), otherUserId: conversationId)
                viewModel.fetchMessages(chatId: convID)
                return
            }
            chat = ChatMessage(id: conversationId, senderName: resolvedRouteName(name), otherUserId: conversationId)
            viewModel.fetchMessages(chatId: conversationId)
        }
    }
}

private func resolvedRouteName(_ name: String?) -> String {
    let trimmed = name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let blocked = ["chat", "direct", "mingle", "mingle user", "unknown"]
    if trimmed.isEmpty || blocked.contains(trimmed.lowercased()) { return "" }
    return trimmed
}

struct ChatDetailPlaceholderView: View {
    let conversationId: String
    let name: String?

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "bubble.left.and.bubble.right.fill")
                .font(.system(size: 48))
                .foregroundColor(.blue)
            Text(resolvedRouteName(name))
                .font(.title2)
                .fontWeight(.bold)
            Text(conversationId)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .navigationTitle(resolvedRouteName(name))
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct EditProfilePlaceholderView: View {
    @State private var name = SessionStore.shared.user?.displayName ?? ""
    @State private var bio = SessionStore.shared.user?.bio ?? ""
    @State private var location = SessionStore.shared.user?.location ?? ""
    @State private var status = ""

    var body: some View {
        Form {
            TextField("Display name", text: $name)
            TextField("Bio", text: $bio)
            TextField("Location", text: $location)
            Button("Save") {
                Task {
                    do {
                        _ = try await API.data("users/me", method: "PUT", json: [
                            "display_name": name,
                            "bio": bio,
                            "location": location
                        ])
                        if let data = try? await API.data("users/me"),
                           let envelope = try? JSONDecoder().decode(UserEnvelope.self, from: data) {
                            SessionStore.shared.updateUser(envelope.user)
                        }
                        status = "Saved"
                    } catch {
                        status = "Could not save profile"
                    }
                }
            }
            if !status.isEmpty {
                Text(status).foregroundColor(.secondary)
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SettingsSubDetailView: View {
    let title: String
    @State private var note = ""
    @State private var readReceipts = true
    @State private var showOnline = true
    @State private var notificationsOn = UserDefaults.standard.bool(forKey: "tj_notifications_on")

    var body: some View {
        Form {
            switch title {
            case "Privacy & Security":
                Toggle("Read receipts", isOn: $readReceipts)
                Toggle("Show online status", isOn: $showOnline)
                Button("Save privacy") { Task { await savePrivacy() } }
            case "Notifications & Sound":
                Toggle("Notifications", isOn: $notificationsOn)
                    .onChange(of: notificationsOn) { value in
                        UserDefaults.standard.set(value, forKey: "tj_notifications_on")
                        note = value ? "Notifications on" : "Notifications off"
                    }
            case "Storage & Data", "Chats & Media":
                Button("Clear cached media") {
                    URLCache.shared.removeAllCachedResponses()
                    note = "Cache cleared"
                }
            case "Account Settings":
                Text(SessionStore.shared.user?.displayName ?? "Signed in")
                Button("Log out") { Task { await SessionActions.logout() } }
            case "Help & Support":
                if let privacy = URL(string: "https://firstappserver.fly.dev/privacy") {
                    Link("Privacy policy", destination: privacy)
                }
                if let terms = URL(string: "https://firstappserver.fly.dev/terms") {
                    Link("Terms", destination: terms)
                }
            default:
                Text(title)
            }
            if !note.isEmpty {
                Text(note).foregroundColor(.secondary)
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .task { await loadPrivacy() }
    }

    private func loadPrivacy() async {
        guard title == "Privacy & Security",
              let data = try? await API.data("users/privacy"),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        if let value = json["show_read_receipts"] as? Bool { readReceipts = value }
        if let value = json["show_online_status"] as? String { showOnline = value != "nobody" }
    }

    private func savePrivacy() async {
        do {
            _ = try await API.data("users/privacy", method: "PUT", json: [
                "show_read_receipts": readReceipts,
                "show_online_status": showOnline ? "everyone" : "nobody"
            ])
            note = "Saved"
        } catch {
            note = "Could not save privacy"
        }
    }
}

enum SessionActions {
    static func logout() async {
        _ = try? await API.data("auth/logout", method: "POST")
        SessionStore.shared.clear()
    }

    static func deleteAccount() async {
        _ = try? await API.data("users/me", method: "DELETE")
        await logout()
    }

    static func syncContacts() async {
        _ = try? await API.data("users/persistent-contacts")
        _ = try? await API.data("users/sync-contacts", method: "POST", json: ["phone_numbers": []])
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
