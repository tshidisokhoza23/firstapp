import Foundation
import AVFoundation

/// Manages call audio feedback tones (ringback and busy tones) using AVAudioPlayer and AVAudioSession.
public final class CallTonePlayer: NSObject, @unchecked Sendable {
    private var audioPlayer: AVAudioPlayer?
    private var timer: Timer?

    public static let shared = CallTonePlayer()

    public override init() {
        super.init()
    }

    deinit {
        stop()
    }

    // MARK: - Audio Session Management

    private func requestFocus() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .defaultToSpeaker])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            print("[CallTonePlayer] Failed to set audio session active: \(error.localizedDescription)")
        }
    }

    private func abandonFocus() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("[CallTonePlayer] Failed to deactivate audio session: \(error.localizedDescription)")
        }
    }

    // MARK: - Tone Playback Actions

    /// Plays continuous outgoing ringback tone until stopped.
    public func playOutgoingRingtone() {
        stop()
        requestFocus()

        guard let toneURL = generateToneAudioURL(frequency: 440.0, pulseDuration: 1.0, totalDuration: 2.0) else {
            playSyntheticBeep()
            return
        }

        do {
            audioPlayer = try AVAudioPlayer(contentsOf: toneURL)
            audioPlayer?.numberOfLoops = -1
            audioPlayer?.volume = 0.8
            audioPlayer?.prepareToPlay()
            audioPlayer?.play()
        } catch {
            print("[CallTonePlayer] Error playing outgoing ringtone: \(error.localizedDescription)")
        }
    }

    /// Plays busy tone and automatically stops after 2.5 seconds.
    public func playBusyTone() {
        stop()
        requestFocus()

        guard let toneURL = generateToneAudioURL(frequency: 480.0, pulseDuration: 0.5, totalDuration: 0.5) else {
            return
        }

        do {
            audioPlayer = try AVAudioPlayer(contentsOf: toneURL)
            audioPlayer?.numberOfLoops = 4
            audioPlayer?.volume = 0.8
            audioPlayer?.prepareToPlay()
            audioPlayer?.play()

            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { [weak self] in
                self?.stop()
            }
        } catch {
            print("[CallTonePlayer] Error playing busy tone: \(error.localizedDescription)")
        }
    }

    /// Stops all tone playback and releases audio session focus.
    public func stop() {
        timer?.invalidate()
        timer = nil

        if let player = audioPlayer, player.isPlaying {
            player.stop()
        }
        audioPlayer = nil
        abandonFocus()
    }

    // MARK: - Tone Generator Helper

    private func playSyntheticBeep() {
        AudioServicesPlaySystemSound(1052) // Standard call ringing sound
    }

    private func generateToneAudioURL(frequency: Float, pulseDuration: Float, totalDuration: Float) -> URL? {
        let sampleRate = 44100.0
        let numSamples = Int(sampleRate * Double(totalDuration))
        let pulseSamples = Int(sampleRate * Double(pulseDuration))

        var samples = [Float](repeating: 0, count: numSamples)
        for i in 0..<pulseSamples {
            let time = Double(i) / sampleRate
            samples[i] = Float(sin(2.0 * .pi * Double(frequency) * time))
        }

        let format = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: sampleRate, channels: 1, interleaved: false)!
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(numSamples)) else { return nil }
        buffer.frameLength = AVAudioFrameCount(numSamples)

        let channels = buffer.floatChannelData!
        for i in 0..<numSamples {
            channels[0][i] = samples[i]
        }

        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("tone_\(Int(frequency)).wav")
        do {
            let audioFile = try AVAudioFile(forWriting: tempURL, settings: format.settings)
            try audioFile.write(from: buffer)
            return tempURL
        } catch {
            return nil
        }
    }
}
