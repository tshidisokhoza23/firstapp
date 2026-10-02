import SwiftUI
import UIKit
import WebRTC

public struct CallBannerOverlay: View {
    @ObservedObject var globalCallState = GlobalCallState.shared
    @ObservedObject var media = CallMediaState.shared
    @StateObject var viewModel = CallViewModel()

    public init() {}

    public var body: some View {
        if globalCallState.isCallActive {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                RemoteCallVideo(track: media.remoteVideo)
                    .ignoresSafeArea()

                VStack {
                    Spacer()

                    if media.remoteVideo == nil {
                    Circle()
                        .fill(Color.gray.opacity(0.3))
                        .frame(width: 120, height: 120)
                        .overlay(
                            Text(String((globalCallState.activeParticipantId ?? "").prefix(1)).uppercased())
                                .font(.system(size: 48, weight: .bold))
                                .foregroundColor(.white)
                        )
                    }

                    Spacer().frame(height: 24)

                    Text(realPersonLabel(
                        names: [globalCallState.contactNames[globalCallState.activeParticipantId ?? ""]],
                        phone: globalCallState.contactPhones[globalCallState.activeParticipantId ?? ""],
                        ids: [globalCallState.activeParticipantId ?? ""]
                    ))
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

struct RemoteCallVideo: UIViewRepresentable {
    let track: RTCVideoTrack?

    func makeUIView(context: Context) -> RTCMTLVideoView {
        let view = RTCMTLVideoView(frame: .zero)
        view.videoContentMode = .scaleAspectFill
        view.backgroundColor = .black
        return view
    }

    func updateUIView(_ uiView: RTCMTLVideoView, context: Context) {
        if context.coordinator.track !== track {
            context.coordinator.track?.remove(uiView)
            track?.add(uiView)
            context.coordinator.track = track
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator {
        var track: RTCVideoTrack?
    }
}
