import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) var dismiss
    @State private var isDarkMode: Bool = false
    @State private var selectedLanguage: String = "English"

    @State private var showingEditProfile: Bool = false
    @State private var showingAccountSettings: Bool = false
    @State private var showingDataStorageSettings: Bool = false
    @State private var showingAlertsSettings: Bool = false
    @State private var showingPrivacySecuritySettings: Bool = false
    @State private var showingSessionsView: Bool = false

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Profile")) {
                    HStack(spacing: 12) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .frame(width: 54, height: 54)
                            .foregroundColor(AppColors.greyMedium)

                        VStack(alignment: .leading, spacing: 4) {
                            Text(authViewModel.currentUser.name)
                                .font(.headline)
                                .foregroundColor(.primary)
                            Text(authViewModel.currentUser.phoneNumber)
                                .font(.subheadline)
                                .foregroundColor(AppColors.greyMedium)
                            if let bio = authViewModel.currentUser.bio, !bio.isEmpty {
                                Text(bio)
                                    .font(.caption)
                                    .foregroundColor(AppColors.greyMedium)
                            }
                        }

                        Spacer()

                        Button(action: { showingEditProfile = true }) {
                            Image(systemName: "pencil.circle.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section(header: Text("Account")) {
                    Button(action: { showingAccountSettings = true }) {
                        HStack {
                            Image(systemName: "person.badge.key.fill")
                                .foregroundColor(AppColors.brandPrimary)
                            Text("Account Settings & Phone")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("App Controls")) {
                    Button(action: { showingDataStorageSettings = true }) {
                        HStack {
                            Image(systemName: "internaldrive.fill")
                                .foregroundColor(.orange)
                            Text("Data & Storage Analytics")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { showingAlertsSettings = true }) {
                        HStack {
                            Image(systemName: "bell.badge.fill")
                                .foregroundColor(.blue)
                            Text("Alerts & Notifications")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { showingPrivacySecuritySettings = true }) {
                        HStack {
                            Image(systemName: "lock.shield.fill")
                                .foregroundColor(.green)
                            Text("Privacy & Security")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { showingSessionsView = true }) {
                        HStack {
                            Image(systemName: "desktopcomputer")
                                .foregroundColor(.purple)
                            Text("Active Sessions")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("Preferences")) {
                    Toggle("Dark Theme", isOn: $isDarkMode)

                    Picker("Language", selection: $selectedLanguage) {
                        Text("English").tag("English")
                        Text("Spanish").tag("Spanish")
                        Text("French").tag("French")
                    }
                }

                Section {
                    Button(action: {
                        authViewModel.logout()
                        dismiss()
                    }) {
                        Text("Log Out")
                            .foregroundColor(.red)
                            .fontWeight(.medium)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingEditProfile) { EditProfileView() }
            .sheet(isPresented: $showingAccountSettings) { AccountSettingsView() }
            .sheet(isPresented: $showingDataStorageSettings) { DataStorageSettingsView() }
            .sheet(isPresented: $showingAlertsSettings) { AlertsNotificationsSettingsView() }
            .sheet(isPresented: $showingPrivacySecuritySettings) { PrivacySecuritySettingsView() }
            .sheet(isPresented: $showingSessionsView) { SessionsView() }
        }
    }
}
