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
    let url: URL
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

/// Android Social tab: `GET media/feed`.
struct SocialFeedView: View {
    @EnvironmentObject private var router: NavigationRouter
    @State private var posts: [FeedPost] = []
    @State private var status = "Loading social feed..."
    @State private var draft = ""
    @State private var replyPost: FeedPost?
    @State private var replyText = ""
    @State private var photoItem: PhotosPickerItem?

    var body: some View {
        Group {
            if posts.isEmpty {
                VStack(spacing: 8) {
                    ProgressView()
                    Text(status).font(.subheadline).foregroundColor(.secondary)
                }
            } else {
                List(posts) { post in
                    VStack(alignment: .leading, spacing: 8) {
                        Button { openProfile(post.authorId) } label: {
                            HStack {
                                AsyncImage(url: FeedPost.resolve(post.authorAvatar)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Image(systemName: "person.fill")
                                }
                                .frame(width: 36, height: 36)
                                .clipShape(Circle())
                                VStack(alignment: .leading) {
                                    Text(post.title).font(.headline).foregroundColor(.primary)
                                    if !post.authorHandle.isEmpty {
                                        Text("@\(post.authorHandle)").font(.caption).foregroundColor(.secondary)
                                    }
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        if !post.content.isEmpty { Text(post.content) }
                        if let url = post.mediaURL {
                            AsyncImage(url: url) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Color.gray.opacity(0.15)
                            }
                            .frame(maxWidth: .infinity, minHeight: 160, maxHeight: 220)
                            .clipped()
                            .cornerRadius(12)
                        }
                        HStack(spacing: 16) {
                            Button(post.isLiked ? "Liked \(post.likes)" : "Like \(post.likes)") {
                                Task { await MediaActions.like(post.id); await load() }
                            }
                            Button("Reply \(post.repliesCount)") { replyPost = post }
                            Button("Repost") { Task { await MediaActions.repost(post.id); await load() } }
                            Button(post.isBookmarked ? "Saved" : "Save") {
                                Task { await MediaActions.bookmark(post.id); await load() }
                            }
                        }
                        .font(.caption.bold())
                    }
                    .padding(.vertical, 4)
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Social")
        .safeAreaInset(edge: .bottom) {
            HStack {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Image(systemName: "photo")
                }
                TextField("What's happening?", text: $draft)
                    .textFieldStyle(.roundedBorder)
                Button("Post") {
                    let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { return }
                    draft = ""
                    Task { try? await MediaActions.create(content: text); await load() }
                }
                .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(8)
            .background(.ultraThinMaterial)
        }
        .onChange(of: photoItem) { item in
            guard let item else { return }
            Task { await attachPhoto(item) }
        }
        .task { await load() }
        .refreshable { await load() }
        .alert("Reply", isPresented: Binding(get: { replyPost != nil }, set: { if !$0 { replyPost = nil } })) {
            TextField("Reply", text: $replyText)
            Button("Send") {
                guard let id = replyPost?.id else { return }
                let text = replyText
                replyText = ""
                replyPost = nil
                Task { try? await MediaActions.reply(id, text: text); await load() }
            }
            Button("Cancel", role: .cancel) { replyText = "" }
        }
    }


    private func attachPhoto(_ item: PhotosPickerItem) async {
        defer { photoItem = nil }
        do {
            guard let file = try await item.loadTransferable(type: PickedFile.self) else { return }
            let data = try Data(contentsOf: file.url)
            let url = try await MediaActions.upload(data: data, filename: "post.jpg", mime: "image/jpeg", folder: "posts")
            let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
            draft = ""
            try await MediaActions.create(content: text, mediaURLs: [url])
            await load()
        } catch {
            status = error.localizedDescription
        }
    }
    private func openProfile(_ userId: String) {
        guard !userId.isEmpty else { return }
        router.navigate(to: .profile(userId: userId))
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed", query: ["limit": "20", "offset": "0"])
            posts = FeedPost.decodeList(data)
            status = posts.isEmpty ? "No posts yet" : ""
        } catch {
            status = error.localizedDescription
        }
    }
}

/// Android Scroll tab: `GET media/feed/shorts`.
struct ShortsFeedView: View {
    var onDirectMessages: () -> Void = {}
    @EnvironmentObject private var router: NavigationRouter
    @State private var shorts: [FeedPost] = []
    @State private var status = "Loading shorts..."
    @State private var videoItem: PhotosPickerItem?
    @State private var uploading = false

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Group {
                if shorts.isEmpty {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text(status).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    TabView {
                        ForEach(shorts) { short in
                            ShortClipView(post: short) {
                                if !short.authorId.isEmpty {
                                    router.navigate(to: .profile(userId: short.authorId))
                                }
                            }
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                }
            }
            HStack(spacing: 8) {
                PhotosPicker(selection: $videoItem, matching: .videos) {
                    Image(systemName: uploading ? "arrow.up.circle" : "plus")
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .clipShape(Circle())
                }
                .disabled(uploading)
                Button(action: onDirectMessages) {
                    Image(systemName: "paperplane.fill")
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .clipShape(Circle())
                }
            }
            .padding()
        }
        .navigationTitle("Scroll")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .refreshable { await load() }
        .onChange(of: videoItem) { item in
            guard let item else { return }
            Task { await uploadShort(item) }
        }
    }

    private func uploadShort(_ item: PhotosPickerItem) async {
        uploading = true
        defer { uploading = false; videoItem = nil }
        do {
            guard let file = try await item.loadTransferable(type: PickedFile.self) else { return }
            let data = try Data(contentsOf: file.url)
            let url = try await MediaActions.upload(data: data, filename: "short.mp4", mime: "video/mp4", folder: "posts")
            try await MediaActions.create(content: "", type: "short", mediaURLs: [url])
            await load()
        } catch {
            status = error.localizedDescription
        }
    }

    private func load() async {
        do {
            let data = try await API.data("media/feed/shorts", query: ["page": "1"])
            shorts = FeedPost.decodeList(data)
            status = shorts.isEmpty ? "No shorts yet" : ""
        } catch {
            status = error.localizedDescription
        }
    }
}

private struct ShortClipView: View {
    let post: FeedPost
    let onProfile: () -> Void
    @State private var player: AVPlayer?

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            if let player {
                VideoPlayer(player: player).ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }
            VStack(alignment: .leading, spacing: 8) {
                Button(action: onProfile) {
                    Text(post.title).font(.headline).foregroundColor(.white)
                }
                if !post.content.isEmpty {
                    Text(post.content).foregroundColor(.white).lineLimit(3)
                }
                Button(post.isLiked ? "Liked \(post.likes)" : "Like \(post.likes)") {
                    Task { await MediaActions.like(post.id) }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(24)
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
