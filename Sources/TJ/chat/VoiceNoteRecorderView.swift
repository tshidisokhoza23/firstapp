import SwiftUI

struct VoiceNoteRecorderView: View {
    var onCancel: () -> Void
    var onSendAudio: () -> Void

    @State private var recordingTimeSeconds: Int = 0
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onCancel) {
                Image(systemName: "trash.fill")
                    .font(.title2)
                    .foregroundColor(.red)
            }

            HStack(spacing: 8) {
                Circle()
                    .fill(Color.red)
                    .frame(width: 10, height: 10)

                Text(formatTime(recordingTimeSeconds))
                    .font(.system(size: 15, weight: .bold, design: .monospaced))
                    .foregroundColor(.primary)
            }

            Spacer()

            Button(action: onSendAudio) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 36))
                    .foregroundColor(AppColors.brandPrimary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(UIColor.tertiarySystemBackground))
        .cornerRadius(24)
        .onReceive(timer) { _ in
            recordingTimeSeconds += 1
        }
    }

    private func formatTime(_ sec: Int) -> String {
        let mins = sec / 60
        let secs = sec % 60
        return String(format: "%02d:%02d", mins, secs)
    }
}
