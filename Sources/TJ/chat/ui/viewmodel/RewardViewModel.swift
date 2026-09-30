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
        adverts = .success([])
        viewedAdverts = .success([])
    }

    public func fetchDashboard() {
        uiState = .loading
        Task {
            if let data = try? await API.data("rewards/dashboard"),
               let dash = try? JSONDecoder().decode(RewardDashboard.self, from: data) {
                self.points = dash.totalPoints
                self.hourlyAdCount = dash.adsWatchedToday
                self.cooldownRemainingSeconds = dash.cooldownRemainingSeconds
                self.hasRewardedAdsAvailable = dash.adsWatchedToday < dash.maxDailyAds && dash.cooldownRemainingSeconds == 0
                self.uiState = .success(dash)
                return
            }
            var balance = SessionStore.shared.user?.walletBalance ?? points
            if let data = try? await API.data("rewards/balance"),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                if let value = json["balance"] as? Double { balance = value }
                else if let value = json["balance"] as? Int { balance = Double(value) }
            }
            self.points = balance
            self.uiState = .success(RewardDashboard(
                totalPoints: balance,
                adsWatchedToday: self.hourlyAdCount,
                maxDailyAds: 5,
                cooldownRemainingSeconds: self.cooldownRemainingSeconds,
                tasks: []
            ))
        }
    }

    public func fetchAdverts() {
        adverts = .success([])
    }

    public func showRewardedAd(taskId: String, onReward: @escaping () -> Void) {
        Task {
            do {
                _ = try await API.data("rewards/award", method: "POST", json: [
                    "amount": 10.0,
                    "source": "REWARDED_AD",
                    "description": "Watched rewarded ad",
                    "task_id": taskId
                ])
                toastMessage = "Earned 10 points!"
                onReward()
            } catch {
                toastMessage = "Reward not awarded"
            }
            fetchDashboard()
        }
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
