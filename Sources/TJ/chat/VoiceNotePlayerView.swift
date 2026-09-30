import SwiftUI

struct VoiceNotePlayerView: View {
    let duration: String
    @State private var isPlaying: Bool = false
    @State private var progress: Double = 0.3

    var body: some View {
        HStack(spacing: 12) {
            Button(action: { isPlaying.toggle() }) {
                Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(AppColors.brandPrimary)
            }

            VStack(alignment: .leading, spacing: 4) {
                // Audio Waveform Bar Visualizer
                HStack(spacing: 3) {
                    ForEach(0..<18, id: \.self) { i in
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Double(i) / 18.0 <= progress ? AppColors.brandPrimary : AppColors.greyMedium.opacity(0.4))
                            .frame(width: 3, height: CGFloat([12, 20, 8, 24, 16, 28, 14, 22, 10, 26, 18, 12, 20, 8, 24, 16, 28, 14][i]))
                    }
                }

                Text(duration)
                    .font(.caption2)
                    .foregroundColor(AppColors.greyMedium)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
    }
}
