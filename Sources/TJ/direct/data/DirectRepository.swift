//
//  DirectRepository.swift
//  TJ
//
//  Swift 5+ Repository for Direct 1-on-1 messaging and call logs.
//

import Foundation
import Combine

// MARK: - Models

public struct DirectChat: Identifiable, Codable, Equatable {
    public let id: String
    public let otherUserId: String
    public var senderName: String
    public var lastMessage: String
    public var timestamp: Int64
    public var unreadCount: Int
    public var avatarUrl: String?
    public var isOnline: Bool
    public var isMuted: Bool
    public var isPinned: Bool
    public var isTyping: Bool
    public var phoneNumber: String?

    public init(
        id: String,
        otherUserId: String,
        senderName: String,
        lastMessage: String = "",
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        unreadCount: Int = 0,
        avatarUrl: String? = nil,
        isOnline: Bool = false,
        isMuted: Bool = false,
        isPinned: Bool = false,
        isTyping: Bool = false,
        phoneNumber: String? = nil
    ) {
        self.id = id
        self.otherUserId = otherUserId
        self.senderName = senderName
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.avatarUrl = avatarUrl
        self.isOnline = isOnline
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.isTyping = isTyping
        self.phoneNumber = phoneNumber
    }
}

// MARK: - Direct Repository Protocol & Implementation

public protocol DirectRepositoryProtocol: AnyObject {
    func observeDirectChats() async -> AsyncStream<[DirectChat]>
    func syncDirectChats() async -> Bool
    func observeCallLogs() async -> AsyncStream<[CallLog]>
    func initiateDM(contactId: String) async throws -> String
    func pinChat(chatId: String, isPinned: Bool) async throws -> Bool
    func muteChat(chatId: String, isMuted: Bool) async throws -> Bool
    func deleteChat(chatId: String) async throws -> Bool
}

@MainActor
public final class DirectRepository: ObservableObject, DirectRepositoryProtocol {
    public static let shared = DirectRepository()

    @Published public private(set) var directChats: [DirectChat] = []
    @Published public private(set) var callLogs: [CallLog] = []

    private let baseURL: URL

    public init(baseURL: URL = URL(string: AppBackend.apiBaseURL + "/chats")!) {
        self.baseURL = baseURL
    }

    // MARK: - Observe Direct Chats

    public func observeDirectChats() async -> AsyncStream<[DirectChat]> {
        AsyncStream { continuation in
            let cancellable = $directChats.sink { chats in
                continuation.yield(chats)
            }
            continuation.onTermination = { _ in
                cancellable.cancel()
            }
        }
    }

    // MARK: - Sync Direct Chats

    @discardableResult
    public func syncDirectChats() async -> Bool {
        do {
            let data = try await API.data("conversations")
            self.directChats = LiveJSON.chats(from: data).filter { $0.type == .direct && !$0.isSecretMingleChat }.map { chat in
                DirectChat(
                    id: chat.id,
                    otherUserId: chat.otherUserId ?? "",
                    senderName: chat.senderName,
                    lastMessage: chat.lastMessage,
                    timestamp: chat.timestamp,
                    unreadCount: chat.unreadCount,
                    avatarUrl: chat.avatarUrl,
                    isOnline: chat.isOnline,
                    isMuted: chat.isMuted,
                    isPinned: chat.isPinned,
                    phoneNumber: chat.phoneNumber
                )
            }
            return true
        } catch {
            print("[DirectRepository] Sync error: \(error)")
            return false
        }
    }

    // MARK: - Observe Call Logs

    public func observeCallLogs() async -> AsyncStream<[CallLog]> {
        AsyncStream { continuation in
            let cancellable = $callLogs.sink { logs in
                continuation.yield(logs)
            }
            continuation.onTermination = { _ in
                cancellable.cancel()
            }
        }
    }

    // MARK: - Initiate DM

    public func initiateDM(contactId: String) async throws -> String {
        let data = try await API.data("chats/dm", method: "POST", json: ["contact_id": contactId])
        let json = LiveJSON.object(data)
        let conversationId = LiveJSON.string(json["conversation_id"])
        guard !conversationId.isEmpty else { throw URLError(.cannotParseResponse) }
        let contactName = LiveJSON.string(json["contact_name"])
        let avatar = LiveJSON.string(json["avatar_url"]).nilIfEmpty

        let newChat = DirectChat(
            id: conversationId,
            otherUserId: contactId,
            senderName: contactName,
            avatarUrl: avatar
        )

        if !directChats.contains(where: { $0.id == conversationId }) {
            directChats.insert(newChat, at: 0)
        }

        return conversationId
    }

    // MARK: - Pin Chat

    public func pinChat(chatId: String, isPinned: Bool) async throws -> Bool {
        if let index = directChats.firstIndex(where: { $0.id == chatId }) {
            directChats[index].isPinned = isPinned
        }
        return true
    }

    // MARK: - Mute Chat

    public func muteChat(chatId: String, isMuted: Bool) async throws -> Bool {
        if let index = directChats.firstIndex(where: { $0.id == chatId }) {
            directChats[index].isMuted = isMuted
        }
        return true
    }

    // MARK: - Delete Chat

    public func deleteChat(chatId: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent(chatId)
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (_, response) = try await URLSession.shared.data(for: request)
        let success = (response as? HTTPURLResponse)?.statusCode == 200

        if success {
            directChats.removeAll { $0.id == chatId }
        }
        return success
    }
}
