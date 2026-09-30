import SwiftUI

struct ChannelDetailView: View {
    let channel: ChannelItem
    @Environment(\.dismiss) var dismiss
    @State private var isMuted: Bool = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    Image(systemName: channel.iconUrl)
                        .font(.system(size: 64))
                        .foregroundColor(AppColors.brandPrimary)
                        .frame(width: 100, height: 100)
                        .background(AppColors.brandPrimary.opacity(0.1))
                        .cornerRadius(24)

                    VStack(spacing: 4) {
                        Text(channel.name)
                            .font(.title2)
                            .fontWeight(.bold)
                        Text("\(channel.subscriberCount) Subscribers • \(channel.category)")
                            .font(.subheadline)
                            .foregroundColor(AppColors.greyMedium)
                    }

                    Text(channel.description)
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    HStack(spacing: 16) {
                        Button(action: {}) {
                            Label("Share Link", systemImage: "square.and.arrow.up")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(AppColors.brandPrimary)
                                .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)

                    List {
                        Section {
                            Toggle("Mute Notifications", isOn: $isMuted)
                            NavigationLink("Shared Media & Documents", destination: Text("Media Catalog"))
                        }
                    }
                    .frame(height: 200)
                }
                .padding()
            }
            .navigationTitle("Channel Info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
