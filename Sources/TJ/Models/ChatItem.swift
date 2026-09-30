import Foundation

struct ChatItem: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let lastMessage: String
    let timestamp: String
    let unreadCount: Int
    let avatarUrl: String
    let isOnline: Bool

    static let samples = [
        ChatItem(id: "c1", name: "Sarah Connor", lastMessage: "See you at the meeting!", timestamp: "10:42 AM", unreadCount: 2, avatarUrl: "https://i.pravatar.cc/150?u=s1", isOnline: true),
        ChatItem(id: "c2", name: "Tech Community", lastMessage: "Dave: The new release is live 🚀", timestamp: "9:15 AM", unreadCount: 5, avatarUrl: "https://i.pravatar.cc/150?u=s2", isOnline: false),
        ChatItem(id: "c3", name: "Michael Scott", lastMessage: "That's what she said!", timestamp: "Yesterday", unreadCount: 0, avatarUrl: "https://i.pravatar.cc/150?u=s3", isOnline: true),
        ChatItem(id: "c4", name: "Design Guild", lastMessage: "Updated the Figma tokens", timestamp: "Monday", unreadCount: 0, avatarUrl: "https://i.pravatar.cc/150?u=s4", isOnline: false)
    ]
}
