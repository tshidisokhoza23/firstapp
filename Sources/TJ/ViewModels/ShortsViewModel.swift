import SwiftUI
import Combine

class ShortsViewModel: ObservableObject {
    @Published var shortVideos: [ShortVideoItem] = ShortVideoItem.samples
    @Published var currentVideoIndex: Int = 0

    func toggleLike(for video: ShortVideoItem) {
        if let index = shortVideos.firstIndex(where: { $0.id == video.id }) {
            shortVideos[index].isLiked.toggle()
            shortVideos[index].likesCount += shortVideos[index].isLiked ? 1 : -1
        }
    }
}
