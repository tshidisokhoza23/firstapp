import Foundation
import Combine
import SwiftUI

@MainActor
public final class ChannelDetailViewModel: ObservableObject {
    @Published public var channel: Resource<Channel> = .loading()
    @Published public var messages: [ChannelChatMessage] = []
    @Published public var contacts: [ChannelContact] = []
    @Published public var members: [ApiUser] = []
    @Published public var toast: String? = nil
    @Published public var didLoadMessages = false

    public var currentUserId: String? { SessionStore.shared.userId }

    public init(repository: ChannelRepository? = nil) {}

    public func loadChannel(channelId: String) {
        Task { await reload(channelId) }
    }

    public func loadMessages(conversationId: String) {
        Task { await reloadMessages(conversationId) }
    }

    private func reload(_ channelId: String) async {
        guard let data = try? await API.data("chats/channels/\(channelId)"),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let parsed = ChannelJSON.parse(json) else {
            if case .success = channel {} else { channel = .error("Channel not found") }
            await reloadMessages(channelId)
            return
        }
        channel = .success(parsed)
        let conv = parsed.conversationId?.isEmpty == false ? parsed.conversationId! : parsed.id
        await reloadMessages(conv)
        await loadMembers(conv)
        await loadAdminChoices()
    }

    private func reloadMessages(_ conversationId: String) async {
        let id = conversationId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { didLoadMessages = true; return }
        do {
            let data = try await API.data("conversations/\(id)/messages")
            let mine = SessionStore.shared.userId
            messages = LiveJSON.messages(from: data, myUserId: mine).map { msg in
                ChannelChatMessage(
                    id: msg.id,
                    senderName: ChannelJSON.visibleName(msg.senderName, id: msg.senderId),
                    message: msg.content,
                    timestamp: ChannelJSON.time(msg.timestamp),
                    mediaUrl: msg.mediaUrls.first
                )
            }
            didLoadMessages = true
        } catch {
            didLoadMessages = true
            toast = error.localizedDescription
        }
    }

    private func loadMembers(_ conversationId: String) async {
        guard let data = try? await API.data("conversations/\(conversationId)/members") else { return }
        let rows = LiveJSON.list(data, keys: ["users", "members", "items"])
        members = rows.compactMap { row in
            let id = LiveJSON.string(row["id"]).nilIfEmpty ?? LiveJSON.string(row["user_id"])
            guard !id.isEmpty else { return nil }
            let name = ChannelJSON.visibleName(
                LiveJSON.string(row["display_name"]).nilIfEmpty ?? LiveJSON.string(row["username"]),
                id: id
            )
            return ApiUser(id: id, username: LiveJSON.string(row["username"]), displayName: name, avatarUrl: LiveJSON.string(row["avatar_url"]).nilIfEmpty)
        }
    }

    private func loadAdminChoices() async {
        let phones = ContactSyncManager.shared.fetchAllContacts().map(\.phoneNumber).filter { !$0.isEmpty }
        guard !phones.isEmpty,
              let data = try? await API.data("users/sync-contacts", method: "POST", json: ["phone_numbers": phones]) else { return }
        contacts = LiveJSON.list(data, keys: ["users", "contacts", "items"]).compactMap { row in
            let id = LiveJSON.string(row["id"]).nilIfEmpty ?? LiveJSON.string(row["user_id"])
            let name = ChannelJSON.visibleName(
                LiveJSON.string(row["display_name"]).nilIfEmpty ?? LiveJSON.string(row["name"]).nilIfEmpty ?? LiveJSON.string(row["username"]),
                id: id
            )
            guard !id.isEmpty, !name.isEmpty else { return nil }
            return ChannelContact(id: id, displayName: name, isRegistered: true)
        }
    }

    public func subscribe(channelId: String) {
        Task {
            do {
                _ = try await API.data("chats/channels/\(channelId)/subscribe", method: "POST")
                toast = "Channel followed"
                await reload(channelId)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func mute(channelId: String, isMuted: Bool) {
        Task {
            do {
                _ = try await API.data("chats/channels/\(channelId)/mute", method: "POST", json: ["is_muted": isMuted])
                await reload(channelId)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func addAdmin(channelId: String, userId: String) {
        Task {
            do {
                _ = try await API.data("chats/channels/\(channelId)/admins", method: "POST", json: ["user_id": userId])
                toast = "Admin added"
                await reload(channelId)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func removeAdmin(channelId: String, userId: String) {
        Task {
            do {
                _ = try await API.data("chats/channels/\(channelId)/admins", method: "DELETE", json: ["user_id": userId])
                toast = "Admin removed"
                await reload(channelId)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func toggleAdminPosting(channelId: String, allowed: Bool) {
        Task {
            do {
                _ = try await API.data("chats/channels/\(channelId)/admin-posting", method: "POST", json: ["admins_can_post": allowed])
                await reload(channelId)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func clearToast() { toast = nil }

    public func sendMessage(
        channelId: String,
        content: String,
        mediaUrls: [String] = [],
        messageType: String = "text",
        fileName: String? = nil
    ) {
        Task {
            let text = content.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty || !mediaUrls.isEmpty else { return }
            let conv = conversationId(fallback: channelId)
            var body: [String: Any] = [
                "client_uuid": UUID().uuidString,
                "message_text": text.isEmpty ? (fileName ?? "Photo") : text,
                "media_type": mediaUrls.isEmpty ? "TEXT" : messageType.uppercased()
            ]
            if !mediaUrls.isEmpty { body["media_urls"] = mediaUrls }
            if let fileName { body["file_name"] = fileName }
            do {
                _ = try await API.data("conversations/\(conv)/messages", method: "POST", json: body)
                toast = "Update sent"
                await reloadMessages(conv)
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    public func sendMediaPost(channelId: String, content: String, mediaUrls: [String], messageType: String = "image", fileName: String? = nil) {
        sendMessage(channelId: channelId, content: content, mediaUrls: mediaUrls, messageType: messageType, fileName: fileName)
    }

    public func createPost(channelId: String, content: String, mediaUrls: [String]) {
        sendMessage(channelId: channelId, content: content, mediaUrls: mediaUrls, messageType: mediaUrls.isEmpty ? "text" : "image")
    }

    public func report(channelId: String, reason: String) {
        Task {
            let target = (channel.data?.createdBy?.isEmpty == false ? channel.data?.createdBy : nil) ?? channelId
            do {
                _ = try await API.data("users/\(target)/report", method: "POST", json: ["reason": reason])
                toast = "Report sent"
            } catch {
                toast = error.localizedDescription
            }
        }
    }

    private func conversationId(fallback: String) -> String {
        if let id = channel.data?.conversationId, !id.isEmpty { return id }
        return fallback
    }
}

enum ChannelJSON {
    static func parse(_ json: [String: Any]) -> Channel? {
        let id = LiveJSON.string(json["id"]).nilIfEmpty ?? LiveJSON.string(json["channel_id"])
        let name = LiveJSON.string(json["name"]).nilIfEmpty ?? LiveJSON.string(json["title"])
        guard !id.isEmpty, !name.isEmpty else { return nil }
        return Channel(
            id: id,
            name: name,
            description: LiveJSON.string(json["description"]),
            followers: LiveJSON.int(json["followers"] ?? json["subscriber_count"]),
            avatarUrl: LiveJSON.string(json["avatar_url"]).nilIfEmpty,
            isSubscribed: LiveJSON.bool(json["is_subscribed"]) || LiveJSON.bool(json["is_following"]),
            isMuted: LiveJSON.bool(json["is_muted"]),
            isAdmin: LiveJSON.bool(json["is_admin"]) || LiveJSON.bool(json["is_owner"]),
            adminsCanPost: LiveJSON.bool(json["admins_can_post"]),
            category: LiveJSON.string(json["category"]).nilIfEmpty ?? "Community",
            createdBy: LiveJSON.string(json["created_by"]).nilIfEmpty,
            lastPostText: LiveJSON.string(json["last_post_text"]),
            lastPostImageUrl: LiveJSON.string(json["last_post_image_url"]).nilIfEmpty,
            conversationId: LiveJSON.string(json["conversation_id"]).nilIfEmpty,
            isTopInCountry: LiveJSON.bool(json["is_top_in_country"]),
            countryCode: LiveJSON.string(json["country_code"])
        )
    }

    static func list(from data: Data) -> [Channel] {
        LiveJSON.list(data, keys: ["channels", "items", "data"]).compactMap(parse)
    }

    static func visibleName(_ raw: String?, id: String) -> String {
        let name = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let blocked = ["user", "someone", "direct", "chat", "channel", "mingle user", "mingle contact", "unknown caller"]
        if name.isEmpty || blocked.contains(name.lowercased()) || LiveJSON.isOpaqueId(name) || name == id { return "" }
        return name
    }

    static func time(_ millis: Int64) -> String {
        guard millis > 0 else { return "" }
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, HH:mm"
        return formatter.string(from: date)
    }
}
