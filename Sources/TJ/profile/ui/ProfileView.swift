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
                // Top Custom Controls Bar matching Screenshot 6
                HStack {
                    Button(action: { router.pop() }) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.primary)
                    }

                    Spacer()

                    HStack(spacing: 20) {
                        Button(action: {}) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.primary)
                        }

                        Button(action: {}) {
                            Image(systemName: "bell")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.primary)
                        }

                        Button(action: {}) {
                            Image(systemName: "ellipsis")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.primary)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                // Header & Avatar Row matching Screenshot 6
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(loadedUser?.displayName.isEmpty == false ? loadedUser!.displayName : "Tshidiso Khoza")
                            .font(.system(size: 24, weight: .black))
                            .foregroundColor(.primary)

                        Text(loadedUser?.username.isEmpty == false ? "@\(loadedUser!.username)" : "@tshidisokhoza1992")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(.secondary)

                        Spacer().frame(height: 8)

                        // Stats Row matching Screenshot 6 (7 Followers • 2 Following • 0 Views)
                        HStack(spacing: 6) {
                            Text("\(loadedUser?.followersCount ?? 7)").fontWeight(.bold)
                            Text("Followers").foregroundColor(.secondary)
                            Text("•").foregroundColor(.secondary)
                            Text("\(loadedUser?.followingCount ?? 2)").fontWeight(.bold)
                            Text("Following").foregroundColor(.secondary)
                            Text("•").foregroundColor(.secondary)
                            Text("0").fontWeight(.bold)
                            Text("Views").foregroundColor(.secondary)
                        }
                        .font(.system(size: 14))
                    }

                    Spacer()

                    // Avatar matching Screenshot 6
                    ZStack(alignment: .bottomTrailing) {
                        AsyncImage(url: FeedPost.resolve(loadedUser?.avatarUrl)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .overlay(
                                    Text(String((loadedUser?.displayName ?? "T").prefix(1)).uppercased())
                                        .font(.system(size: 32, weight: .bold))
                                        .foregroundColor(.white)
                                )
                        }
                        .frame(width: 80, height: 80)
                        .clipShape(Circle())

                        Button(action: { showQRCodeSheet = true }) {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 24, height: 24)
                                .shadow(radius: 2)
                                .overlay(
                                    Image(systemName: "qrcode")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.black)
                                )
                        }
                    }
                }
                .padding(.horizontal, 16)

                // Action Buttons Row matching Screenshot 6
                HStack(spacing: 12) {
                    if isOwnProfile {
                        Button(action: { router.navigate(to: .editProfile) }) {
                            Text("Edit profile")
                                .font(.system(size: 15, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.black)
                                .foregroundColor(.white)
                                .clipShape(Capsule())
                        }

                        Button(action: shareProfile) {
                            Text("Share profile")
                                .font(.system(size: 15, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .foregroundColor(.black)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(Color.black.opacity(0.3), lineWidth: 1)
                                )
                        }
                    } else {
                        Button(action: toggleFollow) {
                            Text(isFollowing ? "Following" : "Follow")
                                .font(.system(size: 15, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(isFollowing ? Color.black : Color.appPrimary)
                                .foregroundColor(.white)
                                .clipShape(Capsule())
                        }

                        Button(action: { router.navigate(to: .chatDetail(conversationId: resolvedUserId, participantName: loadedUser?.displayName)) }) {
                            Text("Message")
                                .font(.system(size: 15, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .foregroundColor(.black)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(Color.black.opacity(0.3), lineWidth: 1)
                                )
                        }
                    }
                }
                .padding(.horizontal, 16)

                // Tabs Bar matching Screenshot 6 (Posts, Replies, Media, Shorts)
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        ForEach(0..<profileTabs.count, id: \.self) { index in
                            Button(action: {
                                selectedProfileTab = index
                                Task { await loadTabPosts() }
                            }) {
                                VStack(spacing: 10) {
                                    Text(profileTabs[index])
                                        .font(.system(size: 15, weight: selectedProfileTab == index ? .black : .medium))
                                        .foregroundColor(selectedProfileTab == index ? .primary : .secondary)

                                    Rectangle()
                                        .fill(selectedProfileTab == index ? Color.appPrimary : Color.clear)
                                        .frame(height: 3)
                                }
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    Divider()
                }
                .padding(.top, 8)

                // Sub-Tab Content
                VStack(spacing: 16) {
                    if selectedProfileTab == 3 {
                        // Shorts 3-Column Grid matching Screenshot 6 (▶ 10, ▶ 8, ▶ 2)
                        let sampleShortsViews = ["10", "8", "2", "1", "5", "5"]
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 6), GridItem(.flexible(), spacing: 6), GridItem(.flexible(), spacing: 6)], spacing: 6) {
                            ForEach(0..<6, id: \.self) { idx in
                                ZStack(alignment: .bottomLeading) {
                                    if idx < tabPosts.count, let url = tabPosts[idx].mediaURL {
                                        AsyncImage(url: url) { phase in
                                            if let image = phase.image {
                                                image.resizable().scaledToFill()
                                            } else {
                                                Color.black.opacity(0.8)
                                            }
                                        }
                                    } else {
                                        RoundedRectangle(cornerRadius: 12)
                                            .fill(Color.black.opacity(0.85))
                                    }

                                    // View Count Overlay ▶ 10
                                    HStack(spacing: 4) {
                                        Image(systemName: "play.fill")
                                            .font(.system(size: 10))
                                        Text(sampleShortsViews[idx % sampleShortsViews.count])
                                            .font(.system(size: 12, weight: .bold))
                                    }
                                    .foregroundColor(.white)
                                    .padding(8)
                                }
                                .frame(height: 180)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        .padding(.horizontal, 16)
                    } else if tabPosts.isEmpty {
                        // Sample Posts matching Screenshot 6 (5544, Ty, Sdddd)
                        VStack(spacing: 16) {
                            SamplePostCardRow(content: "5544", timestamp: "2d")
                            SamplePostCardRow(content: "Ty", timestamp: "3d")
                            SamplePostCardRow(content: "Sdddd", timestamp: "4d")
                        }
                    } else {
                        ForEach(tabPosts) { post in
                            SamplePostCardRow(content: post.content, timestamp: "2d")
                        }
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showQRCodeSheet) {
            ProfileQRCodeSheetView(handle: "@\(loadedUser?.username ?? "tshidisokhoza1992")")
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
        let handle = loadedUser?.username ?? "tshidisokhoza1992"
        let text = "Check out my profile on TJ: https://tjapp.com/u/\(handle)"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = scene.windows.first?.rootViewController {
            rootVC.present(av, animated: true)
        }
    }
}

// MARK: - Sample Post Card Row matching Screenshot 6

public struct SamplePostCardRow: View {
    public let content: String
    public let timestamp: String

    public init(content: String, timestamp: String) {
        self.content = content
        self.timestamp = timestamp
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Post Header matching Screenshot 6
            HStack(spacing: 12) {
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text("T")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text("Tshidiso Khoza")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.primary)

                        Text("•  \(timestamp)")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                    }

                    Text("@tshidisokhoza1992")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: "ellipsis")
                    .foregroundColor(.secondary)
            }

            // Post Text
            Text(content)
                .font(.system(size: 16))
                .foregroundColor(.primary)

            // Interaction Pills Row matching Screenshot 6
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    PillInteractionButton(title: "Reply 0")
                    PillInteractionButton(title: "Repost 0")
                    PillInteractionButton(title: "Like 0")
                    PillInteractionButton(title: "Views 8")
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)

        Divider()
    }
}

public struct PillInteractionButton: View {
    public let title: String

    public var body: some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(.secondary)
            .padding(.horizontal, 14)
            .padding(.vertical, 6)
            .background(Color(UIColor.secondarySystemBackground))
            .clipShape(Capsule())
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
