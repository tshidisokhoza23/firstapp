import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) var dismiss
    @State private var isDarkMode: Bool = false
    @State private var selectedLanguage: String = "English"

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Account")) {
                    HStack(spacing: 12) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .frame(width: 50, height: 50)
                            .foregroundColor(AppColors.greyMedium)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(authViewModel.currentUser.name)
                                .font(.headline)
                            Text(authViewModel.currentUser.phoneNumber)
                                .font(.subheadline)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section(header: Text("Preferences")) {
                    Toggle("Dark Theme", isOn: $isDarkMode)

                    Picker("Language", selection: $selectedLanguage) {
                        Text("English").tag("English")
                        Text("Spanish").tag("Spanish")
                        Text("French").tag("French")
                    }
                }

                Section(header: Text("Privacy & Security")) {
                    NavigationLink("Privacy Settings", destination: Text("Privacy Details"))
                    NavigationLink("Notifications", destination: Text("Notification Options"))
                    NavigationLink("Chat Backups", destination: Text("Cloud Backup Settings"))
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
        }
    }
}
