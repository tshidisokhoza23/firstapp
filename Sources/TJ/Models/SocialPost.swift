import Foundation

struct SocialPost: Identifiable, Codable, Hashable {
    let id: String
    let authorName: String
    let authorAvatar: String
    let timestamp: String
    let textContent: String
    let imageUrl: String?
    var likesCount: Int
    var commentsCount: Int
    var isLiked: Bool

    static let samples = [
        SocialPost(id: "p1", authorName: "Tech News Daily", authorAvatar: "https://i.pravatar.cc/150?u=p1", timestamp: "2h ago", textContent: "Exciting features coming to the TJ platform! Stay tuned for native video calling and live streams 🚀", imageUrl: nil, likesCount: 342, commentsCount: 48, isLiked: false),
        SocialPost(id: "p2", authorName: "Jessica Alba", authorAvatar: "https://i.pravatar.cc/150?u=p2", timestamp: "5h ago", textContent: "Morning coffee & coding! ☕️💻 What are you building today?", imageUrl: "https://picsum.photos/600/400", likesCount: 1280, commentsCount: 95, isLiked: true)
    ]
}
