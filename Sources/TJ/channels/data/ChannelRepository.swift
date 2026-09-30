import Foundation
import Combine

public protocol ChannelRepositoryProtocol: AnyObject {
    func getChannels() async throws -> [Channel]
    func getChannelById(id: String) async throws -> Channel?
    func subscribeToChannel(id: String) async throws -> Bool
    func unsubscribeFromChannel(id: String) async throws -> Bool
    func muteChannel(id: String, isMuted: Bool) async throws -> Bool
    func createChannel(name: String, description: String, category: String) async throws -> Channel
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
