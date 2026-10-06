import Foundation

class StorageManager: ObservableObject {
    static let shared = StorageManager()

    @Published var cacheSizeBytes: Int64 = 0

    init() {
        calculateCacheSize()
    }

    func calculateCacheSize() {
        DispatchQueue.global(qos: .background).async {
            let fileManager = FileManager.default
            var totalSize: Int64 = 0

            let urls = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)
            if let cacheUrl = urls.first {
                totalSize += self.folderSize(at: cacheUrl)
            }

            let tmpUrl = fileManager.temporaryDirectory
            totalSize += self.folderSize(at: tmpUrl)

            DispatchQueue.main.async {
                self.cacheSizeBytes = totalSize
            }
        }
    }

    func clearCache(completion: @escaping () -> Void) {
        DispatchQueue.global(qos: .background).async {
            let fileManager = FileManager.default
            let urls = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)

            if let cacheUrl = urls.first {
                try? fileManager.removeItem(at: cacheUrl)
                try? fileManager.createDirectory(at: cacheUrl, withIntermediateDirectories: true, attributes: nil)
            }

            let tmpUrl = fileManager.temporaryDirectory
            if let contents = try? fileManager.contentsOfDirectory(at: tmpUrl, includingPropertiesForKeys: nil) {
                for file in contents {
                    try? fileManager.removeItem(at: file)
                }
            }

            DispatchQueue.main.async {
                self.cacheSizeBytes = 0
                completion()
            }
        }
    }

    private func folderSize(at url: URL) -> Int64 {
        let fileManager = FileManager.default
        guard let files = fileManager.subpaths(atPath: url.path) else { return 0 }
        var total: Int64 = 0
        for file in files {
            let fullPath = url.appendingPathComponent(file).path
            if let attrs = try? fileManager.attributesOfItem(atPath: fullPath),
               let size = attrs[.size] as? Int64 {
                total += size
            }
        }
        return total
    }

    func formattedCacheSize() -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useMB, .useGB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: cacheSizeBytes)
    }
}
