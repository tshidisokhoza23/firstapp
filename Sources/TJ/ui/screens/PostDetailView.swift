import SwiftUI
import AVKit

struct PostReplyItem: Identifiable {
    let id: String
    let authorId: String
    let author: String
    let text: String
}

struct PostDetailView: View {
    let post: FeedPost
    var onProfileClick: (String) -> Void
    var onClose: () -> Void

    @State private var replies: [PostReplyItem] = []
    @State private var draft = ""
    @State private var liked: Bool
    @State private var likes: Int
    @State private var saved: Bool
    @State private var reposted: Bool
    @State private var reposts: Int
    @State private var shares: Int
    @State private var status = ""
    @State private var showRepost = false
    @State private var showQuote = false
    @State private var quoteText = ""
    @State private var showMore = false

    init(post: FeedPost, onProfileClick: @escaping (String) -> Void, onClose: @escaping () -> Void) {
        self.post = post
        self.onProfileClick = onProfileClick
        self.onClose = onClose
        _liked = State(initialValue: post.isLiked)
        _likes = State(initialValue: post.likes)
        _saved = State(initialValue: post.isBookmarked)
        _reposted = State(initialValue: post.isReposted)
        _reposts = State(initialValue: post.repostCount)
        _shares = State(initialValue: post.shares)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 10) {
                        Button { onProfileClick(post.authorId) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(post.title.isEmpty ? "Post" : post.title)
                                    .font(.headline)
                                if !post.authorHandle.isEmpty {
                                    Text("@\(post.authorHandle)").font(.subheadline).foregroundColor(.secondary)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        Spacer()
                    }
                    if !post.content.isEmpty {
                        Text(post.content).font(.body)
                    }
                    if let url = post.playbackURL ?? post.mediaURL {
                        if post.playsAsVideo || isVideo(url) {
                            VideoPlayer(player: AVPlayer(url: url))
                                .frame(height: 280)
                        } else {
                            AsyncImage(url: url) { image in
                                image.resizable().scaledToFit()
                            } placeholder: {
                                ProgressView()
                            }
                        }
                    }
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            detailPill("Reply \(replies.count == 0 ? post.repliesCount : replies.count)") {}
                            detailPill(reposted ? "Reposted \(reposts)" : "Repost \(reposts)", selected: reposted) { showRepost = true }
                            detailPill(liked ? "Liked \(likes)" : "Like \(likes)", selected: liked) {
                                liked.toggle()
                                likes += liked ? 1 : -1
                                Task { await MediaActions.like(post.id) }
                            }
                            detailPill("Views \(post.views)") {}
                            detailPill(saved ? "Saved \(post.quoteCount)" : "Save \(post.quoteCount)", selected: saved) {
                                saved.toggle()
                                Task { await MediaActions.bookmark(post.id) }
                            }
                            detailPill("Share \(shares)") {
                                shares += 1
                                MediaActions.shareSheet(post)
                                Task { await MediaActions.share(post.id) }
                            }
                            detailPill("More") { showMore = true }
                        }
                    }
                    Divider()
                    Text("Replies").font(.headline)
                    if replies.isEmpty {
                        Text(status.isEmpty ? "No replies yet" : status)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(replies) { reply in
                            VStack(alignment: .leading, spacing: 4) {
                                Button(reply.author) {
                                    if !reply.authorId.isEmpty { onProfileClick(reply.authorId) }
                                }
                                .buttonStyle(.plain)
                                .font(.subheadline.weight(.bold))
                                Text(reply.text).font(.body)
                                HStack(spacing: 16) {
                                    Button("Like") { Task { await MediaActions.likeReply(reply.id) } }
                                    Button("Repost") { Task { await MediaActions.repostReply(reply.id) } }
                                    Button("Save") { Task { await MediaActions.bookmarkReply(reply.id) } }
                                }
                                .font(.caption.weight(.bold))
                                .buttonStyle(.borderless)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 6)
                        }
                    }
                    HStack {
                        TextField("Write a reply", text: $draft)
                            .textFieldStyle(.roundedBorder)
                        Button("Reply") { Task { await sendReply() } }
                            .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
                .padding()
            }
            .navigationTitle("Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onClose)
                }
            }
        }
        .confirmationDialog("Repost", isPresented: $showRepost, titleVisibility: .visible) {
            Button(reposted ? "Undo Repost" : "Repost") {
                reposted.toggle()
                reposts += reposted ? 1 : -1
                Task { await MediaActions.repost(post.id) }
            }
            Button("Quote Post") { showQuote = true }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Post", isPresented: $showMore, titleVisibility: .visible) {
            Button("Share") {
                shares += 1
                MediaActions.shareSheet(post)
                Task { await MediaActions.share(post.id) }
            }
            Button("Copy link") { MediaActions.copyLink(post) }
            Button("Report", role: .destructive) { Task { await MediaActions.report(post.id, reason: "report") } }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: $showQuote) {
            NavigationStack {
                VStack(spacing: 12) {
                    TextField("Add a comment", text: $quoteText, axis: .vertical)
                        .textFieldStyle(.roundedBorder)
                    Button("Post") {
                        let text = quoteText
                        quoteText = ""
                        showQuote = false
                        Task { try? await MediaActions.quote(post.id, text: text) }
                    }
                    .disabled(quoteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Spacer()
                }
                .padding()
                .navigationTitle("Quote Post")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showQuote = false }
                    }
                }
            }
        }
        .task {
            await MediaActions.view(post.id)
            await loadReplies()
        }
    }

    private func detailPill(_ text: String, selected: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(selected ? Color(red: 0.976, green: 0.094, blue: 0.502) : .primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(UIColor.secondarySystemBackground))
                .clipShape(Capsule())
        }
        .buttonStyle(.borderless)
    }

    private func isVideo(_ url: URL) -> Bool {
        let lower = url.absoluteString.lowercased()
        return lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mov") || lower.contains("video")
    }

    private func loadReplies() async {
        var data = try? await API.data("media/posts/\(post.id)/replies")
        if data == nil {
            data = try? await API.data("posts/\(post.id)/replies")
        }
        guard let data else {
            status = "Could not load replies"
            return
        }
        let rows = LiveJSON.list(data, keys: ["replies", "items", "data"])
        let parsed = rows.compactMap { row -> PostReplyItem? in
            let nested = (row["reply"] as? [String: Any]) ?? (row["post"] as? [String: Any]) ?? row
            let id = LiveJSON.string(nested["id"]).isEmpty ? LiveJSON.string(nested["reply_id"]) : LiveJSON.string(nested["id"])
            let text = LiveJSON.string(nested["reply_text"]).isEmpty ? LiveJSON.string(nested["content"]) : LiveJSON.string(nested["reply_text"])
            guard !id.isEmpty || !text.isEmpty else { return nil }
            let author = nested["author"] as? [String: Any]
            let name = LiveJSON.string(nested["author_name"]).isEmpty ? LiveJSON.string(author?["display_name"]) : LiveJSON.string(nested["author_name"])
            return PostReplyItem(
                id: id.isEmpty ? UUID().uuidString : id,
                authorId: LiveJSON.string(nested["author_id"]).isEmpty ? LiveJSON.string(author?["id"]) : LiveJSON.string(nested["author_id"]),
                author: name,
                text: text
            )
        }
        replies = parsed
        status = parsed.isEmpty ? "No replies yet" : ""
    }

    private func sendReply() async {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        do {
            try await MediaActions.reply(post.id, text: text)
            draft = ""
            await loadReplies()
        } catch {
            status = "Reply was not sent"
        }
    }
}
