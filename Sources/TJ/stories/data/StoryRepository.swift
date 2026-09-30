//
//  StoryRepository.swift
//  TJ
//
//  Swift 5+ Repository for story creation, view tracking, muting,
//  viewer list fetching, and story 24-hour expiration filtering.
//

import Foundation
import Combine

// MARK: - Story Models

public enum StoryType: String, Codable, CaseIterable {
    case text
    case image
    case video
}

public struct StoryItem: Identifiable, Codable, Equatable {
    public let id: String
    public let content: String
    public let contentUrl: String?
    public let type: StoryType
    public let timestamp: String
    public let createdAt: Int64
    public var isViewed: Bool
    public let durationMs: Int

    public init(
        id: String,
        content: String,
        contentUrl: String? = nil,
        type: StoryType = .text,
        timestamp: String = "Just now",
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        isViewed: Bool = false,
        durationMs: Int = 5000
    ) {
        self.id = id
        self.content = content
        self.contentUrl = contentUrl
        self.type = type
        self.timestamp = timestamp
        self.createdAt = createdAt
        self.isViewed = isViewed
        self.durationMs = durationMs
    }
}

public struct UserStory: Identifiable, Codable, Equatable {
    public let id: String
    public let userId: String
    public var userName: String
    public let phoneNumber: String?
    public let userAvatarUrl: String?
    public var stories: [StoryItem]
    public var lastUpdated: String
    public var lastUpdatedMillis: Int64
    public var isMuted: Bool

    public init(
        id: String,
        userId: String,
        userName: String,
        phoneNumber: String? = nil,
        userAvatarUrl: String? = nil,
        stories: [StoryItem],
        lastUpdated: String = "",
        lastUpdatedMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        isMuted: Bool = false
    ) {
        self.id = id
        self.userId = userId
        self.userName = userName
        self.phoneNumber = phoneNumber
        self.userAvatarUrl = userAvatarUrl
        self.stories = stories
        self.lastUpdated = lastUpdated
        self.lastUpdatedMillis = lastUpdatedMillis
        self.isMuted = isMuted
    }
}

public struct ApiViewer: Identifiable, Codable, Equatable {
    public let id: String
    public let userId: String
    public let userName: String
    public let avatarUrl: String?
    public let viewedAt: Int64

    public init(id: String = UUID().uuidString, userId: String, userName: String, avatarUrl: String? = nil, viewedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.userId = userId
        self.userName = userName
        self.avatarUrl = avatarUrl
        self.viewedAt = viewedAt
    }
}

// MARK: - Story Repository Protocol & Implementation

public protocol StoryRepositoryProtocol: AnyObject {
    func observeStories() -> AsyncStream<[UserStory]>
    func syncStories() async -> Bool
    func createStory(content: String, contentUrl: String?, type: String, privacy: String) async throws -> Bool
    func markStoryAsViewed(userId: String, storyId: String) async throws -> Bool
    func deleteStory(storyId: String) async throws -> Bool
    func muteStory(userId: String, isMuted: Bool) async throws -> Bool
    func getStoryViewers(storyId: String) async throws -> [ApiViewer]
}

@MainActor
public final class StoryRepository: ObservableObject, StoryRepositoryProtocol {
    public static let shared = StoryRepository()

    private static let dayInMillis: Int64 = 24 * 60 * 60 * 1000

    @Published public private(set) var userStories: [UserStory] = []
    private var storiesSubject = CurrentValueSubject<[UserStory], Never>([])
    private let baseURL: URL
    private let currentUserId: String

    public init(
        baseURL: URL = URL(string: "https://api.tj.app/stories")!,
        currentUserId: String = "me"
    ) {
        self.baseURL = baseURL
        self.currentUserId = currentUserId
    }

    // MARK: - Observe Stories

    public func observeStories() -> AsyncStream<[UserStory]> {
        AsyncStream { continuation in
            let cancellable = $userStories.sink { stories in
                continuation.yield(stories)
            }
            continuation.onTermination = { _ in
                cancellable.cancel()
            }
        }
    }

    // MARK: - Sync Stories

    @discardableResult
    public func syncStories() async -> Bool {
        do {
            let url = baseURL.appendingPathComponent("feed")
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return false }

            struct StoryFeedResponse: Codable {
                let stories: [UserStory]
            }

            let decoded = try JSONDecoder().decode(StoryFeedResponse.self, from: data)
            mergeAndPrune(decoded.stories)
            return true
        } catch {
            print("[StoryRepository] Sync error: \(error)")
            return false
        }
    }

    // MARK: - Create Story

    public func createStory(
        content: String,
        contentUrl: String? = nil,
        type: String = "text",
        privacy: String = "contacts"
    ) async throws -> Bool {
        let url = baseURL.appendingPathComponent("create")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any?] = [
            "content": content.trimmingCharacters(in: .whitespacesAndNewlines),
            "content_url": contentUrl,
            "story_type": type.lowercased(),
            "privacy": privacy
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body.compactMapValues { $0 })

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct CreateStoryResponse: Codable {
            let story: StoryItem?
        }

        let apiItem = (try? JSONDecoder().decode(CreateStoryResponse.self, from: data))?.story
        cacheOwnPostedStory(apiItem: apiItem, content: content, contentUrl: contentUrl, type: type)
        return true
    }

    // MARK: - Mark Story as Viewed

    public func markStoryAsViewed(userId: String, storyId: String) async throws -> Bool {
        // Local update
        for uIndex in userStories.indices {
            if userStories[uIndex].userId == userId || userStories[uIndex].id == userId {
                for sIndex in userStories[uIndex].stories.indices {
                    if userStories[uIndex].stories[sIndex].id == storyId {
                        userStories[uIndex].stories[sIndex].isViewed = true
                    }
                }
            }
        }

        guard !storyId.hasPrefix("local_") && !storyId.hasPrefix("ad_") else { return true }

        let url = baseURL.appendingPathComponent("\(storyId)/view")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    // MARK: - Delete Story

    public func deleteStory(storyId: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent(storyId)
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (_, response) = try await URLSession.shared.data(for: request)
        let success = (response as? HTTPURLResponse)?.statusCode == 200

        if success {
            for index in userStories.indices {
                userStories[index].stories.removeAll { $0.id == storyId }
            }
            userStories.removeAll { $0.stories.isEmpty }
        }
        return success
    }

    // MARK: - Mute Story

    public func muteStory(userId: String, isMuted: Bool) async throws -> Bool {
        for index in userStories.indices {
            if userStories[index].userId == userId || userStories[index].id == userId {
                userStories[index].isMuted = isMuted
            }
        }

        let url = baseURL.appendingPathComponent("users/\(userId)/mute")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["is_muted": isMuted])

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    // MARK: - Get Story Viewers

    public func getStoryViewers(storyId: String) async throws -> [ApiViewer] {
        let url = baseURL.appendingPathComponent("\(storyId)/viewers")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        struct ViewersResponse: Codable {
            let viewers: [ApiViewer]
        }
        return try JSONDecoder().decode(ViewersResponse.self, from: data).viewers
    }

    // MARK: - Helper Methods

    private func mergeAndPrune(_ incoming: [UserStory]) {
        let cutoff = Int64(Date().timeIntervalSince1970 * 1000) - Self.dayInMillis

        var merged = incoming.map { userStory -> UserStory in
            var copy = userStory
            copy.stories = userStory.stories.filter { $0.createdAt >= cutoff }
            return copy
        }.filter { !$0.stories.isEmpty }

        merged.sort { ($0.stories.map(\.createdAt).max() ?? 0) > ($1.stories.map(\.createdAt).max() ?? 0) }
        self.userStories = merged
    }

    private func cacheOwnPostedStory(
        apiItem: StoryItem?,
        content: String,
        contentUrl: String?,
        type: String
    ) {
        let now = apiItem?.createdAt ?? Int64(Date().timeIntervalSince1970 * 1000)
        let storyType = StoryType(rawValue: type.lowercased()) ?? .text

        let newItem = StoryItem(
            id: apiItem?.id ?? "local_\(now)",
            content: apiItem?.content ?? content,
            contentUrl: apiItem?.contentUrl ?? contentUrl,
            type: storyType,
            timestamp: "Just now",
            createdAt: now,
            isViewed: true,
            durationMs: apiItem?.durationMs ?? 5000
        )

        if let index = userStories.firstIndex(where: { $0.userId == currentUserId }) {
            userStories[index].stories.append(newItem)
            userStories[index].lastUpdatedMillis = now
        } else {
            let newBundle = UserStory(
                id: currentUserId,
                userId: currentUserId,
                userName: "My Story",
                stories: [newItem],
                lastUpdated: "Just now",
                lastUpdatedMillis: now
            )
            userStories.insert(newBundle, at: 0)
        }
    }
}
