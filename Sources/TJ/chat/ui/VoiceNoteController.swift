import AVFoundation
import Foundation

/// Records an AAC `.m4a` voice note the same way Android does, then holds it for review.
@MainActor
final class VoiceNoteController: NSObject, ObservableObject, AVAudioPlayerDelegate {
    @Published var isRecording = false
    @Published var isReviewing = false
    @Published var isPlayingReview = false
    @Published var duration = 0
    @Published var error: String?

    private var recorder: AVAudioRecorder?
    private var player: AVAudioPlayer?
    private var fileURL: URL?
    private var timer: Timer?

    func toggle() {
        if isRecording {
            stop()
        } else {
            start()
        }
    }

    func start() {
        error = nil
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
        } catch {
            self.error = "Could not start the microphone"
            return
        }
        session.requestRecordPermission { [weak self] granted in
            Task { @MainActor in
                guard let self else { return }
                guard granted else {
                    self.error = "Allow microphone access to send voice notes"
                    return
                }
                self.beginRecording()
            }
        }
    }

    private func beginRecording() {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("VOICE_\(Int(Date().timeIntervalSince1970 * 1000)).m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
        ]
        do {
            let recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder.record()
            self.recorder = recorder
            fileURL = url
            duration = 0
            isRecording = true
            isReviewing = false
            timer?.invalidate()
            timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
                Task { @MainActor in self?.duration += 1 }
            }
        } catch {
            self.error = "Could not record a voice note"
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        recorder?.stop()
        recorder = nil
        isRecording = false
        isReviewing = fileURL != nil
    }

    func discard() {
        timer?.invalidate()
        timer = nil
        recorder?.stop()
        recorder = nil
        player?.stop()
        player = nil
        if let fileURL {
            try? FileManager.default.removeItem(at: fileURL)
        }
        fileURL = nil
        isRecording = false
        isReviewing = false
        isPlayingReview = false
        duration = 0
    }

    func toggleReviewPlayback() {
        guard let fileURL else { return }
        if isPlayingReview {
            player?.pause()
            isPlayingReview = false
            return
        }
        if player == nil {
            player = try? AVAudioPlayer(contentsOf: fileURL)
            player?.delegate = self
        }
        player?.play()
        isPlayingReview = true
    }

    /// Hands the recorded file to the uploader and clears the review state.
    func takeForSend() -> (url: URL, seconds: Int)? {
        guard let fileURL, isReviewing else { return nil }
        let seconds = duration
        player?.stop()
        player = nil
        isPlayingReview = false
        isReviewing = false
        duration = 0
        self.fileURL = nil
        return (fileURL, seconds)
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.isPlayingReview = false
        }
    }
}
