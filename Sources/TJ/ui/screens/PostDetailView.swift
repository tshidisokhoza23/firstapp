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

    @State private var replies: [FeedPost] = []
    @State private var videoURL: URL?
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
                    if post.isSimpleRepost {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.2.squarepath")
                            Text("\(post.authorName) reposted")
                        }
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.secondary)
                    }
                    HStack(spacing: 10) {
                        Button { onProfileClick(post.displayPost.authorId) } label: {
                            AsyncImage(url: FeedPost.resolve(post.displayPost.authorAvatar)) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Circle().fill(Color.appPrimary.opacity(0.15))
                                    .overlay(
                                        Text(String(post.displayPost.title.prefix(1)).uppercased())
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(.appPrimary)
                                    )
                            }
                            .frame(width: 44, height: 44)
                            .clipShape(Circle())
                        }
                        .buttonStyle(.plain)

                        Button { onProfileClick(post.displayPost.authorId) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 4) {
                                    Text(post.displayPost.title.isEmpty ? "User" : post.displayPost.title)
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(.primary)
                                    if post.displayPost.isVerifiedAuthor {
                                        Image(systemName: "checkmark.seal.fill")
                                            .font(.system(size: 14))
                                            .foregroundColor(Color(red: 0.00, green: 0.82, blue: 1.00))
                                    }
                                    if !post.displayPost.formattedTimestamp.isEmpty {
                                        Text("· \(post.displayPost.formattedTimestamp)")
                                            .font(.system(size: 13))
                                            .foregroundColor(.secondary)
                                    }
                                }
                                if !post.displayPost.authorHandle.isEmpty {
                                    Text("@\(post.displayPost.authorHandle)").font(.subheadline).foregroundColor(.secondary)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        Spacer()
                    }
                    if !post.displayPost.content.isEmpty {
                        Text(post.displayPost.content).font(.body)
                    }
                    if let original = post.quotedPost, !post.isSimpleRepost {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(original.title).font(.subheadline.weight(.bold))
                            if !original.content.isEmpty { Text(original.content).font(.subheadline) }
                        }
                        .padding(10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.2)))
                    }
                    if let url = videoURL ?? post.displayPost.playbackURL ?? post.displayPost.mediaURL {
                        if post.displayPost.playsAsVideo || isVideo(url) {
                            VideoPlayer(player: AVPlayer(url: videoURL ?? url))
                                .frame(height: 280)
                                .cornerRadius(12)
                        } else if !post.displayPost.mediaURLs.isEmpty {
                            PostMediaGrid(urls: post.displayPost.mediaURLs, onMediaClick: { _ in })
                        }
                    }
                    Divider()
                    PostActionBar(
                        post: post,
                        onReply: { draft = post.displayPost.authorHandle.isEmpty ? "" : "@\(post.displayPost.authorHandle) " },
                        onRepost: { showRepost = true },
                        onLike: {
                            liked.toggle()
                            likes += liked ? 1 : -1
                            Task { await MediaActions.like(post.id) }
                        },
                        onSave: {
                            saved.toggle()
                            Task { await MediaActions.bookmark(post.id) }
                        },
                        onShare: {
                            shares += 1
                            MediaActions.shareSheet(post)
                            Task { await MediaActions.share(post.id) }
                        }
                    )
                    Divider()
                    Text("Replies").font(.headline)
                    if replies.isEmpty {
                        Text(status.isEmpty ? "No replies yet" : status)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(replies) { reply in
                            FeedPostCard(
                                post: reply,
                                onOpen: {},
                                onProfile: {
                                    if !reply.authorId.isEmpty { onProfileClick(reply.authorId) }
                                },
                                onReply: { draft = reply.authorHandle.isEmpty ? "" : "@\(reply.authorHandle) " },
                                onRepost: { Task { await MediaActions.repostReply(reply.id) } },
                                onLike: { Task { await MediaActions.likeReply(reply.id) } },
                                onSave: { Task { await MediaActions.bookmarkReply(reply.id) } },
                                onShare: {
                                    MediaActions.shareSheet(reply)
                                    Task { await MediaActions.share(reply.id) }
                                },
                                onMore: { Task { await MediaActions.report(reply.id, reason: "report") } }
                            )
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
            if let remote = post.displayPost.playbackURL ?? post.displayPost.mediaURL {
                videoURL = await AuthenticatedMedia.localFile(for: remote) ?? remote
            }
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
        let parsed = rows.compactMap { row -> FeedPost? in
            var nested = (row["reply"] as? [String: Any]) ?? (row["post"] as? [String: Any]) ?? row
            if LiveJSON.string(nested["id"]).isEmpty {
                nested["id"] = LiveJSON.string(nested["reply_id"])
            }
            if LiveJSON.string(nested["content"]).isEmpty {
                nested["content"] = LiveJSON.string(nested["reply_text"])
            }
            return LiveJSON.post(nested)
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
