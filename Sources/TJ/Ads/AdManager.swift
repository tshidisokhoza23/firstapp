import SwiftUI
import Combine

struct AdItem: Identifiable, Codable, Hashable {
    let id: String
    let sponsorName: String
    let sponsorLogo: String
    let headline: String
    let bodyText: String
    let ctaText: String
    let imageUrl: String
    let destinationUrl: String

    static let samples = [
        AdItem(
            id: "ad1",
            sponsorName: "Tech Cloud Pro",
            sponsorLogo: "cloud.fill",
            headline: "Scale your cloud infrastructure seamlessly",
            bodyText: "Get $200 free cloud credits today with Tech Cloud Pro. Fast, reliable, and developer-friendly.",
            ctaText: "Claim $200 Credit",
            imageUrl: "https://picsum.photos/600/300?random=100",
            destinationUrl: "https://example.com/cloud"
        ),
        AdItem(
            id: "ad2",
            sponsorName: "SwiftUI Masterclass",
            sponsorLogo: "applelogo",
            headline: "Master iOS & SwiftUI in 30 Days",
            bodyText: "Learn advanced architecture, custom animations, and Combine with hands-on projects.",
            ctaText: "Enroll Now",
            imageUrl: "https://picsum.photos/600/300?random=101",
            destinationUrl: "https://example.com/course"
        )
    ]
}

class AdManager: ObservableObject {
    static let shared = AdManager()

    @Published var userPoints: Int = 350
    @Published var isRewardAdShowing: Bool = false
    @Published var sampleAds: [AdItem] = AdItem.samples

    func watchRewardedAd(completion: @escaping (Int) -> Void) {
        isRewardAdShowing = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            self?.userPoints += 50
            self?.isRewardAdShowing = false
            completion(50)
        }
    }
}
