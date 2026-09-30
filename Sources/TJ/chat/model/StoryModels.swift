import Foundation

public enum StoryItemWrapper: Identifiable, Equatable, Sendable {
    case userContent(UserStory)
    case adContent(advert: Advert, id: String)

    public var id: String {
        switch self {
        case .userContent(let story):
            return story.id
        case .adContent(_, let id):
            return id
        }
    }
}

public enum StoryAdInjector {
    public static func injectAds(stories: [UserStory], ads: [Advert], interval: Int = 4) -> [StoryItemWrapper] {
        var result: [StoryItemWrapper] = []
        var adIndex = 0

        for (index, story) in stories.enumerated() {
            result.append(.userContent(story))
            if (index + 1) % interval == 0 && adIndex < ads.count {
                let ad = ads[adIndex]
                let adId = "ad_\(Int64(Date().timeIntervalSince1970 * 1000))_\(adIndex)"
                result.append(.adContent(advert: ad, id: adId))
                adIndex += 1
            }
        }
        return result
    }
}
