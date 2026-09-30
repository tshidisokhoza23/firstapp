import SwiftUI

public struct ProfileView: View {
    let userId: String
    @ObservedObject var router: NavigationRouter

    @State private var selectedProfileTab: Int = 0
    @State private var showQRCodeSheet: Bool = false
    @State private var isFollowing: Bool = false
    @State private var loadedUser: User? = nil
    @State private var tabPosts: [FeedPost] = []

    private var resolvedUserId: String {
        if userId != "me" && userId != "currentUser" && !userId.isEmpty { return userId }
        return loadedUser?.id ?? SessionStore.shared.userId ?? ""
    }

    private var isOwnProfile: Bool {
        if userId == "me" || userId == "currentUser" || userId.isEmpty { return true }
        if let me = SessionStore.shared.userId, me == userId { return true }
        return false
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
                        Text(loadedUser?.displayName.isEmpty == false ? loadedUser!.displayName : (isOwnProfile ? "Your profile" : "User Profile"))
                            .font(.title)
                            .fontWeight(.bold)

                        Text(loadedUser?.username.isEmpty == false ? "@\(loadedUser!.username)" : "@user")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    // Avatar with QR Code Quick Trigger
                    ZStack(alignment: .bottomTrailing) {
                        AsyncImage(url: FeedPost.resolve(loadedUser?.avatarUrl)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Circle()
                                .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .overlay(Image(systemName: "person.fill").font(.system(size: 40)).foregroundColor(.white))
                        }
                        .frame(width: 80, height: 80)
                        .clipShape(Circle())

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
                Text(loadedUser?.bio?.isEmpty == false ? loadedUser!.bio! : "No bio yet.")
                    .font(.body)
                    .foregroundColor(.primary)
                    .padding(.horizontal, 16)

                // Stats Row
                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Text("\(loadedUser?.followersCount ?? 0)").fontWeight(.bold)
                        Text("Followers").foregroundColor(.secondary)
                    }
                    .font(.subheadline)

                    HStack(spacing: 4) {
                        Text("\(loadedUser?.followingCount ?? 0)").fontWeight(.bold)
                        Text("Following").foregroundColor(.secondary)
                    }
                    .font(.subheadline)

                    HStack(spacing: 4) {
                        Text(String(format: "%.0f", loadedUser?.walletBalance ?? 0)).fontWeight(.bold)
                        Text("Points").foregroundColor(.secondary)
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
                        Button(action: toggleFollow) {
                            Text(isFollowing ? "Following" : "Follow")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(isFollowing ? Color(.tertiarySystemBackground) : Color.accentColor)
                                .foregroundColor(isFollowing ? .primary : .white)
                                .cornerRadius(20)
                        }

                        Button(action: { router.navigate(to: .chatDetail(conversationId: resolvedUserId, participantName: loadedUser?.displayName)) }) {
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
                            Button(action: {
                                selectedProfileTab = index
                                Task { await loadTabPosts() }
                            }) {
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

                VStack(spacing: 16) {
                    if tabPosts.isEmpty {
                        Text("Nothing here yet")
                            .foregroundColor(.secondary)
                            .padding(.top, 24)
                    } else if selectedProfileTab == 2 || selectedProfileTab == 3 {
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: selectedProfileTab == 3 ? 3 : 2), spacing: 8) {
                            ForEach(tabPosts) { post in
                                Button {
                                    Task {
                                        await MediaActions.like(post.id)
                                        await loadTabPosts()
                                    }
                                } label: {
                                    AsyncImage(url: post.mediaURL) { image in
                                        image.resizable().scaledToFill()
                                    } placeholder: {
                                        Color.gray.opacity(0.2)
                                    }
                                    .frame(height: selectedProfileTab == 3 ? 160 : 120)
                                    .clipped()
                                    .cornerRadius(12)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    } else {
                        ForEach(tabPosts) { post in
                            Button {
                                Task {
                                    await MediaActions.like(post.id)
                                    await loadTabPosts()
                                }
                            } label: {
                                ProfilePostItemView(
                                    title: post.content.isEmpty ? post.title : post.content,
                                    timestamp: "",
                                    likes: post.likes,
                                    comments: post.repliesCount
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(.vertical, 16)
        }
        .navigationTitle(isOwnProfile ? "My Profile" : "Profile")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showQRCodeSheet) {
            ProfileQRCodeSheetView(handle: "@\(loadedUser?.username ?? "user")")
        }
        .task { await loadProfile() }
    }

    private func loadProfile() async {
        let path = (userId == "me" || userId == "currentUser" || userId.isEmpty) ? "users/me" : "users/\(userId)"
        if let data = try? await API.data(path),
           let envelope = try? JSONDecoder().decode(UserEnvelope.self, from: data) {
            loadedUser = envelope.user
            isFollowing = envelope.user.isFollowing
            if path == "users/me" { SessionStore.shared.updateUser(envelope.user) }
        } else if path == "users/me" {
            loadedUser = SessionStore.shared.user
        }
        await loadTabPosts()
    }

    private func loadTabPosts() async {
        let type: String
        switch selectedProfileTab {
        case 1: type = "reply"
        case 2: type = "media"
        case 3: type = "short"
        default: type = "standard"
        }
        tabPosts = await MediaActions.userPosts(userId: resolvedUserId, type: type)
    }

    private func toggleFollow() {
        let id = resolvedUserId
        guard !id.isEmpty else { return }
        let next = !isFollowing
        isFollowing = next
        Task {
            let path = next ? "users/\(id)/follow" : "users/\(id)/unfollow"
            if (try? await API.data(path, method: "POST")) == nil {
                isFollowing = !next
            }
        }
    }

    private func shareProfile() {
        let handle = loadedUser?.username ?? "me"
        let text = "Check out my profile on TJ: https://tjapp.com/u/\(handle)"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = scene.windows.first?.rootViewController {
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
