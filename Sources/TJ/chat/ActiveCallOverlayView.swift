import SwiftUI

struct ActiveCallOverlayView: View {
    let contactName: String
    let isVideoCall: Bool
    @Environment(\.dismiss) var dismiss
    @State private var isMuted: Bool = false
    @State private var isSpeakerOn: Bool = true
    @State private var callDurationSeconds: Int = 0
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            LinearGradient(colors: [AppColors.brandPrimary, AppColors.darkBackgroundTop], startPoint: .top, endPoint: .bottom)
                .edgesIgnoringSafeArea(.all)

            VStack(spacing: 32) {
                Spacer()

                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 140, height: 140)
                    Image(systemName: "person.fill")
                        .font(.system(size: 70))
                        .foregroundColor(.white)
                }

                VStack(spacing: 8) {
                    Text(contactName)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Text("\(isVideoCall ? "Video" : "Voice") Call • \(formatDuration(callDurationSeconds))")
                        .font(.headline)
                        .foregroundColor(AppColors.brandAccent)
                }

                Spacer()

                // Call Controls
                HStack(spacing: 40) {
                    Button(action: { isMuted.toggle() }) {
                        Circle()
                            .fill(isMuted ? Color.white : Color.white.opacity(0.2))
                            .frame(width: 60, height: 60)
                            .overlay(
                                Image(systemName: isMuted ? "mic.slash.fill" : "mic.fill")
                                    .font(.title2)
                                    .foregroundColor(isMuted ? AppColors.brandPrimary : .white)
                            )
                    }

                    Button(action: { dismiss() }) {
                        Circle()
                            .fill(Color.red)
                            .frame(width: 72, height: 72)
                            .overlay(
                                Image(systemName: "phone.down.fill")
                                    .font(.title)
                                    .foregroundColor(.white)
                            )
                    }

                    Button(action: { isSpeakerOn.toggle() }) {
                        Circle()
                            .fill(isSpeakerOn ? Color.white : Color.white.opacity(0.2))
                            .frame(width: 60, height: 60)
                            .overlay(
                                Image(systemName: isSpeakerOn ? "speaker.wave.3.fill" : "speaker.slash.fill")
                                    .font(.title2)
                                    .foregroundColor(isSpeakerOn ? AppColors.brandPrimary : .white)
                            )
                    }
                }
                .padding(.bottom, 60)
            }
        }
        .onReceive(timer) { _ in
            callDurationSeconds += 1
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        let mins = seconds / 60
        let secs = seconds % 60
        return String(format: "%02d:%02d", mins, secs)
    }
}
