import Foundation

enum ChatExpiry {
    static func seconds(for chatId: String) -> Int {
        UserDefaults.standard.integer(forKey: "tj_disappear_\(chatId)")
    }

    static func dropExpired(_ messages: [Message], chatId: String) -> [Message] {
        let seconds = seconds(for: chatId)
        guard seconds > 0 else { return messages }
        let cutoff = Int64(Date().timeIntervalSince1970 * 1000) - Int64(seconds) * 1000
        return messages.filter { $0.timestamp <= 0 || $0.timestamp >= cutoff }
    }

    static func mediaVisible(_ chatId: String) -> Bool {
        let key = "tj_media_visible_\(chatId)"
        guard UserDefaults.standard.object(forKey: key) != nil else { return true }
        return UserDefaults.standard.bool(forKey: key)
    }
}
