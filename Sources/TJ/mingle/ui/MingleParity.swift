import SwiftUI

struct MingleActivityItem: Identifiable {
    let id: String
    let actorId: String
    let actorName: String
    let avatarURL: String
    let photoURL: String
    let type: String
    let actionText: String
    let timeAgo: String
    let conversationId: String
    var locked: Bool = false

    var isMatch: Bool { type == "match" }
    var isRequest: Bool { type == "message_request" }
}

enum MingleCopy {
    static func realName(_ raw: String) -> String {
        let name = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let blocked = ["user", "someone", "mingle", "mingle user", "mingle contact", "unknown", "match"]
        return blocked.contains(name.lowercased()) ? "" : name
    }

    static func activityText(_ type: String) -> String {
        switch type {
        case "match": return "matched with you! Say hi"
        case "view": return "viewed your Mingle profile"
        case "photo_like": return "liked a photo on your profile"
        case "super_like": return "sent you a Super Like"
        case "message_request": return "You received a new message request on Mingle"
        default: return "liked your Mingle profile"
        }
    }

    static func relative(_ millis: Int64) -> String {
        guard millis > 0 else { return "just now" }
        let then = millis < 1_000_000_000_000 ? millis * 1000 : millis
        let minutes = max(0, Int((Date().timeIntervalSince1970 * 1000 - Double(then)) / 60000))
        if minutes < 1 { return "just now" }
        if minutes < 60 { return "\(minutes)m ago" }
        if minutes < 1440 { return "\(minutes / 60)h ago" }
        return "\(minutes / 1440)d ago"
    }

    static func emptyDeck(reason: String?, radiusKm: Int, denied: Bool) -> (title: String, body: String, action: String) {
        let resolved = reason == "NO_LOCATION" && !denied ? "RETRY_LOCATION" : reason
        switch resolved {
        case "NO_LOCATION":
            return ("Location is required", "Mingle can only show people near you after location is on.", "Enable Location")
        case "RETRY_LOCATION":
            return ("Location is not available", "Your location could not be read. Try again outside, or check that location is on.", "Try again")
        case "NO_CANDIDATE_LOCATION":
            return ("No shared locations yet", "Other people are on Mingle, but none of them have shared a location.", "Check again")
        case "OUT_OF_RADIUS":
            return ("No one within \(radiusKm) km", "People with photos are farther away. Increasing your radius will look farther.", "Increase Radius")
        case "NO_PHOTO":
            return ("No photos nearby", "People in range have not added a Mingle photo yet, so their cards stay hidden.", "Check again")
        case "DIFFERENT_COUNTRY":
            return ("No one from your country", "People in range are in a different country. Mingle only shows the same country.", "Look farther")
        case "CONTACTS_HIDDEN":
            return ("Nearby people are in your contacts", "Mingle hides people saved in your contacts, including matching phone numbers.", "")
        case "ALREADY_SWIPED":
            return ("You've already seen everyone nearby", "Passed and liked profiles stay hidden. Increase your radius to look farther.", "Increase Radius")
        case "PREFERENCES":
            return ("No one matches your filters", "People nearby are outside the age range or gender you are looking for.", "Edit filters")
        case "DAILY_LIMIT":
            return ("Daily profile limit reached", "The free daily browse is used. Spend coins or watch a completed rewarded ad for more profiles.", "")
        default:
            return ("No one else is on Mingle nearby", "When someone nearby opens Mingle and adds a photo, they will show up here.", "Check again")
        }
    }

    static func serverMessage(_ error: Error) -> String {
        guard case APIError.status(_, let body) = error,
              let data = body.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let message = json["error"] as? String, !message.isEmpty else {
            return error.localizedDescription
        }
        return message
    }

    static func hasLink(_ text: String) -> Bool {
        let pattern = "(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|net|org|io|app|me|co|za|ly|gg)\\b)"
        return text.range(of: pattern, options: .regularExpression) != nil
    }
}

struct MingleActivitySheet: View {
    var onOpenChat: (String, String) -> Void
    @State private var rows: [MingleActivityItem] = []
    @State private var note = "Loading..."
    @State private var preview: MingleActivityItem?
    @State private var showRequests = false

    var body: some View {
        NavigationStack {
            Group {
                if rows.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "heart")
                            .font(.system(size: 48))
                            .foregroundColor(.appPrimary.opacity(0.6))
                        Text("No Mingle Activity Yet").font(.headline)
                        Text("When people like or match with your Mingle profile, they will appear right here.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                } else {
                    List(rows) { item in
                        Button {
                            if item.locked { return }
                            if item.isRequest {
                                showRequests = true
                            } else {
                                preview = item
                            }
                        } label: {
                            HStack(alignment: .top, spacing: 12) {
                                avatar(item.avatarURL, fallback: item.actorName)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.isRequest ? item.actionText : [item.actorName, item.actionText].filter { !$0.isEmpty }.joined(separator: " "))
                                        .foregroundColor(.primary)
                                        .lineLimit(2)
                                    if item.locked {
                                        Button("Unlock this like") {
                                            Task { await unlock(item.actorId) }
                                        }
                                        .font(.caption.bold())
                                    }
                                    Text(item.timeAgo).font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Mingle Activity")
            .navigationBarTitleDisplayMode(.inline)
            .task { await load() }
            .sheet(isPresented: $showRequests) {
                MingleRequestsSheet(onOpenChat: onOpenChat)
            }
            .sheet(item: $preview) { item in
                MingleActivityPreview(item: item, onOpenChat: { id, name in
                    preview = nil
                    onOpenChat(id, name)
                })
            }
        }
    }

    private func load() async {
        do {
            let data = try await API.data("mingle/activity")
            let items = LiveJSON.list(data, keys: ["items", "activities"])
            rows = items.compactMap { item in
                let id = LiveJSON.string(item["id"])
                guard !id.isEmpty else { return nil }
                let type = LiveJSON.string(item["type"])
                let created = LiveJSON.string(item["created_at"])
                return MingleActivityItem(
                    id: id,
                    actorId: LiveJSON.string(item["actor_id"]),
                    actorName: MingleCopy.realName(LiveJSON.string(item["actor_name"])),
                    avatarURL: LiveJSON.string(item["actor_avatar_url"]),
                    photoURL: LiveJSON.string(item["photo_url"]),
                    type: type,
                    actionText: MingleCopy.activityText(type),
                    timeAgo: MingleCopy.relative(Int64(created) ?? 0),
                    conversationId: LiveJSON.string(item["conversation_id"]),
                    locked: item["locked"] as? Bool == true
                )
            }
            note = rows.isEmpty ? "No Mingle Activity Yet" : ""
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func unlock(_ actorId: String) async {
        guard !actorId.isEmpty else { return }
        do {
            _ = try await API.data("mingle/admirers/\(actorId)/unlock", method: "POST")
            await load()
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func avatar(_ url: String, fallback: String) -> some View {
        Group {
            if let resolved = FeedPost.resolve(url) {
                AsyncImage(url: resolved) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.2)
                }
            } else {
                Text(String(fallback.prefix(1)))
            }
        }
        .frame(width: 44, height: 44)
        .background(Color.appPrimary.opacity(0.15))
        .clipShape(Circle())
    }
}

struct MingleRequestsSheet: View {
    var onOpenChat: (String, String) -> Void
    @State private var rows: [[String: Any]] = []
    @State private var note = "Loading..."

    var body: some View {
        NavigationStack {
            Group {
                if rows.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "envelope")
                            .font(.system(size: 42))
                            .foregroundColor(.secondary)
                        Text("No message requests").font(.headline)
                        Text("When someone sends you a Mingle intro, it will wait here.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                } else {
                    List(rows.indices, id: \.self) { index in
                        let intro = rows[index]
                        VStack(alignment: .leading, spacing: 8) {
                            Text(MingleCopy.realName(LiveJSON.string(intro["sender_name"]))).font(.headline)
                            let details = [LiveJSON.string(intro["sender_age"]), LiveJSON.string(intro["sender_location"])].filter { !$0.isEmpty && $0 != "0" }
                            if !details.isEmpty {
                                Text(details.joined(separator: " · ")).font(.caption).foregroundColor(.secondary)
                            }
                            Text(LiveJSON.string(intro["message"])).font(.subheadline)
                            HStack {
                                Button("Accept") { Task { await respond(intro, "accept") } }
                                Button("Decline") { Task { await respond(intro, "decline") } }
                                    .foregroundColor(.red)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Message Requests")
            .navigationBarTitleDisplayMode(.inline)
            .task { await load() }
        }
    }

    private func load() async {
        do {
            let data = try await API.data("mingle/intros")
            rows = LiveJSON.list(data, keys: ["items", "intros"])
            note = rows.isEmpty ? "No message requests" : ""
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func respond(_ intro: [String: Any], _ action: String) async {
        let id = LiveJSON.string(intro["id"])
        guard !id.isEmpty else { return }
        do {
            let data = try await API.data("mingle/intro/\(id)/respond", method: "POST", json: ["action": action])
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            if action == "accept" {
                let conversationId = LiveJSON.string(json["conversation_id"])
                if !conversationId.isEmpty {
                    onOpenChat(conversationId, MingleCopy.realName(LiveJSON.string(intro["sender_name"])))
                }
            }
            await load()
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }
}

struct MingleActivityPreview: View {
    let item: MingleActivityItem
    var onOpenChat: (String, String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var matched: Bool
    @State private var note = ""

    init(item: MingleActivityItem, onOpenChat: @escaping (String, String) -> Void) {
        self.item = item
        self.onOpenChat = onOpenChat
        _matched = State(initialValue: item.isMatch)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(matched ? "Matched! You both like each other!" : item.actionText)
                .font(.headline)
            Text(item.actorName).font(.title2.bold())
            Text(item.timeAgo).foregroundColor(.secondary)
            if matched || !item.conversationId.isEmpty {
                Button("Send Message") { Task { await openChat() } }
                    .buttonStyle(.borderedProminent)
            } else if !item.actorId.isEmpty {
                Button("Like Back") { Task { await likeBack() } }
                    .buttonStyle(.borderedProminent)
                Button("Pass") { dismiss() }
            }
            if !note.isEmpty {
                Text(note).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding()
    }

    private func likeBack() async {
        if let me = try? await API.data("mingle/me"),
           let profile = LiveJSON.object(me)["profile"] as? [String: Any],
           let photos = profile["photos"] as? [Any], photos.isEmpty {
            note = "You must upload or complete your Mingle profile before you can like people."
            return
        }
        do {
            let data = try await API.data("mingle/swipe", method: "POST", json: [
                "target_user_id": item.actorId,
                "action": "like"
            ])
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            if json["matched"] as? Bool == true {
                matched = true
                note = "It's a Match with \(item.actorName)"
                let conversationId = LiveJSON.string(json["conversation_id"])
                if !conversationId.isEmpty {
                    onOpenChat(conversationId, item.actorName)
                }
            }
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func openChat() async {
        if !item.conversationId.isEmpty {
            onOpenChat(item.conversationId, item.actorName)
            return
        }
        do {
            let data = try await API.data("mingle/matches/\(item.actorId)/chat", method: "POST")
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            let conversationId = LiveJSON.string(json["conversation_id"])
            guard !conversationId.isEmpty else { return }
            onOpenChat(conversationId, item.actorName)
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }
}

struct MinglePointsGate: View {
    let points: Int
    var onWallet: () -> Void
    var onWatchAd: () -> Void = {}
    var onDismiss: () -> Void
    @ObservedObject private var ads = RewardedAds.shared

    var body: some View {
        VStack(spacing: 16) {
            Text("Wallet Points Required")
                .font(.title3.bold())
            Text("You need more points for this. Watch an ad or open your wallet. Browsing Mingle stays free.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Text("Your Balance")
                .font(.caption)
                .foregroundColor(.secondary)
            Text("\(points) Points")
                .font(.title2.bold())
                .foregroundColor(.appPrimary)
            Text("4 Ads / Window")
                .font(.caption.bold())
            Text("\(ads.batchCount) / 4 watched")
                .font(.caption)
            Button(ads.buttonTitle, action: onWatchAd)
                .buttonStyle(.borderedProminent)
                .disabled(!ads.canWatch)
            if let message = ads.message, !message.isEmpty {
                Text(message).font(.caption).foregroundColor(.secondary)
            }
            Button("Open wallet", action: onWallet)
            Button("Not now", action: onDismiss)
        }
        .padding(24)
    }
}
