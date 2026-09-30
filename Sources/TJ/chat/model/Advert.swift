import Foundation

public struct Advert: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var advertiserName: String
    public var advertiserId: String
    public var title: String
    public var time: String?
    public var avatarUrl: String?
    public var avatarColor: Int32
    public var contentUrl: String?
    public var contentColor: Int32
    public var contentText: String
    public var actionText: String
    public var targetUrl: String
    public var isSponsored: Bool
    public var impressions: String
    public var clicks: String
    public var isNativeAd: Bool
    public var isViewed: Bool

    public init(
        id: String,
        advertiserName: String,
        advertiserId: String = "",
        title: String = "",
        time: String? = nil,
        avatarUrl: String? = nil,
        avatarColor: Int32 = 0,
        contentUrl: String? = nil,
        contentColor: Int32 = 0,
        contentText: String = "",
        actionText: String = "Learn More",
        targetUrl: String = "",
        isSponsored: Bool = true,
        impressions: String = "0",
        clicks: String = "0",
        isNativeAd: Bool = false,
        isViewed: Bool = false
    ) {
        self.id = id
        self.advertiserName = advertiserName
        self.advertiserId = advertiserId
        self.title = title
        self.time = time
        self.avatarUrl = avatarUrl
        self.avatarColor = avatarColor
        self.contentUrl = contentUrl
        self.contentColor = contentColor
        self.contentText = contentText
        self.actionText = actionText
        self.targetUrl = targetUrl
        self.isSponsored = isSponsored
        self.impressions = impressions
        self.clicks = clicks
        self.isNativeAd = isNativeAd
        self.isViewed = isViewed
    }
}
