import SwiftUI
import AVFoundation
import AVKit

enum AuthenticatedMedia {
    static func localFile(for remote: URL) async -> URL? {
        if remote.isFileURL { return remote }
        var hash: UInt64 = 1469598103934665603
        for byte in remote.absoluteString.utf8 {
            hash ^= UInt64(byte)
            hash &*= 1099511628211
        }
        let leaf = remote.lastPathComponent.replacingOccurrences(of: "/", with: "_")
        let ext = remote.pathExtension.isEmpty ? "mp4" : remote.pathExtension
        let dest = FileManager.default.temporaryDirectory.appendingPathComponent("tj-\(String(hash, radix: 16))-\(leaf).\(ext)")
        if FileManager.default.fileExists(atPath: dest.path),
           (try? dest.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0) ?? 0 > 0 {
            return dest
        }
        var request = URLRequest(url: remote)
        AuthSession.apply(to: &request)
        request.setValue(nil, forHTTPHeaderField: "Content-Type")
        do {
            let (tmp, response) = try await URLSession.shared.download(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            guard (200...299).contains(code) else { return nil }
            try? FileManager.default.removeItem(at: dest)
            try FileManager.default.moveItem(at: tmp, to: dest)
            return dest
        } catch {
            return nil
        }
    }
}

struct InAppMediaView: View {
    let urlString: String
    var onClose: () -> Void
    @State private var local: URL?
    @State private var failed = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                if let local {
                    if Self.isVideo(local) {
                        VideoPlayer(player: AVPlayer(url: local))
                            .ignoresSafeArea()
                    } else if let image = UIImage(contentsOfFile: local.path) {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                    } else {
                        Text("Could not open this media").foregroundColor(.white)
                    }
                } else if failed {
                    Text("Could not open this media").foregroundColor(.white)
                } else {
                    ProgressView().tint(.white)
                }
            }
            .navigationTitle("Media")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onClose)
                }
            }
        }
        .task { await load() }
    }

    private func load() async {
        guard let remote = FeedPost.resolve(urlString) ?? URL(string: urlString) else {
            failed = true
            return
        }
        if let file = await AuthenticatedMedia.localFile(for: remote) {
            local = file
        } else {
            failed = true
        }
    }

    static func isVideo(_ url: URL) -> Bool {
        let lower = url.absoluteString.lowercased()
        return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".m4v") || lower.contains(".m3u8") || lower.contains(".webm") || lower.contains("/shorts/") || lower.contains("video")
    }
}
