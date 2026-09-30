import Foundation
import Combine

public struct ChannelPost: Identifiable, Codable, Equatable {
    public var id: String
    public var content: String
    public var imageUrl: String?
    public var timestamp: String
    public var isLiked: Bool

    public init(id: String = UUID().uuidString, content: String, imageUrl: String? = nil, timestamp: String = "Just now", isLiked: Bool = false) {
        self.id = id
        self.content = content
        self.imageUrl = imageUrl
        self.timestamp = timestamp
        self.isLiked = isLiked
    }
}

public struct ChannelChatMessage: Identifiable, Codable, Equatable {
    public var id: String
    public var senderName: String
    public var message: String
    public var timestamp: String

    public init(id: String = UUID().uuidString, senderName: String, message: String, timestamp: String = "Just now") {
        self.id = id
        self.senderName = senderName
        self.message = message
        self.timestamp = timestamp
    }
}

public struct ChannelContact: Identifiable, Codable, Equatable {
    public var id: String
    public var displayName: String
    public var isRegistered: Bool

    public init(id: String, displayName: String, isRegistered: Bool = true) {
        self.id = id
        self.displayName = displayName
        self.isRegistered = isRegistered
    }
}

public protocol ChannelRepositoryProtocol: AnyObject {
    func getChannels() async throws -> [Channel]
    func getChannelById(id: String) async throws -> Channel?
    func getChannel(channelId: String) async -> Channel?
    func subscribeToChannel(id: String) async throws -> Bool
    func subscribeToChannel(channelId: String) async -> Bool
    func unsubscribeFromChannel(id: String) async throws -> Bool
    func muteChannel(id: String, isMuted: Bool) async throws -> Bool
    func muteChannel(channelId: String, isMuted: Bool) async -> Bool
    func createChannel(name: String, description: String, category: String) async throws -> Channel
    func createChannel(name: String, description: String, avatarUrl: String?) async -> Channel
    func addAdmin(channelId: String, userId: String) async -> Bool
    func removeAdmin(channelId: String, userId: String) async -> Bool
    func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Bool
}

@MainActor
public final class ChannelRepository: ObservableObject, ChannelRepositoryProtocol {
    public static let shared = ChannelRepository()

    @Published public private(set) var channels: [Channel] = []
    private let baseURL: URL

    public init(baseURL: URL = URL(string: AppBackend.apiBaseURL + "/channels")!) {
        self.baseURL = baseURL
    }

    public func getChannels() async throws -> [Channel] {
        let data = try await API.data("channels")
        let list = Self.decodeChannels(data)
        self.channels = list
        return list
    }

    public func getChannelById(id: String) async throws -> Channel? {
        if let local = channels.first(where: { $0.id == id }) {
            return local
        }
        let data = try await API.data("chats/channels/\(id)")
        return Self.decodeChannels(data).first
    }

    public func getChannel(channelId: String) async -> Channel? {
        return (try? await getChannelById(id: channelId)) ?? channels.first(where: { $0.id == channelId })
    }

    public func subscribeToChannel(id: String) async throws -> Bool {
        _ = try await API.data("chats/channels/\(id)/subscribe", method: "POST")
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed.toggle()
        }
        return true
    }

    public func subscribeToChannel(channelId: String) async -> Bool {
        return (try? await subscribeToChannel(id: channelId)) ?? false
    }

    public func unsubscribeFromChannel(id: String) async throws -> Bool {
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed = false
            channels[index].followers = max(0, channels[index].followers - 1)
        }
        _ = try await API.data("chats/channels/\(id)/subscribe", method: "POST")
        return true
    }

    public func muteChannel(id: String, isMuted: Bool) async throws -> Bool {
        _ = try await API.data("chats/channels/\(id)/mute", method: "POST", json: ["is_muted": isMuted])
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isMuted = isMuted
        }
        return true
    }

    public func muteChannel(channelId: String, isMuted: Bool) async -> Bool {
        (try? await muteChannel(id: channelId, isMuted: isMuted)) ?? false
    }

    public func createChannel(name: String, description: String, category: String) async throws -> Channel {
        try await createOnServer(name: name, description: description, avatarUrl: nil)
    }

    public func createChannel(name: String, description: String, avatarUrl: String?) async -> Channel {
        if let created = try? await createOnServer(name: name, description: description, avatarUrl: avatarUrl) {
            return created
        }
        return Channel(id: UUID().uuidString, name: name, description: description, avatarUrl: avatarUrl)
    }

    public func addAdmin(channelId: String, userId: String) async -> Bool {
        (try? await API.data("chats/channels/\(channelId)/admins", method: "POST", json: ["user_id": userId])) != nil
    }

    public func removeAdmin(channelId: String, userId: String) async -> Bool {
        (try? await API.data("chats/channels/\(channelId)/admins", method: "DELETE", json: ["user_id": userId])) != nil
    }

    public func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Bool {
        (try? await API.data("chats/channels/\(channelId)/admin-posting", method: "POST", json: ["admins_can_post": allowed])) != nil
    }

    private func createOnServer(name: String, description: String, avatarUrl: String?) async throws -> Channel {
        var body: [String: Any] = ["name": name, "description": description]
        if let avatarUrl, !avatarUrl.isEmpty { body["avatar_url"] = avatarUrl }
        let data = try await API.data("chats/channels", method: "POST", json: body)
        guard let created = Self.decodeChannels(data).first else {
            throw URLError(.cannotParseResponse)
        }
        channels.append(created)
        return created
    }

    private static func decodeChannels(_ data: Data) -> [Channel] {
        guard let json = try? JSONSerialization.jsonObject(with: data) else { return [] }
        let raw: [Any]
        if let obj = json as? [String: Any], let list = obj["channels"] as? [Any] {
            raw = list
        } else if let list = json as? [Any] {
            raw = list
        } else if let obj = json as? [String: Any] {
            raw = [obj]
        } else {
            raw = []
        }
        return raw.compactMap { ($0 as? [String: Any]).flatMap(channel(from:)) }
    }

    private static func channel(from json: [String: Any]) -> Channel? {
        guard let id = json["id"] as? String ?? (json["id"] as? NSNumber)?.stringValue, !id.isEmpty else { return nil }
        let name = json["name"] as? String ?? "Channel"
        return Channel(
            id: id,
            name: name,
            description: json["description"] as? String ?? "",
            followers: (json["followers"] as? NSNumber)?.intValue ?? (json["subscriber_count"] as? NSNumber)?.intValue ?? 0,
            avatarUrl: json["avatar_url"] as? String,
            isVerified: (json["is_verified"] as? NSNumber)?.boolValue ?? false,
            isSubscribed: (json["is_subscribed"] as? NSNumber)?.boolValue ?? false,
            isMuted: (json["is_muted"] as? NSNumber)?.boolValue ?? false,
            isAdmin: (json["is_admin"] as? NSNumber)?.boolValue ?? false,
            adminsCanPost: (json["admins_can_post"] as? NSNumber)?.boolValue ?? false,
            category: json["category"] as? String ?? "Community",
            createdBy: json["created_by"] as? String,
            lastPostText: json["last_post_text"] as? String ?? "",
            lastPostImageUrl: json["last_post_image_url"] as? String,
            lastPostTime: (json["last_post_time"] as? NSNumber)?.stringValue ?? "",
            conversationId: json["conversation_id"] as? String,
            isTopInCountry: (json["is_top_in_country"] as? NSNumber)?.boolValue ?? false,
            countryCode: json["country_code"] as? String ?? ""
        )
    }
}

public struct ChannelUser: Identifiable, Codable {
    public var id: String
    public var displayName: String
    public var isRegistered: Bool

    public init(id: String, displayName: String, isRegistered: Bool = true) {
        self.id = id
        self.displayName = displayName
        self.isRegistered = isRegistered
    }
}
