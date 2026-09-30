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

    public init(baseURL: URL = URL(string: "https://api.tj.app/channels")!) {
        self.baseURL = baseURL
    }

    public func getChannels() async throws -> [Channel] {
        let (data, response) = try await URLSession.shared.data(from: baseURL)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        let list = try JSONDecoder().decode([Channel].self, from: data)
        self.channels = list
        return list
    }

    public func getChannelById(id: String) async throws -> Channel? {
        if let local = channels.first(where: { $0.id == id }) {
            return local
        }
        let url = baseURL.appendingPathComponent(id)
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        return try JSONDecoder().decode(Channel.self, from: data)
    }

    public func getChannel(channelId: String) async -> Channel? {
        return (try? await getChannelById(id: channelId)) ?? channels.first(where: { $0.id == channelId })
    }

    public func subscribeToChannel(id: String) async throws -> Bool {
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed = true
            channels[index].followers += 1
        }
        let url = baseURL.appendingPathComponent("\(id)/subscribe")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func subscribeToChannel(channelId: String) async -> Bool {
        return (try? await subscribeToChannel(id: channelId)) ?? false
    }

    public func unsubscribeFromChannel(id: String) async throws -> Bool {
        if let index = channels.firstIndex(where: { $0.id == id }) {
            channels[index].isSubscribed = false
            channels[index].followers = max(0, channels[index].followers - 1)
        }
        let url = baseURL.appendingPathComponent("\(id)/unsubscribe")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func muteChannel(id: String, isMuted: Bool) async throws -> Bool {
        let url = baseURL.appendingPathComponent("\(id)/mute")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["is_muted": isMuted])

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func muteChannel(channelId: String, isMuted: Bool) async -> Bool {
        return (try? await muteChannel(id: channelId, isMuted: isMuted)) ?? false
    }

    public func createChannel(name: String, description: String, category: String) async throws -> Channel {
        let url = baseURL.appendingPathComponent("create")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = ["name": name, "description": description, "category": category]
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let created = try JSONDecoder().decode(Channel.self, from: data)
        channels.append(created)
        return created
    }

    public func createChannel(name: String, description: String, avatarUrl: String?) async -> Channel {
        let newChannel = Channel(
            id: UUID().uuidString,
            name: name,
            description: description,
            followers: 1,
            avatarUrl: avatarUrl
        )
        channels.append(newChannel)
        return newChannel
    }

    public func addAdmin(channelId: String, userId: String) async -> Bool {
        return true
    }

    public func removeAdmin(channelId: String, userId: String) async -> Bool {
        return true
    }

    public func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Bool {
        return true
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
