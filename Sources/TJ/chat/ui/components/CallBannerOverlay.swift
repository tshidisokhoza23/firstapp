import SwiftUI

public struct CallBannerOverlay: View {
    @ObservedObject var globalCallState = GlobalCallState.shared
    @StateObject var viewModel = CallViewModel()

    public init() {}

    public var body: some View {
        if globalCallState.isCallActive {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)

                VStack {
                    Spacer()

                    Circle()
                        .fill(Color.gray.opacity(0.3))
                        .frame(width: 120, height: 120)
                        .overlay(
                            Text(String((globalCallState.activeParticipantId ?? "U").prefix(1)).uppercased())
                                .font(.system(size: 48, weight: .bold))
                                .foregroundColor(.white)
                        )

                    Spacer().frame(height: 24)

                    Text(globalCallState.contactNames[globalCallState.activeParticipantId ?? ""] ?? globalCallState.activeParticipantId ?? "Calling...")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)

                    Text(globalCallState.isConnectionEstablished ? formatDuration(globalCallState.callDurationSeconds) : "Calling...")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)

                    Spacer()

                    HStack(spacing: 36) {
                        Button(action: {
                            viewModel.toggleSpeaker()
                        }) {
                            ZStack {
                                Circle()
                                    .fill(viewModel.isSpeakerOn ? Color.white : Color.white.opacity(0.2))
                                    .frame(width: 56, height: 56)
                                Image(systemName: viewModel.isSpeakerOn ? "speaker.wave.3.fill" : "speaker.wave.1.fill")
                                    .foregroundColor(viewModel.isSpeakerOn ? .black : .white)
                            }
                        }

                        Button(action: {
                            viewModel.toggleMic()
                        }) {
                            ZStack {
                                Circle()
                                    .fill(viewModel.isMicMuted ? Color.white : Color.white.opacity(0.2))
                                    .frame(width: 56, height: 56)
                                Image(systemName: viewModel.isMicMuted ? "mic.slash.fill" : "mic.fill")
                                    .foregroundColor(viewModel.isMicMuted ? .black : .white)
                            }
                        }

                        Button(action: {
                            viewModel.endCall()
                        }) {
                            ZStack {
                                Circle()
                                    .fill(Color.red)
                                    .frame(width: 64, height: 64)
                                Image(systemName: "phone.down.fill")
                                    .font(.system(size: 28))
                                    .foregroundColor(.white)
                            }
                        }
                    }
                    .padding(.bottom, 48)
                }
            }
            .transition(.move(edge: .bottom))
            .animation(.easeInOut, value: globalCallState.isCallActive)
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        let mins = seconds / 60
        let secs = seconds % 60
        return String(format: "%02d:%02d", mins, secs)
    }
}
