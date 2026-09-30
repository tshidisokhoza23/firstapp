import Foundation

public struct PrivacySettings: Codable, Equatable, Sendable {
    public var showLastSeen: String
    public var showOnlineStatus: String
    public var showReadReceipts: Bool
    public var whoCanAddToGroups: String
    public var whoCanSeeStories: String
    public var excludedStoryUserIds: [String]

    enum CodingKeys: String, CodingKey {
        case showLastSeen = "show_last_seen"
        case showOnlineStatus = "show_online_status"
        case showReadReceipts = "show_read_receipts"
        case whoCanAddToGroups = "who_can_add_to_groups"
        case whoCanSeeStories = "who_can_see_stories"
        case excludedStoryUserIds = "excluded_story_user_ids"
    }

    public init(
        showLastSeen: String = "everyone",
        showOnlineStatus: String = "everyone",
        showReadReceipts: Bool = true,
        whoCanAddToGroups: String = "everyone",
        whoCanSeeStories: String = "my_contacts",
        excludedStoryUserIds: [String] = []
    ) {
        self.showLastSeen = showLastSeen
        self.showOnlineStatus = showOnlineStatus
        self.showReadReceipts = showReadReceipts
        self.whoCanAddToGroups = whoCanAddToGroups
        self.whoCanSeeStories = whoCanSeeStories
        self.excludedStoryUserIds = excludedStoryUserIds
    }
}
