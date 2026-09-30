import Foundation

// MARK: - Common & Response Models

public struct AddMemberRequest: Codable, Sendable {
    public let userId: String

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
    }

    public init(userId: String) {
        self.userId = userId
    }
}

public struct RenameGroupRequest: Codable, Sendable {
    public let name: String

    public init(name: String) {
        self.name = name
    }
}

public struct EditMessageRequest: Codable, Sendable {
    public let messageText: String

    enum CodingKeys: String, CodingKey {
        case messageText = "message_text"
    }

    public init(messageText: String) {
        self.messageText = messageText
    }
}

public struct ReactMessageRequest: Codable, Sendable {
    public let emoji: String

    public init(emoji: String) {
        self.emoji = emoji
    }
}

public struct ReactionsResponse: Codable, Sendable {
    public let reactions: [String: Int]

    public init(reactions: [String: Int] = [:]) {
        self.reactions = reactions
    }
}

public struct ConversationPrefsRequest: Codable, Sendable {
    public let isMuted: Bool?
    public let isPinned: Bool?
    public let isArchived: Bool?

    enum CodingKeys: String, CodingKey {
        case isMuted = "is_muted"
        case isPinned = "is_pinned"
        case isArchived = "is_archived"
    }

    public init(isMuted: Bool? = nil, isPinned: Bool? = nil, isArchived: Bool? = nil) {
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.isArchived = isArchived
    }
}

public struct ReportMessageRequest: Codable, Sendable {
    public let reason: String

    public init(reason: String) {
        self.reason = reason
    }
}

public struct PinMessageRequest: Codable, Sendable {
    public let messageId: String?

    enum CodingKeys: String, CodingKey {
        case messageId = "message_id"
    }

    public init(messageId: String?) {
        self.messageId = messageId
    }
}

public struct StarMessageRequest: Codable, Sendable {
    public let starred: Bool

    public init(starred: Bool) {
        self.starred = starred
    }
}

public struct ReadReceiptRequest: Codable, Sendable {
    public let lastReadMessageId: String

    enum CodingKeys: String, CodingKey {
        case lastReadMessageId = "last_read_message_id"
    }

    public init(lastReadMessageId: String) {
        self.lastReadMessageId = lastReadMessageId
    }
}

public struct StatusUpdateRequest: Codable, Sendable {
    public let status: String

    public init(status: String) {
        self.status = status
    }
}

public struct IceServerConfig: Codable, Equatable, Sendable {
    public let urls: [String]
    public let url: String?
    public let username: String?
    public let credential: String?

    public init(urls: [String] = [], url: String? = nil, username: String? = nil, credential: String? = nil) {
        self.urls = urls
        self.url = url
        self.username = username
        self.credential = credential
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.url = try container.decodeIfPresent(String.self, forKey: .url)
        self.username = try container.decodeIfPresent(String.self, forKey: .username)
        self.credential = try container.decodeIfPresent(String.self, forKey: .credential)

        if let list = try? container.decode([String].self, forKey: .urls) {
            self.urls = list
        } else if let single = try? container.decode(String.self, forKey: .urls) {
            self.urls = [single]
        } else {
            self.urls = []
        }
    }

    public func resolvedUrls() -> [String] {
        if !urls.isEmpty {
            return urls
        }
        if let url = url, !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return [url]
        }
        return []
    }
}

public struct IceServersResponse: Codable, Sendable {
    public let iceServers: [IceServerConfig]

    enum CodingKeys: String, CodingKey {
        case iceServers = "ice_servers"
    }

    public init(iceServers: [IceServerConfig]) {
        self.iceServers = iceServers
    }
}

public struct ChatSummary: Codable, Sendable {
    public let conversationId: String
    public let title: String
    public let type: String
    public let lastMessage: String
    public let time: Int64
    public let unreadCount: Int
    public let pinnedMessageId: String?
    public let isMuted: Bool
    public let isPinned: Bool
    public let isSubscribed: Bool
    public let isFollowing: Bool
    public let isAdmin: Bool
    public let adminsCanPost: Bool
    public let contactId: String?
    public let channelId: String?
    public let phoneNumber: String?
    public let avatarUrl: String?
    public let createdBy: String?
    public let isOnline: Bool
    public let lastSeen: Int64

    enum CodingKeys: String, CodingKey {
        case conversationId = "conversation_id"
        case title
        case type
        case lastMessage = "last_message"
        case time
        case unreadCount = "unread_count"
        case pinnedMessageId = "pinned_message_id"
        case isMuted = "is_muted"
        case isPinned = "is_pinned"
        case isSubscribed = "is_subscribed"
        case isFollowing = "is_following"
        case isAdmin = "is_admin"
        case adminsCanPost = "admins_can_post"
        case contactId = "contact_id"
        case channelId = "channel_id"
        case phoneNumber = "phone_number"
        case avatarUrl = "avatar_url"
        case createdBy = "created_by"
        case isOnline = "is_online"
        case lastSeen = "last_seen"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.conversationId = try container.decode(String.self, forKey: .conversationId)
        self.title = (try? container.decode(String.self, forKey: .title)) ?? ""
        self.type = (try? container.decode(String.self, forKey: .type)) ?? "direct"
        self.lastMessage = (try? container.decode(String.self, forKey: .lastMessage)) ?? ""
        self.time = (try? container.decode(Int64.self, forKey: .time)) ?? 0
        self.unreadCount = (try? container.decode(Int.self, forKey: .unreadCount)) ?? 0
        self.pinnedMessageId = try? container.decodeIfPresent(String.self, forKey: .pinnedMessageId)
        self.isMuted = (try? container.decode(Bool.self, forKey: .isMuted)) ?? false
        self.isPinned = (try? container.decode(Bool.self, forKey: .isPinned)) ?? false
        self.isSubscribed = (try? container.decode(Bool.self, forKey: .isSubscribed)) ?? false
        self.isFollowing = (try? container.decode(Bool.self, forKey: .isFollowing)) ?? false
        self.isAdmin = (try? container.decode(Bool.self, forKey: .isAdmin)) ?? false
        self.adminsCanPost = (try? container.decode(Bool.self, forKey: .adminsCanPost)) ?? false
        self.contactId = try? container.decodeIfPresent(String.self, forKey: .contactId)
        self.channelId = try? container.decodeIfPresent(String.self, forKey: .channelId)
        self.phoneNumber = try? container.decodeIfPresent(String.self, forKey: .phoneNumber)
        self.avatarUrl = try? container.decodeIfPresent(String.self, forKey: .avatarUrl)
        self.createdBy = try? container.decodeIfPresent(String.self, forKey: .createdBy)
        self.isOnline = (try? container.decode(Bool.self, forKey: .isOnline)) ?? false
        self.lastSeen = (try? container.decode(Int64.self, forKey: .lastSeen)) ?? 0
    }
}

public struct ChatSummaryResponse: Codable, Sendable {
    public let chats: [ChatSummary]

    public init(chats: [ChatSummary]) {
        self.chats = chats
    }
}

public struct ApiApplicantProfile: Codable, Sendable {
    public let fullName: String
    public let bio: String?
    public let cvUrl: String?

    enum CodingKeys: String, CodingKey {
        case fullName = "full_name"
        case bio
        case cvUrl = "cv_url"
    }
}

public struct ApiJobDetails: Codable, Sendable {
    public let title: String
}

public struct ApiMessage: Codable, Sendable {
    public let id: String
    public let clientUuid: String
    public let conversationId: String
    public let senderId: String
    public let senderName: String
    public let receiverId: String?
    public let messageText: String
    public let mediaType: String
    public let mediaUrl: String?
    public let mediaUrls: [String]?
    public let duration: String?
    public let fileName: String?
    public let fileSize: String?
    public let contactName: String?
    public let contactPhone: String?
    public let latitude: Double?
    public let longitude: Double?
    public let isStarred: Bool?
    public let isDelivered: Bool?
    public let isRead: Bool?
    public let status: String?
    public let createdAt: Int64
    public let direction: String
    public let reactions: [String: Int]?
    public let mentionUserIds: [String]?
    public let replyToId: String?
    public let replyToText: String?
    public let replyToSenderName: String?
    public let applicantProfile: ApiApplicantProfile?
    public let jobDetails: ApiJobDetails?
    public let linkPreview: LinkPreview?
    public let audioAmplitudes: [Int]?
    public let scheduledAt: Int64?

    enum CodingKeys: String, CodingKey {
        case id
        case clientUuid = "client_uuid"
        case conversationId = "conversation_id"
        case senderId = "sender_id"
        case senderName = "sender_name"
        case receiverId = "receiver_id"
        case messageText = "message_text"
        case mediaType = "media_type"
        case mediaUrl = "media_url"
        case mediaUrls = "media_urls"
        case duration
        case fileName = "file_name"
        case fileSize = "file_size"
        case contactName = "contact_name"
        case contactPhone = "contact_phone"
        case latitude
        case longitude
        case isStarred = "is_starred"
        case isDelivered = "is_delivered"
        case isRead = "is_read"
        case status
        case createdAt = "created_at"
        case direction
        case reactions
        case mentionUserIds = "mention_user_ids"
        case replyToId = "reply_to_id"
        case replyToText = "reply_to_text"
        case replyToSenderName = "reply_to_sender_name"
        case applicantProfile = "applicant_profile"
        case jobDetails = "job_details"
        case linkPreview = "link_preview"
        case audioAmplitudes = "audio_amplitudes"
        case scheduledAt = "scheduled_at"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = (try? container.decode(String.self, forKey: .id)) ?? ""
        self.clientUuid = (try? container.decode(String.self, forKey: .clientUuid)) ?? ""
        self.conversationId = (try? container.decode(String.self, forKey: .conversationId)) ?? ""
        self.senderId = (try? container.decode(String.self, forKey: .senderId)) ?? ""
        self.senderName = (try? container.decode(String.self, forKey: .senderName)) ?? ""
        self.receiverId = try? container.decodeIfPresent(String.self, forKey: .receiverId)
        self.messageText = (try? container.decode(String.self, forKey: .messageText)) ?? ""
        self.mediaType = (try? container.decode(String.self, forKey: .mediaType)) ?? "TEXT"
        self.mediaUrl = try? container.decodeIfPresent(String.self, forKey: .mediaUrl)
        self.mediaUrls = try? container.decodeIfPresent([String].self, forKey: .mediaUrls)
        self.duration = try? container.decodeIfPresent(String.self, forKey: .duration)
        self.fileName = try? container.decodeIfPresent(String.self, forKey: .fileName)
        self.fileSize = try? container.decodeIfPresent(String.self, forKey: .fileSize)
        self.contactName = try? container.decodeIfPresent(String.self, forKey: .contactName)
        self.contactPhone = try? container.decodeIfPresent(String.self, forKey: .contactPhone)
        self.latitude = try? container.decodeIfPresent(Double.self, forKey: .latitude)
        self.longitude = try? container.decodeIfPresent(Double.self, forKey: .longitude)
        self.isStarred = try? container.decodeIfPresent(Bool.self, forKey: .isStarred)
        self.isDelivered = try? container.decodeIfPresent(Bool.self, forKey: .isDelivered)
        self.isRead = try? container.decodeIfPresent(Bool.self, forKey: .isRead)
        self.status = try? container.decodeIfPresent(String.self, forKey: .status)
        self.createdAt = (try? container.decode(Int64.self, forKey: .createdAt)) ?? 0
        self.direction = (try? container.decode(String.self, forKey: .direction)) ?? "incoming"
        self.reactions = try? container.decodeIfPresent([String: Int].self, forKey: .reactions)
        self.mentionUserIds = try? container.decodeIfPresent([String].self, forKey: .mentionUserIds)
        self.replyToId = try? container.decodeIfPresent(String.self, forKey: .replyToId)
        self.replyToText = try? container.decodeIfPresent(String.self, forKey: .replyToText)
        self.replyToSenderName = try? container.decodeIfPresent(String.self, forKey: .replyToSenderName)
        self.applicantProfile = try? container.decodeIfPresent(ApiApplicantProfile.self, forKey: .applicantProfile)
        self.jobDetails = try? container.decodeIfPresent(ApiJobDetails.self, forKey: .jobDetails)
        self.linkPreview = try? container.decodeIfPresent(LinkPreview.self, forKey: .linkPreview)
        self.audioAmplitudes = try? container.decodeIfPresent([Int].self, forKey: .audioAmplitudes)
        self.scheduledAt = try? container.decodeIfPresent(Int64.self, forKey: .scheduledAt)
    }
}

public struct MessagesResponse: Codable, Sendable {
    public let messages: [ApiMessage]
    public let page: Int

    public init(messages: [ApiMessage], page: Int = 1) {
        self.messages = messages
        self.page = page
    }
}

public struct MessageResponse: Codable, Sendable {
    public let message: ApiMessage

    public init(message: ApiMessage) {
        self.message = message
    }
}

public struct SyncDeltaResponse: Codable, Sendable {
    public let latestSequence: Int64
    public let messages: [ApiMessage]

    enum CodingKeys: String, CodingKey {
        case latestSequence = "latest_sequence"
        case messages
    }

    public init(latestSequence: Int64 = 0, messages: [ApiMessage] = []) {
        self.latestSequence = latestSequence
        self.messages = messages
    }
}

public struct InitiateDMRequest: Codable, Sendable {
    public let contactId: String

    enum CodingKeys: String, CodingKey {
        case contactId = "contact_id"
    }

    public init(contactId: String) {
        self.contactId = contactId
    }
}

public struct CreateGroupRequest: Codable, Sendable {
    public let title: String
    public let memberIds: [String]

    enum CodingKeys: String, CodingKey {
        case title
        case memberIds = "member_ids"
    }

    public init(title: String, memberIds: [String]) {
        self.title = title
        self.memberIds = memberIds
    }
}

public struct ConversationResponse: Codable, Sendable {
    public let conversationId: String

    enum CodingKeys: String, CodingKey {
        case conversationId = "conversation_id"
    }

    public init(conversationId: String) {
        self.conversationId = conversationId
    }
}

public struct InitiateCallRequest: Codable, Sendable {
    public let receiverId: String
    public let callType: String

    enum CodingKeys: String, CodingKey {
        case receiverId = "receiver_id"
        case callType = "call_type"
    }

    public init(receiverId: String, callType: String) {
        self.receiverId = receiverId
        self.callType = callType
    }
}

public struct InitiateCallResponse: Codable, Sendable {
    public let sessionId: String

    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
    }

    public init(sessionId: String) {
        self.sessionId = sessionId
    }
}

public struct SendMessageRequest: Codable, Sendable {
    public let clientUuid: String
    public let messageText: String
    public let mediaType: String
    public let mediaUrls: [String]
    public let duration: String?
    public let fileName: String?
    public let fileSize: String?
    public let contactName: String?
    public let contactPhone: String?
    public let latitude: Double?
    public let longitude: Double?
    public let mentionUserIds: [String]?
    public let receiverId: String?
    public let replyToId: String?
    public let replyToText: String?
    public let replyToSenderName: String?
    public let scheduledAt: Int64?
    public let linkPreview: LinkPreview?
    public let audioAmplitudes: [Int]?

    enum CodingKeys: String, CodingKey {
        case clientUuid = "client_uuid"
        case messageText = "message_text"
        case mediaType = "media_type"
        case mediaUrls = "media_urls"
        case duration
        case fileName = "file_name"
        case fileSize = "file_size"
        case contactName = "contact_name"
        case contactPhone = "contact_phone"
        case latitude
        case longitude
        case mentionUserIds = "mention_user_ids"
        case receiverId = "receiver_id"
        case replyToId = "reply_to_id"
        case replyToText = "reply_to_text"
        case replyToSenderName = "reply_to_sender_name"
        case scheduledAt = "scheduled_at"
        case linkPreview = "link_preview"
        case audioAmplitudes = "audio_amplitudes"
    }

    public init(
        clientUuid: String,
        messageText: String,
        mediaType: String = "TEXT",
        mediaUrls: [String] = [],
        duration: String? = nil,
        fileName: String? = nil,
        fileSize: String? = nil,
        contactName: String? = nil,
        contactPhone: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        mentionUserIds: [String]? = nil,
        receiverId: String? = nil,
        replyToId: String? = nil,
        replyToText: String? = nil,
        replyToSenderName: String? = nil,
        scheduledAt: Int64? = nil,
        linkPreview: LinkPreview? = nil,
        audioAmplitudes: [Int]? = nil
    ) {
        self.clientUuid = clientUuid
        self.messageText = messageText
        self.mediaType = mediaType
        self.mediaUrls = mediaUrls
        self.duration = duration
        self.fileName = fileName
        self.fileSize = fileSize
        self.contactName = contactName
        self.contactPhone = contactPhone
        self.latitude = latitude
        self.longitude = longitude
        self.mentionUserIds = mentionUserIds
        self.receiverId = receiverId
        self.replyToId = replyToId
        self.replyToText = replyToText
        self.replyToSenderName = replyToSenderName
        self.scheduledAt = scheduledAt
        self.linkPreview = linkPreview
        self.audioAmplitudes = audioAmplitudes
    }
}

public struct GroupJoinResponse: Codable, Sendable {
    public let status: String?
    public let groupId: String?
    public let conversationId: String?

    enum CodingKeys: String, CodingKey {
        case status
        case groupId = "group_id"
        case conversationId = "conversation_id"
    }
}

public struct GroupInviteCodeResponse: Codable, Sendable {
    public let inviteCode: String

    enum CodingKeys: String, CodingKey {
        case inviteCode = "invite_code"
    }
}

public struct ApiReader: Codable, Sendable {
    public let userId: String
    public let readAt: Int64

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case readAt = "read_at"
    }
}

public struct CallLogsResponse: Codable, Sendable {
    public let calls: [ApiCallLog]

    public init(calls: [ApiCallLog] = []) {
        self.calls = calls
    }
}

public struct ApiCallLog: Codable, Sendable {
    public let id: String?
    public let sessionId: String?
    public let callerId: String?
    public let calleeId: String?
    public let contactId: String?
    public let contactName: String?
    public let contactAvatar: String?
    public let callType: String?
    public let status: String?
    public let durationSeconds: Int
    public let startedAt: Int64
    public let endedAt: Int64?

    enum CodingKeys: String, CodingKey {
        case id
        case sessionId = "session_id"
        case callerId = "caller_id"
        case calleeId = "callee_id"
        case contactId = "contact_id"
        case contactName = "contact_name"
        case contactAvatar = "contact_avatar"
        case callType = "call_type"
        case status
        case durationSeconds = "duration_seconds"
        case startedAt = "started_at"
        case endedAt = "ended_at"
    }
}

// MARK: - ChatApi Protocol & URLSession Implementation

public protocol ChatApi: Sendable {
    func getChats() async throws -> ChatSummaryResponse
    func syncDelta(since: Int64) async throws -> SyncDeltaResponse
    func getChatDetails(id: String) async throws -> ChatSummary
    func getMessages(conversationID: String, cursor: String?, limit: Int?) async throws -> MessagesResponse
    func sendMessage(conversationID: String, request: SendMessageRequest) async throws -> MessageResponse
    func markAsRead(conversationID: String, request: ReadReceiptRequest) async throws -> StatusResponse
    func getMembers(conversationID: String) async throws -> [ApiUser]
    func updateApplicationStatus(conversationID: String, request: StatusUpdateRequest) async throws -> StatusResponse
    func addGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse
    func removeGroupMember(groupId: String, userId: String) async throws -> StatusResponse
    func promoteToAdmin(groupId: String, userId: String) async throws -> StatusResponse
    func leaveGroup(groupId: String) async throws -> StatusResponse
    func initiateDM(request: InitiateDMRequest) async throws -> ConversationResponse
    func createGroup(request: CreateGroupRequest) async throws -> ConversationResponse
    func getGroupInfo(groupId: String) async throws -> GroupInfoResponse
    func renameGroup(groupId: String, request: RenameGroupRequest) async throws -> StatusResponse
    func addChatGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse
    func removeChatGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse
    func promoteChatGroupAdmin(groupId: String, request: AddMemberRequest) async throws -> StatusResponse
    func demoteChatGroupAdmin(groupId: String, request: AddMemberRequest) async throws -> StatusResponse
    func updateGroupAvatar(groupId: String, request: AvatarUrlRequest) async throws -> StatusResponse
    func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse
    func leaveChatGroup(groupId: String) async throws -> StatusResponse
    func generateGroupInviteCode(groupId: String) async throws -> GroupInviteCodeResponse
    func joinGroupByInvite(inviteCode: String) async throws -> GroupJoinResponse
    func deleteConversation(conversationID: String) async throws -> StatusResponse
    func clearConversation(conversationID: String) async throws -> StatusResponse
    func deleteMessage(conversationID: String, messageID: String) async throws -> StatusResponse
    func editMessage(conversationID: String, messageID: String, request: EditMessageRequest) async throws -> MessageResponse
    func getMessageReaders(messageID: String) async throws -> [ApiReader]
    func reactToMessage(conversationID: String, messageID: String, request: ReactMessageRequest) async throws -> ReactionsResponse
    func initiateCall(request: InitiateCallRequest) async throws -> InitiateCallResponse
    func getCallLogs() async throws -> CallLogsResponse
    func subscribeToChannel(channelId: String) async throws -> StatusResponse
    func getICEServers() async throws -> IceServersResponse
    func updateConversationPrefs(conversationID: String, request: ConversationPrefsRequest) async throws -> StatusResponse
    func reportMessage(messageID: String, request: ReportMessageRequest) async throws -> StatusResponse
    func pinMessage(conversationID: String, request: PinMessageRequest) async throws -> StatusResponse
    func starMessage(messageID: String, request: StarMessageRequest) async throws -> StatusResponse
    func unstarMessage(messageID: String) async throws -> StatusResponse
    func getStarredMessages() async throws -> MessagesResponse
}

public final class URLSessionChatApi: ChatApi, @unchecked Sendable {
    private let session: URLSession
    private let baseURLProvider: @Sendable () -> String
    private let tokenProvider: @Sendable () -> String?

    public init(
        session: URLSession = .shared,
        baseURLProvider: @escaping @Sendable () -> String,
        tokenProvider: @escaping @Sendable () -> String?
    ) {
        self.session = session
        self.baseURLProvider = baseURLProvider
        self.tokenProvider = tokenProvider
    }

    private func buildURLRequest(
        path: String,
        method: String = "GET",
        queryItems: [URLQueryItem]? = nil,
        body: Data? = nil
    ) throws -> URLRequest {
        var base = baseURLProvider().trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if !base.hasPrefix("http://") && !base.hasPrefix("https://") {
            base = "https://" + base
        }
        guard var components = URLComponents(string: "\(base)/\(path)") else {
            throw URLError(.badURL)
        }
        if let queryItems = queryItems, !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else {
            throw URLError(.badURL)
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = tokenProvider(), !token.isEmpty {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        req.httpBody = body
        return req
    }

    private func perform<T: Decodable>(_ request: URLRequest) async throws -> T {
        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            throw URLError(.init(rawValue: statusCode))
        }
        let decoder = JSONDecoder()
        return try decoder.decode(T.self, from: data)
    }

    private func encode<T: Encodable>(_ value: T) throws -> Data {
        let encoder = JSONEncoder()
        return try encoder.encode(value)
    }

    public func getChats() async throws -> ChatSummaryResponse {
        let req = try buildURLRequest(path: "conversations")
        return try await perform(req)
    }

    public func syncDelta(since: Int64) async throws -> SyncDeltaResponse {
        let req = try buildURLRequest(path: "sync/delta", queryItems: [URLQueryItem(name: "since", value: String(since))])
        return try await perform(req)
    }

    public func getChatDetails(id: String) async throws -> ChatSummary {
        let req = try buildURLRequest(path: "conversations/\(id)")
        return try await perform(req)
    }

    public func getMessages(conversationID: String, cursor: String?, limit: Int?) async throws -> MessagesResponse {
        var items: [URLQueryItem] = []
        if let cursor = cursor { items.append(URLQueryItem(name: "cursor", value: cursor)) }
        items.append(URLQueryItem(name: "limit", value: String(limit ?? 50)))
        let req = try buildURLRequest(path: "conversations/\(conversationID)/messages", queryItems: items)
        return try await perform(req)
    }

    public func sendMessage(conversationID: String, request: SendMessageRequest) async throws -> MessageResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/messages", method: "POST", body: body)
        return try await perform(req)
    }

    public func markAsRead(conversationID: String, request: ReadReceiptRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/read", method: "POST", body: body)
        return try await perform(req)
    }

    public func getMembers(conversationID: String) async throws -> [ApiUser] {
        let req = try buildURLRequest(path: "conversations/\(conversationID)/members")
        return try await perform(req)
    }

    public func updateApplicationStatus(conversationID: String, request: StatusUpdateRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/application-status", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func addGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(groupId)/members", method: "POST", body: body)
        return try await perform(req)
    }

    public func removeGroupMember(groupId: String, userId: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(groupId)/members/\(userId)", method: "DELETE")
        return try await perform(req)
    }

    public func promoteToAdmin(groupId: String, userId: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(groupId)/admin/\(userId)", method: "POST")
        return try await perform(req)
    }

    public func leaveGroup(groupId: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(groupId)/leave", method: "POST")
        return try await perform(req)
    }

    public func initiateDM(request: InitiateDMRequest) async throws -> ConversationResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/dm", method: "POST", body: body)
        return try await perform(req)
    }

    public func createGroup(request: CreateGroupRequest) async throws -> ConversationResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/group", method: "POST", body: body)
        return try await perform(req)
    }

    public func getGroupInfo(groupId: String) async throws -> GroupInfoResponse {
        let req = try buildURLRequest(path: "chats/groups/\(groupId)")
        return try await perform(req)
    }

    public func renameGroup(groupId: String, request: RenameGroupRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func addChatGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/members", method: "POST", body: body)
        return try await perform(req)
    }

    public func removeChatGroupMember(groupId: String, request: AddMemberRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/members", method: "DELETE", body: body)
        return try await perform(req)
    }

    public func promoteChatGroupAdmin(groupId: String, request: AddMemberRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/admins", method: "POST", body: body)
        return try await perform(req)
    }

    public func demoteChatGroupAdmin(groupId: String, request: AddMemberRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/admins", method: "DELETE", body: body)
        return try await perform(req)
    }

    public func updateGroupAvatar(groupId: String, request: AvatarUrlRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/avatar", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "chats/channels/\(channelId)/avatar", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func leaveChatGroup(groupId: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/leave", method: "POST")
        return try await perform(req)
    }

    public func generateGroupInviteCode(groupId: String) async throws -> GroupInviteCodeResponse {
        let req = try buildURLRequest(path: "chats/groups/\(groupId)/invite-code", method: "POST")
        return try await perform(req)
    }

    public func joinGroupByInvite(inviteCode: String) async throws -> GroupJoinResponse {
        let req = try buildURLRequest(path: "chats/groups/join/\(inviteCode)", method: "POST")
        return try await perform(req)
    }

    public func deleteConversation(conversationID: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(conversationID)", method: "DELETE")
        return try await perform(req)
    }

    public func clearConversation(conversationID: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(conversationID)/clear", method: "POST")
        return try await perform(req)
    }

    public func deleteMessage(conversationID: String, messageID: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "conversations/\(conversationID)/messages/\(messageID)", method: "DELETE")
        return try await perform(req)
    }

    public func editMessage(conversationID: String, messageID: String, request: EditMessageRequest) async throws -> MessageResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/messages/\(messageID)", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func getMessageReaders(messageID: String) async throws -> [ApiReader] {
        let req = try buildURLRequest(path: "messages/\(messageID)/readers")
        return try await perform(req)
    }

    public func reactToMessage(conversationID: String, messageID: String, request: ReactMessageRequest) async throws -> ReactionsResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/messages/\(messageID)/reactions", method: "POST", body: body)
        return try await perform(req)
    }

    public func initiateCall(request: InitiateCallRequest) async throws -> InitiateCallResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "calls/initiate", method: "POST", body: body)
        return try await perform(req)
    }

    public func getCallLogs() async throws -> CallLogsResponse {
        let req = try buildURLRequest(path: "calls")
        return try await perform(req)
    }

    public func subscribeToChannel(channelId: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "chats/channels/\(channelId)/subscribe", method: "POST")
        return try await perform(req)
    }

    public func getICEServers() async throws -> IceServersResponse {
        let req = try buildURLRequest(path: "calls/ice_servers")
        return try await perform(req)
    }

    public func updateConversationPrefs(conversationID: String, request: ConversationPrefsRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/prefs", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func reportMessage(messageID: String, request: ReportMessageRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "messages/\(messageID)/report", method: "POST", body: body)
        return try await perform(req)
    }

    public func pinMessage(conversationID: String, request: PinMessageRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "conversations/\(conversationID)/pinned-message", method: "PATCH", body: body)
        return try await perform(req)
    }

    public func starMessage(messageID: String, request: StarMessageRequest) async throws -> StatusResponse {
        let body = try encode(request)
        let req = try buildURLRequest(path: "messages/\(messageID)/star", method: "POST", body: body)
        return try await perform(req)
    }

    public func unstarMessage(messageID: String) async throws -> StatusResponse {
        let req = try buildURLRequest(path: "messages/\(messageID)/star", method: "DELETE")
        return try await perform(req)
    }

    public func getStarredMessages() async throws -> MessagesResponse {
        let req = try buildURLRequest(path: "messages/starred")
        return try await perform(req)
    }
}
