import Foundation
import AVKit
import Combine

@MainActor
final class ShortsPlaybackCoordinator: ObservableObject {
    static let shared = ShortsPlaybackCoordinator()

    @Published var activeShortId: String? = nil
    private var currentPlayer: AVPlayer?

    func play(shortId: String, url: URL) -> AVPlayer {
        stopCurrent()

        let player = AVPlayer(url: url)
        self.currentPlayer = player
        self.activeShortId = shortId
        player.play()
        return player
    }

    func stopCurrent() {
        currentPlayer?.pause()
        currentPlayer?.replaceCurrentItem(with: nil)
        currentPlayer = nil
        activeShortId = nil
    }

    func pauseCurrent() {
        currentPlayer?.pause()
    }

    func resumeCurrent() {
        currentPlayer?.play()
    }
}
