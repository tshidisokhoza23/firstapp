//
//  StoryRepository.swift
//  TJ
//
//  Swift 5+ Repository for story creation, view tracking, muting,
//  viewer list fetching, and story 24-hour expiration filtering.
//

import Foundation
import Combine

// MARK: - Story Viewer DTO

public struct ApiViewer: Identifiable, Codable, Equatable {
    public let id: String
    public let userId: String
    public let userName: String
    public let avatarUrl: String?
    public let viewedAt: Int64
    public var viewedLabel: String = ""

    public init(id: String = UUID().uuidString, userId: String, userName: String, avatarUrl: String? = nil, viewedAt: Int64 = 0, viewedLabel: String = "") {
        self.id = id
        self.userId = userId
        self.userName = userName
        self.avatarUrl = avatarUrl
        self.viewedAt = viewedAt
        self.viewedLabel = viewedLabel
    }
}

// MARK: - Story Repository Protocol & Implementation

public protocol StoryRepositoryProtocol: AnyObject {
    func observeStories() async -> AsyncStream<[UserStory]>
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
        baseURL: URL = URL(string: AppBackend.apiBaseURL + "/chats/stories")!,
        currentUserId: String = "me"
    ) {
        self.baseURL = baseURL
        self.currentUserId = currentUserId
        let cutoff = Int64(Date().timeIntervalSince1970 * 1000) - Self.dayInMillis
        let saved = AppDiskCache.loadCodable([UserStory].self, name: "stories") ?? []
        userStories = saved.compactMap { story in
            var copy = story
            copy.stories = story.stories.filter { $0.createdAt >= cutoff }
            return copy.stories.isEmpty ? nil : copy
        }
    }

    // MARK: - Observe Stories

    public func observeStories() async -> AsyncStream<[UserStory]> {
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
            let data = try await API.data("chats/stories")
            mergeAndPrune(UserStory.decodeFeed(data))
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
        var body: [String: Any] = [
            "content": content.trimmingCharacters(in: .whitespacesAndNewlines),
            "story_type": type.lowercased(),
            "privacy": privacy
        ]
        if let contentUrl, !contentUrl.isEmpty { body["content_url"] = contentUrl }
        let data = try await API.data("chats/stories", method: "POST", json: body)
        let apiItem = UserStory.decodeFeed(data).first?.stories.first
        cacheOwnPostedStory(apiItem: apiItem, content: content, contentUrl: contentUrl, type: type)
        _ = await syncStories()
        return true
    }

    // MARK: - Mark Story as Viewed

    public func markStoryAsViewed(userId: String, storyId: String) async throws -> Bool {
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

        _ = try await API.data("chats/stories/\(storyId)/view", method: "POST")
        return true
    }

    // MARK: - Delete Story

    public func deleteStory(storyId: String) async throws -> Bool {
        do {
            _ = try await API.data("chats/stories/\(storyId)", method: "DELETE")
        } catch {
            return false
        }
        for index in userStories.indices {
            userStories[index].stories.removeAll { $0.id == storyId }
        }
        userStories.removeAll { $0.stories.isEmpty }
        return true
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
        let data = try await API.data("chats/stories/\(storyId)/viewers")
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let list = json["viewers"] as? [Any] else { return [] }
        return list.compactMap { row -> ApiViewer? in
            guard let row = row as? [String: Any] else { return nil }
            let id = (row["id"] as? String) ?? ""
            guard !id.isEmpty else { return nil }
            let name = (row["display_name"] as? String) ?? (row["user_name"] as? String) ?? ""
            return ApiViewer(
                id: id,
                userId: id,
                userName: name,
                avatarUrl: row["avatar_url"] as? String,
                viewedLabel: (row["viewed_at"] as? String) ?? ""
            )
        }
    }

    // MARK: - Helper Methods

    private func mergeAndPrune(_ incoming: [UserStory]) {
        let cutoff = Int64(Date().timeIntervalSince1970 * 1000) - Self.dayInMillis

        var merged = incoming.map { userStory -> UserStory in
            var copy = userStory
            copy.stories = userStory.stories.filter { $0.createdAt >= cutoff }
            return copy
        }.filter { !$0.stories.isEmpty }

        merged.sort { ($0.stories.map { $0.createdAt }.max() ?? 0) > ($1.stories.map { $0.createdAt }.max() ?? 0) }
        self.userStories = merged
        AppDiskCache.saveCodable(merged, name: "stories")
    }

    private func cacheOwnPostedStory(
        apiItem: StoryItem?,
        content: String,
        contentUrl: String?,
        type: String
    ) {
        let now = apiItem?.createdAt ?? Int64(Date().timeIntervalSince1970 * 1000)
        let storyType = StoryType(rawValue: type.uppercased()) ?? .text

        let newItem = StoryItem(
            id: apiItem?.id ?? "local_\(now)",
            content: apiItem?.content ?? content,
            contentUrl: apiItem?.contentUrl ?? contentUrl,
            type: storyType,
            timestamp: "Just now",
            createdAt: now,
            backgroundColorArgb: 0xFF546E7A,
            isViewed: true,
            durationMs: apiItem?.durationMs ?? 5000,
            expiresAt: "",
            viewsCount: 0
        )

        if let index = userStories.firstIndex(where: { $0.userId == currentUserId }) {
            userStories[index].stories.append(newItem)
        } else {
            let newBundle = UserStory(
                id: currentUserId,
                userId: currentUserId,
                userName: "My Story",
                stories: [newItem],
                lastUpdated: "Just now"
            )
            userStories.insert(newBundle, at: 0)
        }
    }
}
