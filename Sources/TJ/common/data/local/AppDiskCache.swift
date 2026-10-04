import Foundation

enum AppDiskCache {
    private static let folder: URL = {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let dir = base.appendingPathComponent("tj-cache", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()

    static func save(_ data: Data, name: String) {
        let url = folder.appendingPathComponent(safe(name))
        try? data.write(to: url, options: .atomic)
    }

    static func load(name: String) -> Data? {
        let url = folder.appendingPathComponent(safe(name))
        return try? Data(contentsOf: url)
    }

    static func remove(name: String) {
        let url = folder.appendingPathComponent(safe(name))
        try? FileManager.default.removeItem(at: url)
    }

    static func saveCodable<T: Encodable>(_ value: T, name: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        save(data, name: name)
    }

    static func loadCodable<T: Decodable>(_ type: T.Type, name: String) -> T? {
        guard let data = load(name: name) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    static func saveResponse(path: String, query: [String: String]?, data: Data) {
        save(data, name: responseName(path: path, query: query))
    }

    static func loadResponse(path: String, query: [String: String]?) -> Data? {
        load(name: responseName(path: path, query: query))
    }

    private static func responseName(path: String, query: [String: String]?) -> String {
        let items = (query ?? [:]).sorted { $0.key < $1.key }.map { "\($0.key)=\($0.value)" }.joined(separator: "&")
        return "get-" + path + "?" + items
    }

    private static func safe(_ name: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-_."))
        let cleaned = name.unicodeScalars.map { allowed.contains($0) ? Character($0) : "_" }
        let text = String(cleaned)
        if text.count <= 180 { return text }
        return String(text.prefix(180))
    }
}
