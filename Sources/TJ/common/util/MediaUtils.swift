import Foundation
import UIKit
import AVFoundation

public final class MediaUtils {

    private static let thumbnailCache = NSCache<NSString, UIImage>()

    public static func compressImage(imageURL: URL) -> URL? {
        do {
            let data = try Data(contentsOf: imageURL)
            guard let image = UIImage(data: data) else { return nil }

            let maxDimension: CGFloat = 1920.0
            let originalSize = image.size
            var targetSize = originalSize

            let maxSide = max(originalSize.width, originalSize.height)
            if maxSide > maxDimension {
                let scale = maxDimension / maxSide
                targetSize = CGSize(width: originalSize.width * scale, height: originalSize.height * scale)
            }

            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
            image.draw(in: CGRect(origin: .zero, size: targetSize))
            let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            guard let finalImage = resizedImage,
                  let compressedData = finalImage.jpegData(compressionQuality: 0.8) else {
                return nil
            }

            let outFile = FileManager.default.temporaryDirectory.appendingPathComponent("mingle_\(UUID().uuidString).jpg")
            try compressedData.write(to: outFile)
            return outFile
        } catch {
            print("[MediaUtils] Compress image failed: \(error.localizedDescription)")
            return nil
        }
    }

    public static func processImageForAvatar(imageURL: URL) -> URL? {
        do {
            let data = try Data(contentsOf: imageURL)
            guard let originalImage = UIImage(data: data) else { return nil }

            let size = min(originalImage.size.width, originalImage.size.height)
            let x = (originalImage.size.width - size) / 2.0
            let y = (originalImage.size.height - size) / 2.0

            let cropRect = CGRect(x: x, y: y, width: size, height: size)
            guard let cgImage = originalImage.cgImage?.cropping(to: cropRect) else { return nil }
            let croppedImage = UIImage(cgImage: cgImage, scale: originalImage.scale, orientation: originalImage.imageOrientation)

            let targetSize = CGSize(width: 512, height: 512)
            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
            croppedImage.draw(in: CGRect(origin: .zero, size: targetSize))
            let avatarImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            guard let finalAvatar = avatarImage,
                  let compressedData = finalAvatar.jpegData(compressionQuality: 0.85) else {
                return nil
            }

            let outFile = FileManager.default.temporaryDirectory.appendingPathComponent("avatar_\(Int(Date().timeIntervalSince1970)).jpg")
            try compressedData.write(to: outFile)
            return outFile
        } catch {
            print("[MediaUtils] Avatar processing failed: \(error.localizedDescription)")
            return nil
        }
    }

    public static func createImageFileURL() -> URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent("CAMERA_\(Int(Date().timeIntervalSince1970)).jpg")
    }

    public static func formatUrl(_ urlString: String?, baseUrl: String = "https://firstapp.com") -> String? {
        guard let urlString = urlString, !urlString.isEmpty else { return nil }

        var cleanBase = baseUrl
        if cleanBase.hasSuffix("/api/v1/") {
            cleanBase = String(cleanBase.dropLast(8))
        } else if cleanBase.hasSuffix("/api/v1") {
            cleanBase = String(cleanBase.dropLast(7))
        } else if cleanBase.hasSuffix("/") {
            cleanBase = String(cleanBase.dropLast(1))
        }

        if urlString.hasPrefix("http:") || urlString.hasPrefix("https:") || urlString.hasPrefix("data:") || urlString.hasPrefix("file:") {
            var formatted = urlString
            if formatted.contains("://localhost") || formatted.contains("://127.0.0.1") {
                formatted = formatted
                    .replacingOccurrences(of: "http://127.0.0.1:8000", with: cleanBase)
                    .replacingOccurrences(of: "http://localhost:8000", with: cleanBase)
                    .replacingOccurrences(of: "http://127.0.0.1", with: cleanBase)
                    .replacingOccurrences(of: "http://localhost", with: cleanBase)
            }
            if formatted.hasPrefix("http://") && cleanBase.hasPrefix("https://") {
                formatted = formatted.replacingOccurrences(of: "http://", with: "https://")
            }
            return formatted
        }

        var cleanUrl = urlString.hasPrefix("/") ? String(urlString.dropFirst()) : urlString
        if !cleanUrl.hasPrefix("uploads/") {
            cleanUrl = "uploads/\(cleanUrl)"
        }

        return "\(cleanBase)/\(cleanUrl)"
    }

    public static func isVideo(_ urlString: String?) -> Bool {
        guard let s = urlString?.lowercased() else { return false }
        return s.contains(".mp4") || s.contains(".mov") || s.contains(".avi") ||
               s.contains(".webm") || s.contains(".mkv") || s.contains(".3gp") ||
               s.contains("video") || s.contains("/shorts/") || s.contains(".tmp")
    }

    public static func getVideoThumbnail(url: URL) async -> UIImage? {
        let asset = AVAsset(url: url)
        let imageGenerator = AVAssetImageGenerator(asset: asset)
        imageGenerator.appliesPreferredTrackTransform = true

        let time = CMTime(seconds: 1.0, preferredTimescale: 600)
        return await withCheckedContinuation { continuation in
            imageGenerator.generateCGImagesAsynchronously(forTimes: [NSValue(time: time)]) { _, cgImage, _, _, error in
                if let cgImage = cgImage {
                    continuation.resume(returning: UIImage(cgImage: cgImage))
                } else {
                    continuation.resume(returning: nil)
                }
            }
        }
    }

    public static func getVideoDuration(url: URL) async -> Double {
        let asset = AVAsset(url: url)
        do {
            let duration = try await asset.load(.duration)
            return CMTimeGetSeconds(duration)
        } catch {
            return 0.0
        }
    }

    // MARK: - Thumbnail Extraction

    public static func extractThumbnails(
        videoURL: URL,
        count: Int = 8,
        thumbWidth: Int = 120,
        thumbHeight: Int = 120
    ) async -> [UIImage] {
        let cacheKeyBase = "\(videoURL.absoluteString)_\(count)_\(thumbWidth)x\(thumbHeight)" as NSString
        var cachedImages: [UIImage] = []
        var allCached = true

        for i in 0..<count {
            let key = "\(cacheKeyBase)_\(i)" as NSString
            if let cached = thumbnailCache.object(forKey: key) {
                cachedImages.append(cached)
            } else {
                allCached = false
                break
            }
        }

        if allCached && cachedImages.count == count {
            return cachedImages
        }

        let asset = AVAsset(url: videoURL)
        guard let duration = try? await asset.load(.duration),
              CMTimeGetSeconds(duration) > 0 else {
            return []
        }

        let totalSeconds = CMTimeGetSeconds(duration)
        let stepSeconds = totalSeconds / Double(max(count, 1))

        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: thumbWidth, height: thumbHeight)

        var times: [NSValue] = []
        for i in 0..<count {
            let time = CMTime(seconds: Double(i) * stepSeconds, preferredTimescale: 600)
            times.append(NSValue(time: time))
        }

        var extracted: [UIImage] = []
        for (index, timeValue) in times.enumerated() {
            let key = "\(cacheKeyBase)_\(index)" as NSString
            if let cached = thumbnailCache.object(forKey: key) {
                extracted.append(cached)
                continue
            }

            let time = timeValue.timeValue
            if let cgImage = try? generator.copyCGImage(at: time, actualTime: nil) {
                let img = UIImage(cgImage: cgImage)
                thumbnailCache.setObject(img, forKey: key)
                extracted.append(img)
            }
        }

        return extracted
    }

    public static func calculateEstimatedSizeBytes(startMs: Int64, endMs: Int64, totalBitrateBps: Int64) -> Float {
        let durationSec = max(Float(endMs - startMs) / 1000.0, 0)
        let bytes = (durationSec * Float(totalBitrateBps)) / 8.0
        return bytes / (1024.0 * 1024.0)
    }

    public static func estimateTrimmedSizeMb(startMs: Int64, endMs: Int64, totalDurationMs: Int64, totalFileSizeBytes: Int64) -> Float {
        guard totalDurationMs > 0, totalFileSizeBytes > 0 else { return 0.0 }
        let selectedMs = max(endMs - startMs, 0)
        let ratio = Float(selectedMs) / Float(totalDurationMs)
        let estimatedBytes = Float(totalFileSizeBytes) * ratio
        return estimatedBytes / (1024.0 * 1024.0)
    }
}
