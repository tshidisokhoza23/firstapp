import SwiftUI

struct FullMusicPlayerView: View {
    @EnvironmentObject var streamViewModel: StreamViewModel
    @Environment(\.dismiss) var dismiss
    @State private var progress: Double = 0.35

    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.down")
                        .font(.title2)
                        .foregroundColor(.primary)
                }
                Spacer()
                Text("Now Playing")
                    .font(.headline)
                Spacer()
                Button(action: {}) {
                    Image(systemName: "ellipsis")
                        .font(.title2)
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal)
            .padding(.top)

            Spacer()

            // Album Art
            RoundedRectangle(cornerRadius: 24)
                .fill(LinearGradient(colors: [AppColors.brandPrimary, AppColors.darkBackgroundTop], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 280, height: 280)
                .overlay(
                    Image(systemName: "music.note")
                        .font(.system(size: 80))
                        .foregroundColor(.white.opacity(0.8))
                )
                .shadow(radius: 12)

            Spacer()

            if let song = streamViewModel.currentSong {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(song.title)
                            .font(.title2)
                            .fontWeight(.bold)
                        Spacer()
                        Image(systemName: "heart")
                            .font(.title2)
                    }
                    Text("\(song.artist) — \(song.album)")
                        .font(.subheadline)
                        .foregroundColor(AppColors.greyMedium)
                }
                .padding(.horizontal, 24)
            }

            // Progress Bar
            VStack(spacing: 6) {
                Slider(value: $progress)
                    .tint(AppColors.brandPrimary)
                HStack {
                    Text("1:25")
                        .font(.caption)
                        .foregroundColor(AppColors.greyMedium)
                    Spacer()
                    Text("3:50")
                        .font(.caption)
                        .foregroundColor(AppColors.greyMedium)
                }
            }
            .padding(.horizontal, 24)

            // Player Controls
            HStack(spacing: 40) {
                Button(action: {}) {
                    Image(systemName: "shuffle")
                        .font(.title2)
                        .foregroundColor(AppColors.greyMedium)
                }
                Button(action: {}) {
                    Image(systemName: "backward.fill")
                        .font(.title)
                        .foregroundColor(.primary)
                }
                Button(action: { streamViewModel.togglePlayPause() }) {
                    Image(systemName: streamViewModel.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(AppColors.brandPrimary)
                }
                Button(action: {}) {
                    Image(systemName: "forward.fill")
                        .font(.title)
                        .foregroundColor(.primary)
                }
                Button(action: {}) {
                    Image(systemName: "repeat")
                        .font(.title2)
                        .foregroundColor(AppColors.greyMedium)
                }
            }
            .padding(.bottom, 40)
        }
    }
}

struct StreamView: View {
    @EnvironmentObject var streamViewModel: StreamViewModel
    @State private var showingFullPlayer: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Stream Music")
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                Button(action: {}) {
                    Image(systemName: "magnifyingglass")
                        .font(.title3)
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)

            // Trending Music List
            List(streamViewModel.songs) { song in
                Button(action: {
                    streamViewModel.playSong(song)
                }) {
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(AppColors.brandPrimary.opacity(0.15))
                            .frame(width: 50, height: 50)
                            .overlay(
                                Image(systemName: "music.note")
                                    .font(.title2)
                                    .foregroundColor(AppColors.brandPrimary)
                            )

                        VStack(alignment: .leading, spacing: 4) {
                            Text(song.title)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.primary)
                            Text("\(song.artist) • \(song.album)")
                                .font(.system(size: 13))
                                .foregroundColor(AppColors.greyMedium)
                        }

                        Spacer()

                        Text(song.duration)
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.greyMedium)
                    }
                    .padding(.vertical, 4)
                }
            }
            .listStyle(PlainListStyle())

            // Mini Player Bar
            if let song = streamViewModel.currentSong {
                Button(action: { showingFullPlayer = true }) {
                    HStack(spacing: 12) {
                        Image(systemName: "music.note")
                            .font(.title3)
                            .foregroundColor(AppColors.brandAccent)
                            .frame(width: 40, height: 40)
                            .background(AppColors.brandPrimary)
                            .cornerRadius(8)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(song.title)
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.primary)
                            Text(song.artist)
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.greyMedium)
                        }

                        Spacer()

                        Button(action: {
                            streamViewModel.togglePlayPause()
                        }) {
                            Image(systemName: streamViewModel.isPlaying ? "pause.fill" : "play.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 10)
                    .background(Color(UIColor.secondarySystemBackground))
                    .padding(.bottom, 60)
                }
            }
        }
        .fullScreenCover(isPresented: $showingFullPlayer) {
            FullMusicPlayerView()
        }
    }
}
