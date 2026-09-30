import Foundation
import SwiftUI
import Combine

public struct Advert: Identifiable, Hashable, Codable {
    public var id: String
    public var advertiserName: String
    public var title: String
    public var actionText: String
    public var avatarColorHex: String
    public var contentColorHex: String
    public var isViewed: Bool

    public init(
        id: String = UUID().uuidString,
        advertiserName: String = "Rewards",
        title: String = "",
        actionText: String = "Watch",
        avatarColorHex: String = "#CCCCCC",
        contentColorHex: String = "#EEEEEE",
        isViewed: Bool = false
    ) {
        self.id = id
        self.advertiserName = advertiserName
        self.title = title
        self.actionText = actionText
        self.avatarColorHex = avatarColorHex
        self.contentColorHex = contentColorHex
        self.isViewed = isViewed
    }
}

public struct RewardDashboard: Codable {
    public var points: Double
    public var dailyStreak: Int
    public var tasksCompleted: Int

    public init(points: Double = 0.0, dailyStreak: Int = 0, tasksCompleted: Int = 0) {
        self.points = points
        self.dailyStreak = dailyStreak
        self.tasksCompleted = tasksCompleted
    }
}

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
    @Published public var adverts: ResourceState<[Advert]> = .loading([])
    @Published public var viewedAdverts: ResourceState<[Advert]> = .loading([])
    @Published public var points: Double = 0.0

    public init() {
        fetchDashboard()
        fetchAdverts()
    }

    public func fetchDashboard() {
        uiState = .loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            let dash = RewardDashboard(points: self?.points ?? 0.0, dailyStreak: 1, tasksCompleted: 5)
            self?.uiState = .success(dash)
        }
    }

    public func fetchAdverts() {
        var list: [Advert] = []
        for i in 1...30 {
            list.append(
                Advert(
                    id: "virtual_ad_\(i)",
                    advertiserName: "Rewards",
                    title: "Extra Reward Slot #\(i)",
                    actionText: "Watch",
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
