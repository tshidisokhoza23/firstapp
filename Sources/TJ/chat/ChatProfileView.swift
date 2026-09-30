import SwiftUI

struct ChatProfileView: View {
    let chat: ChatItem
    @Environment(\.dismiss) var dismiss
    @State private var isMuted: Bool = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .frame(width: 100, height: 100)
                        .foregroundColor(AppColors.greyMedium)

                    VStack(spacing: 4) {
                        Text(chat.name)
                            .font(.title)
                            .fontWeight(.bold)
                        Text(chat.isOnline ? "Online" : "Offline")
                            .font(.subheadline)
                            .foregroundColor(chat.isOnline ? .green : AppColors.greyMedium)
                    }

                    HStack(spacing: 32) {
                        VStack {
                            Image(systemName: "phone.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                            Text("Audio")
                                .font(.caption)
                        }
                        VStack {
                            Image(systemName: "video.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                            Text("Video")
                                .font(.caption)
                        }
                        VStack {
                            Image(systemName: "bell.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                            Text("Mute")
                                .font(.caption)
                        }
                    }
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)

                    List {
                        Section {
                            Toggle("Mute Notifications", isOn: $isMuted)
                            NavigationLink("Media, Links, and Docs", destination: Text("Shared Media Catalog"))
                            NavigationLink("Starred Messages", destination: StarredMessagesView())
                        }

                        Section {
                            Button("Block Contact") { }
                                .foregroundColor(.red)
                            Button("Report Contact") { }
                                .foregroundColor(.red)
                        }
                    }
                    .frame(height: 300)
                }
                .padding()
            }
            .navigationTitle("Contact Info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
