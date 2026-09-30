import SwiftUI
import Combine

class MingleViewModel: ObservableObject {
    @Published var profiles: [MingleProfileModel] = MingleProfileModel.samples
    @Published var currentCardIndex: Int = 0

    init() {
        Task { @MainActor in
            let remoteCandidates = await NetworkClient.shared.fetchMingleCandidates(radiusKm: 50.0)
            if !remoteCandidates.isEmpty {
                self.profiles = remoteCandidates
            }
        }
    }

    func passCurrent() {
        if currentCardIndex < profiles.count - 1 {
            currentCardIndex += 1
        }
    }

    func likeCurrent() {
        if currentCardIndex < profiles.count - 1 {
            currentCardIndex += 1
        }
    }
}
