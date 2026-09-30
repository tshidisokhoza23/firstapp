import SwiftUI

public enum ConversationType: String, Codable {
    case direct = "direct"
    case group = "group"
    case channel = "channel"
    case mingle = "mingle"
}

public struct ChatMessage: Identifiable, Hashable, Codable {
    public var id: String
    public var senderName: String
    public var displayName: String?
    public var phoneNumber: String?
    public var avatarUrl: String?
    public var avatarColorHex: String?
    public var lastMessage: String
    public var lastMessageIsFromMe: Bool
    public var lastMessageRead: Bool
    public var lastMessageStatus: MessageStatus
    public var timestamp: Int64
    public var unreadCount: Int
    public var isOnline: Bool
    public var isTyping: Bool
    public var isMuted: Bool
    public var isPinned: Bool
    public var isArchived: Bool
    public var isGroup: Bool
    public var isMingle: Bool
    public var type: ConversationType
    public var otherUserId: String?
    public var lastSeen: Int64
    public var createdBy: String?
    public var isAdmin: Bool

    public init(
        id: String = UUID().uuidString,
        senderName: String = "",
        displayName: String? = nil,
        phoneNumber: String? = nil,
        avatarUrl: String? = nil,
        avatarColorHex: String? = nil,
        lastMessage: String = "",
        lastMessageIsFromMe: Bool = false,
        lastMessageRead: Bool = false,
        lastMessageStatus: MessageStatus = .sent,
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        unreadCount: Int = 0,
        isOnline: Bool = false,
        isTyping: Bool = false,
        isMuted: Bool = false,
        isPinned: Bool = false,
        isArchived: Bool = false,
        isGroup: Bool = false,
        isMingle: Bool = false,
        type: ConversationType = .direct,
        otherUserId: String? = nil,
        lastSeen: Int64 = 0,
        createdBy: String? = nil,
        isAdmin: Bool = false
    ) {
        self.id = id
        self.senderName = senderName
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.avatarUrl = avatarUrl
        self.avatarColorHex = avatarColorHex
        self.lastMessage = lastMessage
        self.lastMessageIsFromMe = lastMessageIsFromMe
        self.lastMessageRead = lastMessageRead
        self.lastMessageStatus = lastMessageStatus
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.isOnline = isOnline
        self.isTyping = isTyping
        self.isMuted = isMuted
        self.isPinned = isPinned
        self.isArchived = isArchived
        self.isGroup = isGroup
        self.isMingle = isMingle
        self.type = type
        self.otherUserId = otherUserId
        self.lastSeen = lastSeen
        self.createdBy = createdBy
        self.isAdmin = isAdmin
    }

    public func consistentName() -> String {
        if let d = displayName, !d.isEmpty { return d }
        if !senderName.isEmpty { return senderName }
        return phoneNumber ?? id
    }
}

public struct ChatItemRow: View {
    public let chat: ChatMessage
    public var hasStory: Bool = false
    public var allStoriesViewed: Bool = false
    public var onAvatarClick: () -> Void = {}

    public init(
        chat: ChatMessage,
        hasStory: Bool = false,
        allStoriesViewed: Bool = false,
        onAvatarClick: @escaping () -> Void = {}
    ) {
        self.chat = chat
        self.hasStory = hasStory
        self.allStoriesViewed = allStoriesViewed
        self.onAvatarClick = onAvatarClick
    }

    private var name: String {
        let n = chat.consistentName()
        return n.isEmpty ? "Chat" : n
    }

    private var formattedTime: String {
        let date = Date(timeIntervalSince1970: TimeInterval(chat.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    public var body: some View {
        HStack(spacing: 12) {
            ZStack(alignment: .bottomTrailing) {
                if let urlString = chat.avatarUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image.resizable().scaledToFill()
                        } else {
                            initialsView
                        }
                    }
                    .frame(width: 52, height: 52)
                    .clipShape(Circle())
                    .onTapGesture { onAvatarClick() }
                } else {
                    initialsView
                        .onTapGesture { onAvatarClick() }
                }

                if chat.isOnline && !chat.isMingle && !chat.isGroup && chat.type != .channel {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 14, height: 14)
                        .overlay(Circle().stroke(Color(UIColor.systemBackground), lineWidth: 2))
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(name)
                        .font(.system(size: 16, weight: .bold))
                        .lineLimit(1)

                    Spacer()

                    Text(formattedTime)
                        .font(.system(size: 12))
                        .foregroundColor(chat.unreadCount > 0 ? .accentColor : .gray)
                }

                HStack(spacing: 4) {
                    if chat.unreadCount == 0 && !chat.isTyping && !chat.lastMessage.isEmpty && chat.lastMessageIsFromMe {
                        StatusTickView(status: chat.lastMessageStatus)
                    }

                    Text(chat.isTyping ? "typing..." : chat.lastMessage)
                        .font(.system(size: 14))
                        .foregroundColor(chat.isTyping ? .accentColor : .secondary)
                        .lineLimit(1)

                    Spacer()

                    if chat.isMuted {
                        Image(systemName: "bell.slash.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }

                    if chat.isPinned {
                        Image(systemName: "pin.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }

                    if chat.unreadCount > 0 {
                        Text("\(chat.unreadCount)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.accentColor)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
    }

    private var initialsView: some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.15))
                .frame(width: 52, height: 52)
            Text(String(name.prefix(1)).uppercased())
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.accentColor)
        }
    }
}

public struct StatusTickView: View {
    public let status: MessageStatus

    public init(status: MessageStatus) {
        self.status = status
    }

    public var body: some View {
        switch status {
        case .read:
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 12))
                .foregroundColor(.blue)
        case .delivered:
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 12))
                .foregroundColor(.gray)
        case .sent:
            Image(systemName: "checkmark")
                .font(.system(size: 12))
                .foregroundColor(.gray)
        case .failed:
            Image(systemName: "exclamationmark.circle.fill")
                .font(.system(size: 12))
                .foregroundColor(.red)
        case .sending, .pending:
            Image(systemName: "clock")
                .font(.system(size: 12))
                .foregroundColor(.gray)
        }
    }
}
