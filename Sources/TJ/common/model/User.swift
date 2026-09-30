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
}
