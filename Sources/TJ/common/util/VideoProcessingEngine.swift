import Foundation
import AVFoundation

public enum VideoUploadProfile {
    case story
    case short
    case general

    public var maxDurationSeconds: Double {
        switch self {
        case .story: return 30.0
        case .short: return 60.0
        case .general: return 180.0
        }
    }

    public var maxBytes: Int64 {
        switch self {
        case .story: return 15 * 1024 * 1024
        case .short: return 30 * 1024 * 1024
        case .general: return 40 * 1024 * 1024
        }
    }

    public var presetName: String {
        return AVAssetExportPreset1280x720
    }
}

public enum VideoProcessingError: LocalizedError {
    case durationExceeded(Double, Double)
    case fileSizeExceeded(Int64)
    case exportFailed(String)

    public var errorDescription: String? {
        switch self {
        case .durationExceeded(let current, let max):
            return "Video is \(Int(current))s. Max allowed is \(Int(max))s."
        case .fileSizeExceeded(let maxBytes):
            let mb = maxBytes / (1024 * 1024)
            return "Compressed video exceeds max allowed size (\(mb) MB)."
        case .exportFailed(let reason):
            return "Export failed: \(reason)"
        }
    }
}

public final class VideoProcessingEngine {

    public static func trimVideo(
        inputURL: URL,
        outputURL: URL,
        startMs: Int64,
        endMs: Int64
    ) async throws -> Bool {
        let asset = AVAsset(url: inputURL)

        guard let exportSession = AVAssetExportSession(
            asset: asset,
            presetName: AVAssetExportPresetHighestQuality
        ) else {
            throw VideoProcessingError.exportFailed("Could not create export session")
        }

        let startTime = CMTime(value: startMs, timescale: 1000)
        let endTime = CMTime(value: endMs, timescale: 1000)
        let timeRange = CMTimeRange(start: startTime, end: endTime)

        exportSession.outputURL = outputURL
        exportSession.outputFileType = .mp4
        exportSession.timeRange = timeRange

        return await withCheckedContinuation { continuation in
            exportSession.exportAsynchronously {
                switch exportSession.status {
                case .completed:
                    continuation.resume(returning: true)
                default:
                    let errorMsg = exportSession.error?.localizedDescription ?? "Unknown export error"
                    print("[VideoProcessingEngine] Export status error: \(errorMsg)")
                    continuation.resume(returning: false)
                }
            }
        }
    }

    public static func exportForUpload(
        inputURL: URL,
        profile: VideoUploadProfile
    ) async throws -> URL {
        let asset = AVAsset(url: inputURL)
        let duration = try await asset.load(.duration)
        let durationSeconds = CMTimeGetSeconds(duration)

        if durationSeconds > profile.maxDurationSeconds + 0.75 {
            throw VideoProcessingError.durationExceeded(durationSeconds, profile.maxDurationSeconds)
        }

        let tempDir = FileManager.default.temporaryDirectory.appendingPathComponent("media_temp", isDirectory: true)
        try? FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
        let outputURL = tempDir.appendingPathComponent("compressed_\(Int(Date().timeIntervalSince1970)).mp4")

        let startMs: Int64 = 0
        let endMs: Int64 = Int64(durationSeconds * 1000.0)

        let success = try await trimVideo(inputURL: inputURL, outputURL: outputURL, startMs: startMs, endMs: endMs)
        guard success else {
            throw VideoProcessingError.exportFailed("Could not compress video")
        }

        let fileSize = (try? FileManager.default.attributesOfItem(atPath: outputURL.path)[.size] as? NSNumber)?.int64Value ?? 0
        if fileSize > profile.maxBytes {
            try? FileManager.default.removeItem(at: outputURL)
            throw VideoProcessingError.fileSizeExceeded(profile.maxBytes)
        }

        return outputURL
    }

    public static func formatDurationMs(_ ms: Int64) -> String {
        let totalSeconds = max(ms / 1000, 0)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}
