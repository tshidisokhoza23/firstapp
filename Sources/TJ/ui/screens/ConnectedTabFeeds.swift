import SwiftUI
import AVKit
import PhotosUI
import UniformTypeIdentifiers

struct FeedPost: Identifiable, Decodable {
    let id: String
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

    init(from decoder: Decoder) throws {
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

    init(
        id: String,
        authorId: String,
        authorName: String,
        authorHandle: String,
        authorAvatar: String?,
        content: String,
        mediaURLs: [String],
        likes: Int,
        repliesCount: Int,
        postType: String,
        isLiked: Bool,
        isBookmarked: Bool
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

    private static func flexInt(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) -> Int {
        if let value = try? c.decode(Int.self, forKey: key) { return value }
        if let value = try? c.decode(Double.self, forKey: key) { return Int(value) }
        return 0
    }

    var title: String {
        if !authorName.isEmpty { return authorName }
        if !authorHandle.isEmpty { return "@\(authorHandle)" }
        return "TJ"
    }

    var mediaURL: URL? { Self.resolve(mediaURLs.first) }

    static func resolve(_ raw: String?) -> URL? {
        guard let raw, !raw.isEmpty else { return nil }
        if raw.hasPrefix("http") { return URL(string: raw) }
        return URL(string: AppBackend.origin + "/" + raw.trimmingCharacters(in: CharacterSet(charactersIn: "/")))
    }

    static func decodeList(_ data: Data) -> [FeedPost] {
        LiveJSON.posts(from: data)
    }
}

enum MediaActions {
    static func like(_ id: String) async { _ = try? await API.data("posts/\(id)/like", method: "POST") }
    static func bookmark(_ id: String) async { _ = try? await API.data("posts/\(id)/bookmark", method: "POST") }
    static func repost(_ id: String) async { _ = try? await API.data("posts/\(id)/repost", method: "POST") }
    static func view(_ id: String) async { _ = try? await API.data("posts/\(id)/view", method: "POST") }
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

struct PickedFile: Transferable {
    let url: URLAPI.session
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .movie) { file in
            SentTransferredFile(file.url)
        } importing: { received in
            Self(url: Self.copy(received.file, ext: "mp4"))
        }
        FileRepresentation(contentType: .image) { file in
            SentTransferredFile(file.url)
        } importing: { received in
            Self(url: Self.copy(received.file, ext: "jpg"))
        }
    }

    private static func copy(_ source: URL, ext: String) -> URL {
        let dest = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).\(ext)")
        try? FileManager.default.removeItem(at: dest)
        try? FileManager.default.copyItem(at: source, to: dest)
        return dest
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
    @State private var ads: [Advert] = []
    @State private var status = "Loading social feed..."
    @State private var draft = ""
    @State private var selectedCategory = 0
    @State private var replyPost: FeedPost?
    @State private var replyText = ""
    @State private var photoItem: PhotosPickerItem?

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

            if !ads.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(ads) { ad in
                            AdvertCard(ad: ad)
                                .frame(width: 280)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
            }

            // Main Posts Feed List
            Group {
                if posts.isEmpty {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text(status).font(.subheadline).foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(posts) { post in
                        VStack(alignment: .leading, spacing: 10) {
                            // Repost Header matching Screenshot 2
                            HStack(spacing: 6) {
                                Image(systemName: "arrow.2.squarepath")
                                    .font(.system(size: 12))
                                Text("thapelo khara reposted")
                                    .font(.system(size: 13, weight: .medium))
                            }
                            .foregroundColor(.secondary)

                            // Post Header matching Screenshot 2
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

                                VStack(alignment: .leading, spacing: 2) {
                                    HStack(spacing: 6) {
                                        Text(post.title)
                                            .font(.system(size: 15, weight: .bold))
                                            .foregroundColor(.primary)

                                        Text("•  3d")
                                            .font(.system(size: 13))
                                            .foregroundColor(.secondary)
                                    }

                                    Text(!post.authorHandle.isEmpty ? "@\(post.authorHandle)" : "@tshidisokhoza1992")
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Image(systemName: "ellipsis")
                                    .foregroundColor(.secondary)
                            }

                            if !post.content.isEmpty {
                                Text(post.content)
                                    .font(.system(size: 16))
                                    .foregroundColor(.primary)
                            }

                            // Post Media with Central Play Overlay matching Screenshot 2
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

                                    Circle()
                                        .fill(Color.white.opacity(0.85))
                                        .frame(width: 54, height: 54)

                                    Image(systemName: "play.fill")
                                        .font(.system(size: 22))
                                        .foregroundColor(.black)
                                        .offset(x: 2)
                                }
                            }

                            // Interaction Pills Row matching Screenshot 2 (Reply 0, Repost 0, Like 0, Views 1)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    PillInteractionButton(title: "Reply \(post.repliesCount)")
                                    PillInteractionButton(title: "Repost 0")
                                    PillInteractionButton(title: "Like \(post.likes)")
                                    PillInteractionButton(title: "Views 1")
                                }
                            }
                        }
                        .padding(.vertical, 8)
                    }
                    .listStyle(.plain)
                }
            }
        }
        .task { await load() }
        .refreshable { await load() }
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed", query: ["limit": "20", "offset": "0"])
            let loaded = FeedPost.decodeList(data)
            posts = Self.withAds(loaded, ads: ads)
            status = loaded.isEmpty ? "No posts yet" : ""
        } catch {
            status = error.localizedDescription
        }
        if let data = try? await API.data("adverts") {
            ads = LiveJSON.adverts(from: data).filter { !$0.isViewed }
            if !posts.isEmpty {
                let organic = posts.filter { !$0.id.hasPrefix("ad-") }
                posts = Self.withAds(organic, ads: ads)
            }
        }
    }

    private static func withAds(_ posts: [FeedPost], ads: [Advert]) -> [FeedPost] {
        guard !ads.isEmpty else { return posts }
        var mixed: [FeedPost] = []
        var adIndex = 0
        for (index, post) in posts.enumerated() {
            mixed.append(post)
            if (index + 1) % 5 == 0 {
                let ad = ads[adIndex % ads.count]
                adIndex += 1
                mixed.append(FeedPost(
                    id: "ad-\(ad.id)-\(index)",
                    authorId: ad.advertiserId,
                    authorName: ad.advertiserName.isEmpty ? "Sponsored" : ad.advertiserName,
                    authorHandle: "sponsored",
                    authorAvatar: ad.avatarUrl,
                    content: ad.contentText.isEmpty ? ad.title : ad.contentText,
                    mediaURLs: ad.contentUrl.map { [$0] } ?? [],
                    likes: 0,
                    repliesCount: 0,
                    postType: "ad",
                    isLiked: false,
                    isBookmarked: false
                ))
            }
        }
        return mixed
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
    @State private var ads: [Advert] = []
    @State private var status = "Loading shorts..."

    var body: some View {
        Group {
            if shorts.isEmpty && ads.isEmpty {
                VStack(spacing: 8) {
                    ProgressView()
                    Text(status).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                }
                .padding()
            } else {
                TabView {
                    ForEach(slides) { slide in
                        switch slide {
                        case .clip(let short):
                            ShortClipView(post: short, onProfile: {
                                if !short.authorId.isEmpty {
                                    onProfileClick(short.authorId)
                                }
                            })
                        case .ad(let ad):
                            ShortAdPage(ad: ad)
                        }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .ignoresSafeArea()
            }
        }
        .task { await load() }
        .refreshable { await load() }
    }

    private var slides: [ShortSlide] {
        var mixed: [ShortSlide] = shorts.map { .clip($0) }
        guard !ads.isEmpty else { return mixed }
        var adIndex = 0
        var cursor = 0
        while cursor < mixed.count {
            cursor += 8
            if cursor <= mixed.count {
                mixed.insert(.ad(ads[adIndex % ads.count]), at: cursor)
                adIndex += 1
                cursor += 1
            }
        }
        if mixed.isEmpty { return ads.map { .ad($0) } }
        return mixed
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed/shorts", query: ["page": "1"])
            shorts = FeedPost.decodeList(data)
            status = shorts.isEmpty ? "No shorts yet" : ""
        } catch {
            status = error.localizedDescription
        }
        if let data = try? await API.data("adverts") {
            ads = LiveJSON.adverts(from: data).filter { !$0.isViewed }
            if shorts.isEmpty && !ads.isEmpty { status = "" }
        }
    }
}

private enum ShortSlide: Identifiable {
    case clip(FeedPost)
    case ad(Advert)

    var id: String {
        switch self {
        case .clip(let post): return post.id
        case .ad(let ad): return "ad-\(ad.id)"
        }
    }
}

private struct ShortAdPage: View {
    let ad: Advert

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 16) {
                Text("Sponsored")
                    .font(.caption.bold())
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.orange)
                    .clipShape(Capsule())
                if let url = FeedPost.resolve(ad.contentUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFit()
                    } placeholder: {
                        Color.white.opacity(0.1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 280)
                    .clipped()
                }
                Text(ad.advertiserName)
                    .font(.title3.bold())
                    .foregroundColor(.white)
                Text(ad.contentText.isEmpty ? ad.title : ad.contentText)
                    .font(.body)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }
        }
    }
}

private struct AdvertCard: View {
    let ad: Advert

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Sponsored")
                .font(.caption.bold())
                .foregroundColor(.orange)
            Text(ad.advertiserName.isEmpty ? ad.title : ad.advertiserName)
                .font(.headline)
                .lineLimit(1)
            if let url = FeedPost.resolve(ad.contentUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.2)
                }
                .frame(height: 120)
                .frame(maxWidth: .infinity)
                .clipped()
                .cornerRadius(12)
            }
            Text(ad.contentText.isEmpty ? ad.title : ad.contentText)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
        .padding(12)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
    }
}

// MARK: - Short Clip View matching Screenshot 8

private struct ShortClipView: View {
    let post: FeedPost
    let onProfile: () -> Void
    @State private var player: AVPlayer?

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Full Screen Video Player
            if let player {
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
                            Circle()
                                .fill(Color.gray.opacity(0.4))
                                .frame(width: 44, height: 44)
                                .overlay(
                                    Text("T")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(.white)
                                )

                            Text(post.title.isEmpty ? "Tshidiso Khoza" : post.title)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    if !post.content.isEmpty {
                        Text(post.content)
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                            .lineLimit(2)
                    }
                }

                Spacer()

                // Right Sidebar Buttons matching Screenshot 8
                VStack(spacing: 20) {
                    // Author Avatar Circle at top of sidebar
                    Button(action: onProfile) {
                        Circle()
                            .fill(Color.gray.opacity(0.4))
                            .frame(width: 48, height: 48)
                            .overlay(
                                Text("T")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(.white)
                            )
                    }

                    // Like Button
                    VStack(spacing: 4) {
                        Image(systemName: post.isLiked ? "heart.fill" : "heart.fill")
                            .font(.system(size: 28))
                            .foregroundColor(post.isLiked ? .red : .white)
                        Text("\(post.likes > 0 ? post.likes : 2)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }

                    // Comment Button
                    VStack(spacing: 4) {
                        Image(systemName: "message.fill")
                            .font(.system(size: 26))
                            .foregroundColor(.white)
                        Text("0")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }

                    // Repost Button
                    VStack(spacing: 4) {
                        Image(systemName: "arrow.2.squarepath")
                            .font(.system(size: 26))
                            .foregroundColor(.white)
                        Text("1")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }

                    // Save Button
                    VStack(spacing: 4) {
                        Image(systemName: post.isBookmarked ? "bookmark.fill" : "bookmark")
                            .font(.system(size: 26))
                            .foregroundColor(.white)
                        Text("Save")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }

                    // Share Button
                    VStack(spacing: 4) {
                        Image(systemName: "arrowshape.turn.up.right.fill")
                            .font(.system(size: 26))
                            .foregroundColor(.white)
                        Text("Share")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
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
        .onAppear {
            if player == nil, let url = post.mediaURL {
                let next = AVPlayer(url: url)
                player = next
                next.play()
            } else {
                player?.play()
            }
            Task { await MediaActions.view(post.id) }
        }
        .onDisappear { player?.pause() }
    }
}
