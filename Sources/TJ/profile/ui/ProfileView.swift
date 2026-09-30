import SwiftUI

public struct ProfileView: View {
    let userId: String
    @ObservedObject var router: NavigationRouter

    @State private var selectedProfileTab: Int = 0
    @State private var showQRCodeSheet: Bool = false
    @State private var isFollowing: Bool = false

    private var isOwnProfile: Bool {
        userId == "currentUser" || userId.isEmpty
    }

    private let profileTabs = ["Posts", "Replies", "Media", "Shorts"]

    public init(userId: String, router: NavigationRouter) {
        self.userId = userId
        self.router = router
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header & Avatar Row
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(isOwnProfile ? "Alex Morgan" : "User Profile")
                            .font(.title)
                            .fontWeight(.bold)

                        Text(isOwnProfile ? "@alexmorgan" : "@user_\(userId.prefix(6))")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    // Avatar with QR Code Quick Trigger
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 80, height: 80)
                            .overlay(
                                Image(systemName: "person.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(.white)
                            )

                        Button(action: { showQRCodeSheet = true }) {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 26, height: 26)
                                .shadow(radius: 2)
                                .overlay(
                                    Image(systemName: "qrcode")
                                        .font(.caption)
                                        .foregroundColor(.black)
                                )
                        }
                    }
                }
                .padding(.horizontal, 16)

                // Bio & Status
                Text("Senior Mobile Engineer & Open Source Contributor. Building awesome iOS & Android applications. 🚀")
                    .font(.body)
                    .foregroundColor(.primary)
                    .padding(.horizontal, 16)

                // Stats Row
                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Text("1,240").fontWeight(.bold)
                        Text("Followers").foregroundColor(.secondary)
                    }
                    .font(.subheadline)

                    HStack(spacing: 4) {
                        Text("380").fontWeight(.bold)
                        Text("Following").foregroundColor(.secondary)
                    }
                    .font(.subheadline)

                    HStack(spacing: 4) {
                        Text("12.5K").fontWeight(.bold)
                        Text("Views").foregroundColor(.secondary)
                    }
                    .font(.subheadline)
                }
                .padding(.horizontal, 16)

                // Action Buttons Row
                HStack(spacing: 12) {
                    if isOwnProfile {
                        Button(action: { router.navigate(to: .editProfile) }) {
                            Text("Edit profile")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.primary)
                                .foregroundColor(Color(.systemBackground))
                                .cornerRadius(20)
                        }

                        Button(action: shareProfile) {
                            Text("Share profile")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color(.tertiarySystemBackground))
                                .foregroundColor(.primary)
                                .cornerRadius(20)
                        }
                    } else {
                        Button(action: { isFollowing.toggle() }) {
                            Text(isFollowing ? "Following" : "Follow")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(isFollowing ? Color(.tertiarySystemBackground) : Color.accentColor)
                                .foregroundColor(isFollowing ? .primary : .white)
                                .cornerRadius(20)
                        }

                        Button(action: { router.navigate(to: .chatDetail(conversationId: userId, participantName: "User")) }) {
                            Text("Message")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color(.tertiarySystemBackground))
                                .foregroundColor(.primary)
                                .cornerRadius(20)
                        }
                    }
                }
                .padding(.horizontal, 16)

                // Content Filter Segment Tabs
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        ForEach(0..<profileTabs.count, id: \.self) { index in
                            Button(action: { selectedProfileTab = index }) {
                                VStack(spacing: 8) {
                                    Text(profileTabs[index])
                                        .font(.subheadline)
                                        .fontWeight(selectedProfileTab == index ? .bold : .medium)
                                        .foregroundColor(selectedProfileTab == index ? .primary : .secondary)

                                    Rectangle()
                                        .fill(selectedProfileTab == index ? Color.accentColor : Color.clear)
                                        .frame(height: 2)
                                }
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    Divider()
                }
                .padding(.top, 8)

                // Tab Content Body
                VStack(spacing: 16) {
                    switch selectedProfileTab {
                    case 0:
                        ProfilePostItemView(title: "Architecture patterns in Swift 5.9 and SwiftUI", timestamp: "2h ago", likes: 42, comments: 8)
                        ProfilePostItemView(title: "WebRTC peer connection state handling tips", timestamp: "1d ago", likes: 128, comments: 24)
                    case 1:
                        ProfilePostItemView(title: "Replying to @developer: Exactly, SwiftUI state management makes this seamless!", timestamp: "3h ago", likes: 12, comments: 2)
                    case 2:
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.purple.opacity(0.3))
                                .frame(height: 120)
                                .overlay(Image(systemName: "photo.fill").font(.title))
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.blue.opacity(0.3))
                                .frame(height: 120)
                                .overlay(Image(systemName: "video.fill").font(.title))
                        }
                        .padding(.horizontal, 16)
                    case 3:
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                            ForEach(0..<3) { idx in
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.orange.opacity(0.3))
                                    .frame(height: 160)
                                    .overlay(Image(systemName: "play.circle.fill").font(.title))
                            }
                        }
                        .padding(.horizontal, 16)
                    default:
                        EmptyView()
                    }
                }
            }
            .padding(.vertical, 16)
        }
        .navigationTitle(isOwnProfile ? "My Profile" : "Profile")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showQRCodeSheet) {
            ProfileQRCodeSheetView(handle: "@alexmorgan")
        }
    }

    private func shareProfile() {
        let text = "Check out my profile on TJ App: https://tjapp.com/u/alexmorgan"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = windowScene.windows.first?.rootViewController {
            rootVC.present(av, animated: true)
        }
    }
}

// MARK: - Subcomponents

struct ProfilePostItemView: View {
    let title: String
    let timestamp: String
    let likes: Int
    let comments: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.body)
                .fontWeight(.medium)

            HStack {
                Text(timestamp)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Image(systemName: "heart")
                        Text("\(likes)")
                    }
                    HStack(spacing: 4) {
                        Image(systemName: "bubble.right")
                        Text("\(comments)")
                    }
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding(.horizontal, 16)
    }
}

struct ProfileQRCodeSheetView: View {
    let handle: String
    @Environment(\.dismiss) var dismiss

    var body: some View {
        VStack(spacing: 24) {
            Text("Scan QR Code")
                .font(.headline)

            VStack(spacing: 16) {
                Image(systemName: "qrcode")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180, height: 180)
                    .padding(20)
                    .background(Color.white)
                    .cornerRadius(16)
                    .shadow(radius: 4)

                Text(handle)
                    .font(.title3)
                    .fontWeight(.bold)
            }

            Button("Done") {
                dismiss()
            }
            .fontWeight(.semibold)
            .padding(.horizontal, 32)
            .padding(.vertical, 10)
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(20)
        }
        .padding(32)
    }
}
