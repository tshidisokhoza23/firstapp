import SwiftUI
import AVKit

struct ShortVideoItemView: View {
    let shortId: String
    let videoUrlString: String
    let isCurrentPage: Bool

    @StateObject private var coordinator = ShortsPlaybackCoordinator.shared
    @State private var localPlayer: AVPlayer?

    var body: some View {
        ZStack {
            if let player = localPlayer {
                VideoPlayer(player: player)
                    .disabled(true)
                    .ignoresSafeArea()
            } else {
                Rectangle()
                    .fill(Color.black)
                    .aspectRatio(9/16, contentMode: .fill)
                    .overlay(
                        VStack(spacing: 12) {
                            ProgressView().tint(.white)
                            Text("Loading Shorts...")
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.7))
                        }
                    )
            }
        }
        .onChange(of: isCurrentPage) { isVisible in
            if isVisible {
                startPlayback()
            } else {
                tearDownPlayback()
            }
        }
        .onAppear {
            if isCurrentPage {
                startPlayback()
            }
        }
        .onDisappear {
            tearDownPlayback()
        }
    }

    private func startPlayback() {
        let sampleUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        guard let url = URL(string: sampleUrl) else { return }
        localPlayer = coordinator.play(shortId: shortId, url: url)
    }

    private func tearDownPlayback() {
        localPlayer?.pause()
        localPlayer?.replaceCurrentItem(with: nil)
        localPlayer = nil
        if coordinator.activeShortId == shortId {
            coordinator.stopCurrent()
        }
    }
}
