import Foundation

// MARK: - Auth API Services & DTOs

public protocol AuthApiService: Sendable {
    func register(request: RegisterRequest) async throws -> AuthResponse
    func login(request: LoginRequest) async throws -> AuthResponse
    func refresh(request: TokenRefreshRequest) async throws -> AuthResponse
    func logout(request: TokenRefreshRequest) async throws -> StatusResponse
    func forgotPassword(request: ForgotPasswordRequest) async throws -> StatusResponse
    func requestOTP(body: [String: String]) async throws -> StatusResponse
    func verifyOTP(body: [String: String]) async throws -> StatusResponse
    func googleSignIn(idToken: String) async throws -> AuthResponse
    func completeProfile(displayName: String?, username: String?, phoneNumber: String?, categories: [String]?) async throws -> User
    func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool]
}

public struct GoogleSignInBody: Codable, Sendable {
    public let idToken: String
    enum CodingKeys: String, CodingKey { case idToken = "id_token" }
    public init(idToken: String) { self.idToken = idToken }
}

public struct CompleteProfileBody: Codable, Sendable {
    public let displayName: String?
    public let username: String?
    public let phoneNumber: String?
    public let categories: [String]?
    enum CodingKeys: String, CodingKey {
        case displayName = "display_name"
        case username
        case phoneNumber = "phone_number"
        case categories
    }
}

public struct UserEnvelope: Codable, Sendable {
    public let user: User
}

public final class AuthApi: AuthApiService, @unchecked Sendable {
    public static let shared = AuthApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func register(request: RegisterRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/register", method: .post, body: request)
    }

    public func login(request: LoginRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/login", method: .post, body: request)
    }

    public func refresh(request: TokenRefreshRequest) async throws -> AuthResponse {
        return try await client.request(path: "auth/refresh", method: .post, body: request)
    }

    public func logout(request: TokenRefreshRequest) async throws -> StatusResponse {
        return try await client.request(path: "auth/logout", method: .post, body: request)
    }

    public func forgotPassword(request: ForgotPasswordRequest) async throws -> StatusResponse {
        return try await client.request(path: "auth/forgot-password", method: .post, body: request)
    }

    public func requestOTP(body: [String: String]) async throws -> StatusResponse {
        return try await client.request(path: "auth/request-otp", method: .post, body: body)
    }

    public func verifyOTP(body: [String: String]) async throws -> StatusResponse {
        return try await client.request(path: "auth/verify-otp", method: .post, body: body)
    }

    public func googleSignIn(idToken: String) async throws -> AuthResponse {
        return try await client.request(path: "auth/google", method: .post, body: GoogleSignInBody(idToken: idToken))
    }

    public func completeProfile(displayName: String?, username: String?, phoneNumber: String?, categories: [String]?) async throws -> User {
        let body = CompleteProfileBody(displayName: displayName, username: username, phoneNumber: phoneNumber, categories: categories)
        let envelope: UserEnvelope = try await client.request(path: "users/complete-profile", method: .post, body: body)
        return envelope.user
    }

    public func checkAvailability(username: String?, email: String?, phone: String?) async throws -> [String: Bool] {
        var queryItems: [URLQueryItem] = []
        if let username = username { queryItems.append(URLQueryItem(name: "username", value: username)) }
        if let email = email { queryItems.append(URLQueryItem(name: "email", value: email)) }
        if let phone = phone { queryItems.append(URLQueryItem(name: "phone", value: phone)) }

        return try await client.request(path: "auth/availability", method: .get, queryItems: queryItems)
    }
}

// MARK: - User API Services & DTOs

public protocol UserApiService: Sendable {
    func getMyUser() async throws -> User
    func getUser(userId: String) async throws -> User
    func updateMyUser(user: User) async throws -> User
    func deleteAccount() async throws -> StatusResponse
    func registerPushToken(token: String) async throws -> StatusResponse
    func blockUser(userId: String) async throws -> StatusResponse
    func unblockUser(userId: String) async throws -> StatusResponse
    func followUser(userId: String) async throws -> StatusResponse
    func unfollowUser(userId: String) async throws -> StatusResponse
    func getFollowers(userId: String) async throws -> [User]
    func getFollowing(userId: String) async throws -> [User]
    func getBlockedUsers() async throws -> [User]
    func getPrivacySettings() async throws -> PrivacySettings
    func updatePrivacySettings(settings: PrivacySettings) async throws -> StatusResponse
    func updateAvatar(avatarUrl: String) async throws -> StatusResponse
    func syncContacts(phoneNumbers: [String]) async throws -> [Contact]
    func getPersistentContacts() async throws -> [Contact]
    func search(query: String) async throws -> [User]
}

public struct PushTokenBody: Codable, Sendable {
    public let token: String
    public let platform: String
    enum CodingKeys: String, CodingKey {
        case token, platform
    }
    public init(token: String, platform: String = "ios") {
        self.token = token
        self.platform = platform
    }
}

public struct SyncContactsBody: Codable, Sendable {
    public let phoneNumbers: [String]
    enum CodingKeys: String, CodingKey {
        case phoneNumbers = "phone_numbers"
    }
    public init(phoneNumbers: [String]) {
        self.phoneNumbers = phoneNumbers
    }
}

public final class UserApi: UserApiService, @unchecked Sendable {
    public static let shared = UserApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getMyUser() async throws -> User {
        return try await client.request(path: "users/me")
    }

    public func getUser(userId: String) async throws -> User {
        return try await client.request(path: "users/\(userId)")
    }

    public func updateMyUser(user: User) async throws -> User {
        return try await client.request(path: "users/me", method: .put, body: user)
    }

    public func deleteAccount() async throws -> StatusResponse {
        return try await client.request(path: "users/me", method: .delete)
    }

    public func registerPushToken(token: String) async throws -> StatusResponse {
        return try await client.request(path: "devices/push-token", method: .post, body: PushTokenBody(token: token))
    }

    public func blockUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/block", method: .post)
    }

    public func unblockUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/unblock", method: .post)
    }

    public func followUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/follow", method: .post)
    }

    public func unfollowUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "users/\(userId)/unfollow", method: .post)
    }

    public func getFollowers(userId: String) async throws -> [User] {
        return try await client.request(path: "users/\(userId)/followers")
    }

    public func getFollowing(userId: String) async throws -> [User] {
        return try await client.request(path: "users/\(userId)/following")
    }

    public func getBlockedUsers() async throws -> [User] {
        return try await client.request(path: "users/blocked")
    }

    public func getPrivacySettings() async throws -> PrivacySettings {
        return try await client.request(path: "users/privacy")
    }

    public func updatePrivacySettings(settings: PrivacySettings) async throws -> StatusResponse {
        return try await client.request(path: "users/privacy", method: .put, body: settings)
    }

    public func updateAvatar(avatarUrl: String) async throws -> StatusResponse {
        return try await client.request(path: "users/avatar", method: .put, body: AvatarUrlRequest(avatarUrl: avatarUrl))
    }

    public func syncContacts(phoneNumbers: [String]) async throws -> [Contact] {
        return try await client.request(path: "users/sync-contacts", method: .post, body: SyncContactsBody(phoneNumbers: phoneNumbers))
    }

    public func getPersistentContacts() async throws -> [Contact] {
        return try await client.request(path: "users/persistent-contacts")
    }

    public func search(query: String) async throws -> [User] {
        let queryItems = [URLQueryItem(name: "query", value: query)]
        return try await client.request(path: "search", queryItems: queryItems)
    }
}

// MARK: - Channel API Services & DTOs

public struct ChannelsResponse: Codable, Sendable {
    public let channels: [ApiChannel]
}

public struct ApiChannel: Codable, Identifiable, Sendable {
    public let id: String
    public let name: String
    public let description: String?
    public let avatarUrl: String?
    public let subscriberCount: Int
    public let isSubscribed: Bool
    public let isAdmin: Bool

    enum CodingKeys: String, CodingKey {
        case id, name, description
        case avatarUrl = "avatar_url"
        case subscriberCount = "subscriber_count"
        case isSubscribed = "is_subscribed"
        case isAdmin = "is_admin"
    }
}

public struct CreateChannelRequest: Codable, Sendable {
    public let name: String
    public let description: String?
    public let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case name, description
        case avatarUrl = "avatar_url"
    }

    public init(name: String, description: String? = nil, avatarUrl: String? = nil) {
        self.name = name
        self.description = description
        self.avatarUrl = avatarUrl
    }
}

public protocol ChannelApiService: Sendable {
    func getChannels() async throws -> ChannelsResponse
    func getChatsChannels() async throws -> ChannelsResponse
    func searchChannels(query: String) async throws -> ChannelsResponse
    func getChannel(channelId: String) async throws -> ApiChannel
    func createChannel(request: CreateChannelRequest) async throws -> ApiChannel
    func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse
    func subscribeToChannel(channelId: String) async throws -> StatusResponse
    func muteChannel(channelId: String, request: MuteRequest) async throws -> StatusResponse
    func addChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse
    func toggleAdminPosting(channelId: String, request: ToggleAdminPostingRequest) async throws -> StatusResponse
    func removeChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse
}

public final class ChannelApi: ChannelApiService, @unchecked Sendable {
    public static let shared = ChannelApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getChannels() async throws -> ChannelsResponse {
        return try await client.request(path: "channels", method: .get)
    }

    public func getChatsChannels() async throws -> ChannelsResponse {
        return try await client.request(path: "chats/channels", method: .get)
    }

    public func searchChannels(query: String) async throws -> ChannelsResponse {
        let queryItems = [URLQueryItem(name: "query", value: query)]
        return try await client.request(path: "chats/channels/search", method: .get, queryItems: queryItems)
    }

    public func getChannel(channelId: String) async throws -> ApiChannel {
        return try await client.request(path: "chats/channels/\(channelId)", method: .get)
    }

    public func createChannel(request: CreateChannelRequest) async throws -> ApiChannel {
        return try await client.request(path: "chats/channels", method: .post, body: request)
    }

    public func updateChannelAvatar(channelId: String, request: AvatarUrlRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/avatar", method: .patch, body: request)
    }

    public func subscribeToChannel(channelId: String) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/subscribe", method: .post)
    }

    public func muteChannel(channelId: String, request: MuteRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/mute", method: .post, body: request)
    }

    public func addChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admins", method: .post, body: request)
    }

    public func toggleAdminPosting(channelId: String, request: ToggleAdminPostingRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admin-posting", method: .post, body: request)
    }

    public func removeChannelAdmin(channelId: String, request: UserIdRequest) async throws -> StatusResponse {
        return try await client.request(path: "chats/channels/\(channelId)/admins", method: .delete, body: request)
    }
}

// MARK: - Job API Services & DTOs

public struct JobsResponse: Codable, Sendable {
    public let jobs: [ApiJob]
    public let next_cursor: String?
}

public struct JobApplicationsResponse: Codable, Sendable {
    public let applications: [ApiJobApplication]
}

public struct CreateJobRequest: Codable, Sendable {
    public let title: String
    public let description: String
    public let job_category: String
    public let required_skills: [String]
    public let duration_days: Int
    public let company: String?
    public let location_city: String?
    public let category: String?
    public let skills: [String]?

    public init(
        title: String,
        description: String,
        job_category: String,
        required_skills: [String],
        duration_days: Int,
        company: String? = nil,
        location_city: String? = nil,
        category: String? = nil,
        skills: [String]? = nil
    ) {
        self.title = title
        self.description = description
        self.job_category = job_category
        self.required_skills = required_skills
        self.duration_days = duration_days
        self.company = company
        self.location_city = location_city
        self.category = category
        self.skills = skills
    }
}

public struct ApiJob: Codable, Sendable {
    public let id: String
    public let title: String
    public let company_name: String
    public let company_logo_url: String?
    public let location: String
    public let salary_range: String?
    public let job_type: String?
    public let posted_time: Int64
    public let description: String?
    public let requirements: [String]?
    public let is_verified: Bool
    public let created_by: String?
    public let is_applied: Bool
    public let is_saved: Bool
    public let is_external: Bool?
    public let source_partner: String?
    public let apply_redirect_url: String?
    public let tracking_apply_url: String?
    public let apply_url: String?
    public let has_applied_internal: Bool?
    public let has_clicked_external: Bool?

    public func getTrackingUrl() -> String? {
        apply_redirect_url ?? tracking_apply_url ?? apply_url
    }

    public func toModel() -> Job {
        Job(
            id: id,
            title: title,
            companyName: company_name,
            companyLogoUrl: company_logo_url,
            location: location,
            logoHex: "#2196F3",
            salaryRange: salary_range ?? "",
            jobType: job_type ?? "Full-time",
            postedTime: posted_time,
            description: description ?? "",
            requirements: requirements ?? [],
            isVerified: is_verified,
            isApplied: is_applied || (has_applied_internal ?? false),
            isSaved: is_saved,
            createdBy: created_by,
            isExternal: is_external ?? false,
            sourcePartner: source_partner,
            trackingApplyUrl: getTrackingUrl(),
            hasAppliedInternal: is_applied || (has_applied_internal ?? false),
            hasClickedExternal: has_clicked_external ?? false
        )
    }
}

public struct ApiJobApplication: Codable, Sendable {
    public let id: String
    public let user_id: String
    public let job_id: String
    public let applied_date: Int64
    public var status: String
    public let title: String?
    public let company_name: String?
    public let location: String?
    public let conversation_id: String?

    public func toModel() -> JobApplication {
        JobApplication(
            id: id,
            jobId: job_id,
            jobTitle: title ?? "Job",
            companyName: company_name ?? "",
            location: location ?? "",
            appliedDate: applied_date,
            status: ApplicationStatus(rawValue: status.uppercased()) ?? .pending,
            logoHex: "#2196F3",
            conversationId: conversation_id
        )
    }
}

public protocol JobApiProtocol: Sendable {
    func getJobs(page: Int, cursor: String?, limit: Int) async throws -> JobsResponse
    func search(query: String) async throws -> JobsResponse
    func applyForJob(jobId: String) async throws -> ConversationResponse
    func saveJob(jobId: String) async throws -> StatusResponse
    func unsaveJob(jobId: String) async throws -> StatusResponse
    func getJobApplications() async throws -> JobApplicationsResponse
    func withdrawApplication(applicationId: String) async throws -> StatusResponse
    func getJobProfile() async throws -> JobProfileData
    func updateJobProfile(profile: JobProfileData) async throws -> StatusResponse
    func createJob(request: CreateJobRequest) async throws -> ApiJob
    func getSavedJobs() async throws -> JobsResponse
}

public final class JobApi: JobApiProtocol, @unchecked Sendable {
    public static let shared = JobApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared, baseURL: URL? = nil, sessionStore: SessionStore = .shared) {
        self.client = client
    }

    public func getJobs(page: Int = 1, cursor: String? = nil, limit: Int = 50) async throws -> JobsResponse {
        var queryItems = [
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "limit", value: "\(limit)")
        ]
        if let cursor = cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        return try await client.request(path: "jobs", queryItems: queryItems)
    }

    public func search(query: String) async throws -> JobsResponse {
        return try await client.request(path: "jobs/search", queryItems: [URLQueryItem(name: "query", value: query)])
    }

    public func applyForJob(jobId: String) async throws -> ConversationResponse {
        return try await client.request(path: "jobs/\(jobId)/apply", method: .post)
    }

    public func saveJob(jobId: String) async throws -> StatusResponse {
        return try await client.request(path: "jobs/\(jobId)/save", method: .post)
    }

    public func unsaveJob(jobId: String) async throws -> StatusResponse {
        return try await client.request(path: "jobs/\(jobId)/save", method: .delete)
    }

    public func getJobApplications() async throws -> JobApplicationsResponse {
        return try await client.request(path: "users/me/applications")
    }

    public func withdrawApplication(applicationId: String) async throws -> StatusResponse {
        return try await client.request(path: "job-applications/\(applicationId)", method: .delete)
    }

    public func getJobProfile() async throws -> JobProfileData {
        return try await client.request(path: "users/me/job-profile")
    }

    public func updateJobProfile(profile: JobProfileData) async throws -> StatusResponse {
        return try await client.request(path: "users/me/job-profile", method: .put, body: profile)
    }

    public func createJob(request: CreateJobRequest) async throws -> ApiJob {
        return try await client.request(path: "jobs", method: .post, body: request)
    }

    public func getSavedJobs() async throws -> JobsResponse {
        return try await client.request(path: "users/me/saved-jobs")
    }
}

// MARK: - Social & Media API Services & DTOs

public struct MediaFeedResponse: Codable, Sendable {
    public let posts: [FeedPost]?
    public let media: [FeedPost]?
}

public protocol SocialApiService: Sendable {
    func getFeed() async throws -> MediaFeedResponse
    func getShorts() async throws -> MediaFeedResponse
    func getStories() async throws -> [UserStory]
    func postStory(mediaUrl: String) async throws -> StoryItem
    func getNotifications() async throws -> [NotificationItem]
}

public struct NotificationItem: Codable, Identifiable, Sendable {
    public let id: String
    public let title: String
    public let body: String
    public let createdAt: Int64
    enum CodingKeys: String, CodingKey {
        case id, title, body
        case createdAt = "created_at"
    }
}

public final class SocialApi: SocialApiService, @unchecked Sendable {
    public static let shared = SocialApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getFeed() async throws -> MediaFeedResponse {
        return try await client.request(path: "media/feed")
    }

    public func getShorts() async throws -> MediaFeedResponse {
        return try await client.request(path: "media/feed/shorts")
    }

    public func getStories() async throws -> [UserStory] {
        return try await client.request(path: "chats/stories")
    }

    public func postStory(mediaUrl: String) async throws -> StoryItem {
        return try await client.request(path: "chats/stories", method: .post, body: ["media_url": mediaUrl])
    }

    public func getNotifications() async throws -> [NotificationItem] {
        return try await client.request(path: "social/notifications")
    }
}

public protocol MediaApiService: Sendable {
    func getAdverts() async throws -> [Advert]
    func getViewedAdverts() async throws -> [Advert]
    func markAdvertViewed(advertId: String) async throws -> StatusResponse
    func trackAdvertClick(advertId: String) async throws -> StatusResponse
    func trackAdvertLike(advertId: String) async throws -> StatusResponse
}

public final class MediaApi: MediaApiService, @unchecked Sendable {
    public static let shared = MediaApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getAdverts() async throws -> [Advert] {
        return try await client.request(path: "adverts")
    }

    public func getViewedAdverts() async throws -> [Advert] {
        return try await client.request(path: "adverts/viewed")
    }

    public func markAdvertViewed(advertId: String) async throws -> StatusResponse {
        return try await client.request(path: "adverts/\(advertId)/view", method: .post)
    }

    public func trackAdvertClick(advertId: String) async throws -> StatusResponse {
        return try await client.request(path: "adverts/\(advertId)/click", method: .post)
    }

    public func trackAdvertLike(advertId: String) async throws -> StatusResponse {
        return try await client.request(path: "adverts/\(advertId)/like", method: .post)
    }
}

// MARK: - Drive API Services & DTOs

public protocol DriveApiService: Sendable {
    func getRideOptions() async throws -> [ApiRideOption]
    func getTrips() async throws -> [ApiTrip]
    func requestRide(pickup: String, destination: String) async throws -> ApiTrip
}

public struct ApiRideOption: Codable, Identifiable, Sendable {
    public let id: String
    public let name: String
    public let priceEstimate: Double
    enum CodingKeys: String, CodingKey {
        case id, name
        case priceEstimate = "price_estimate"
    }
}

public struct ApiTrip: Codable, Identifiable, Sendable {
    public let id: String
    public let pickup: String
    public let destination: String
    public let status: String
}

public final class DriveApi: DriveApiService, @unchecked Sendable {
    public static let shared = DriveApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getRideOptions() async throws -> [ApiRideOption] {
        return try await client.request(path: "ride-options")
    }

    public func getTrips() async throws -> [ApiTrip] {
        return try await client.request(path: "trips")
    }

    public func requestRide(pickup: String, destination: String) async throws -> ApiTrip {
        return try await client.request(path: "ride/request", method: .post, body: ["pickup": pickup, "destination": destination])
    }
}

// MARK: - Mingle API Services & DTOs

public protocol MingleApiService: Sendable {
    func getFeed(radiusKm: Double) async throws -> [MingleUser]
    func getProfile() async throws -> MingleUser
    func likeUser(userId: String) async throws -> StatusResponse
}

public struct MingleUser: Codable, Identifiable, Sendable {
    public let id: String
    public let displayName: String
    public let bio: String?
    public let avatarUrl: String?
    public let distanceKm: Double?

    enum CodingKeys: String, CodingKey {
        case id, bio
        case displayName = "display_name"
        case avatarUrl = "avatar_url"
        case distanceKm = "distance_km"
    }
}

public final class MingleApi: MingleApiService, @unchecked Sendable {
    public static let shared = MingleApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getFeed(radiusKm: Double = 50.0) async throws -> [MingleUser] {
        let queryItems = [URLQueryItem(name: "radius_km", value: "\(radiusKm)")]
        return try await client.request(path: "mingle/feed", queryItems: queryItems)
    }

    public func getProfile() async throws -> MingleUser {
        return try await client.request(path: "mingle/me")
    }

    public func likeUser(userId: String) async throws -> StatusResponse {
        return try await client.request(path: "mingle/like", method: .post, body: ["user_id": userId])
    }
}

// MARK: - Rewards API Services & DTOs

public struct RewardBalanceResponse: Codable, Sendable {
    public let balance: Int
    public let currency: String?
}

public protocol RewardsApiService: Sendable {
    func getBalance() async throws -> RewardBalanceResponse
}

public final class RewardsApi: RewardsApiService, @unchecked Sendable {
    public static let shared = RewardsApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func getBalance() async throws -> RewardBalanceResponse {
        return try await client.request(path: "rewards/balance")
    }
}

// MARK: - System API Services & DTOs

public protocol SystemApiService: Sendable {
    func resetAndSeed() async throws -> StatusResponse
    func resetAds() async throws -> StatusResponse
}

public final class SystemApi: SystemApiService, @unchecked Sendable {
    public static let shared = SystemApi()
    private let client: NetworkClient

    public init(client: NetworkClient = .shared) {
        self.client = client
    }

    public func resetAndSeed() async throws -> StatusResponse {
        return try await client.request(path: "dev/reset-and-seed", method: .post)
    }

    public func resetAds() async throws -> StatusResponse {
        return try await client.request(path: "dev/reset-ads", method: .post)
    }
}
