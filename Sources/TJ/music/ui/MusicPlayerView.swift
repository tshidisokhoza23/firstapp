import SwiftUI

// MARK: - Music Track Models

public struct MusicTrack: Identifiable, Hashable {
    public let id: String
    public let title: String
    public let artist: String
    public let album: String
    public let durationFormatted: String
    public let durationSeconds: Double
    public let coverGradient: [Color]
    public var isFavorite: Bool

    public init(id: String, title: String, artist: String, album: String, durationFormatted: String, durationSeconds: Double, coverGradient: [Color], isFavorite: Bool = false) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.durationFormatted = durationFormatted
        self.durationSeconds = durationSeconds
        self.coverGradient = coverGradient
        self.isFavorite = isFavorite
    }
}

// MARK: - MusicPlayerView

public struct MusicPlayerView: View {
    @State private var selectedTab: MusicBrowserTab = .trending
    @State private var currentTrack: MusicTrack? = nil
    @State private var isPlaying: Bool = false
    @State private var playbackPosition: Double = 42.0
    @State private var isShuffleOn: Bool = false
    @State private var isRepeatOn: Bool = false
    @State private var showExpandedPlayer: Bool = false
    @State private var volumeLevel: Double = 0.8

    enum MusicBrowserTab: String, CaseIterable, Identifiable {
        case trending = "Trending"
        case playlists = "Playlists"
        case library = "Library"
        case uploads = "Uploads"

        var id: String { rawValue }
    }

    @State private var tracks: [MusicTrack] = [
        MusicTrack(id: "t1", title: "Midnight Cyberpunk Synth", artist: "TJ Audio Lab", album: "Neon Dreams 2026", durationFormatted: "3:45", durationSeconds: 225.0, coverGradient: [.purple, .blue], isFavorite: true),
        MusicTrack(id: "t2", title: "Acoustic Sunset Beats", artist: "Luna Horizon", album: "Chill Vibes Vol 2", durationFormatted: "2:50", durationSeconds: 170.0, coverGradient: [.orange, .pink], isFavorite: false),
        MusicTrack(id: "t3", title: "Deep House Resonance", artist: "DJ Apex", album: "Clubland Essentials", durationFormatted: "4:12", durationSeconds: 252.0, coverGradient: [.blue, .teal], isFavorite: true),
        MusicTrack(id: "t4", title: "Lo-Fi Study Session", artist: "Code & Coffee", album: "Focus Hours", durationFormatted: "2:15", durationSeconds: 135.0, coverGradient: [.indigo, .purple], isFavorite: false),
        MusicTrack(id: "t5", title: "Orchestral Theme Main", artist: "TJ Symphony", album: "App Soundtrack", durationFormatted: "5:02", durationSeconds: 302.0, coverGradient: [.red, .orange], isFavorite: false)
    ]

    public init() {
        _currentTrack = State(initialValue: tracks.first)
    }

    public var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                VStack(spacing: 0) {
                    // Filter Tabs
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(MusicBrowserTab.allCases) { tab in
                                Button(action: { selectedTab = tab }) {
                                    Text(tab.rawValue)
                                        .font(.subheadline)
                                        .fontWeight(selectedTab == tab ? .bold : .medium)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                        .background(selectedTab == tab ? Color.purple : Color(.secondarySystemBackground))
                                        .foregroundColor(selectedTab == tab ? .white : .primary)
                                        .cornerRadius(16)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }

                    // Track List
                    List {
                        Section(header: Text(selectedTab.rawValue.uppercased()).font(.caption.bold())) {
                            ForEach(tracks) { track in
                                trackRowView(track)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }

                // Mini Player Bar (when collapsed)
                if let track = currentTrack {
                    miniPlayerBar(track)
                }
            }
            .navigationTitle("Music & Audio")
            .sheet(isPresented: $showExpandedPlayer) {
                if let track = currentTrack {
                    expandedPlayerSheet(track)
                }
            }
        }
    }

    // MARK: - Subviews

    private func trackRowView(_ track: MusicTrack) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 10)
                .fill(LinearGradient(colors: track.coverGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 48, height: 48)
                .overlay(
                    Image(systemName: "music.note")
                        .foregroundColor(.white)
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(track.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(currentTrack?.id == track.id ? .purple : .primary)
                    .lineLimit(1)

                Text("\(track.artist) • \(track.album)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            Text(track.durationFormatted)
                .font(.caption)
                .foregroundColor(.secondary)

            Button(action: {
                if let idx = tracks.firstIndex(where: { $0.id == track.id }) {
                    tracks[idx].isFavorite.toggle()
                }
            }) {
                Image(systemName: track.isFavorite ? "heart.fill" : "heart")
                    .foregroundColor(track.isFavorite ? .red : .gray.opacity(0.4))
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture {
            currentTrack = track
            isPlaying = true
        }
    }

    private func miniPlayerBar(_ track: MusicTrack) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(LinearGradient(colors: track.coverGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 42, height: 42)
                .overlay(Image(systemName: "music.note").font(.caption).foregroundColor(.white))

            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.subheadline.bold())
                    .lineLimit(1)
                Text(track.artist)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Button(action: { isPlaying.toggle() }) {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.title3)
                    .foregroundColor(.primary)
            }

            Button(action: { playNextTrack() }) {
                Image(systemName: "forward.fill")
                    .font(.subheadline)
                    .foregroundColor(.primary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(.systemElevatedBackground))
        .cornerRadius(16)
        .shadow(radius: 8)
        .padding(.horizontal, 12)
        .padding(.bottom, 8)
        .onTapGesture {
            showExpandedPlayer = true
        }
    }

    private func expandedPlayerSheet(_ track: MusicTrack) -> some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                // Album Artwork
                RoundedRectangle(cornerRadius: 24)
                    .fill(LinearGradient(colors: track.coverGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 260, height: 260)
                    .overlay(
                        Image(systemName: "music.quaver")
                            .font(.system(size: 90))
                            .foregroundColor(.white.opacity(0.8))
                    )
                    .shadow(color: track.coverGradient.first?.opacity(0.4) ?? .clear, radius: 20, y: 10)

                // Track Title & Artist
                VStack(spacing: 6) {
                    Text(track.title)
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)

                    Text("\(track.artist) — \(track.album)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)

                // Seek Slider & Timers
                VStack(spacing: 6) {
                    Slider(value: $playbackPosition, in: 0...track.durationSeconds)
                        .accentColor(.purple)

                    HStack {
                        Text(formatSeconds(playbackPosition))
                        Spacer()
                        Text(track.durationFormatted)
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }
                .padding(.horizontal, 28)

                // Controls Row
                HStack(spacing: 32) {
                    Button(action: { isShuffleOn.toggle() }) {
                        Image(systemName: "shuffle")
                            .foregroundColor(isShuffleOn ? .purple : .gray)
                    }

                    Button(action: { playPreviousTrack() }) {
                        Image(systemName: "backward.fill")
                            .font(.title2)
                            .foregroundColor(.primary)
                    }

                    Button(action: { isPlaying.toggle() }) {
                        Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.purple)
                    }

                    Button(action: { playNextTrack() }) {
                        Image(systemName: "forward.fill")
                            .font(.title2)
                            .foregroundColor(.primary)
                    }

                    Button(action: { isRepeatOn.toggle() }) {
                        Image(systemName: "repeat")
                            .foregroundColor(isRepeatOn ? .purple : .gray)
                    }
                }

                // Volume Bar
                HStack(spacing: 12) {
                    Image(systemName: "speaker.fill").font(.caption).foregroundColor(.secondary)
                    Slider(value: $volumeLevel, in: 0...1)
                        .accentColor(.purple)
                    Image(systemName: "speaker.wave.3.fill").font(.caption).foregroundColor(.secondary)
                }
                .padding(.horizontal, 36)

                Spacer()
            }
            .navigationBarItems(trailing: Button("Done") { showExpandedPlayer = false })
        }
    }

    private func playNextTrack() {
        guard let current = currentTrack, let idx = tracks.firstIndex(where: { $0.id == current.id }) else { return }
        let nextIdx = (idx + 1) % tracks.count
        currentTrack = tracks[nextIdx]
        playbackPosition = 0
    }

    private func playPreviousTrack() {
        guard let current = currentTrack, let idx = tracks.firstIndex(where: { $0.id == current.id }) else { return }
        let prevIdx = (idx - 1 + tracks.count) % tracks.count
        currentTrack = tracks[prevIdx]
        playbackPosition = 0
    }

    private func formatSeconds(_ seconds: Double) -> String {
        let mins = Int(seconds) / 60
        let secs = Int(seconds) % 60
        return String(format: "%d:%02d", mins, secs)
    }
}

#Preview {
    MusicPlayerView()
}
