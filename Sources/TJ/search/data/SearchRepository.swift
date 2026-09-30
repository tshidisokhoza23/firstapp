//
//  SearchRepository.swift
//  TJ
//
//  Swift 5+ Repository for Unified Search across messages, contacts,
//  channels, jobs, music (songs and artists), posts, and global search.
//

import Foundation
import Combine

// MARK: - Search Enums & Models

public enum SearchType: String, Codable, CaseIterable {
    case global
    case chats
    case jobs
    case channels
    case music
}

public enum SearchFilter: String, Codable, CaseIterable {
    case all
    case media
    case links
    case docs
}

public struct SearchContact: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let username: String?
    public let avatarUrl: String?

    public init(id: String, name: String, username: String? = nil, avatarUrl: String? = nil) {
        self.id = id
        self.name = name
        self.username = username
        self.avatarUrl = avatarUrl
    }
}

public struct SearchMessage: Identifiable, Codable, Equatable {
    public let id: String
    public let conversationId: String
    public let senderName: String
    public let text: String
    public let timestamp: Int64

    public init(id: String, conversationId: String, senderName: String, text: String, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.conversationId = conversationId
        self.senderName = senderName
        self.text = text
        self.timestamp = timestamp
    }
}

public struct SearchChannel: Identifiable, Codable, Equatable {
    public let id: String
    public let title: String
    public let description: String?
    public let memberCount: Int
    public let avatarUrl: String?

    public init(id: String, title: String, description: String? = nil, memberCount: Int = 0, avatarUrl: String? = nil) {
        self.id = id
        self.title = title
        self.description = description
        self.memberCount = memberCount
        self.avatarUrl = avatarUrl
    }
}

public struct SearchJobItem: Identifiable, Codable, Equatable {
    public let id: String
    public let title: String
    public let companyName: String
    public let location: String
    public let salaryRange: String?
    public let jobType: String

    public init(id: String, title: String, companyName: String, location: String, salaryRange: String? = nil, jobType: String = "Full-time") {
        self.id = id
        self.title = title
        self.companyName = companyName
        self.location = location
        self.salaryRange = salaryRange
        self.jobType = jobType
    }
}

public struct SearchResult: Codable, Equatable {
    public var chats: [DirectChat]
    public var contacts: [SearchContact]
    public var messages: [SearchMessage]
    public var channels: [SearchChannel]
    public var jobs: [SearchJobItem]
    public var songs: [Song]
    public var artists: [SearchContact]

    public init(
        chats: [DirectChat] = [],
        contacts: [SearchContact] = [],
        messages: [SearchMessage] = [],
        channels: [SearchChannel] = [],
        jobs: [SearchJobItem] = [],
        songs: [Song] = [],
        artists: [SearchContact] = []
    ) {
        self.chats = chats
        self.contacts = contacts
        self.messages = messages
        self.channels = channels
        self.jobs = jobs
        self.songs = songs
        self.artists = artists
    }

    public var isEmpty: Bool {
        chats.isEmpty && contacts.isEmpty && messages.isEmpty && channels.isEmpty && jobs.isEmpty && songs.isEmpty && artists.isEmpty
    }
}

// MARK: - Search Repository Protocol & Implementation

public protocol SearchRepositoryProtocol: AnyObject {
    func search(query: String, type: SearchType, filter: SearchFilter) async throws -> SearchResult
}

@MainActor
public final class SearchRepository: ObservableObject, SearchRepositoryProtocol {
    public static let shared = SearchRepository()

    @Published public private(set) var lastSearchResult: SearchResult = SearchResult()
    @Published public private(set) var isSearching = false

    private let baseURL: URL

    public init(baseURL: URL = URL(string: "https://api.tj.app/search")!) {
        self.baseURL = baseURL
    }

    public func search(
        query: String,
        type: SearchType = .global,
        filter: SearchFilter = .all
    ) async throws -> SearchResult {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            let emptyRes = SearchResult()
            self.lastSearchResult = emptyRes
            return emptyRes
        }

        self.isSearching = true
        defer { self.isSearching = false }

        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "q", value: trimmed),
            URLQueryItem(name: "type", value: type.rawValue),
            URLQueryItem(name: "filter", value: filter.rawValue)
        ]

        guard let url = components?.url else {
            throw URLError(.badURL)
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else {
                let fallback = localFallbackSearch(query: trimmed, type: type)
                self.lastSearchResult = fallback
                return fallback
            }

            let result = try JSONDecoder().decode(SearchResult.self, from: data)
            self.lastSearchResult = result
            return result
        } catch {
            let fallback = localFallbackSearch(query: trimmed, type: type)
            self.lastSearchResult = fallback
            return fallback
        }
    }

    private func localFallbackSearch(query: String, type: SearchType) -> SearchResult {
        let q = query.lowercased()
        var result = SearchResult()

        if type == .global || type == .chats {
            result.contacts = [
                SearchContact(id: "1", name: "Alice Johnson", username: "alice"),
                SearchContact(id: "2", name: "Bob Smith", username: "bob")
            ].filter { $0.name.lowercased().contains(q) || ($0.username?.lowercased().contains(q) ?? false) }
        }

        if type == .global || type == .jobs {
            result.jobs = [
                SearchJobItem(id: "j1", title: "iOS Developer", companyName: "Tech Corp", location: "Johannesburg"),
                SearchJobItem(id: "j2", title: "SwiftUI Engineer", companyName: "Innovate AI", location: "Remote")
            ].filter { $0.title.lowercased().contains(q) || $0.companyName.lowercased().contains(q) }
        }

        if type == .global || type == .channels {
            result.channels = [
                SearchChannel(id: "c1", title: "Tech News", memberCount: 1500),
                SearchChannel(id: "c2", title: "Music Lovers", memberCount: 3200)
            ].filter { $0.title.lowercased().contains(q) }
        }

        return result
    }
}
