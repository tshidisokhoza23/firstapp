import SwiftUI

public struct ChannelDetailView: View {
    public var channel: Channel
    public var posts: [ChannelPost]
    public var currentUserId: String?
    public var messages: [ChannelChatMessage]
    public var contacts: [ChannelContact]
    public var members: [ApiUser]

    public var onBack: () -> Void
    public var onSubscribe: (String) -> Void
    public var onMute: (String, Bool) -> Void
    public var onCreatePost: () -> Void
    public var onSendPost: (String, [String]) -> Void
    public var onAddAdmin: (String) -> Void
    public var onRemoveAdmin: (String) -> Void
    public var onToggleAdminPosting: (Bool) -> Void
    public var onOpenDiscussion: () -> Void
    public var onReportChannel: (String, String) -> Void

    @State private var showMenu = false
    @State private var showInfo = false
    @State private var showAddAdmin = false
    @State private var showUnfollowConfirm = false
    @State private var showReportDialog = false
    @State private var postText = ""
    @State private var selectedReportReason = "Spam or misleading"

    private let reportReasons = [
        "Spam or misleading",
        "Inappropriate content",
        "Harassment or hate speech",
        "Copyright violation",
        "Other"
    ]

    public init(
        channel: Channel,
        posts: [ChannelPost] = [],
        currentUserId: String? = nil,
        messages: [ChannelChatMessage] = [],
        contacts: [ChannelContact] = [],
        members: [ApiUser] = [],
        onBack: @escaping () -> Void = {},
        onSubscribe: @escaping (String) -> Void = { _ in },
        onMute: @escaping (String, Bool) -> Void = { _, _ in },
        onCreatePost: @escaping () -> Void = {},
        onSendPost: @escaping (String, [String]) -> Void = { _, _ in },
        onAddAdmin: @escaping (String) -> Void = { _ in },
        onRemoveAdmin: @escaping (String) -> Void = { _ in },
        onToggleAdminPosting: @escaping (Bool) -> Void = { _ in },
        onOpenDiscussion: @escaping () -> Void = {},
        onReportChannel: @escaping (String, String) -> Void = { _, _ in }
    ) {
        self.channel = channel
        self.posts = posts
        self.currentUserId = currentUserId
        self.messages = messages
        self.contacts = contacts
        self.members = members
        self.onBack = onBack
        self.onSubscribe = onSubscribe
        self.onMute = onMute
        self.onCreatePost = onCreatePost
        self.onSendPost = onSendPost
        self.onAddAdmin = onAddAdmin
        self.onRemoveAdmin = onRemoveAdmin
        self.onToggleAdminPosting = onToggleAdminPosting
        self.onOpenDiscussion = onOpenDiscussion
        self.onReportChannel = onReportChannel
    }

    private var isCreator: Bool {
        if let uid = currentUserId, !uid.isEmpty, channel.createdBy == uid {
            return true
        }
        return false
    }

    private var isAdmin: Bool {
        channel.isAdmin || isCreator
    }

    private var canPost: Bool {
        isCreator || (channel.isAdmin && channel.adminsCanPost)
    }

    private var isFollower: Bool {
        channel.isSubscribed || isAdmin
    }

    private var displayPosts: [ChannelPost] {
        let mapped = messages.map { msg in
            ChannelPost(
                id: msg.id,
                authorId: msg.senderId,
                authorName: channel.name,
                authorHandle: "@channel",
                authorAvatarUrl: channel.avatarUrl,
                content: msg.content,
                mediaUrls: msg.mediaUrls,
                timestamp: "Just now"
            )
        }
        var combined = mapped + posts
        var seen = Set<String>()
        combined.removeAll { p in
            if seen.contains(p.id) { return true }
            seen.insert(p.id)
            return false
        }
        return combined
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Navigation Bar
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.primary)
                }

                Button(action: { showInfo.toggle() }) {
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(channel.avatarColor)
                                .frame(width: 40, height: 40)

                            if let urlStr = channel.avatarUrl, let url = URL(string: urlStr) {
                                AsyncImage(url: url) { img in
                                    img.resizable().scaledToFill()
                                } placeholder: {
                                    ProgressView()
                                }
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())
                            } else {
                                Text(String(channel.name.prefix(1)).uppercased())
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(channel.name)
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(.primary)

                                if channel.isVerified || isAdmin {
                                    Image(systemName: "checkmark.seal.fill")
                                        .font(.system(size: 14))
                                        .foregroundColor(Color(hex: "00A884"))
                                }
                            }

                            Text("\(channel.followers) followers")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                Button(action: {
                    if isFollower {
                        onMute(channel.id, !channel.isMuted)
                    }
                }) {
                    Image(systemName: channel.isMuted ? "bell.slash" : "bell")
                        .font(.system(size: 18))
                        .foregroundColor(.primary)
                }

                Menu {
                    Button("Channel info") { showInfo = true }
                    Button(channel.isMuted ? "Unmute notifications" : "Mute notifications") {
                        if isFollower {
                            onMute(channel.id, !channel.isMuted)
                        }
                    }
                    if isAdmin {
                        Button("Add Admin") { showAddAdmin = true }
                    }
                    if isCreator {
                        Toggle("Admins can post", isOn: Binding(
                            get: { channel.adminsCanPost },
                            set: { onToggleAdminPosting($0) }
                        ))
                    }
                    if isFollower && !isCreator {
                        Button(role: .destructive) {
                            showUnfollowConfirm = true
                        } label: {
                            Text(channel.isSubscribed ? "Unfollow channel" : "Follow channel")
                        }
                    }
                    Button("Report channel", role: .destructive) {
                        showReportDialog = true
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 18))
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(uiColor: .systemBackground))

            Divider()

            // Scrollable Updates / Feed Content
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if showInfo {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("About this channel")
                                .font(.system(size: 18, weight: .bold))

                            Text(channel.description.isEmpty ? "Broadcast updates from \(channel.name). Followers can view posts; only the owner and allowed admins can publish." : channel.description)
                                .font(.system(size: 15))
                                .foregroundColor(.secondary)

                            Divider().padding(.vertical, 8)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 12)
                    }

                    Text("Updates")
                        .font(.system(size: 18, weight: .bold))
                        .padding(.horizontal, 16)

                    if displayPosts.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "photo.on.rectangle.angled")
                                .font(.system(size: 60))
                                .foregroundColor(.secondary.opacity(0.3))
                            Text("No updates yet")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                    } else {
                        ForEach(displayPosts) { post in
                            ChannelPostCardView(post: post)
                        }
                    }
                }
                .padding(.vertical, 16)
            }

            // Bottom Bar Actions
            if !isFollower {
                VStack {
                    Button(action: { onSubscribe(channel.id) }) {
                        Text("Follow channel")
                            .font(.system(size: 16, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color.primary)
                            .foregroundColor(Color(uiColor: .systemBackground))
                            .cornerRadius(16)
                    }
                    .padding(16)
                }
                .background(Color(uiColor: .secondarySystemBackground))
            } else if canPost {
                HStack(spacing: 12) {
                    TextField("Write an update", text: $postText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())

                    Button(action: {
                        if !postText.trimmingCharacters(in: .whitespaces).isEmpty {
                            onSendPost(postText, [])
                            postText = ""
                        }
                    }) {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.appPrimary)
                    }
                }
                .padding(12)
                .background(Color(uiColor: .secondarySystemBackground))
            }
        }
        .sheet(isPresented: $showAddAdmin) {
            VStack {
                Text("Add admin")
                    .font(.headline)
                    .padding()
                List(contacts.filter { $0.isRegistered }) { contact in
                    Button(contact.displayName) {
                        onAddAdmin(contact.id)
                        showAddAdmin = false
                    }
                }
            }
        }
        .alert("Unfollow channel?", isPresented: $showUnfollowConfirm) {
            Button("Cancel", role: .cancel) { }
            Button("Unfollow", role: .destructive) {
                onSubscribe(channel.id)
            }
        } message: {
            Text("You will stop seeing new posts from \(channel.name) in your chats.")
        }
        .sheet(isPresented: $showReportDialog) {
            VStack(spacing: 16) {
                Text("Report Channel")
                    .font(.title2.bold())

                Text("Help us understand the issue. Why are you reporting this channel?")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                ForEach(reportReasons, id: \.self) { reason in
                    HStack {
                        Text(reason)
                        Spacer()
                        if selectedReportReason == reason {
                            Image(systemName: "checkmark")
                                .foregroundColor(.appPrimary)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { selectedReportReason = reason }
                    .padding(.vertical, 8)
                }

                HStack {
                    Button("Cancel") { showReportDialog = false }
                    Spacer()
                    Button("Report") {
                        onReportChannel(channel.id, selectedReportReason)
                        showReportDialog = false
                    }
                    .foregroundColor(.red)
                }
                .padding(.top, 16)
            }
            .padding(24)
            .presentationDetents([.medium])
        }
    }
}

public struct ChannelPostCardView: View {
    public var post: ChannelPost

    public init(post: ChannelPost) {
        self.post = post
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(post.content)
                .font(.system(size: 15))
                .foregroundColor(.primary)

            HStack {
                Text("❤️ \(post.likes)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(uiColor: .tertiarySystemBackground))
                    .cornerRadius(12)

                Spacer()

                Text(post.timestamp)
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)

                Image(systemName: "arrowshape.turn.up.right.fill")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(16)
        .padding(.horizontal, 16)
    }
}
