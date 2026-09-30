import SwiftUI

struct PlaylistView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var streamViewModel: StreamViewModel

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(LinearGradient(colors: [AppColors.brandPrimary, AppColors.brandAccent], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(height: 220)
                        .overlay(
                            VStack(spacing: 8) {
                                Image(systemName: "music.note.list")
                                    .font(.system(size: 60))
                                    .foregroundColor(.white)
                                Text("Top Trending Hits")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                            }
                        )

                    HStack(spacing: 16) {
                        Button(action: {
                            if let first = streamViewModel.songs.first {
                                streamViewModel.playSong(first)
                            }
                        }) {
                            Label("Play All", systemImage: "play.fill")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(AppColors.brandPrimary)
                                .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)

                    List(streamViewModel.songs) { song in
                        Button(action: { streamViewModel.playSong(song) }) {
                            HStack {
                                Text(song.title)
                                    .font(.headline)
                                Spacer()
                                Text(song.duration)
                                    .font(.caption)
                                    .foregroundColor(AppColors.greyMedium)
                            }
                        }
                    }
                    .frame(height: 250)
                }
            }
            .navigationTitle("Playlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
