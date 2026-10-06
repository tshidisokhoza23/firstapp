import Foundation

struct ChatItem: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var lastMessage: String
    var timestamp: String
    var unreadCount: Int
    var avatarUrl: String
    var isOnline: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case conversationId = "conversation_id"
        case name
        case title
        case contactName = "contact_name"
        case lastMessage
        case lastMessageSnake = "last_message"
        case timestamp
        case time
        case unreadCount
        case unreadCountSnake = "unread_count"
        case avatarUrl
        case avatarUrlSnake = "avatar_url"
        case isOnline
        case isOnlineSnake = "is_online"
    }

    init(id: String, name: String, lastMessage: String, timestamp: String, unreadCount: Int, avatarUrl: String, isOnline: Bool) {
        self.id = id
        self.name = name
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.avatarUrl = avatarUrl
        self.isOnline = isOnline
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        self.id = (try? container.decode(String.self, forKey: .id)) ??
                  (try? container.decode(String.self, forKey: .conversationId)) ??
                  UUID().uuidString

        self.name = (try? container.decode(String.self, forKey: .title)) ??
                    (try? container.decode(String.self, forKey: .contactName)) ??
                    (try? container.decode(String.self, forKey: .name)) ??
                    "Conversation"

        self.lastMessage = (try? container.decode(String.self, forKey: .lastMessageSnake)) ??
                           (try? container.decode(String.self, forKey: .lastMessage)) ??
                           ""

        if let timeMs = try? container.decode(Int64.self, forKey: .time) {
            let date = Date(timeIntervalSince1970: TimeInterval(timeMs) / 1000.0)
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            self.timestamp = formatter.string(from: date)
        } else {
            self.timestamp = (try? container.decode(String.self, forKey: .timestamp)) ?? "Just now"
        }

        self.unreadCount = (try? container.decode(Int.self, forKey: .unreadCountSnake)) ??
                           (try? container.decode(Int.self, forKey: .unreadCount)) ??
                           0

        self.avatarUrl = (try? container.decode(String.self, forKey: .avatarUrlSnake)) ??
                         (try? container.decode(String.self, forKey: .avatarUrl)) ??
                         ""

        self.isOnline = (try? container.decode(Bool.self, forKey: .isOnlineSnake)) ??
                        (try? container.decode(Bool.self, forKey: .isOnline)) ??
                        false
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(lastMessage, forKey: .lastMessage)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(unreadCount, forKey: .unreadCount)
        try container.encode(avatarUrl, forKey: .avatarUrl)
        try container.encode(isOnline, forKey: .isOnline)
    }
}


