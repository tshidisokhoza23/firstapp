import Foundation

struct StoryItem: Identifiable, Codable, Hashable {
    let id: String
    let userName: String
    let avatarUrl: String
    let isLive: Bool
    let hasUnseenStory: Bool

    static let samples = [
        StoryItem(id: "st0", userName: "Your Story", avatarUrl: "https://i.pravatar.cc/150?u=usr_1", isLive: false, hasUnseenStory: false),
        StoryItem(id: "st1", userName: "Emma W.", avatarUrl: "https://i.pravatar.cc/150?u=st1", isLive: true, hasUnseenStory: true),
        StoryItem(id: "st2", userName: "John D.", avatarUrl: "https://i.pravatar.cc/150?u=st2", isLive: false, hasUnseenStory: true),
        StoryItem(id: "st3", userName: "Sophia", avatarUrl: "https://i.pravatar.cc/150?u=st3", isLive: false, hasUnseenStory: false)
    ]
}
