import SwiftUI
import LocalAuthentication

struct PrivacyScopePickerSheet: View {
    let title: String
    @Binding var scope: PrivacyScope
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            Form {
                Picker("Who can see my \(title.lowercased())", selection: $scope) {
                    ForEach(PrivacyScope.allCases) { item in
                        Text(item.rawValue).tag(item)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct PrivacySecuritySettingsView: View {
    @Environment(\.dismiss) var dismiss
    @State private var privacy = PrivacySettingsModel()

    @State private var editingScopeTitle: String? = nil
    @State private var showingPasscodeSetup: Bool = false
    @State private var showingBlockedUsers: Bool = false

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Privacy Controls")) {
                    Button(action: { editingScopeTitle = "Last Seen & Online" }) {
                        HStack {
                            Text("Last Seen & Online")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(privacy.lastSeen.rawValue)
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { editingScopeTitle = "Phone Number" }) {
                        HStack {
                            Text("Phone Number")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(privacy.phoneNumber.rawValue)
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { editingScopeTitle = "Profile Photo" }) {
                        HStack {
                            Text("Profile Photo")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(privacy.profilePhoto.rawValue)
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { editingScopeTitle = "Stories" }) {
                        HStack {
                            Text("Stories")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(privacy.stories.rawValue)
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("Messaging Features")) {
                    Toggle("Read Receipts", isOn: $privacy.readReceipts)
                    Button(action: { showingBlockedUsers = true }) {
                        HStack {
                            Text("Blocked Users")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("App Security")) {
                    Button(action: { showingPasscodeSetup = true }) {
                        HStack {
                            Text("Passcode Lock")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(privacy.isPasscodeEnabled ? "On" : "Off")
                                .font(.caption)
                                .foregroundColor(privacy.isPasscodeEnabled ? .green : AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Toggle("Unlock with Touch/Face ID", isOn: $privacy.isBiometricsEnabled)
                        .disabled(!privacy.isPasscodeEnabled)
                        .onChange(of: privacy.isBiometricsEnabled) { newValue in
                            if newValue {
                                authenticateBiometrics()
                            }
                        }
                }
            }
            .navigationTitle("Privacy & Security")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(item: $editingScopeTitle) { title in
                if title == "Last Seen & Online" {
                    PrivacyScopePickerSheet(title: title, scope: $privacy.lastSeen)
                } else if title == "Phone Number" {
                    PrivacyScopePickerSheet(title: title, scope: $privacy.phoneNumber)
                } else if title == "Profile Photo" {
                    PrivacyScopePickerSheet(title: title, scope: $privacy.profilePhoto)
                } else {
                    PrivacyScopePickerSheet(title: title, scope: $privacy.stories)
                }
            }
            .sheet(isPresented: $showingPasscodeSetup) {
                PasscodeSetupView { pin in
                    privacy.passcodePIN = pin
                    privacy.isPasscodeEnabled = true
                }
            }
            .sheet(isPresented: $showingBlockedUsers) {
                BlockedUsersListView()
            }
        }
    }

    private func authenticateBiometrics() {
        let context = LAContext()
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Enable Biometric Lock for TJ") { success, _ in
                DispatchQueue.main.async {
                    if !success {
                        privacy.isBiometricsEnabled = false
                    }
                }
            }
        } else {
            privacy.isBiometricsEnabled = false
        }
    }
}
