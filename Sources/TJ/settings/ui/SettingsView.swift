import SwiftUI

public struct SettingsView: View {
    @ObservedObject var router: NavigationRouter

    @State private var showLogoutConfirm: Bool = false
    @State private var showResetConfirm: Bool = false
    @State private var showDeleteAccountConfirm: Bool = false
    @State private var deleteFailure: String?
    @State private var localPage: SettingsLocalPage?

    public init(router: NavigationRouter) {
        self.router = router
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // User Profile Header Card
                HStack(spacing: 16) {
                    AsyncImage(url: FeedPost.resolve(SessionStore.shared.user?.avatarUrl)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle()
                            .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .overlay(
                                Image(systemName: "person.fill")
                                    .font(.title)
                                    .foregroundColor(.white)
                            )
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text(SessionStore.shared.user?.displayName.isEmpty == false ? SessionStore.shared.user!.displayName : "Your account")
                            .font(.title3)
                            .fontWeight(.bold)

                        if let username = SessionStore.shared.user?.username, !username.isEmpty {
                            Text("@\(username)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        if let phone = SessionStore.shared.user?.phoneNumber, !phone.isEmpty {
                            Text(phone)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Text((SessionStore.shared.user?.bio?.isEmpty == false ? SessionStore.shared.user?.bio : nil) ?? "No bio set")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }

                    Spacer()

                    Button(action: { localPage = .editProfile }) {
                        Image(systemName: "pencil.circle.fill")
                            .font(.title2)
                            .foregroundColor(.accentColor)
                    }
                }
                .padding(16)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(20)
                .padding(.horizontal, 16)

                // Preferences Section
                VStack(alignment: .leading, spacing: 12) {
                    Text("PREFERENCES")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        SettingsGridCard(
                            icon: "bubble.left.and.bubble.right.fill",
                            title: "Chats",
                            color: .blue
                        ) {
                            localPage = .chats
                        }

                        SettingsGridCard(
                            icon: "bell.fill",
                            title: "Alerts",
                            color: .orange
                        ) {
                            localPage = .alerts
                        }

                        SettingsGridCard(
                            icon: "lock.shield.fill",
                            title: "Security",
                            color: .green
                        ) {
                            localPage = .security
                        }

                        SettingsGridCard(
                            icon: "internaldrive.fill",
                            title: "Storage & Data",
                            color: .teal
                        ) {
                            localPage = .storage
                        }

                        SettingsGridCard(
                            icon: "laptopcomputer.and.iphone",
                            title: "Sessions",
                            color: .purple
                        ) {
                            localPage = .sessions
                        }

                        SettingsGridCard(
                            icon: "globe",
                            title: "Language",
                            color: .pink
                        ) {
                            localPage = .language
                        }
                    }
                    .padding(.horizontal, 16)
                }

                // Support Section
                VStack(alignment: .leading, spacing: 12) {
                    Text("SUPPORT")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        SettingsGridCard(
                            icon: "questionmark.circle.fill",
                            title: "Help Center",
                            color: .yellow
                        ) {
                            localPage = .help
                        }

                        SettingsGridCard(
                            icon: "doc.text.fill",
                            title: "Terms & EULA",
                            color: .indigo
                        ) {
                            if let url = URL(string: "https://firstappserver.fly.dev/privacy") {
                                UIApplication.shared.open(url)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                }

                // Management Actions Section
                VStack(alignment: .leading, spacing: 8) {
                    Text("MANAGEMENT")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)

                    VStack(spacing: 0) {
                        SettingsRowButton(icon: "arrow.triangle.2.circlepath", title: "Refresh & Sync Contacts", color: .blue) {
                            showResetConfirm = true
                        }

                        Divider().padding(.leading, 48)

                        SettingsRowButton(icon: "rectangle.portrait.and.arrow.right", title: "Log Out", color: .primary) {
                            showLogoutConfirm = true
                        }

                        Divider().padding(.leading, 48)

                        SettingsRowButton(icon: "trash.fill", title: "Terminate Account", color: .red) {
                            showDeleteAccountConfirm = true
                        }
                    }
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }

                // Footer
                VStack(spacing: 4) {
                    Text("TJ MESSENGER")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                    if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String, !version.isEmpty {
                        Text(version)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
            .padding(.vertical, 16)
        }
        .safeAreaInset(edge: .bottom) { LiveBannerAd() }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $localPage) { page in
            NavigationStack {
                VStack(spacing: 0) {
                switch page {
                case .chats: ChatSettingsForm()
                case .alerts: AlertSettingsForm()
                case .language: LanguageSettingsForm()
                case .security: SecuritySettingsForm()
                case .storage: StorageSettingsForm()
                case .sessions: SettingsSubDetailView(title: "Sessions")
                case .help: HelpSettingsForm()
                case .account: AccountView()
                case .editProfile: EditProfilePlaceholderView()
                }
                LiveBannerAd()
                }
            }
        }
        .alert("Log Out", isPresented: $showLogoutConfirm) {
            Button("Log Out", role: .destructive) {
                Task {
                    await SessionActions.logout()
                    router.popToRoot()
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to log out of your account?")
        }
        .alert("Reset & Sync", isPresented: $showResetConfirm) {
            Button("Sync Now") {
                Task { await SessionActions.syncContacts() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will refresh local chat caches and re-synchronize contacts.")
        }
        .alert("Delete Account", isPresented: $showDeleteAccountConfirm) {
            Button("Delete Forever", role: .destructive) {
                Task {
                    let removed = await SessionActions.deleteAccount()
                    if removed {
                        router.popToRoot()
                    } else {
                        deleteFailure = "Account was not deleted. The server did not remove this account."
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This action is permanent and cannot be undone. All data will be removed.")
        }
        .alert("Account", isPresented: Binding(get: { deleteFailure != nil }, set: { if !$0 { deleteFailure = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(deleteFailure ?? "")
        }
    }
}

enum SettingsLocalPage: String, Identifiable {
    case chats, alerts, language, security, storage, sessions, help, account, editProfile
    var id: String { rawValue }
}
// MARK: - Helper Card Subcomponents

struct SettingsGridCard: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: icon)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(color)
                    )

                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(16)
        }
    }
}

struct SettingsRowButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(color)
                    .frame(width: 24)

                Text(title)
                    .font(.body)
                    .foregroundColor(color == .red ? .red : .primary)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
    }
}
