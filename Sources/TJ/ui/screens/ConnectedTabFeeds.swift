import SwiftUI
import AVKit
import PhotosUI
import UniformTypeIdentifiers
import UIKit

public struct FeedPost: Identifiable, Codable, Sendable {
    public let id: String
    let authorId: String
    let authorName: String
    let authorHandle: String
    let authorAvatar: String?
    let content: String
    let mediaURLs: [String]
    var likes: Int
    var repliesCount: Int
    let postType: String
    var isLiked: Bool
    var isBookmarked: Bool
    var views: Int
    var shares: Int
    var repostCount: Int
    var quoteCount: Int
    var isReposted: Bool
    let mediaType: String
    var isVerifiedAuthor: Bool
    var timestamp: Int64
    var isAuthorFollowed: Bool
    var repostOfId: String
    var originalAuthorId: String
    var originalAuthorName: String
    var originalAuthorHandle: String
    var originalAuthorAvatar: String?
    var originalContent: String
    var originalMediaURLs: [String]
    var originalMediaType: String
    var originalPostType: String

    enum CodingKeys: String, CodingKey {
        case id
        case authorId = "author_id"
        case authorName = "author_name"
        case authorHandle = "author_handle"
        case authorAvatar = "author_avatar_url"
        case content
        case views
        case shares
        case repostCount = "repost_count"
        case quoteCount = "quote_count"
        case isReposted = "is_reposted"
        case mediaType = "media_type"
        case mediaURLs = "media_urls"
        case likes
        case repliesCount = "replies_count"
        case postType = "post_type"
        case isLiked = "is_liked"
        case isBookmarked = "is_bookmarked"
    }

    init(
        id: String = UUID().uuidString,
        authorId: String = "",
        authorName: String = "",
        authorHandle: String = "",
        authorAvatar: String? = nil,
        content: String = "",
        mediaURLs: [String] = [],
        likes: Int = 0,
        repliesCount: Int = 0,
        postType: String = "",
        isLiked: Bool = false,
        isBookmarked: Bool = false,
        views: Int = 0,
        shares: Int = 0,
        repostCount: Int = 0,
        quoteCount: Int = 0,
        isReposted: Bool = false,
        mediaType: String = "",
        isVerifiedAuthor: Bool = false,
        timestamp: Int64 = 0,
        isAuthorFollowed: Bool = false,
        repostOfId: String = "",
        originalAuthorId: String = "",
        originalAuthorName: String = "",
        originalAuthorHandle: String = "",
        originalAuthorAvatar: String? = nil,
        originalContent: String = "",
        originalMediaURLs: [String] = [],
        originalMediaType: String = "",
        originalPostType: String = ""
    ) {
        self.id = id
        self.authorId = authorId
        self.authorName = authorName
        self.authorHandle = authorHandle
        self.authorAvatar = authorAvatar
        self.content = content
        self.mediaURLs = mediaURLs
        self.likes = likes
        self.repliesCount = repliesCount
        self.postType = postType
        self.isLiked = isLiked
        self.isBookmarked = isBookmarked
        self.views = views
        self.shares = shares
        self.repostCount = repostCount
        self.quoteCount = quoteCount
        self.isReposted = isReposted
        self.mediaType = mediaType
        self.isVerifiedAuthor = isVerifiedAuthor
        self.timestamp = timestamp
        self.isAuthorFollowed = isAuthorFollowed
        self.repostOfId = repostOfId
        self.originalAuthorId = originalAuthorId
        self.originalAuthorName = originalAuthorName
        self.originalAuthorHandle = originalAuthorHandle
        self.originalAuthorAvatar = originalAuthorAvatar
        self.originalContent = originalContent
        self.originalMediaURLs = originalMediaURLs
        self.originalMediaType = originalMediaType
        self.originalPostType = originalPostType
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(authorId, forKey: .authorId)
        try c.encode(authorName, forKey: .authorName)
        try c.encode(authorHandle, forKey: .authorHandle)
        try c.encodeIfPresent(authorAvatar, forKey: .authorAvatar)
        try c.encode(content, forKey: .content)
        try c.encode(views, forKey: .views)
        try c.encode(shares, forKey: .shares)
        try c.encode(repostCount, forKey: .repostCount)
        try c.encode(quoteCount, forKey: .quoteCount)
        try c.encode(isReposted, forKey: .isReposted)
        try c.encode(mediaType, forKey: .mediaType)
        try c.encode(mediaURLs, forKey: .mediaURLs)
        try c.encode(likes, forKey: .likes)
        try c.encode(repliesCount, forKey: .repliesCount)
        try c.encode(postType, forKey: .postType)
        try c.encode(isLiked, forKey: .isLiked)
        try c.encode(isBookmarked, forKey: .isBookmarked)
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        views = Self.flexInt(c, .views)
        shares = Self.flexInt(c, .shares)
        repostCount = Self.flexInt(c, .repostCount)
        quoteCount = Self.flexInt(c, .quoteCount)
        isReposted = (try? c.decode(Bool.self, forKey: .isReposted)) ?? false
        mediaType = try c.decodeIfPresent(String.self, forKey: .mediaType) ?? ""
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        authorId = try c.decodeIfPresent(String.self, forKey: .authorId) ?? ""
        authorName = try c.decodeIfPresent(String.self, forKey: .authorName) ?? ""
        authorHandle = try c.decodeIfPresent(String.self, forKey: .authorHandle) ?? ""
        authorAvatar = try c.decodeIfPresent(String.self, forKey: .authorAvatar)
        content = try c.decodeIfPresent(String.self, forKey: .content) ?? ""
        mediaURLs = (try? c.decode([String].self, forKey: .mediaURLs)) ?? []
        likes = Self.flexInt(c, .likes)
        repliesCount = Self.flexInt(c, .repliesCount)
        postType = try c.decodeIfPresent(String.self, forKey: .postType) ?? ""
        isLiked = (try? c.decode(Bool.self, forKey: .isLiked)) ?? false
        isBookmarked = (try? c.decode(Bool.self, forKey: .isBookmarked)) ?? false
        isVerifiedAuthor = false
        timestamp = 0
        isAuthorFollowed = false
        repostOfId = ""
        originalAuthorId = ""
        originalAuthorName = ""
        originalAuthorHandle = ""
        originalAuthorAvatar = nil
        originalContent = ""
        originalMediaURLs = []
        originalMediaType = ""
        originalPostType = ""
    }

    private static func flexInt(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) -> Int {
        if let value = try? c.decode(Int.self, forKey: key) { return value }
        if let value = try? c.decode(Double.self, forKey: key) { return Int(value) }
        return 0
    }

    var title: String {
        if !authorName.isEmpty { return authorName }
        if !authorHandle.isEmpty { return "@\(authorHandle)" }
        return ""
    }

    var formattedTimestamp: String {
        guard timestamp > 0 else { return "" }
        let now = Date().timeIntervalSince1970
        let ts = timestamp > 1_000_000_000_000 ? Double(timestamp) / 1000 : Double(timestamp)
        let seconds = max(0, Int64(now - ts))
        if seconds < 60 { return "now" }
        let minutes = seconds / 60
        if minutes < 60 { return "\(minutes)m" }
        let hours = minutes / 60
        if hours < 24 { return "\(hours)h" }
        let days = hours / 24
        if days < 7 { return "\(days)d" }
        let weeks = days / 7
        return "\(weeks)w"
    }

    var mediaURL: URL? { Self.resolve(mediaURLs.first) }

    var playbackURL: URL? {
        let preferred = mediaURLs.first { raw in
            let lower = raw.lowercased()
            return !lower.contains("thumb") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mov") || lower.contains(".m4v") || lower.contains(".webm") || lower.contains(".tmp") || lower.contains("video") || lower.contains("/shorts/"))
        } ?? mediaURLs.first { !$0.lowercased().contains("thumb") } ?? mediaURLs.first
        return Self.resolve(preferred)
    }

    var playsAsVideo: Bool {
        let type = mediaType.uppercased()
        if type.contains("VIDEO") { return true }
        if postType == "short" { return playbackURL != nil }
        let raw = (playbackURL ?? mediaURL)?.absoluteString.lowercased() ?? ""
        return raw.contains(".mp4") || raw.contains(".m3u8") || raw.contains(".mov") || raw.contains("video") || raw.contains("/shorts/")
    }

    var hasOriginal: Bool {
        !repostOfId.isEmpty && (!originalContent.isEmpty || !originalMediaURLs.isEmpty || !originalAuthorName.isEmpty)
    }

    var isSimpleRepost: Bool {
        hasOriginal && content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var quotedPost: FeedPost? {
        guard hasOriginal else { return nil }
        return FeedPost(
            id: repostOfId,
            authorId: originalAuthorId,
            authorName: originalAuthorName,
            authorHandle: originalAuthorHandle,
            authorAvatar: originalAuthorAvatar,
            content: originalContent,
            mediaURLs: originalMediaURLs,
            postType: originalPostType,
            mediaType: originalMediaType
        )
    }

    var displayPost: FeedPost { isSimpleRepost ? (quotedPost ?? self) : self }

    static func resolve(_ raw: String?) -> URL? {
        guard var raw, !raw.isEmpty else { return nil }
        let origin = AppBackend.origin.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") || raw.hasPrefix("data:") || raw.hasPrefix("file:") {
            if raw.contains("://localhost") || raw.contains("://127.0.0.1"), let host = URL(string: raw)?.host {
                raw = raw
                    .replacingOccurrences(of: "http://\(host)", with: origin)
                    .replacingOccurrences(of: "https://\(host)", with: origin)
            }
            if raw.hasPrefix("http://"), origin.hasPrefix("https://") {
                raw = "https://" + String(raw.dropFirst("http://".count))
            }
            return URL(string: raw)
        }
        var path = raw.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if !path.hasPrefix("uploads/") {
            path = "uploads/" + path
        }
        return URL(string: origin + "/" + path)
    }

    static func decodeList(_ data: Data) -> [FeedPost] {
        if let list = try? JSONDecoder().decode([FeedPost].self, from: data) {
            return list
        }
        struct Envelope: Decodable {
            struct Item: Decodable { let post: FeedPost? }
            let items: [Item]?
            let shorts: [FeedPost]?
            let posts: [FeedPost]?
        }
        guard let envelope = try? JSONDecoder().decode(Envelope.self, from: data) else { return [] }
        if let shorts = envelope.shorts { return shorts }
        if let posts = envelope.posts { return posts }
        return (envelope.items ?? []).compactMap(\.post)
    }
}

enum MediaActions {
    static func like(_ id: String) async { _ = try? await API.data("posts/\(id)/like", method: "POST") }
    static func bookmark(_ id: String) async { _ = try? await API.data("posts/\(id)/bookmark", method: "POST") }
    static func repost(_ id: String) async { _ = try? await API.data("posts/\(id)/repost", method: "POST") }
    static func view(_ id: String) async { _ = try? await API.data("posts/\(id)/view", method: "POST") }
    static func share(_ id: String) async { _ = try? await API.data("posts/\(id)/share", method: "POST") }
    static func reply(_ id: String, text: String) async throws {
        _ = try await API.data("posts/\(id)/replies", method: "POST", json: ["reply_text": text])
    }
    static func quote(_ id: String, text: String) async throws {
        _ = try await API.data("posts", method: "POST", json: [
            "content": text,
            "post_type": "standard",
            "repost_of_id": id
        ])
    }
    static func report(_ id: String, reason: String) async {
        _ = try? await API.data("posts/\(id)/report", method: "POST", json: ["reason": reason])
    }
    static func shareSheet(_ post: FeedPost) {
        let link = "https://firstapp.com/post/\(post.id)"
        let text = post.content.isEmpty ? link : "\(post.content)\n\(link)"
        DispatchQueue.main.async {
            let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let root = scene.windows.first?.rootViewController else { return }
            var presenter = root
            while let presented = presenter.presentedViewController { presenter = presented }
            presenter.present(controller, animated: true)
        }
    }
    static func copyLink(_ post: FeedPost) {
        UIPasteboard.general.string = "https://firstapp.com/post/\(post.id)"
    }
    static func likeReply(_ id: String) async { _ = try? await API.data("post-replies/\(id)/like", method: "POST") }
    static func repostReply(_ id: String) async { _ = try? await API.data("post-replies/\(id)/repost", method: "POST") }
    static func bookmarkReply(_ id: String) async { _ = try? await API.data("post-replies/\(id)/bookmark", method: "POST") }
    static func create(content: String, type: String = "standard", mediaURLs: [String] = []) async throws {
        _ = try await API.data("posts", method: "POST", json: [
            "content": content,
            "post_type": type,
            "media_urls": mediaURLs
        ])
    }
    static func userPosts(userId: String, type: String) async -> [FeedPost] {
        guard !userId.isEmpty, userId != "me", userId != "currentUser" else { return [] }
        guard let data = try? await API.data("posts", query: ["user_id": userId, "type": type, "page": "1"]) else { return [] }
        let parsed = LiveJSON.posts(from: data)
        return parsed.isEmpty ? FeedPost.decodeList(data) : parsed
    }

    static func upload(data: Data, filename: String, mime: String, folder: String) async throws -> String {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        func append(_ text: String) { body.append(Data(text.utf8)) }
        append("--\(boundary)\r\nContent-Disposition: form-data; name=\"folder\"\r\n\r\n\(folder)\r\n")
        append("--\(boundary)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\nContent-Type: \(mime)\r\n\r\n")
        body.append(data)
        append("\r\n--\(boundary)--\r\n")
        guard var req = API.request("uploads", method: "POST", body: body) else { throw APIError.badURL }
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        let (responseData, response) = try await URLSession.shared.data(for: req)
        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
        guard (200...299).contains(code),
              let json = try? JSONSerialization.jsonObject(with: responseData) as? [String: Any],
              let url = json["url"] as? String, !url.isEmpty else {
            throw APIError.status(code, String(data: responseData, encoding: .utf8) ?? "upload failed")
        }
        return url
    }
}

/// Android Social tab: `GET media/feed` matching Screenshot 2.
public struct ConnectedSocialFeedView: View {
    public var onProfileClick: (String) -> Void
    public var onDirectMessagesClick: () -> Void

    public init(onProfileClick: @escaping (String) -> Void = { _ in }, onDirectMessagesClick: @escaping () -> Void = {}) {
        self.onProfileClick = onProfileClick
        self.onDirectMessagesClick = onDirectMessagesClick
    }

    public var body: some View {
        SocialFeedView(onProfileClick: onProfileClick, onDirectMessagesClick: onDirectMessagesClick)
    }
}

struct SocialFeedView: View {
    public var onProfileClick: (String) -> Void = { _ in }
    public var onDirectMessagesClick: () -> Void = {}

    @State private var posts: [FeedPost] = []
    @State private var status = "Loading social feed..."
    @State private var draft = ""
    @State private var selectedCategory = 0
    @State private var replyPost: FeedPost?
    @State private var replyText = ""
    @State private var photoItem: PhotosPickerItem?
    @State private var selectedPost: FeedPost?
    @State private var repostTarget: FeedPost?
    @State private var moreTarget: FeedPost?
    @State private var quoteTarget: FeedPost?
    @State private var quoteText = ""
    @State private var showSocialMenu = false
    @State private var showCreatePost = false
    @State private var showSavedPosts = false
    @State private var showSocialNotes = false
    @State private var showSocialSettings = false
    @State private var showSocialSearch = false

    private let categories = [
        ("all", "All"), ("tech", "Technology"), ("music", "Music"), ("gaming", "Gaming"),
        ("sports", "Sports"), ("news", "News"), ("entertainment", "Entertainment"), ("art", "Art")
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Top App Bar matching Screenshot 2
            HStack {
                Button(action: { showSocialMenu = true }) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer()

                AppLogo(size: 36)

                Spacer()

                Button(action: { showSocialSearch = true }) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Category Sub-Pills Row matching Screenshot 2
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(0..<categories.count, id: \.self) { idx in
                        let isSelected = selectedCategory == idx
                        Button(action: {
                            selectedCategory = idx
                            Task { await load() }
                        }) {
                            Text(categories[idx].1)
                                .font(.system(size: 13, weight: .black))
                                .padding(.horizontal, 18)
                                .padding(.vertical, 8)
                                .background(isSelected ? Color.appPrimary : Color(UIColor.secondarySystemBackground))
                                .foregroundColor(isSelected ? .white : .primary)
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
            }

            Divider()

            // Main Posts Feed List
            Group {
                if posts.isEmpty {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text(status).font(.subheadline).foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(posts.enumerated()), id: \.element.id) { index, post in
                                FeedPostCard(
                                    post: post,
                                    onOpen: { selectedPost = post },
                                    onProfile: { onProfileClick(post.authorId) },
                                    onReply: { selectedPost = post },
                                    onRepost: { repostTarget = post },
                                    onLike: { toggleLike(post.id) },
                                    onSave: { toggleSave(post.id) },
                                    onShare: { share(post.id) },
                                    onMore: { moreTarget = post }
                                )
                                if (index + 1) % 4 == 0 {
                                    NativeAdRow(slot: "feed-\(post.id)")
                                }
                            }
                        }
                    }
                }
            }
        }
        .fullScreenCover(isPresented: Binding(get: { selectedPost != nil }, set: { if !$0 { selectedPost = nil } })) {
            if let post = selectedPost {
                PostDetailView(post: post, onProfileClick: onProfileClick, onClose: { selectedPost = nil })
            }
        }
        .confirmationDialog("Repost", isPresented: Binding(get: { repostTarget != nil }, set: { if !$0 { repostTarget = nil } }), titleVisibility: .visible) {
            Button(repostTarget?.isReposted == true ? "Undo Repost" : "Repost") {
                if let id = repostTarget?.id { toggleRepost(id) }
            }
            Button("Quote Post") {
                quoteTarget = repostTarget
                quoteText = ""
            }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: $showSocialMenu) {
            SocialMenuSheet(
                onProfile: {
                    showSocialMenu = false
                    let myId = UserDefaults.standard.string(forKey: "tj_user_id") ?? ""
                    if !myId.isEmpty { onProfileClick(myId) }
                },
                onCreate: { showSocialMenu = false; showCreatePost = true },
                onNotifications: { showSocialMenu = false; showSocialNotes = true },
                onMessages: { showSocialMenu = false; onDirectMessagesClick() },
                onSettings: { showSocialMenu = false; showSocialSettings = true },
                onSaved: { showSocialMenu = false; showSavedPosts = true }
            )
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showCreatePost) {
            CreatePostSheet { text, media in
                showCreatePost = false
                Task {
                    try? await MediaActions.create(content: text, mediaURLs: media)
                    await load()
                }
            }
        }
        .sheet(isPresented: $showSavedPosts) {
            SavedPostsSheet { post in
                showSavedPosts = false
                selectedPost = post
            }
        }
        .sheet(isPresented: $showSocialNotes) {
            SocialNotesSheet { item in
                showSocialNotes = false
                Task { await openNotification(item) }
            }
        }
        .sheet(isPresented: $showSocialSearch) {
            SocialPostSearch { post in
                showSocialSearch = false
                selectedPost = post
            }
        }
        .sheet(isPresented: $showSocialSettings) {
            SettingsView(router: NavigationRouter())
        }
        .confirmationDialog("Post", isPresented: Binding(get: { moreTarget != nil }, set: { if !$0 { moreTarget = nil } }), titleVisibility: .visible) {
            Button("Share") { if let id = moreTarget?.id { share(id) } }
            Button("Copy link") { if let post = moreTarget { MediaActions.copyLink(post) } }
            Button("Save") { if let id = moreTarget?.id { toggleSave(id) } }
            Button("Report", role: .destructive) { if let id = moreTarget?.id { Task { await MediaActions.report(id, reason: "report") } } }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(item: $quoteTarget) { post in
            NavigationStack {
                VStack(spacing: 12) {
                    TextField("Add a comment", text: $quoteText, axis: .vertical)
                        .textFieldStyle(.roundedBorder)
                    Button("Post") {
                        let text = quoteText.trimmingCharacters(in: .whitespacesAndNewlines)
                        quoteTarget = nil
                        Task {
                            try? await MediaActions.quote(post.id, text: text)
                            await load()
                        }
                    }
                    .disabled(quoteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Spacer()
                }
                .padding()
                .navigationTitle("Quote Post")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { quoteTarget = nil }
                    }
                }
            }
        }
        .task { await load() }
        .refreshable { await load() }
    }

    private func postIndex(_ id: String) -> Int? {
        posts.firstIndex(where: { $0.id == id })
    }

    private func toggleLike(_ id: String) {
        guard let index = postIndex(id) else { return }
        posts[index].isLiked.toggle()
        posts[index].likes += posts[index].isLiked ? 1 : -1
        Task { await MediaActions.like(id) }
    }

    private func toggleSave(_ id: String) {
        guard let index = postIndex(id) else { return }
        posts[index].isBookmarked.toggle()
        Task { await MediaActions.bookmark(id) }
    }

    private func toggleRepost(_ id: String) {
        guard let index = postIndex(id) else { return }
        posts[index].isReposted.toggle()
        posts[index].repostCount += posts[index].isReposted ? 1 : -1
        Task { await MediaActions.repost(id) }
    }

    private func share(_ id: String) {
        guard let index = postIndex(id) else { return }
        posts[index].shares += 1
        let post = posts[index]
        MediaActions.shareSheet(post)
        Task { await MediaActions.share(id) }
    }

    private func attachPhoto(_ item: PhotosPickerItem) async {
        defer { photoItem = nil }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else { return }
            let url = try await MediaActions.upload(data: data, filename: "post.jpg", mime: "image/jpeg", folder: "posts")
            let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
            draft = ""
            try await MediaActions.create(content: text, mediaURLs: [url])
            await load()
        } catch {
            status = error.localizedDescription
        }
    }

    private func openNotification(_ item: SocialNoteItem) async {
        _ = try? await API.data("social/notifications/\(item.id)/read", method: "POST")
        if item.action == "FOLLOW", !item.actorId.isEmpty {
            onProfileClick(item.actorId)
            return
        }
        guard !item.postId.isEmpty,
              let data = try? await API.data("posts/\(item.postId)"),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let post = LiveJSON.post(json["post"] as? [String: Any] ?? json) else { return }
        selectedPost = post
    }

    private func load() async {
        var query = ["limit": "20", "offset": "0"]
        let categoryId = categories[selectedCategory].0
        if categoryId != "all" { query["category_id"] = categoryId }
        if posts.isEmpty, let cached = AppDiskCache.loadResponse(path: "media/feed", query: query) {
            let parsed = LiveJSON.posts(from: cached)
            posts = parsed.isEmpty ? FeedPost.decodeList(cached) : parsed
            if !posts.isEmpty { status = "" }
        }
        do {
            let data = try await API.data("media/feed", query: query)
            let parsed = LiveJSON.posts(from: data)
            posts = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
            status = posts.isEmpty ? "No posts yet" : ""
        } catch {
            if posts.isEmpty { status = error.localizedDescription }
        }
    }
}

struct FeedPostCard: View {
    let post: FeedPost
    var onOpen: () -> Void
    var onProfile: () -> Void
    var onReply: () -> Void
    var onRepost: () -> Void
    var onLike: () -> Void
    var onSave: () -> Void
    var onShare: () -> Void
    var onMore: () -> Void

    @State private var isExpanded = false
    private var shown: FeedPost { post.displayPost }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Repost Header
            if post.isSimpleRepost {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.2.squarepath")
                        .font(.system(size: 13, weight: .bold))
                    Text("\(post.authorName) reposted")
                        .font(.system(size: 13, weight: .medium))
                }
                .foregroundColor(.secondary)
            }

            // Top Row: Profile Header & Follow Button
            HStack(spacing: 10) {
                Button(action: onProfile) {
                    AsyncImage(url: FeedPost.resolve(shown.authorAvatar)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle()
                            .fill(Color.appPrimary.opacity(0.15))
                            .overlay(
                                Text(String(shown.title.prefix(1)).uppercased())
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.appPrimary)
                            )
                    }
                    .frame(width: 42, height: 42)
                    .clipShape(Circle())
                }
                .buttonStyle(.borderless)

                Button(action: onProfile) {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Text(shown.title.isEmpty ? (shown.authorHandle.isEmpty ? "User" : shown.authorHandle) : shown.title)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.primary)
                                .lineLimit(1)
                            if shown.isVerifiedAuthor {
                                Image(systemName: "checkmark.seal.fill")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(red: 0.00, green: 0.82, blue: 1.00))
                            }
                            if !shown.formattedTimestamp.isEmpty {
                                Text("· \(shown.formattedTimestamp)")
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                            }
                        }
                        if !shown.authorHandle.isEmpty {
                            Text("@\(shown.authorHandle)")
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }
                }
                .buttonStyle(.borderless)

                Spacer()

                if !shown.isAuthorFollowed && shown.authorId != (UserDefaults.standard.string(forKey: "tj_user_id") ?? "") {
                    Button("Follow") {
                        Task { _ = try? await API.data("users/\(shown.authorId)/follow", method: "POST") }
                    }
                    .font(.system(size: 13, weight: .bold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(Color.primary)
                    .foregroundColor(Color(UIColor.systemBackground))
                    .clipShape(Capsule())
                    .buttonStyle(.borderless)
                }

                Button(action: onMore) {
                    Image(systemName: "ellipsis")
                        .foregroundColor(.secondary)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.borderless)
            }

            // Caption Text
            if !shown.content.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text(shown.content)
                        .font(.system(size: 15))
                        .lineSpacing(3)
                        .foregroundColor(.primary)
                        .lineLimit(isExpanded ? nil : 5)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if !isExpanded && shown.content.count > 200 {
                        Button("Show More") { isExpanded = true }
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.appPrimary)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture(perform: onOpen)
            }

            // Media Grid or Video Player
            if !shown.mediaURLs.isEmpty {
                if shown.playsAsVideo, let url = shown.playbackURL ?? shown.mediaURL {
                    FeedVideoPreview(url: url)
                        .frame(maxWidth: .infinity)
                        .frame(height: 260)
                        .clipped()
                        .cornerRadius(12)
                        .contentShape(Rectangle())
                        .onTapGesture(perform: onOpen)
                } else {
                    PostMediaGrid(urls: shown.mediaURLs, onMediaClick: { _ in onOpen() })
                }
            }

            // Quoted Post Embed
            if let original = post.quotedPost, !post.isSimpleRepost {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 6) {
                        AsyncImage(url: FeedPost.resolve(original.authorAvatar)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Circle().fill(Color.gray.opacity(0.3))
                        }
                        .frame(width: 20, height: 20)
                        .clipShape(Circle())

                        Text(original.title)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.primary)
                        if !original.authorHandle.isEmpty {
                            Text("@\(original.authorHandle)")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                    if !original.content.isEmpty {
                        Text(original.content)
                            .font(.system(size: 14))
                            .lineLimit(3)
                            .foregroundColor(.primary)
                    }
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.2), lineWidth: 1))
                .contentShape(Rectangle())
                .onTapGesture(perform: onOpen)
            }

            // Social Action Bar
            PostActionBar(
                post: post,
                onReply: onReply,
                onRepost: onRepost,
                onLike: onLike,
                onSave: onSave,
                onShare: onShare
            )
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
    }
}

struct PostMediaGrid: View {
    let urls: [String]
    var onMediaClick: (Int) -> Void

    var body: some View {
        let validURLs = urls.compactMap { FeedPost.resolve($0) }
        guard !validURLs.isEmpty else { return AnyView(EmptyView()) }

        if validURLs.count == 1 {
            return AnyView(
                AsyncImage(url: validURLs[0]) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.2)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 250)
                .clipped()
                .cornerRadius(12)
                .contentShape(Rectangle())
                .onTapGesture { onMediaClick(0) }
            )
        } else if validURLs.count == 2 {
            return AnyView(
                HStack(spacing: 4) {
                    gridImage(validURLs[0], index: 0)
                    gridImage(validURLs[1], index: 1)
                }
                .frame(height: 200)
                .cornerRadius(12)
            )
        } else if validURLs.count == 3 {
            return AnyView(
                HStack(spacing: 4) {
                    gridImage(validURLs[0], index: 0)
                    VStack(spacing: 4) {
                        gridImage(validURLs[1], index: 1)
                        gridImage(validURLs[2], index: 2)
                    }
                }
                .frame(height: 220)
                .cornerRadius(12)
            )
        } else {
            return AnyView(
                VStack(spacing: 4) {
                    HStack(spacing: 4) {
                        gridImage(validURLs[0], index: 0)
                        gridImage(validURLs[1], index: 1)
                    }
                    HStack(spacing: 4) {
                        gridImage(validURLs[2], index: 2)
                        ZStack {
                            gridImage(validURLs[3], index: 3)
                            if validURLs.count > 4 {
                                Color.black.opacity(0.5)
                                Text("+\(validURLs.count - 4)")
                                    .font(.title2.bold())
                                    .foregroundColor(.white)
                            }
                        }
                    }
                }
                .frame(height: 240)
                .cornerRadius(12)
            )
        }
    }

    private func gridImage(_ url: URL, index: Int) -> some View {
        AsyncImage(url: url) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Color.gray.opacity(0.2)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .contentShape(Rectangle())
        .onTapGesture { onMediaClick(index) }
    }
}

struct PostActionBar: View {
    let post: FeedPost
    var onReply: () -> Void
    var onRepost: () -> Void
    var onLike: () -> Void
    var onSave: () -> Void
    var onShare: () -> Void

    var body: some View {
        HStack {
            actionButton(
                icon: "bubble.left",
                count: post.repliesCount,
                color: .secondary,
                action: onReply
            )
            Spacer()
            actionButton(
                icon: "arrow.2.squarepath",
                count: post.repostCount,
                color: post.isReposted ? Color(red: 0.00, green: 0.73, blue: 0.49) : .secondary,
                action: onRepost
            )
            Spacer()
            actionButton(
                icon: post.isLiked ? "heart.fill" : "heart",
                count: post.likes,
                color: post.isLiked ? Color(red: 0.976, green: 0.094, blue: 0.502) : .secondary,
                action: onLike
            )
            Spacer()
            actionButton(
                icon: "chart.bar",
                count: post.views,
                color: .secondary,
                action: {}
            )
            Spacer()
            actionButton(
                icon: post.isBookmarked ? "bookmark.fill" : "bookmark",
                count: post.quoteCount,
                color: post.isBookmarked ? Color(red: 0.114, green: 0.608, blue: 0.941) : .secondary,
                action: onSave
            )
            Spacer()
            Button(action: onShare) {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                    .frame(minWidth: 32, minHeight: 32)
            }
            .buttonStyle(.borderless)
        }
        .padding(.top, 4)
    }

    private func actionButton(icon: String, count: Int, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 15))
                if count > 0 {
                    Text(formatNumber(count))
                        .font(.system(size: 12, weight: .medium))
                }
            }
            .foregroundColor(color)
            .frame(minWidth: 32, minHeight: 32)
        }
        .buttonStyle(.borderless)
    }

    private func formatNumber(_ num: Int) -> String {
        if num >= 1_000_000 { return String(format: "%.1fM", Double(num) / 1_000_000) }
        if num >= 1_000 { return String(format: "%.1fK", Double(num) / 1_000) }
        return "\(num)"
    }
}

private struct PostPill: View {
    let text: String
    var selected: Bool = false
    var tint: Color = .primary
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(selected ? tint : .primary)
                .padding(.horizontal, 14)
                .frame(minHeight: 36)
                .background(Color(UIColor.secondarySystemBackground))
                .clipShape(Capsule())
        }
        .buttonStyle(.borderless)
        .frame(minHeight: 44)
    }
}

private struct FeedVideoPreview: View {
    let url: URL
    var body: some View {
        ZStack {
            Color.black
            Image(systemName: "play.circle.fill")
                .font(.system(size: 42))
                .foregroundColor(.white)
        }
    }
}

/// Android Scroll tab: `GET media/feed/shorts` matching Screenshot 8.
public struct ConnectedShortsFeedView: View {
    public var onProfileClick: (String) -> Void

    public init(onProfileClick: @escaping (String) -> Void = { _ in }) {
        self.onProfileClick = onProfileClick
    }

    public var body: some View {
        ShortsFeedView(onProfileClick: onProfileClick)
    }
}

/// Android Court/Mingle tab matching Screenshot 5.
public struct ConnectedMingleFeedView: View {
    public var onProfileClick: (String) -> Void
    public var onChatClick: (String) -> Void

    public init(onProfileClick: @escaping (String) -> Void = { _ in }, onChatClick: @escaping (String) -> Void = { _ in }) {
        self.onProfileClick = onProfileClick
        self.onChatClick = onChatClick
    }

    public var body: some View {
        MingleView(onMatchClick: { conversationId, _ in onChatClick(conversationId) })
    }
}

struct ShortsFeedView: View {
    public var onProfileClick: (String) -> Void = { _ in }
    @State private var shorts: [FeedPost] = []
    @State private var status = "Loading shorts..."
    @State private var selectedShort: FeedPost?
    @State private var page = 0
    @State private var showCreateShort = false
    @State private var showShortSearch = false
    @State private var commentShort: FeedPost?
    @State private var dismissedShortAds: Set<Int> = []
    @State private var showShortsAd = false

    var body: some View {
        Group {
            if shorts.isEmpty {
                VStack(spacing: 12) {
                    if status == "Loading shorts..." { ProgressView() }
                    Text(status).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                    if status == "No shorts yet" {
                        Button("Create Short") { showCreateShort = true }
                            .font(.headline)
                    }
                }
                .padding()
            } else {
                GeometryReader { geo in
                VerticalShortsPlayer(posts: shorts, page: $page)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .overlay(alignment: .topLeading) {
                        Button { showCreateShort = true } label: {
                            Image(systemName: "plus")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                                .background(Color.black.opacity(0.35))
                                .clipShape(Circle())
                        }
                        .padding(.top, 48)
                        .padding(.leading, 16)
                    }
                    .overlay(alignment: .topTrailing) {
                        if shorts.indices.contains(page) {
                            Button { showShortSearch = true } label: {
                                Image(systemName: "magnifyingglass")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(width: 44, height: 44)
                                    .background(Color.black.opacity(0.35))
                                    .clipShape(Circle())
                            }
                            .padding(.top, 48)
                            .padding(.trailing, 16)
                        }
                    }
                    .overlay(alignment: .bottom) {
                        if shorts.indices.contains(page) {
                            ShortsChrome(
                                post: shorts[page],
                                onProfile: {
                                    let authorId = shorts[page].authorId
                                    if !authorId.isEmpty { onProfileClick(authorId) }
                                },
                                onOpen: { selectedShort = shorts[page] },
                                onComment: { commentShort = shorts[page] },
                                onLike: { toggleShortLike(shorts[page].id) },
                                onSave: { toggleShortSave(shorts[page].id) },
                                onShare: { shareShort(shorts[page].id) }
                            )
                        }
                    }
                }
                .ignoresSafeArea()
            }
        }
        .onChange(of: page) { newPage in
            guard shorts.indices.contains(newPage), (newPage + 1) % 8 == 0, !dismissedShortAds.contains(newPage) else { return }
            #if canImport(GoogleMobileAds)
            if NativeAdPool.shared.claim(slot: "short-\(newPage)") != nil {
                showShortsAd = true
            } else {
                dismissedShortAds.insert(newPage)
            }
            #else
            dismissedShortAds.insert(newPage)
            #endif
        }
        .fullScreenCover(isPresented: $showShortsAd) {
            #if canImport(GoogleMobileAds)
            NativeAdCover(continueTitle: "Continue", slot: "short-\(page)") {
                dismissedShortAds.insert(page)
                showShortsAd = false
            }
            #else
            Color.clear.onAppear { showShortsAd = false }
            #endif
        }
        .fullScreenCover(isPresented: Binding(get: { selectedShort != nil }, set: { if !$0 { selectedShort = nil } })) {
            if let post = selectedShort {
                PostDetailView(post: post, onProfileClick: onProfileClick, onClose: { selectedShort = nil })
            }
        }
        .sheet(item: $commentShort) { post in
            ShortCommentsSheet(post: post) { count in
                if let index = shorts.firstIndex(where: { $0.id == post.id }) {
                    shorts[index].repliesCount = count
                }
            }
        }
        .sheet(isPresented: $showCreateShort) {
            CreateShortSheet {
                showCreateShort = false
                Task { await load() }
            }
        }
        .sheet(isPresented: $showShortSearch) {
            LiveSearchView(
                onOpenProfile: { id in
                    showShortSearch = false
                    onProfileClick(id)
                },
                onOpenChat: { id, _ in
                    showShortSearch = false
                    NotificationCenter.default.post(name: TJEvents.openChat, object: nil, userInfo: ["id": id])
                }
            )
        }
        .task {
            #if canImport(GoogleMobileAds)
            NativeAdPool.shared.warm()
            #endif
            await load()
        }
        .refreshable { await load() }
    }

    private func load() async {
        if shorts.isEmpty, let cached = AppDiskCache.loadResponse(path: "media/feed/shorts", query: ["page": "1"]) {
            let parsed = LiveJSON.posts(from: cached)
            shorts = parsed.isEmpty ? FeedPost.decodeList(cached) : parsed
            if !shorts.isEmpty { status = "" }
        }
        do {
            var collected: [FeedPost] = []
            for page in 1...5 {
                let data = try await API.data("media/feed/shorts", query: ["page": "\(page)"])
                let parsed = LiveJSON.posts(from: data)
                let batch = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
                if batch.isEmpty { break }
                for post in batch where !collected.contains(where: { $0.id == post.id }) {
                    collected.append(post)
                }
                if batch.count < 5 { break }
            }
            shorts = collected
            status = shorts.isEmpty ? "No shorts yet" : ""
        } catch {
            if shorts.isEmpty { status = error.localizedDescription }
        }
    }

    private func toggleShortLike(_ id: String) {
        guard let index = shorts.firstIndex(where: { $0.id == id }) else { return }
        shorts[index].isLiked.toggle()
        shorts[index].likes += shorts[index].isLiked ? 1 : -1
        Task { await MediaActions.like(id) }
    }

    private func toggleShortSave(_ id: String) {
        guard let index = shorts.firstIndex(where: { $0.id == id }) else { return }
        shorts[index].isBookmarked.toggle()
        Task { await MediaActions.bookmark(id) }
    }

    private func shareShort(_ id: String) {
        guard let index = shorts.firstIndex(where: { $0.id == id }) else { return }
        shorts[index].shares += 1
        MediaActions.shareSheet(shorts[index])
        Task { await MediaActions.share(id) }
    }
}

private struct ShortsChrome: View {
    let post: FeedPost
    var onProfile: () -> Void
    var onOpen: () -> Void
    var onComment: () -> Void = {}
    var onLike: () -> Void
    var onSave: () -> Void
    var onShare: () -> Void
    @State private var showMore = false

    var body: some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 8) {
                Button(action: onProfile) {
                    Text(post.title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                }
                if !post.content.isEmpty {
                    Text(post.content)
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                        .lineLimit(2)
                        .onTapGesture(perform: onOpen)
                }
            }
            Spacer(minLength: 0)
            VStack(spacing: 18) {
                Button(action: onLike) {
                    VStack(spacing: 4) {
                        Image(systemName: post.isLiked ? "heart.fill" : "heart")
                            .font(.system(size: 28))
                            .foregroundColor(post.isLiked ? .red : .white)
                        Text("\(post.likes)").font(.system(size: 12, weight: .bold)).foregroundColor(.white)
                    }
                }
                Button(action: onComment) {
                    VStack(spacing: 4) {
                        Image(systemName: "message.fill").font(.system(size: 26)).foregroundColor(.white)
                        Text("\(post.repliesCount)").font(.system(size: 12, weight: .bold)).foregroundColor(.white)
                    }
                }
                Button(action: onSave) {
                    Image(systemName: post.isBookmarked ? "bookmark.fill" : "bookmark")
                        .font(.system(size: 26))
                        .foregroundColor(.white)
                }
                Button(action: onShare) {
                    Image(systemName: "arrowshape.turn.up.right.fill")
                        .font(.system(size: 26))
                        .foregroundColor(.white)
                }
                Button { showMore = true } label: {
                    Image(systemName: "ellipsis").font(.system(size: 24, weight: .bold)).foregroundColor(.white)
                }
                .confirmationDialog("Short", isPresented: $showMore, titleVisibility: .visible) {
                    Button("Share", action: onShare)
                    Button("Copy link") { MediaActions.copyLink(post) }
                    Button("Report", role: .destructive) { Task { await MediaActions.report(post.id, reason: "report") } }
                    Button("Cancel", role: .cancel) {}
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 32)
    }
}

// MARK: - Short Clip View matching Screenshot 8

private struct ShortClipView: View {
    let post: FeedPost
    var isActive: Bool = false
    var showsVideo: Bool = true
    let onProfile: () -> Void
    var onOpen: () -> Void = {}
    var onLike: () -> Void = {}
    var onSave: () -> Void = {}
    var onShare: () -> Void = {}
    @State private var player: AVPlayer?
    @State private var showMore = false

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            if showsVideo {
                if let player {
                    ShortPlayerSurface(player: player)
                        .ignoresSafeArea()
                } else if let image = post.mediaURL {
                    AsyncImage(url: image) { loaded in
                        loaded.resizable().scaledToFill()
                    } placeholder: {
                        Color.black
                    }
                    .ignoresSafeArea()
                } else {
                    Color.black.ignoresSafeArea()
                }
            }

            // Top Right Search Icon Overlay matching Screenshot 8
            HStack {
                Spacer()
                Button(action: onOpen) {
                    Circle()
                        .fill(Color.black.opacity(0.35))
                        .frame(width: 44, height: 44)
                        .overlay(
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                        )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 48)
            .frame(maxHeight: .infinity, alignment: .top)

            // Right Action Sidebar matching Screenshot 8
            HStack(alignment: .bottom) {
                // Bottom Left Author Info Overlay matching Screenshot 8
                VStack(alignment: .leading, spacing: 8) {
                    Button(action: onProfile) {
                        HStack(spacing: 10) {
                            AsyncImage(url: FeedPost.resolve(post.authorAvatar)) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Circle().fill(Color.gray.opacity(0.4))
                                    .overlay(Text(String(post.title.prefix(1)).uppercased()).foregroundColor(.white))
                            }
                            .frame(width: 44, height: 44)
                            .clipShape(Circle())
                            Text(post.title)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                        }
                    }

                    if !post.content.isEmpty {
                        Text(post.content)
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                            .lineLimit(2)
                            .onTapGesture(perform: onOpen)
                    }
                }

                Spacer()

                VStack(spacing: 20) {
                    Button(action: onProfile) {
                        AsyncImage(url: FeedPost.resolve(post.authorAvatar)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Circle().fill(Color.gray.opacity(0.4))
                                .overlay(Text(String(post.title.prefix(1)).uppercased()).foregroundColor(.white))
                        }
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                    }

                    Button(action: onLike) {
                        VStack(spacing: 4) {
                            Image(systemName: post.isLiked ? "heart.fill" : "heart")
                                .font(.system(size: 28))
                                .foregroundColor(post.isLiked ? .red : .white)
                            Text("\(post.likes)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    Button(action: onOpen) {
                        VStack(spacing: 4) {
                            Image(systemName: "message.fill")
                                .font(.system(size: 26))
                                .foregroundColor(.white)
                            Text("\(post.repliesCount)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    Button(action: onSave) {
                        VStack(spacing: 4) {
                            Image(systemName: post.isBookmarked ? "bookmark.fill" : "bookmark")
                                .font(.system(size: 26))
                                .foregroundColor(.white)
                            Text("Save")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    Button(action: onShare) {
                        VStack(spacing: 4) {
                            Image(systemName: "arrowshape.turn.up.right.fill")
                                .font(.system(size: 26))
                                .foregroundColor(.white)
                            Text("Share")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    Button { showMore = true } label: {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .confirmationDialog("Short", isPresented: $showMore, titleVisibility: .visible) {
                        Button("Share", action: onShare)
                        Button("Copy link") { MediaActions.copyLink(post) }
                        Button("Report", role: .destructive) { Task { await MediaActions.report(post.id, reason: "report") } }
                        Button("Cancel", role: .cancel) {}
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .onAppear { if showsVideo { startPlayback() } }
        .onChange(of: isActive) { active in
            guard showsVideo else { return }
            if active { startPlayback() } else { player?.pause() }
        }
        .onDisappear { player?.pause() }
    }

    private func startPlayback() {
        guard showsVideo else { return }
        guard isActive || player == nil else {
            player?.play()
            return
        }
        guard let url = post.playbackURL ?? post.mediaURL else { return }
        if player == nil {
            let next = AVPlayer(url: url)
            next.isMuted = false
            player = next
        }
        if isActive {
            player?.play()
            Task { await MediaActions.view(post.id) }
        }
    }
}

private struct ShortPlayerSurface: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> ShortPlayerView {
        let view = ShortPlayerView()
        view.playerLayer.player = player
        view.playerLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: ShortPlayerView, context: Context) {
        uiView.playerLayer.player = player
    }
}

private final class ShortPlayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }
    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
}

private struct VerticalShortsPlayer: UIViewControllerRepresentable {
    let posts: [FeedPost]
    @Binding var page: Int

    func makeUIViewController(context: Context) -> ShortsPagingController {
        let controller = ShortsPagingController()
        controller.onPage = { newPage in
            if page != newPage { page = newPage }
        }
        return controller
    }

    func updateUIViewController(_ controller: ShortsPagingController, context: Context) {
        controller.onPage = { newPage in
            if page != newPage { page = newPage }
        }
        controller.setPosts(posts, page: page)
    }
}

private struct SocialMenuSheet: View {
    var onProfile: () -> Void
    var onCreate: () -> Void
    var onNotifications: () -> Void
    var onMessages: () -> Void
    var onSettings: () -> Void
    var onSaved: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Social").font(.headline).padding(.top, 16).padding(.bottom, 8)
            menuRow("View Profile", onProfile)
            menuRow("Create Post", onCreate)
            menuRow("Notifications", onNotifications)
            menuRow("Direct Messages", onMessages)
            menuRow("Settings", onSettings)
            menuRow("Saved Posts", onSaved)
            Spacer()
        }
        .padding(.horizontal, 24)
    }

    private func menuRow(_ title: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).foregroundColor(.primary).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 12)
        }
    }
}

private struct CreatePostSheet: View {
    var onPost: (String, [String]) -> Void
    @State private var text = ""
    @State private var photo: PhotosPickerItem?
    @State private var media: [String] = []
    @State private var note = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading) {
                TextField("What's happening?", text: $text, axis: .vertical)
                    .padding()
                PhotosPicker(selection: $photo, matching: .images) {
                    Label(media.isEmpty ? "Add photo" : "Photo added", systemImage: "photo")
                        .padding(.horizontal)
                }
                if !note.isEmpty {
                    Text(note).font(.caption).foregroundColor(.secondary).padding(.horizontal)
                }
                Spacer()
            }
            .navigationTitle("Create Post")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Post") { onPost(text, media) }
                        .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && media.isEmpty)
                }
            }
            .onChange(of: photo) { item in
                guard let item else { return }
                Task { await upload(item) }
            }
        }
    }

    private func upload(_ item: PhotosPickerItem) async {
        do {
            guard let data = try await item.loadTransferable(type: Data.self),
                  let jpeg = UIImage(data: data)?.jpegData(compressionQuality: 0.85) else {
                note = "Could not read that photo"
                return
            }
            let url = try await MediaActions.upload(data: jpeg, filename: "post.jpg", mime: "image/jpeg", folder: "posts")
            if !url.isEmpty { media = [url] }
            note = ""
        } catch {
            note = "Could not upload that photo"
        }
    }
}
struct SocialNoteItem: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let actorId: String
    let postId: String
    let action: String
}

private struct SavedPostsSheet: View {
    var onOpen: (FeedPost) -> Void
    @State private var posts: [FeedPost] = []
    @State private var note = "Loading..."

    var body: some View {
        NavigationStack {
            List(posts) { post in
                Button { onOpen(post) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(post.authorName).font(.headline).foregroundColor(.primary)
                        Text(post.content).lineLimit(2).font(.subheadline).foregroundColor(.secondary)
                    }
                }
            }
            .overlay { if posts.isEmpty { Text(note).foregroundColor(.secondary) } }
            .navigationTitle("Saved Posts")
            .task { await load() }
        }
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed", query: ["category_id": "saved", "limit": "20", "offset": "0"])
            let parsed = LiveJSON.posts(from: data)
            posts = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
            note = posts.isEmpty ? "No saved posts yet" : ""
        } catch {
            note = "Could not load saved posts"
        }
    }
}

private struct SocialNotesSheet: View {
    var onOpen: (SocialNoteItem) -> Void
    @State private var rows: [SocialNoteItem] = []
    @State private var note = "Loading..."

    var body: some View {
        NavigationStack {
            List(rows) { row in
                Button { onOpen(row) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(row.title).font(.headline).foregroundColor(.primary)
                        if !row.subtitle.isEmpty { Text(row.subtitle).font(.caption).foregroundColor(.secondary) }
                    }
                }
            }
            .overlay { if rows.isEmpty { Text(note).foregroundColor(.secondary) } }
            .navigationTitle("Notifications")
            .task { await load() }
        }
    }

    private func load() async {
        do {
            let data = try await API.data("social/notifications")
            rows = LiveJSON.list(data, keys: ["items", "notifications"]).compactMap { item in
                let id = LiveJSON.string(item["id"])
                guard !id.isEmpty else { return nil }
                let rawName = LiveJSON.string(item["actor_name"])
                let name = (rawName == "User" || rawName == "Someone") ? "" : rawName
                let action = LiveJSON.string(item["action_text"])
                let snippet = LiveJSON.string(item["post_snippet"])
                return SocialNoteItem(
                    id: id,
                    title: name.isEmpty ? action : name,
                    subtitle: name.isEmpty ? snippet : action,
                    actorId: LiveJSON.string(item["actor_id"]),
                    postId: LiveJSON.string(item["target_post_id"]),
                    action: LiveJSON.string(item["action_type"])
                )
            }
            note = rows.isEmpty ? "No social notifications yet" : ""
        } catch {
            note = "Could not load notifications"
        }
    }
}

private final class ShortsPagingController: UIViewController, UIScrollViewDelegate {
    var onPage: ((Int) -> Void)?
    private let scrollView = UIScrollView()
    private var posts: [FeedPost] = []
    private var players: [AVPlayer] = []
    private var layers: [AVPlayerLayer] = []
    private var hosts: [UIView] = []
    private var appliedPage = -1
    private var loopObservers: [NSObjectProtocol] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        scrollView.isPagingEnabled = true
        scrollView.delegate = self
        scrollView.showsVerticalScrollIndicator = false
        scrollView.bounces = true
        scrollView.contentInsetAdjustmentBehavior = .never
        view.addSubview(scrollView)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        layoutPages()
        if appliedPage < 0, !posts.isEmpty, view.bounds.height > 1 {
            appliedPage = 0
            play(0)
            onPage?(0)
        }
    }

    func setPosts(_ posts: [FeedPost], page: Int) {
        let ids = posts.map(\.id)
        if ids != self.posts.map(\.id) {
            self.posts = posts
            rebuild()
        }
        guard view.bounds.height > 1, posts.indices.contains(page), page != appliedPage else { return }
        appliedPage = page
        scrollView.setContentOffset(CGPoint(x: 0, y: view.bounds.height * CGFloat(page)), animated: false)
        play(page)
    }

    private func rebuild() {
        loopObservers.forEach { NotificationCenter.default.removeObserver($0) }
        loopObservers.removeAll()
        children.forEach { child in
            child.willMove(toParent: nil)
            child.view.removeFromSuperview()
            child.removeFromParent()
        }
        hosts.forEach { $0.removeFromSuperview() }
        hosts.removeAll()
        players.removeAll()
        layers.removeAll()
        appliedPage = -1
        for post in posts {
            let host = UIView()
            host.backgroundColor = .black
            host.isUserInteractionEnabled = false
            let player = AVPlayer()
            player.isMuted = false
            let layer = AVPlayerLayer(player: player)
            layer.videoGravity = .resizeAspectFill
            host.layer.addSublayer(layer)
            let label = UILabel()
            label.text = post.authorName.isEmpty ? post.content : post.authorName
            label.textColor = .white
            label.font = .boldSystemFont(ofSize: 16)
            label.tag = 8
            host.addSubview(label)
            if post.playbackURL == nil {
                let missing = UILabel()
                missing.text = "This short has no video"
                missing.textColor = .white
                missing.textAlignment = .center
                missing.tag = 7
                host.addSubview(missing)
            }
            scrollView.addSubview(host)
            hosts.append(host)
            players.append(player)
            layers.append(layer)
        }
        layoutPages()
    }

    private func layoutPages() {
        let size = view.bounds.size
        guard size.height > 1 else { return }
        scrollView.frame = view.bounds
        scrollView.contentSize = CGSize(width: size.width, height: size.height * CGFloat(max(posts.count, 1)))
        for (index, host) in hosts.enumerated() {
            host.frame = CGRect(x: 0, y: size.height * CGFloat(index), width: size.width, height: size.height)
            if layers.indices.contains(index) {
                layers[index].frame = host.bounds
            }
            host.viewWithTag(7)?.frame = host.bounds
            host.viewWithTag(8)?.frame = CGRect(x: 16, y: host.bounds.height - 92, width: host.bounds.width - 32, height: 24)
        }
    }

    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        settle()
    }

    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        if !decelerate { settle() }
    }

    private func settle() {
        let height = max(view.bounds.height, 1)
        let index = Int(round(scrollView.contentOffset.y / height))
        let clamped = min(max(index, 0), max(posts.count - 1, 0))
        appliedPage = clamped
        onPage?(clamped)
        play(clamped)
    }

    private func play(_ index: Int) {
        guard players.indices.contains(index) else { return }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .moviePlayback)
        try? session.setActive(true)
        for (offset, player) in players.enumerated() where offset != index {
            player.pause()
        }
        let player = players[index]
        guard let remote = posts[index].playbackURL ?? posts[index].mediaURL else {
            showNote(index, "This short has no video")
            return
        }
        let postId = posts[index].id
        Task {
            let playable = await AuthenticatedMedia.localFile(for: remote) ?? remote
            await MainActor.run {
                guard players.indices.contains(index) else { return }
                let item = AVPlayerItem(url: playable)
                player.replaceCurrentItem(with: item)
                let end = NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: item, queue: .main) { _ in
                    player.seek(to: .zero)
                    player.play()
                }
                let failed = NotificationCenter.default.addObserver(forName: .AVPlayerItemFailedToPlayToEndTime, object: item, queue: .main) { note in
                    let message = (note.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error)?.localizedDescription ?? "Video could not play"
                    self.showNote(index, message)
                }
                loopObservers.append(end)
                loopObservers.append(failed)
                player.play()
                if layers.indices.contains(index) {
                    layers[index].player = player
                }
            }
            await MediaActions.view(postId)
        }
    }

    private func showNote(_ index: Int, _ text: String) {
        guard hosts.indices.contains(index) else { return }
        let host = hosts[index]
        let label = (host.viewWithTag(7) as? UILabel) ?? UILabel()
        label.tag = 7
        label.text = text
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 3
        label.frame = host.bounds
        if label.superview == nil { host.addSubview(label) }
    }
}
