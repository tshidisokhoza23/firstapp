import Foundation
import Combine
import SwiftUI

public protocol ChannelRepository {
    func getChannels() -> AnyPublisher<Resource<[Channel]>, Never>
    func observeChannels() -> AnyPublisher<Resource<[Channel]>, Never>
    func syncChannels() async -> Bool
    func subscribeToChannel(channelId: String) async -> Resource<Bool>
    func muteChannel(channelId: String, isMuted: Bool) async -> Resource<Bool>
    func createChannel(name: String, description: String, avatarUrl: String?) async -> Resource<Channel>
    func getChannel(channelId: String) async -> Resource<Channel>
    func updateChannelAvatar(channelId: String, avatarUrl: String) async -> Resource<Bool>
    func searchChannels(query: String) async -> [Channel]
    func addAdmin(channelId: String, userId: String) async -> Resource<Bool>
    func removeAdmin(channelId: String, userId: String) async -> Resource<Bool>
    func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Resource<Bool>
}

public final class ChannelRepositoryImpl: ChannelRepository, ObservableObject {
    public static let shared = ChannelRepositoryImpl()

    @Published public private(set) var channelsList: [Channel] = []
    private var topInCountry: [Channel] = []
    private let channelsSubject = CurrentValueSubject<Resource<[Channel]>, Never>(.loading())

    public init() {}

    public func getChannels() -> AnyPublisher<Resource<[Channel]>, Never> {
        return observeChannels()
    }

    public func observeChannels() -> AnyPublisher<Resource<[Channel]>, Never> {
        return channelsSubject.eraseToAnyPublisher()
    }

    public func syncChannels() async -> Bool {
        // Simulated network sync
        try? await Task.sleep(nanoseconds: 300_000_000)
        return true
    }

    public func subscribeToChannel(channelId: String) async -> Resource<Bool> {
        if let idx = channelsList.firstIndex(where: { $0.id == channelId || $0.conversationId == channelId }) {
            channelsList[idx].isSubscribed.toggle()
            channelsSubject.send(.success(channelsList))
            return .success(channelsList[idx].isSubscribed)
        }
        return .error("Channel not found")
    }

    public func muteChannel(channelId: String, isMuted: Bool) async -> Resource<Bool> {
        if let idx = channelsList.firstIndex(where: { $0.id == channelId }) {
            channelsList[idx].isMuted = isMuted
            channelsSubject.send(.success(channelsList))
            return .success(true)
        }
        return .error("Channel not found")
    }

    public func createChannel(name: String, description: String, avatarUrl: String?) async -> Resource<Channel> {
        let channel = Channel(
            id: UUID().uuidString,
            name: name,
            description: description,
            followers: "1",
            avatarUrl: avatarUrl,
            isSubscribed: true,
            isAdmin: true,
            adminsCanPost: true,
            conversationId: UUID().uuidString
        )
        channelsList.append(channel)
        channelsSubject.send(.success(channelsList))
        return .success(channel)
    }

    public func getChannel(channelId: String) async -> Resource<Channel> {
        if let ch = channelsList.first(where: { $0.id == channelId || $0.conversationId == channelId }) {
            return .success(ch)
        }
        let fallback = Channel(id: channelId, name: "Channel", description: "", followers: "0")
        return .success(fallback)
    }

    public func updateChannelAvatar(channelId: String, avatarUrl: String) async -> Resource<Bool> {
        if let idx = channelsList.firstIndex(where: { $0.id == channelId }) {
            channelsList[idx].avatarUrl = avatarUrl
            channelsSubject.send(.success(channelsList))
            return .success(true)
        }
        return .error("Channel not found")
    }

    public func searchChannels(query: String) async -> [Channel] {
        return channelsList.filter { $0.name.localizedCaseInsensitiveContains(query) }
    }

    public func addAdmin(channelId: String, userId: String) async -> Resource<Bool> {
        return .success(true)
    }

    public func removeAdmin(channelId: String, userId: String) async -> Resource<Bool> {
        return .success(true)
    }

    public func updateAdminPostingPermission(channelId: String, allowed: Bool) async -> Resource<Bool> {
        if let idx = channelsList.firstIndex(where: { $0.id == channelId }) {
            channelsList[idx].adminsCanPost = allowed
            channelsSubject.send(.success(channelsList))
            return .success(true)
        }
        return .error("Channel not found")
    }
}

// MARK: - Supporting Models
public struct ChannelPost: Identifiable, Codable {
    public var id: String
    public var authorId: String
    public var authorName: String
    public var authorHandle: String
    public var authorAvatarUrl: String?
    public var content: String
    public var mediaUrls: [String]
    public var timestamp: String
    public var likes: Int
    public var repliesCount: Int

    public init(
        id: String,
        authorId: String,
        authorName: String,
        authorHandle: String = "@channel",
        authorAvatarUrl: String? = nil,
        content: String,
        mediaUrls: [String] = [],
        timestamp: String = "Just now",
        likes: Int = 0,
        repliesCount: Int = 0
    ) {
        self.id = id
        self.authorId = authorId
        self.authorName = authorName
        self.authorHandle = authorHandle
        self.authorAvatarUrl = authorAvatarUrl
        self.content = content
        self.mediaUrls = mediaUrls
        self.timestamp = timestamp
        self.likes = likes
        self.repliesCount = repliesCount
    }
}

public struct ChannelChatMessage: Identifiable, Codable {
    public var id: String
    public var conversationId: String
    public var content: String
    public var senderId: String
    public var timestamp: Int64
    public var isFromMe: Bool
    public var type: String
    public var mediaUrls: [String]

    public init(
        id: String = UUID().uuidString,
        conversationId: String,
        content: String,
        senderId: String,
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        isFromMe: Bool = false,
        type: String = "text",
        mediaUrls: [String] = []
    ) {
        self.id = id
        self.conversationId = conversationId
        self.content = content
        self.senderId = senderId
        self.timestamp = timestamp
        self.isFromMe = isFromMe
        self.type = type
        self.mediaUrls = mediaUrls
    }
}

public struct ChannelContact: Identifiable, Codable {
    public var id: String
    public var displayName: String
    public var isRegistered: Bool

    public init(id: String, displayName: String, isRegistered: Bool = true) {
        self.id = id
        self.displayName = displayName
        self.isRegistered = isRegistered
    }
}

public struct ApiUser: Identifiable, Codable {
    public var id: String
    public var display_name: String?

    public init(id: String, display_name: String? = nil) {
        self.id = id
        self.display_name = display_name
    }
}
