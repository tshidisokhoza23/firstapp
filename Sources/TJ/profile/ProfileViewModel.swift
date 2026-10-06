import SwiftUI

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var isFollowing: Bool = false
    @Published var followersCount: Int = 1420
    @Published var isLoading: Bool = false

    func toggleFollowState() {
        let previousState = isFollowing
        let previousCount = followersCount

        isFollowing.toggle()
        followersCount += isFollowing ? 1 : -1

        Task {
            try? await Task.sleep(nanoseconds: 300_000_000)
        }
    }
}
