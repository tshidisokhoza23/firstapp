import SwiftUI
import Combine

class StreamViewModel: ObservableObject {
    @Published var songs: [SongItem] = SongItem.samples
    @Published var currentSong: SongItem? = SongItem.samples.first
    @Published var isPlaying: Bool = false

    func togglePlayPause() {
        isPlaying.toggle()
    }

    func playSong(_ song: SongItem) {
        currentSong = song
        isPlaying = true
    }
}
