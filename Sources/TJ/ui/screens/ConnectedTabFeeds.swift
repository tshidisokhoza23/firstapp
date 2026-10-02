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
    let likes: Int
    let repliesCount: Int
    let postType: String
    let isLiked: Bool
    let isBookmarked: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case authorId = "author_id"
        case authorName = "author_name"
        case authorHandle = "author_handle"
        case authorAvatar = "author_avatar_url"
        case content
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
        isBookmarked: Bool = false
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
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
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
            return !lower.contains("thumb") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mov") || lower.contains("video"))
        } ?? mediaURLs.first { !$0.lowercased().contains("thumb") } ?? mediaURLs.first
        return Self.resolve(preferred)
    }

    static func resolve(_ raw: String?) -> URL? {
        guard let raw, !raw.isEmpty else { return nil }
        if raw.hasPrefix("http") { return URL(string: raw) }
        return URL(string: AppBackend.origin + "/" + raw.trimmingCharacters(in: CharacterSet(charactersIn: "/")))
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
        return FeedPost.decodeList(data)
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

    private let categories = ["All", "Technology", "Music", "Gaming"]

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
                        Button(action: { selectedCategory = idx }) {
                            Text(categories[idx])
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
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 10) {
                                Button { onProfileClick(post.authorId) } label: {
                                    AsyncImage(url: FeedPost.resolve(post.authorAvatar)) { image in
                                        image.resizable().scaledToFill()
                                    } placeholder: {
                                        Circle()
                                            .fill(Color.gray.opacity(0.3))
                                            .overlay(Text(String(post.title.prefix(1)).uppercased()).font(.system(size: 16, weight: .bold)).foregroundColor(.white))
                                    }
                                    .frame(width: 44, height: 44)
                                    .clipShape(Circle())
                                }

                                Button { onProfileClick(post.authorId) } label: {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(post.title)
                                            .font(.system(size: 15, weight: .bold))
                                            .foregroundColor(.primary)
                                            .lineLimit(1)
                                        if !post.authorHandle.isEmpty {
                                            Text("@\(post.authorHandle)")
                                                .font(.system(size: 13))
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)

                                Spacer()

                                Image(systemName: "ellipsis")
                                    .foregroundColor(.secondary)
                            }

                            Button {
                                selectedPost = post
                            } label: {
                                VStack(alignment: .leading, spacing: 10) {
                                    if !post.content.isEmpty {
                                        Text(post.content)
                                            .font(.system(size: 16))
                                            .foregroundColor(.primary)
                                            .multilineTextAlignment(.leading)
                                    }
                                    if let url = post.mediaURL {
                                        ZStack {
                                            AsyncImage(url: url) { image in
                                                image.resizable().scaledToFill()
                                            } placeholder: {
                                                Color.black.opacity(0.8)
                                            }
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 240)
                                            .clipped()
                                            .cornerRadius(16)
                                        }
                                    }
                                }
                            }
                            .buttonStyle(.plain)

                            HStack(spacing: 8) {
                                Button { selectedPost = post } label: {
                                    Text("Reply \(post.repliesCount)")
                                        .font(.system(size: 13, weight: .bold))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(Color(UIColor.secondarySystemBackground))
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.borderless)
                                Button {
                                    Task { await MediaActions.like(post.id) }
                                } label: {
                                    Text("Like \(post.likes)")
                                        .font(.system(size: 13, weight: .bold))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(Color(UIColor.secondarySystemBackground))
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                        .padding(.vertical, 8)
                        .padding(.horizontal, 16)
                            }
                        }
                    }
                }
            }
        }
        .fullScreenCover(item: $selectedPost) { post in
            PostDetailView(post: post, onProfileClick: onProfileClick, onClose: { selectedPost = nil })
        }
        .task { await load() }
        .refreshable { await load() }
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
            let data = try await API.data("media/feed", query: ["limit": "20", "offset": "0"])
            let parsed = LiveJSON.posts(from: data)
            posts = parsed.isEmpty ? FeedPost.decodeList(data) : parsed
            status = posts.isEmpty ? "No posts yet" : ""
        } catch {
            status = error.localizedDescription
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
        MingleView()
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
                GeometryReader { proxy in
                    TabView(selection: $page) {
                        ForEach(Array(shorts.enumerated()), id: \.element.id) { index, short in
                            ShortClipView(post: short, isActive: page == index, onProfile: {
                                if !short.authorId.isEmpty {
                                    onProfileClick(short.authorId)
                                }
                            }, onOpen: {
                                selectedShort = short
                            })
                            .rotationEffect(.degrees(-90))
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .tag(index)
                        }
                    }
                    .frame(width: proxy.size.height, height: proxy.size.width)
                    .rotationEffect(.degrees(90), anchor: .topLeading)
                    .offset(x: proxy.size.width)
                    .tabViewStyle(.page(indexDisplayMode: .never))
                }
                .ignoresSafeArea()
            }
        }
        .fullScreenCover(item: $selectedShort) { post in
            PostDetailView(post: post, onProfileClick: onProfileClick, onClose: { selectedShort = nil })
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
}

// MARK: - Short Clip View matching Screenshot 8

private struct ShortClipView: View {
    let post: FeedPost
    var isActive: Bool = false
    let onProfile: () -> Void
    var onOpen: () -> Void = {}
    @State private var player: AVPlayer?

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Full Screen Video Player
            if lShortPlayerSurface(player: player)
                    .ignoresSafeArea()
            } else if let image = post.mediaURL {
                AsyncImage(url: image) { loaded in
                    loaded.resizable().scaledToFill()
                } placeholder: {
                    Color.black
                }
            VideoPlayer(player: player)
                    .ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }

            // Top Right Search Icon Overlay matching Screenshot 8
            VStack {
                HStack {
                    Spacer()
                    Button(action: {}) {
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

                Spacer()
            }

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

                    Button {
                        Task { await MediaActions.like(post.id) }
                    } label: {
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

                    Button {
                        Task { await MediaActions.bookmark(post.id) }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: post.isBookmarked ? "bookmark.fill" : "bookmark")
                                .font(.system(size: 26))
                                .foregroundColor(.white)
                            Text("Save")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    Button {
                        Task { await MediaActions.share(post.id) }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: "arrowshape.turn.up.right.fill")
                                .font(.system(size: 26))
                                .foregroundColor(.white)
                            Text("Share")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    // Option Menu Dots
                    Button(action: {}) {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .onAppear { startPlayback() }
        .onChange(of: isActive) { active in
            if active { startPlayback() } else { player?.pause() }
        }
        .onDisappear { player?.pause() }

    private func startPlayback() {
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
