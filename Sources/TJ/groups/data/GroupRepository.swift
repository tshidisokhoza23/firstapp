//
//  GroupRepository.swift
//  TJ
//
//  Swift 5+ Repository for Group creation, member management,
//  admin permissions, group messaging, and invite codes.
//

import Foundation
import Combine

// MARK: - Models

public struct GroupMember: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let avatarUrl: String?
    public let isAdmin: Bool
    public let joinedAt: Int64

    public init(
        id: String,
        name: String,
        avatarUrl: String? = nil,
        isAdmin: Bool = false,
        joinedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.name = name
        self.avatarUrl = avatarUrl
        self.isAdmin = isAdmin
        self.joinedAt = joinedAt
    }
}

public struct GroupInfoResponse: Identifiable, Codable, Equatable {
    public var id: String { conversation_id ?? group_id ?? UUID().uuidString }
    public let conversation_id: String?
    public let group_id: String?
    public let title: String?
    public let name: String?
    public let avatar_url: String?
    public let created_by: String?
    public let members: [GroupMember]?
    public let invite_code: String?

    public var groupName: String {
        title?.isEmpty == false ? title! : (name?.isEmpty == false ? name! : "Unnamed Group")
    }
}

public struct GroupChat: Identifiable, Codable, Equatable {
    public let id: String
    public var name: String
    public var avatarUrl: String?
    public var createdBy: String?
    public var lastMessage: String?
    public var timestamp: Int64
    public var unreadCount: Int

    public init(
        id: String,
        name: String,
        avatarUrl: String? = nil,
        createdBy: String? = nil,
        lastMessage: String? = nil,
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        unreadCount: Int = 0
    ) {
        self.id = id
        self.name = name
        self.avatarUrl = avatarUrl
        self.createdBy = createdBy
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
    }
}

// MARK: - Group Repository Protocol & Implementation

public protocol GroupRepositoryProtocol: AnyObject {
    func observeGroups() async -> AsyncStream<[GroupChat]>
    func syncGroups() async -> Bool
    func createGroup(name: String, contactIds: [String]) async throws -> GroupChat
    func getGroupInfo(groupId: String) async throws -> GroupInfoResponse
    func addGroupMember(groupId: String, userId: String) async throws -> Bool
    func removeGroupMember(groupId: String, userId: String) async throws -> Bool
    func promoteToAdmin(groupId: String, userId: String) async throws -> Bool
    func demoteAdmin(groupId: String, userId: String) async throws -> Bool
    func updateGroupAvatar(groupId: String, avatarUrl: String) async throws -> Bool
    func renameGroup(groupId: String, name: String) async throws -> Bool
    func leaveGroup(groupId: String) async throws -> Bool
    func generateGroupInviteCode(groupId: String) async throws -> String
    func joinGroupByInvite(inviteCode: String) async throws -> String
}

@MainActor
public final class GroupRepository: ObservableObject, GroupRepositoryProtocol {
    public static let shared = GroupRepository()

    @Published public private(set) var groups: [GroupChat] = []
    private let baseURL: URL

    public init(baseURL: URL = URL(string: AppBackend.apiBaseURL + "/chats/groups")!) {
        self.baseURL = baseURL
    }

    // MARK: - Observe Groups

    public func observeGroups() async -> AsyncStream<[GroupChat]> {
        AsyncStream { continuation in
            let cancellable = $groups.sink { list in
                continuation.yield(list)
            }
            continuation.onTermination = { _ in
                cancellable.cancel()
            }
        }
    }

    // MARK: - Sync Groups

    @discardableResult
    public func syncGroups() async -> Bool {
        do {
            let url = baseURL.appendingPathComponent("list")
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return false }

            struct GroupsListResponse: Codable {
                let groups: [GroupChat]
            }

            let decoded = try JSONDecoder().decode(GroupsListResponse.self, from: data)
            self.groups = decoded.groups
            return true
        } catch {
            print("[GroupRepository] Sync error: \(error)")
            return false
        }
    }

    // MARK: - Create Group

    public func createGroup(name: String, contactIds: [String]) async throws -> GroupChat {
        let url = baseURL.appendingPathComponent("create")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = ["name": name, "members": contactIds]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct CreateGroupResponse: Codable {
            let conversation_id: String
        }

        let convId = try JSONDecoder().decode(CreateGroupResponse.self, from: data).conversation_id
        let newGroup = GroupChat(id: convId, name: name)
        groups.append(newGroup)
        return newGroup
    }

    // MARK: - Get Group Info

    public func getGroupInfo(groupId: String) async throws -> GroupInfoResponse {
        let url = baseURL.appendingPathComponent("\(groupId)/info")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let info = try JSONDecoder().decode(GroupInfoResponse.self, from: data)
        if let index = groups.firstIndex(where: { $0.id == groupId }) {
            groups[index].name = info.groupName
            if let avatar = info.avatar_url { groups[index].avatarUrl = avatar }
        }
        return info
    }

    // MARK: - Member & Admin Management

    public func addGroupMember(groupId: String, userId: String) async throws -> Bool {
        return try await performMemberAction(groupId: groupId, userId: userId, endpoint: "members/add")
    }

    public func removeGroupMember(groupId: String, userId: String) async throws -> Bool {
        return try await performMemberAction(groupId: groupId, userId: userId, endpoint: "members/remove")
    }

    public func promoteToAdmin(groupId: String, userId: String) async throws -> Bool {
        return try await performMemberAction(groupId: groupId, userId: userId, endpoint: "admin/promote")
    }

    public func demoteAdmin(groupId: String, userId: String) async throws -> Bool {
        return try await performMemberAction(groupId: groupId, userId: userId, endpoint: "admin/demote")
    }

    private func performMemberAction(groupId: String, userId: String, endpoint: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("\(groupId)/\(endpoint)")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["user_id": userId])

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    // MARK: - Update Avatar & Rename

    public func updateGroupAvatar(groupId: String, avatarUrl: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("\(groupId)/avatar")
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["avatar_url": avatarUrl])

        let (_, response) = try await URLSession.shared.data(for: request)
        let ok = (response as? HTTPURLResponse)?.statusCode == 200
        if ok {
            if let index = groups.firstIndex(where: { $0.id == groupId }) {
                groups[index].avatarUrl = avatarUrl
            }
        }
        return ok
    }

    public func renameGroup(groupId: String, name: String) async throws -> Bool {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { throw URLError(.cannotParseResponse) }

        let url = baseURL.appendingPathComponent("\(groupId)/rename")
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["name": trimmed])

        let (_, response) = try await URLSession.shared.data(for: request)
        let ok = (response as? HTTPURLResponse)?.statusCode == 200
        if ok {
            if let index = groups.firstIndex(where: { $0.id == groupId }) {
                groups[index].name = trimmed
            }
        }
        return ok
    }

    // MARK: - Leave Group

    public func leaveGroup(groupId: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("\(groupId)/leave")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (_, response) = try await URLSession.shared.data(for: request)
        let ok = (response as? HTTPURLResponse)?.statusCode == 200
        if ok {
            groups.removeAll { $0.id == groupId }
        }
        return ok
    }

    // MARK: - Invite Links

    public func generateGroupInviteCode(groupId: String) async throws -> String {
        let url = baseURL.appendingPathComponent("\(groupId)/invite-code")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct InviteResponse: Codable {
            let invite_code: String
        }
        return try JSONDecoder().decode(InviteResponse.self, from: data).invite_code
    }

    public func joinGroupByInvite(inviteCode: String) async throws -> String {
        let url = baseURL.appendingPathComponent("join-invite")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["invite_code": inviteCode])

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct JoinResponse: Codable {
            let conversation_id: String?
            let group_id: String?
        }

        let res = try JSONDecoder().decode(JoinResponse.self, from: data)
        guard let joinedId = res.conversation_id ?? res.group_id else {
            throw URLError(.cannotParseResponse)
        }
        await syncGroups()
        return joinedId
    }
}
