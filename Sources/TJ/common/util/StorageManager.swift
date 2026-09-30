import Foundation

public final class MediaCacheManager {

    public static func getTempMediaDir() -> URL {
        let cachesDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let dir = cachesDir.appendingPathComponent("media_temp", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    public static func createTempFile(prefix: String = "media_", suffix: String = ".tmp") -> URL {
        let dir = getTempMediaDir()
        let filename = "\(prefix)\(UUID().uuidString)\(suffix)"
        return dir.appendingPathComponent(filename)
    }

    public static func cleanExpiredCache(maxAgeSeconds: TimeInterval = 24 * 60 * 60) {
        let dir = getTempMediaDir()
        guard let files = try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.contentModificationDateKey]) else {
            return
        }

        let cutoffDate = Date().addingTimeInterval(-maxAgeSeconds)
        for file in files {
            if let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
               let modDate = attributes[.modificationDate] as? Date,
               modDate < cutoffDate {
                try? FileManager.default.removeItem(at: file)
            }
        }
    }
}

public final class UserScopedStorage {

    private let userId: String

    public init(userId: String) {
        self.userId = userId
    }

    public func getAttachmentsDir() -> URL {
        let docsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = docsDir.appendingPathComponent("attachments/\(userId)", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    public func getTempDir() -> URL {
        let cachesDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let dir = cachesDir.appendingPathComponent("temp/\(userId)", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    public func clearUserData() {
        try? FileManager.default.removeItem(at: getAttachmentsDir())
        try? FileManager.default.removeItem(at: getTempDir())
    }
}
