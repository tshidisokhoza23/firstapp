import Foundation

public struct User: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var username: String
    public var email: String
    public var displayName: String
    public var phoneNumber: String
    public var createdAt: String
    public var avatarUrl: String?
    public var bannerUrl: String?
    public var bio: String?
    public var location: String?
    public var isVerified: Bool
    public var isArtist: Bool
    public var isDriver: Bool
    public var isProfileComplete: Bool
    public var missingFields: [String]?

    public var followersCount: Int
    public var followingCount: Int
    public var isFollowing: Bool

    // Vocab specific fields
    public var xp: Int
    public var level: Int
    public var streak: Int
    public var isPremium: Bool
    public var lastDailyUnlock: Int64
    public var adsWatchedToday: Int
    public var unlockedLessonCount: Int
    public var nativeLanguage: String
    public var targetLanguage: String
    public var completedLessonIds: [String]?
    public var lessonMastery: [String: Int]?
    public var streakFreezes: Int
    public var lastStreakActivity: Int64
    public var walletBalance: Double

    // Job Profile Fields
    public var jobFullName: String?
    public var jobEmail: String?
    public var jobPhone: String?
    public var jobLocation: String?
    public var jobEducation: String?
    public var jobExperience: String?
    public var jobSkills: String?
    public var jobAboutMe: String?
    public var jobLinkedinUrl: String?
    public var jobPortfolioUrl: String?
    public var jobWorkPreference: String?
    public var jobAvailability: String?
    public var jobExpectedSalary: String?

    // E2EE
    public var publicKey: String?

    enum CodingKeys: String, CodingKey {
        case id
        case username
        case email
        case displayName = "display_name"
        case phoneNumber = "phone_number"
        case createdAt = "created_at"
        case avatarUrl = "avatar_url"
        case bannerUrl = "banner_url"
        case bio
        case location
        case isVerified = "is_verified"
        case isArtist = "is_artist"
        case isDriver = "is_driver"
        case isProfileComplete = "is_profile_complete"
        case missingFields = "missing_fields"

        case followersCount = "followers_count"
        case followingCount = "following_count"
        case isFollowing = "is_following"

        case xp
        case level
        case streak
        case isPremium = "is_premium"
        case lastDailyUnlock = "last_daily_unlock"
        case adsWatchedToday = "ads_watched_today"
        case unlockedLessonCount = "unlocked_lesson_count"
        case nativeLanguage = "native_language"
        case targetLanguage = "target_language"
        case completedLessonIds = "completed_lesson_ids"
        case lessonMastery = "lesson_mastery"
        case streakFreezes = "streak_freezes"
        case lastStreakActivity = "last_streak_activity"
        case walletBalance = "wallet_balance"

        case jobFullName = "job_full_name"
        case jobEmail = "job_email"
        case jobPhone = "job_phone"
        case jobLocation = "job_location"
        case jobEducation = "job_education"
        case jobExperience = "job_experience"
        case jobSkills = "job_skills"
        case jobAboutMe = "job_about_me"
        case jobLinkedinUrl = "job_linkedin_url"
        case jobPortfolioUrl = "job_portfolio_url"
        case jobWorkPreference = "job_work_preference"
        case jobAvailability = "job_availability"
        case jobExpectedSalary = "job_expected_salary"

        case publicKey = "public_key"
    }

    public init(
        id: String,
        username: String = "",
        email: String = "",
        displayName: String = "",
        phoneNumber: String = "",
        createdAt: String = "",
        avatarUrl: String? = nil,
        bannerUrl: String? = nil,
        bio: String? = nil,
        location: String? = nil,
        isVerified: Bool = false,
        isArtist: Bool = false,
        isDriver: Bool = false,
        isProfileComplete: Bool = true,
        missingFields: [String]? = [],
        followersCount: Int = 0,
        followingCount: Int = 0,
        isFollowing: Bool = false,
        xp: Int = 0,
        level: Int = 1,
        streak: Int = 0,
        isPremium: Bool = false,
        lastDailyUnlock: Int64 = 0,
        adsWatchedToday: Int = 0,
        unlockedLessonCount: Int = 1,
        nativeLanguage: String = "English",
        targetLanguage: String = "Spanish",
        completedLessonIds: [String]? = [],
        lessonMastery: [String: Int]? = [:],
        streakFreezes: Int = 0,
        lastStreakActivity: Int64 = 0,
        walletBalance: Double = 0.0,
        jobFullName: String? = nil,
        jobEmail: String? = nil,
        jobPhone: String? = nil,
        jobLocation: String? = nil,
        jobEducation: String? = nil,
        jobExperience: String? = nil,
        jobSkills: String? = nil,
        jobAboutMe: String? = nil,
        jobLinkedinUrl: String? = nil,
        jobPortfolioUrl: String? = nil,
        jobWorkPreference: String? = nil,
        jobAvailability: String? = nil,
        jobExpectedSalary: String? = nil,
        publicKey: String? = nil
    ) {
        self.id = id
        self.username = username
        self.email = email
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.createdAt = createdAt
        self.avatarUrl = avatarUrl
        self.bannerUrl = bannerUrl
        self.bio = bio
        self.location = location
        self.isVerified = isVerified
        self.isArtist = isArtist
        self.isDriver = isDriver
        self.isProfileComplete = isProfileComplete
        self.missingFields = missingFields
        self.followersCount = followersCount
        self.followingCount = followingCount
        self.isFollowing = isFollowing
        self.xp = xp
        self.level = level
        self.streak = streak
        self.isPremium = isPremium
        self.lastDailyUnlock = lastDailyUnlock
        self.adsWatchedToday = adsWatchedToday
        self.unlockedLessonCount = unlockedLessonCount
        self.nativeLanguage = nativeLanguage
        self.targetLanguage = targetLanguage
        self.completedLessonIds = completedLessonIds
        self.lessonMastery = lessonMastery
        self.streakFreezes = streakFreezes
        self.lastStreakActivity = lastStreakActivity
        self.walletBalance = walletBalance
        self.jobFullName = jobFullName
        self.jobEmail = jobEmail
        self.jobPhone = jobPhone
        self.jobLocation = jobLocation
        self.jobEducation = jobEducation
        self.jobExperience = jobExperience
        self.jobSkills = jobSkills
        self.jobAboutMe = jobAboutMe
        self.jobLinkedinUrl = jobLinkedinUrl
        self.jobPortfolioUrl = jobPortfolioUrl
        self.jobWorkPreference = jobWorkPreference
        self.jobAvailability = jobAvailability
        self.jobExpectedSalary = jobExpectedSalary
        self.publicKey = publicKey
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        username = (try? c.decodeIfPresent(String.self, forKey: .username)) ?? ""
        email = (try? c.decodeIfPresent(String.self, forKey: .email)) ?? ""
        displayName = (try? c.decodeIfPresent(String.self, forKey: .displayName)) ?? ""
        phoneNumber = (try? c.decodeIfPresent(String.self, forKey: .phoneNumber)) ?? ""
        if let text = try? c.decode(String.self, forKey: .createdAt) {
            createdAt = text
        } else if let millis = try? c.decode(Int64.self, forKey: .createdAt) {
            createdAt = String(millis)
        } else {
            createdAt = ""
        }
        avatarUrl = try? c.decodeIfPresent(String.self, forKey: .avatarUrl)
        bannerUrl = try? c.decodeIfPresent(String.self, forKey: .bannerUrl)
        bio = try? c.decodeIfPresent(String.self, forKey: .bio)
        location = try? c.decodeIfPresent(String.self, forKey: .location)
        isVerified = (try? c.decodeIfPresent(Bool.self, forKey: .isVerified)) ?? false
        isArtist = (try? c.decodeIfPresent(Bool.self, forKey: .isArtist)) ?? false
        isDriver = (try? c.decodeIfPresent(Bool.self, forKey: .isDriver)) ?? false
        isProfileComplete = (try? c.decodeIfPresent(Bool.self, forKey: .isProfileComplete)) ?? false
        missingFields = try? c.decodeIfPresent([String].self, forKey: .missingFields)
        followersCount = Self.flexInt(c, .followersCount)
        followingCount = Self.flexInt(c, .followingCount)
        isFollowing = (try? c.decodeIfPresent(Bool.self, forKey: .isFollowing)) ?? false
        xp = Self.flexInt(c, .xp)
        level = max(1, Self.flexInt(c, .level))
        streak = Self.flexInt(c, .streak)
        isPremium = (try? c.decodeIfPresent(Bool.self, forKey: .isPremium)) ?? false
        lastDailyUnlock = Self.flexInt64(c, .lastDailyUnlock)
        adsWatchedToday = Self.flexInt(c, .adsWatchedToday)
        unlockedLessonCount = max(1, Self.flexInt(c, .unlockedLessonCount))
        nativeLanguage = (try? c.decodeIfPresent(String.self, forKey: .nativeLanguage)) ?? "English"
        targetLanguage = (try? c.decodeIfPresent(String.self, forKey: .targetLanguage)) ?? "Spanish"
        completedLessonIds = try? c.decodeIfPresent([String].self, forKey: .completedLessonIds)
        lessonMastery = try? c.decodeIfPresent([String: Int].self, forKey: .lessonMastery)
        streakFreezes = Self.flexInt(c, .streakFreezes)
        lastStreakActivity = Self.flexInt64(c, .lastStreakActivity)
        walletBalance = (try? c.decodeIfPresent(Double.self, forKey: .walletBalance)) ?? 0
        jobFullName = try? c.decodeIfPresent(String.self, forKey: .jobFullName)
        jobEmail = try? c.decodeIfPresent(String.self, forKey: .jobEmail)
        jobPhone = try? c.decodeIfPresent(String.self, forKey: .jobPhone)
        jobLocation = try? c.decodeIfPresent(String.self, forKey: .jobLocation)
        jobEducation = try? c.decodeIfPresent(String.self, forKey: .jobEducation)
        jobExperience = try? c.decodeIfPresent(String.self, forKey: .jobExperience)
        jobSkills = try? c.decodeIfPresent(String.self, forKey: .jobSkills)
        jobAboutMe = try? c.decodeIfPresent(String.self, forKey: .jobAboutMe)
        jobLinkedinUrl = try? c.decodeIfPresent(String.self, forKey: .jobLinkedinUrl)
        jobPortfolioUrl = try? c.decodeIfPresent(String.self, forKey: .jobPortfolioUrl)
        jobWorkPreference = try? c.decodeIfPresent(String.self, forKey: .jobWorkPreference)
        jobAvailability = try? c.decodeIfPresent(String.self, forKey: .jobAvailability)
        jobExpectedSalary = try? c.decodeIfPresent(String.self, forKey: .jobExpectedSalary)
        publicKey = try? c.decodeIfPresent(String.self, forKey: .publicKey)
    }

    private static func flexInt(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) -> Int {
        if let value = try? c.decode(Int.self, forKey: key) { return value }
        if let value = try? c.decode(Int64.self, forKey: key) { return Int(value) }
        if let value = try? c.decode(Double.self, forKey: key) { return Int(value) }
        return 0
    }

    private static func flexInt64(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) -> Int64 {
        if let value = try? c.decode(Int64.self, forKey: key) { return value }
        if let value = try? c.decode(Int.self, forKey: key) { return Int64(value) }
        if let value = try? c.decode(Double.self, forKey: key) { return Int64(value) }
        return 0
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(username, forKey: .username)
        try c.encode(email, forKey: .email)
        try c.encode(displayName, forKey: .displayName)
        try c.encode(phoneNumber, forKey: .phoneNumber)
        try c.encode(createdAt, forKey: .createdAt)
        try c.encodeIfPresent(avatarUrl, forKey: .avatarUrl)
        try c.encodeIfPresent(bio, forKey: .bio)
        try c.encode(isProfileComplete, forKey: .isProfileComplete)
        try c.encode(walletBalance, forKey: .walletBalance)
        try c.encodeIfPresent(publicKey, forKey: .publicKey)
    }
}
