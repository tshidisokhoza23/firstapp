import SwiftUI
import PhotosUI

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
    @State private var pollVotes: [String: Int] = [:]
    @State private var hideMutePrompt = false
    @State private var channelPhoto: PhotosPickerItem?
    @StateObject private var liveModel = ChannelDetailViewModel()

    private var shownChannel: Channel { liveModel.channel.data ?? channel }
    private var liveMessages: [ChannelChatMessage] { liveModel.didLoadMessages ? liveModel.messages : messages }
    private var adminChoices: [ChannelContact] { liveModel.contacts.isEmpty ? contacts : liveModel.contacts }

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
        let uid = currentUserId?.isEmpty == false ? currentUserId : SessionStore.shared.userId
        if let uid, !uid.isEmpty, shownChannel.createdBy == uid {
            return true
        }
        return false
    }

    private var isAdmin: Bool {
        shownChannel.isAdmin || isCreator
    }

    private var canPost: Bool {
        isCreator || (shownChannel.isAdmin && shownChannel.adminsCanPost)
    }

    private var isFollower: Bool {
        shownChannel.isSubscribed || isAdmin
    }

    private var displayPosts: [ChannelPost] {
        let mapped = liveMessages.map { msg in
            ChannelPost(
                id: msg.id,
                content: msg.message,
                imageUrl: msg.mediaUrl,
                timestamp: msg.timestamp
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
            // Top Navigation Bar matching WhatsApp Screenshots
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.primary)
                }

                Button(action: { showInfo.toggle() }) {
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(channel.logoColor)
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

                            Text(isAdmin ? "Public channel" : "\(shownChannel.followers) followers")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                if isFollower {
                    Button(action: {
                        liveModel.mute(channelId: shownChannel.id, isMuted: !shownChannel.isMuted); onMute(shownChannel.id, !shownChannel.isMuted)
                    }) {
                        Image(systemName: shownChannel.isMuted ? "bell.slash" : "bell")
                            .font(.system(size: 18))
                            .foregroundColor(.primary)
                    }
                }

                ShareLink(item: "Check out \(channel.name) channel on FirstApp!") {
                    Image(systemName: "link")
                        .font(.system(size: 18))
                        .foregroundColor(.primary)
                }

                Menu {
                    Button("Channel info") { showInfo = true }
                    Button(shownChannel.isMuted ? "Unmute notifications" : "Mute notifications") {
                        if isFollower {
                            liveModel.mute(channelId: shownChannel.id, isMuted: !shownChannel.isMuted); onMute(shownChannel.id, !shownChannel.isMuted)
                        }
                    }
                    if isAdmin {
                        Button("Add Admin") { showAddAdmin = true }
                    }
                    if isCreator {
                        Toggle("Admins can post", isOn: Binding(
                            get: { shownChannel.adminsCanPost },
                            set: { liveModel.toggleAdminPosting(channelId: shownChannel.id, allowed: $0); onToggleAdminPosting($0) }
                        ))
                    }
                    if isFollower && !isCreator {
                        Button(role: .destructive) {
                            showUnfollowConfirm = true
                        } label: {
                            Text(shownChannel.isSubscribed ? "Unfollow channel" : "Follow channel")
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

            // Scrollable Channel Updates Feed
            ZStack {
                Color(hex: "E5DDD5")
                    .opacity(0.3)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .center, spacing: 16) {
                        // System Privacy Notice Bubbles (Screenshot 4)
                        VStack(spacing: 8) {
                            HStack(spacing: 6) {
                                Image(systemName: "antenna.radiowaves.left.and.right")
                                    .font(.system(size: 12))
                                Text("This channel is public and visible to anyone, including TJ. There is added privacy for your profile and phone number.")
                                    .font(.system(size: 12))
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(uiColor: .systemBackground))
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.04), radius: 2, y: 1)

                            if isAdmin {
                                Text("You created this channel, \"\(channel.name)\"")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color(uiColor: .systemBackground))
                                    .cornerRadius(12)
                                    .shadow(color: Color.black.opacity(0.04), radius: 2, y: 1)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 12)

                        // Start Growing Card for Admins (Screenshot 4)
                        if isAdmin {
                            VStack(spacing: 16) {
                                ZStack(alignment: .bottomTrailing) {
                                    Circle()
                                        .fill(channel.logoColor)
                                        .frame(width: 80, height: 80)
                                    Text(String(channel.name.prefix(1)).uppercased())
                                        .font(.system(size: 32, weight: .bold))
                                        .foregroundColor(.white)

                                    Circle()
                                        .fill(Color(hex: "00A884"))
                                        .frame(width: 28, height: 28)
                                        .overlay(
                                            Image(systemName: "camera.fill")
                                                .font(.system(size: 12))
                                                .foregroundColor(.white)
                                        )
                                }

                                Text("Start growing \"\(channel.name)\"")
                                    .font(.system(size: 18, weight: .bold))

                                VStack(spacing: 12) {
                                    Button(action: { showInfo = true }) {
                                        HStack {
                                            Image(systemName: "pencil")
                                                .foregroundColor(Color(hex: "00A884"))
                                            Text("Add description")
                                                .font(.system(size: 15, weight: .medium))
                                                .foregroundColor(Color(hex: "00A884"))
                                        }
                                    }

                                    Button(action: { }) {
                                        HStack {
                                            Image(systemName: "target")
                                                .foregroundColor(Color(hex: "00A884"))
                                            Text("Share to my status")
                                                .font(.system(size: 15, weight: .medium))
                                                .foregroundColor(Color(hex: "00A884"))
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 24)
                                                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                                        )
                                    }

                                    Button(action: { showAddAdmin = true }) {
                                        HStack {
                                            Image(systemName: "plus")
                                                .foregroundColor(Color(hex: "00A884"))
                                            Text("Invite admins")
                                                .font(.system(size: 15, weight: .medium))
                                                .foregroundColor(Color(hex: "00A884"))
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 24)
                                                .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                                        )
                                    }
                                }
                            }
                            .padding(20)
                            .background(Color(uiColor: .systemBackground))
                            .cornerRadius(20)
                            .padding(.horizontal, 16)
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                        }

                        // Feed Posts
                        if displayPosts.isEmpty && !isAdmin {
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
            }

            // Bottom Bar Actions (States 1, 2, 3 from Screenshots)
            if !isFollower {
                // State 1: Non-follower (Screenshot 3)
                VStack(spacing: 12) {
                    Button(action: { liveModel.subscribe(channelId: shownChannel.id); onSubscribe(shownChannel.id) }) {
                        Text("Follow channel")
                            .font(.system(size: 17, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color(hex: "00A884"))
                            .foregroundColor(.white)
                            .cornerRadius(25)
                    }

                    HStack(spacing: 4) {
                        Text("This channel has added privacy for your profile and phone number.")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                        Text("Learn more")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.blue)
                    }
                }
                .padding(16)
                .background(Color(uiColor: .systemBackground))
            } else if shownChannel.isMuted && !hideMutePrompt {
                // State 2: Muted Follower Banner (Screenshot 2)
                VStack(spacing: 12) {
                    Text("Unmute to get notifications for this channel.")
                        .font(.system(size: 14))
                        .foregroundColor(.primary)

                    HStack(spacing: 12) {
                        Button(action: { hideMutePrompt = true }) {
                            Text("Not now")
                                .font(.system(size: 15, weight: .medium))
                                .frame(maxWidth: .infinity)
                                .frame(height: 44)
                                .background(Color(uiColor: .secondarySystemBackground))
                                .foregroundColor(.primary)
                                .cornerRadius(22)
                        }

                        Button(action: { liveModel.mute(channelId: shownChannel.id, isMuted: false); onMute(shownChannel.id, false) }) {
                            HStack(spacing: 6) {
                                Image(systemName: "bell.slash.fill")
                                    .font(.system(size: 14))
                                Text("Unmute")
                                    .font(.system(size: 15, weight: .bold))
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(Color(hex: "E8F5E9"))
                            .foregroundColor(Color(hex: "00A884"))
                            .cornerRadius(22)
                        }
                    }
                }
                .padding(16)
                .background(Color(uiColor: .systemBackground))
            } else if canPost {
                // State 3: Admin Input Bar (Screenshot 4)
                HStack(spacing: 12) {
                    HStack(spacing: 8) {
                        Image(systemName: "face.smiling")
                            .foregroundColor(.gray)

                        TextField("Write an update", text: $postText)

                        PhotosPicker(selection: $channelPhoto, matching: .images) {
                            Image(systemName: "paperclip")
                                .foregroundColor(.gray)
                        }
                        PhotosPicker(selection: $channelPhoto, matching: .images) {
                            Image(systemName: "camera")
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(uiColor: .systemBackground))
                    .cornerRadius(24)

                    Button(action: {
                        if !postText.trimmingCharacters(in: .whitespaces).isEmpty {
                            let target = shownChannel.conversationId?.isEmpty == false ? shownChannel.conversationId! : shownChannel.id
                            liveModel.sendMessage(channelId: target, content: postText)
                            onSendPost(postText, [])
                            postText = ""
                        }
                    }) {
                        Circle()
                            .fill(Color(hex: "00A884"))
                            .frame(width: 48, height: 48)
                            .overlay(
                                Image(systemName: postText.isEmpty ? "mic.fill" : "paperplane.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(.white)
                            )
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
                List(adminChoices.filter { $0.isRegistered && !$0.displayName.isEmpty }, id: \.id) { contact in
                    Button(action: {
                        liveModel.addAdmin(channelId: shownChannel.id, userId: contact.id); onAddAdmin(contact.id)
                        showAddAdmin = false
                    }) {
                        Text(contact.displayName)
                    }
                }
            }
        }
        .alert("Unfollow channel?", isPresented: $showUnfollowConfirm) {
            Button("Cancel", role: .cancel) { }
            Button("Unfollow", role: .destructive) {
                liveModel.subscribe(channelId: shownChannel.id); onSubscribe(shownChannel.id)
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
                                .foregroundColor(Color(hex: "00A884"))
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
                        liveModel.report(channelId: shownChannel.createdBy?.isEmpty == false ? shownChannel.createdBy! : shownChannel.id, reason: selectedReportReason)
                        onReportChannel(shownChannel.id, selectedReportReason)
                        showReportDialog = false
                    }
                    .foregroundColor(.red)
                }
                .padding(.top, 16)
            }
            .padding(24)
            .presentationDetents([.medium])
        }
        .task(id: channel.id) { liveModel.loadChannel(channelId: channel.id) }
        .onChange(of: channelPhoto) { item in
            guard let item else { return }
            Task { await uploadChannelPhoto(item) }
        }
        .alert("Channel", isPresented: Binding(get: { liveModel.toast != nil }, set: { if !$0 { liveModel.toast = nil } })) {
            Button("OK", role: .cancel) { liveModel.clearToast() }
        } message: {
            Text(liveModel.toast ?? "")
        }
    }
    private func uploadChannelPhoto(_ item: PhotosPickerItem) async {
        defer { channelPhoto = nil }
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        do {
            let url = try await MediaActions.upload(data: data, filename: "channel.jpg", mime: "image/jpeg", folder: "channels")
            let target = shownChannel.conversationId?.isEmpty == false ? shownChannel.conversationId! : shownChannel.id
            let caption = postText.trimmingCharacters(in: .whitespacesAndNewlines)
            liveModel.sendMediaPost(channelId: target, content: caption, mediaUrls: [url], messageType: "image", fileName: "channel.jpg")
            postText = ""
        } catch {
            liveModel.toast = error.localizedDescription
        }
    }



}

public struct ChannelPostCardView: View {
    public var post: ChannelPost

    public init(post: ChannelPost) {
        self.post = post
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Text Content
            if !post.content.isEmpty {
                Text(post.content)
                    .font(.system(size: 15))
                    .foregroundColor(.primary)
            }

            // Optional Image/Video
            if let url = FeedPost.resolve(post.imageUrl) {
                AsyncImage(url: url) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    Rectangle().fill(Color.gray.opacity(0.2))
                }
                .frame(maxHeight: 240)
                .cornerRadius(12)
            }

            // Reactions & Forward Pill Row (Screenshots 1-3)
            HStack(spacing: 8) {
                // Emoji Reaction Pill
                HStack(spacing: 4) {
                    Text("❤️ 👄 😂 👍")
                        .font(.system(size: 12))
                    Text(post.isLiked ? "Liked" : "")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(uiColor: .systemBackground))
                .cornerRadius(16)
                .shadow(color: Color.black.opacity(0.04), radius: 2, y: 1)

                // Forward Pill
                HStack(spacing: 4) {
                    Image(systemName: "arrowshape.turn.up.right.fill")
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                    Text("Forward")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(uiColor: .systemBackground))
                .cornerRadius(16)
                .shadow(color: Color.black.opacity(0.04), radius: 2, y: 1)

                Spacer()

                // Timestamp Pill
                Text(post.timestamp)
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(uiColor: .systemBackground))
                    .cornerRadius(12)
            }
        }
        .padding(12)
        .background(Color(uiColor: .systemBackground))
        .cornerRadius(16)
        .padding(.horizontal, 16)
        .shadow(color: Color.black.opacity(0.04), radius: 2, y: 1)
    }
}
