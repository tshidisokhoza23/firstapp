import Foundation
import SwiftUI
import Combine

public enum RewardUiState {
    case loading
    case success(RewardDashboard)
    case error(String)
}

@MainActor
public class RewardViewModel: ObservableObject {
    @Published public var uiState: RewardUiState = .loading
    @Published public var toastMessage: String? = nil
    @Published public var hasRewardedAdsAvailable: Bool = true
    @Published public var cooldownRemainingSeconds: Int = 0
    @Published public var hourlyAdCount: Int = 0
    @Published public var adverts: Resource<[Advert]> = .loading()
    @Published public var viewedAdverts: Resource<[Advert]> = .loading()
    @Published public var points: Double = 0.0

    public init() {
        fetchDashboard()
        fetchAdverts()
    }

    public func fetchDashboard() {
        uiState = .loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            let dash = RewardDashboard(
                totalPoints: self?.points ?? 0.0,
                adsWatchedToday: self?.hourlyAdCount ?? 0,
                maxDailyAds: 5,
                cooldownRemainingSeconds: self?.cooldownRemainingSeconds ?? 0,
                tasks: []
            )
            self?.uiState = .success(dash)
        }
    }

    public func fetchAdverts() {
        var list: [Advert] = []
        for i in 1...30 {
            list.append(
                Advert(
                    id: "virtual_ad_\(i)",
                    advertiserName: "TJ Rewards",
                    title: "Extra Reward Slot #\(i)",
                    contentText: "Watch ad to earn points",
                    isViewed: false
                )
            )
        }
        adverts = .success(list)
    }

    public func showRewardedAd(taskId: String, onReward: @escaping () -> Void) {
        points += 10.0
        toastMessage = "Earned 10 points!"
        onReward()
        fetchDashboard()
    }

    public func clearToast() {
        toastMessage = nil
    }

    public func markAdvertAsViewed(advertId: String) {
        if var list = adverts.data, let idx = list.firstIndex(where: { $0.id == advertId }) {
            list[idx].isViewed = true
            adverts = .success(list)
        }
    }
}
