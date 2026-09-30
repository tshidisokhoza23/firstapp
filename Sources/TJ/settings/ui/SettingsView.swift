import SwiftUI

public struct SettingsView: View {
    @ObservedObject var router: NavigationRouter

    @State private var showLogoutConfirm: Bool = false
    @State private var showResetConfirm: Bool = false
    @State private var showDeleteAccountConfirm: Bool = false

    public init(router: NavigationRouter) {
        self.router = router
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // User Profile Header Card
                HStack(spacing: 16) {
                    Circle()
                        .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 64, height: 64)
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.title)
                                .foregroundColor(.white)
                        )

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Alex Morgan")
                            .font(.title3)
                            .fontWeight(.bold)

                        Text("@alexmorgan • +1 (555) 019-2834")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text("Building seamless cross-platform tools.")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }

                    Spacer()

                    Button(action: { router.navigate(to: .editProfile) }) {
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
                            title: "Chats & Media",
                            color: .blue
                        ) {
                            router.navigate(to: .storageSettings)
                        }

                        SettingsGridCard(
                            icon: "bell.fill",
                            title: "Notifications",
                            color: .orange
                        ) {
                            router.navigate(to: .notificationSettings)
                        }

                        SettingsGridCard(
                            icon: "lock.shield.fill",
                            title: "Privacy & Security",
                            color: .green
                        ) {
                            router.navigate(to: .privacySettings)
                        }

                        SettingsGridCard(
                            icon: "internaldrive.fill",
                            title: "Storage & Data",
                            color: .teal
                        ) {
                            router.navigate(to: .storageSettings)
                        }

                        SettingsGridCard(
                            icon: "laptopcomputer.and.iphone",
                            title: "Sessions",
                            color: .purple
                        ) {
                            router.navigate(to: .accountSettings)
                        }

                        SettingsGridCard(
                            icon: "globe",
                            title: "Language",
                            color: .pink
                        ) {
                            router.navigate(to: .accountSettings)
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
                            router.navigate(to: .helpSettings)
                        }

                        SettingsGridCard(
                            icon: "doc.text.fill",
                            title: "Terms & EULA",
                            color: .indigo
                        ) {
                            router.navigate(to: .helpSettings)
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
                    Text("Version 1.0.6 • Swift Native")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
            .padding(.vertical, 16)
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Log Out", isPresented: $showLogoutConfirm) {
            Button("Log Out", role: .destructive) {
                router.popToRoot()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to log out of your account?")
        }
        .alert("Reset & Sync", isPresented: $showResetConfirm) {
            Button("Sync Now") {
                // Perform contact sync
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will refresh local chat caches and re-synchronize contacts.")
        }
        .alert("Delete Account", isPresented: $showDeleteAccountConfirm) {
            Button("Delete Forever", role: .destructive) {
                router.popToRoot()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This action is permanent and cannot be undone. All data will be removed.")
        }
    }
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
