import SwiftUI

struct ShortCommentsSheet: View {
    let post: FeedPost
    var onCount: (Int) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss
    @State private var comments: [FeedPost] = []
    @State private var status = "Loading comments..."
    @State private var text = ""
    @State private var sending = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if comments.isEmpty {
                    Text(status)
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .padding(24)
                } else {
                    List(comments) { reply in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(visible(reply.authorName, id: reply.authorId))
                                .font(.system(size: 14, weight: .semibold))
                            Text(reply.content)
                                .font(.system(size: 15))
                            Button {
                                Task { await like(reply) }
                            } label: {
                                Label("\(reply.likes)", systemImage: reply.isLiked ? "heart.fill" : "heart")
                                    .font(.system(size: 13))
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.vertical, 4)
                    }
                    .listStyle(.plain)
                }
                HStack(spacing: 8) {
                    TextField("Add a comment", text: $text)
                        .textFieldStyle(.roundedBorder)
                    Button(sending ? "Sending" : "Send") {
                        Task { await send() }
                    }
                    .disabled(sending || text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .padding(12)
            }
            .navigationTitle("Comments")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .task(id: post.id) { await load() }
    }

    private func load() async {
        do {
            let data = try await API.data("media/posts/\(post.id)/replies")
            let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            let items = object?["items"] as? [[String: Any]] ?? []
            comments = items.compactMap { item in
                let kind = LiveJSON.string(item["type"]).lowercased()
                if kind == "external_ad_placeholder" || kind == "admob" { return nil }
                let reply = item["reply"] as? [String: Any] ?? item
                guard let parsed = LiveJSON.post(reply), !parsed.id.isEmpty else { return nil }
                return parsed
            }
            status = comments.isEmpty ? "No comments yet. Be the first!" : ""
            onCount(comments.count)
        } catch {
            comments = []
            status = error.localizedDescription
        }
    }

    private func send() async {
        let body = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        sending = true
        defer { sending = false }
        do {
            _ = try await API.data("media/posts/\(post.id)/replies", method: "POST", json: ["reply_text": body])
            text = ""
            await load()
        } catch {
            status = error.localizedDescription
        }
    }

    private func like(_ reply: FeedPost) async {
        do {
            _ = try await API.data("post-replies/\(reply.id)/like", method: "POST")
            await load()
        } catch {
            status = error.localizedDescription
        }
    }

    private func visible(_ raw: String, id: String) -> String {
        let name = ChannelJSON.visibleName(raw, id: id)
        if !name.isEmpty { return name }
        let handle = ChannelJSON.visibleName(nil, id: id)
        return handle.isEmpty ? "" : handle
    }
}
