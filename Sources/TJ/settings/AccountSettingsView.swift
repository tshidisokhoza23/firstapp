import SwiftUI

struct AccountSettingsView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authViewModel: AuthViewModel

    @State private var securityAlertNotifications: Bool = true
    @State private var showingChangePhoneWizard: Bool = false

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Account Credentials")) {
                    HStack {
                        Text("Phone Number")
                        Spacer()
                        Text(authViewModel.currentUser.phoneNumber)
                            .foregroundColor(AppColors.greyMedium)
                    }

                    Button(action: { showingChangePhoneWizard = true }) {
                        HStack {
                            Text("Change Phone Number")
                                .foregroundColor(AppColors.brandPrimary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("Security Alerts")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Toggle("Security Notifications", isOn: $securityAlertNotifications)
                        Text("Receive alert notifications when an unknown device or IP signs in to your account")
                            .font(.caption2)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }

                Section {
                    Button("Delete Account") { }
                        .foregroundColor(.red)
                }
            }
            .navigationTitle("Account Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showingChangePhoneWizard) {
                ChangePhoneWizardView()
            }
        }
    }
}
