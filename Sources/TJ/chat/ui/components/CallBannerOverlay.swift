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
            let isVideo = globalCallState.activeCallType == .video
            let connected = globalCallState.isConnectionEstablished
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                if isVideo {
                    CallVideoSurface(track: localIsFull || !connected ? media.localVideo : media.remoteVideo)
                        .ignoresSafeArea()
                    if connected {
                        CallVideoSurface(track: localIsFull ? media.remoteVideo : media.localVideo)
                            .frame(width: 110, height: 160)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.4), lineWidth: 1))
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                            .padding(.top, 72)
                            .padding(.trailing, 16)
                            .onTapGesture { localIsFull.toggle() }
                    }
                }

                VStack {
                    if isVideo {
                        Text(statusText)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 4)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Capsule())
                            .padding(.top, 24)
                        if !connected {
                            Text(personName)
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.top, 8)
                        }
                    }
                    Spacer()
                    if !isVideo {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: 120, height: 120)
                            .overlay(
                                Text(String(personName.prefix(1)).uppercased())
                                    .font(.system(size: 48, weight: .bold))
                                    .foregroundColor(.white)
                            )
                        Text(personName)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.top, 16)
                        Text(statusText)
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                    }
                    Spacer()
                    HStack(spacing: 28) {
                        Button(action: { viewModel.toggleSpeaker() }) {
                            ZStack {
                                Circle()
                                    .fill(viewModel.isSpeakerOn ? Color.white : Color.white.opacity(0.2))
                                    .frame(width: 56, height: 56)
                                Image(systemName: viewModel.isSpeakerOn ? "speaker.wave.3.fill" : "speaker.wave.1.fill")
                                    .foregroundColor(viewModel.isSpeakerOn ? .black : .white)
                            }
                        }
                        if isVideo {
                            Button(action: { viewModel.switchCamera() }) {
                                ZStack {
                                    Circle().fill(Color.white.opacity(0.2)).frame(width: 56, height: 56)
                                    Image(systemName: "camera.rotate.fill").foregroundColor(.white)
                                }
                            }
                        }
                        Button(action: { viewModel.toggleMic() }) {
                            ZStack {
                                Circle()
                                    .fill(viewModel.isMicMuted ? Color.white : Color.white.opacity(0.2))
                                    .frame(width: 56, height: 56)
                                Image(systemName: viewModel.isMicMuted ? "mic.slash.fill" : "mic.fill")
                                    .foregroundColor(viewModel.isMicMuted ? .black : .white)
                            }
                        }
                        Button(action: { viewModel.endCall() }) {
                            ZStack {
                                Circle().fill(Color.red).frame(width: 64, height: 64)
                                Image(systemName: "phone.down.fill").font(.system(size: 28)).foregroundColor(.white)
                            }
                        }
                    }
                    .padding(.bottom, 48)
                }
            }
            .onAppear { viewModel.syncSpeaker(globalCallState.activeCallType == .video) }
            .transition(.move(edge: .bottom))
            .animation(.easeInOut, value: globalCallState.isCallActive)
        }
    }

    @State private var localIsFull = false

    private var personName: String {
        realPersonLabel(
            names: [globalCallState.contactNames[globalCallState.activeParticipantId ?? ""]],
            phone: globalCallState.contactPhones[globalCallState.activeParticipantId ?? ""],
            ids: [globalCallState.activeParticipantId ?? ""]
        )
    }

    private var statusText: String {
        if globalCallState.isConnectionEstablished { return formatDuration(globalCallState.callDurationSeconds) }
        if globalCallState.isRinging { return "Ringing..." }
        if globalCallState.isAnswered { return "Connecting..." }
        return "Calling..."
    }

    private func formatDuration(_ seconds: Int) -> String {
        let mins = seconds / 60
        let secs = seconds % 60
        return String(format: "%02d:%02d", mins, secs)
    }
}

struct CallVideoSurface: UIViewRepresentable {
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
