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

    private let categories = [
        ("all", "All"), ("tech", "Technology"), ("music", "Music"), ("gaming", "Gaming"),
        ("sports", "Sports"), ("news", "News"), ("entertainment", "Entertainment"), ("art", "Art")
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Top App Bar matching Screenshot 2
            HStack {
                Button(action: {}) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer()

                AppLogo(size: 36)

                Spacer()

                Button(action: {}) {
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
                            ForEach(posts) { post in
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

    private func load() async {
        do {
            var query = ["limit": "20", "offset": "0"]
            let categoryId = categories[selectedCategory].0
            if categoryId != "all" { query["category_id"] = categoryId }
            let data = try await API.data("media/feed", query: query)
            let parsed = LiveJSON.posts(from: data)
            posts = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
            status = posts.isEmpty ? "No posts yet" : ""
        } catch {
            status = error.localizedDescription
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

    private var shown: FeedPost { post.displayPost }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if post.isSimpleRepost {
                HStack(spacing: 8) {
                    Image(systemName: "repeat")
                        .font(.system(size: 12, weight: .bold))
                    Text("\(post.authorName) reposted")
                        .font(.system(size: 12, weight: .medium))
                }
                .foregroundColor(.secondary)
            }
            HStack(spacing: 10) {
                Button(action: onProfile) {
                    AsyncImage(url: FeedPost.resolve(shown.authorAvatar)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .overlay(Text(String(shown.title.prefix(1)).uppercased()).font(.system(size: 16, weight: .bold)).foregroundColor(.white))
                    }
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())
                }
                .buttonStyle(.borderless)

                Button(action: onProfile) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(shown.title.isEmpty ? shown.authorHandle : shown.title)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.primary)
                            .lineLimit(1)
                        if !shown.authorHandle.isEmpty {
                            Text("@\(shown.authorHandle)")
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .buttonStyle(.borderless)

                Spacer()

                Button(action: onMore) {
                    Image(systemName: "ellipsis")
                        .foregroundColor(.secondary)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.borderless)
            }

            VStack(alignment: .leading, spacing: 10) {
                if !shown.content.isEmpty {
                    Text(shown.content)
                        .font(.system(size: 16))
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                if let original = post.quotedPost, !post.isSimpleRepost {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(original.title)
                            .font(.system(size: 14, weight: .bold))
                        if !original.content.isEmpty {
                            Text(original.content)
                                .font(.system(size: 14))
                                .lineLimit(4)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                }
                if let url = shown.mediaURL, !shown.playsAsVideo {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.black.opacity(0.8)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 240)
                    .clipped()
                    .cornerRadius(16)
                } else if post.playsAsVideo, let url = post.playbackURL ?? post.mediaURL {
                    FeedVideoPreview(url: url)
                        .frame(maxWidth: .infinity)
                        .frame(height: 240)
                        .clipped()
                        .cornerRadius(16)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: onOpen)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    PostPill(text: "Reply \(post.repliesCount)", action: onReply)
                    PostPill(text: "Repost \(post.repostCount)", selected: post.isReposted, tint: Color(red: 0, green: 0.73, blue: 0.49), action: onRepost)
                    PostPill(text: "Like \(post.likes)", selected: post.isLiked, tint: Color(red: 0.976, green: 0.094, blue: 0.502), action: onLike)
                    PostPill(text: "Views \(post.views)", action: {})
                    PostPill(text: post.isBookmarked ? "Saved \(post.quoteCount)" : "Save \(post.quoteCount)", selected: post.isBookmarked, tint: Color(red: 0.114, green: 0.608, blue: 0.941), action: onSave)
                    PostPill(text: "Share \(post.shares)", action: onShare)
                }
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 16)
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

    var body: some View {
        Group {
            if shorts.isEmpty {
                VStack(spacing: 8) {
                    ProgressView()
                    Text(status).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                }
                .padding()
            } else {
                GeometryReader { geo in
                VerticalShortsPlayer(posts: shorts, page: $page)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .overlay(alignment: .topTrailing) {
                        if shorts.indices.contains(page) {
                            Button { selectedShort = shorts[page] } label: {
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
        .fullScreenCover(isPresented: Binding(get: { selectedShort != nil }, set: { if !$0 { selectedShort = nil } })) {
            if let post = selectedShort {
                PostDetailView(post: post, onProfileClick: onProfileClick, onClose: { selectedShort = nil })
            }
        }
        .task { await load() }
        .refreshable { await load() }
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed/shorts", query: ["page": "1"])
            let parsed = LiveJSON.posts(from: data)
            shorts = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
            status = shorts.isEmpty ? "No shorts yet" : ""
        } catch {
            status = error.localizedDescription
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
                Button(action: onOpen) {
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

private final class ShortsPagingController: UIViewController, UIScrollViewDelegate {
    var onPage: ((Int) -> Void)?
    private let scrollView = UIScrollView()
    private var posts: [FeedPost] = []
    private var players: [AVPlayer] = []
    private var controllers: [AVPlayerViewController] = []
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
        controllers.removeAll()
        appliedPage = -1
        for post in posts {
            let host = UIView()
            host.backgroundColor = .black
            host.isUserInteractionEnabled = false
            let player = AVPlayer()
            player.isMuted = false
            let controller = AVPlayerViewController()
            controller.showsPlaybackControls = false
            controller.videoGravity = .resizeAspectFill
            controller.player = player
            controller.view.backgroundColor = .black
            controller.view.isUserInteractionEnabled = false
            addChild(controller)
            host.addSubview(controller.view)
            controller.didMove(toParent: self)
            if post.playbackURL == nil {
                let label = UILabel()
                label.text = "This short has no video"
                label.textColor = .white
                label.textAlignment = .center
                label.tag = 7
                host.addSubview(label)
            }
            scrollView.addSubview(host)
            hosts.append(host)
            players.append(player)
            controllers.append(controller)
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
            if controllers.indices.contains(index) {
                controllers[index].view.frame = host.bounds
            }
            host.viewWithTag(7)?.frame = host.bounds
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
                if controllers.indices.contains(index) {
                    controllers[index].player = player
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
