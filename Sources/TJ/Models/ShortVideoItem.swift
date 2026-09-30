import Foundation

struct ShortVideoItem: Identifiable, Codable, Hashable {
    let id: String
    let creatorName: String
    let creatorAvatar: String
    let videoTitle: String
    let audioTrack: String
    var likesCount: Int
    var commentsCount: Int
    var isLiked: Bool

    static let samples = [
        ShortVideoItem(id: "sv1", creatorName: "@dev_vibes", creatorAvatar: "https://i.pravatar.cc/150?u=sv1", videoTitle: "How to build slick animations in SwiftUI in under 60s! ✨ #ios #swiftui", audioTrack: "Original Sound - dev_vibes", likesCount: 15400, commentsCount: 320, isLiked: true),
        ShortVideoItem(id: "sv2", creatorName: "@travel_bug", creatorAvatar: "https://i.pravatar.cc/150?u=sv2", videoTitle: "Sunset over Santorini 🌅 Best travel spots 2026", audioTrack: "Chill Waves - Sunset Beats", likesCount: 42100, commentsCount: 890, isLiked: false)
    ]
}
