import SwiftUI

struct AlertsNotificationsSettingsView: View {
    @Environment(\.dismiss) var dismiss

    @State private var alertPrivateChats: Bool = true
    @State private var alertGroups: Bool = true
    @State private var alertChannels: Bool = true
    @State private var inAppSounds: Bool = true
    @State private var inAppVibrate: Bool = true
    @State private var showMessagePreviews: Bool = true

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Message Alerts")) {
                    Toggle("Private Chats", isOn: $alertPrivateChats)
                    Toggle("Groups", isOn: $alertGroups)
                    Toggle("Channels", isOn: $alertChannels)
                }

                Section(header: Text("In-App Notifications")) {
                    Toggle("In-App Sounds", isOn: $inAppSounds)
                    Toggle("In-App Vibrate", isOn: $inAppVibrate)
                }

                Section(header: Text("Notification Content")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Toggle("Message Previews", isOn: $showMessagePreviews)
                        Text("Hide message body text in push notification alerts when disabled")
                            .font(.caption2)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
            }
            .navigationTitle("Alerts & Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
