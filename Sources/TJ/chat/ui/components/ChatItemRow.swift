import SwiftUI

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
