import Foundation

struct SongItem: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let artist: String
    let album: String
    let duration: String
    let coverUrl: String

    static let samples = [
        SongItem(id: "s1", title: "Midnight City", artist: "M83", album: "Hurry Up, We're Dreaming", duration: "4:03", coverUrl: "https://picsum.photos/200?random=10"),
        SongItem(id: "s2", title: "Starboy", artist: "The Weeknd", album: "Starboy", duration: "3:50", coverUrl: "https://picsum.photos/200?random=11"),
        SongItem(id: "s3", title: "Blinding Lights", artist: "The Weeknd", album: "After Hours", duration: "3:20", coverUrl: "https://picsum.photos/200?random=12")
    ]
}
