import Foundation

public struct RewardTask: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var title: String
    public var points: Double
    public var isActive: Bool
    public var isCompleted: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case points
        case isActive = "is_active"
        case isCompleted = "is_completed"
    }

    public init(
        id: String,
        title: String,
        points: Double,
        isActive: Bool,
        isCompleted: Bool
    ) {
        self.id = id
        self.title = title
        self.points = points
        self.isActive = isActive
        self.isCompleted = isCompleted
    }
}

public struct RewardDashboard: Codable, Equatable, Sendable {
    public var totalPoints: Double
    public var adsWatchedToday: Int
    public var maxDailyAds: Int
    public var cooldownRemainingSeconds: Int
    public var tasks: [RewardTask]

    enum CodingKeys: String, CodingKey {
        case totalPoints = "total_points"
        case adsWatchedToday = "ads_watched_today"
        case maxDailyAds = "max_daily_ads"
        case cooldownRemainingSeconds = "cooldown_remaining_seconds"
        case tasks
    }

    public init(
        totalPoints: Double,
        adsWatchedToday: Int,
        maxDailyAds: Int,
        cooldownRemainingSeconds: Int,
        tasks: [RewardTask]
    ) {
        self.totalPoints = totalPoints
        self.adsWatchedToday = adsWatchedToday
        self.maxDailyAds = maxDailyAds
        self.cooldownRemainingSeconds = cooldownRemainingSeconds
        self.tasks = tasks
    }
}
