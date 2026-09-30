import Foundation

public enum MessageMarkers {
    private static let mark = "\u{2060}"
    private static let sep = "\u{001f}"
    private static let markerRegex = try? NSRegularExpression(pattern: "\u{2060}\\[(?:storyReply|contact):[^\\]]*\\]")

    public struct StoryReply: Equatable, Codable, Sendable {
        public var storyId: String
        public var userId: String
        public var author: String
        public var summary: String
        public var mediaUrl: String

        public init(storyId: String, userId: String, author: String = "", summary: String = "", mediaUrl: String = "") {
            self.storyId = storyId
            self.userId = userId
            self.author = author
            self.summary = summary
            self.mediaUrl = mediaUrl
        }
    }

    public struct SharedContact: Equatable, Codable, Sendable {
        public var name: String
        public var phone: String

        public init(name: String, phone: String) {
            self.name = name
            self.phone = phone
        }
    }

    public static func storyReplyId(storyId: String, userId: String) -> String {
        let id = storyId.replacingOccurrences(of: "story_", with: "").components(separatedBy: "|").first ?? ""
        let user = userId.replacingOccurrences(of: "user_", with: "")
        return user.trimmingCharacters(in: .whitespaces).isEmpty ? "story_\(id)" : "story_\(id)|\(user)"
    }

    public static func parseStoryReplyId(replyToId: String?) -> StoryReply? {
        guard let replyToId = replyToId, !replyToId.trimmingCharacters(in: .whitespaces).isEmpty, replyToId.hasPrefix("story_") else {
            return nil
        }
        let rest = String(replyToId.dropFirst("story_".count))
        let parts = rest.components(separatedBy: "|")
        guard let storyId = parts.first?.trimmingCharacters(in: .whitespaces), !storyId.isEmpty else {
            return nil
        }
        let userId = parts.count > 1 ? parts[1].trimmingCharacters(in: .whitespaces) : ""
        return StoryReply(storyId: storyId, userId: userId)
    }

    public static func storyWire(text: String, replyToId: String?, replyToText: String?, author: String?) -> String {
        guard let ref = parseStoryReplyId(replyToId: replyToId) else { return text }
        let summary = strip(replyToText)
        var media = ""
        if let replyToText = replyToText,
           let regex = try? NSRegularExpression(pattern: "\\[mediaUrl:(.*?)\\]"),
           let match = regex.firstMatch(in: replyToText, range: NSRange(replyToText.startIndex..., in: replyToText)),
           let range = Range(match.range(at: 1), in: replyToText) {
            media = String(replyToText[range]).trimmingCharacters(in: .whitespaces)
        }
        let payload = [ref.storyId, ref.userId, author ?? "", summary, media].joined(separator: sep)
        return strip(text) + mark + "[storyReply:\(payload)]"
    }

    public static func contactWire(name: String?, phone: String?, fallback: String = "Shared Contact") -> String {
        let safeName = name?.replacingOccurrences(of: sep, with: " ").trimmingCharacters(in: .whitespaces) ?? ""
        let safePhone = phone?.replacingOccurrences(of: sep, with: " ").trimmingCharacters(in: .whitespaces) ?? ""
        if safeName.isEmpty && safePhone.isEmpty { return fallback }
        return "Shared Contact" + mark + "[contact:\(safeName)\(sep)\(safePhone)]"
    }

    public static func strip(_ content: String?) -> String {
        guard let content = content, !content.isEmpty else { return "" }
        guard let regex = markerRegex else { return content.trimmingCharacters(in: .whitespaces) }
        let range = NSRange(content.startIndex..., in: content)
        let stripped = regex.stringByReplacingMatches(in: content, range: range, withTemplate: "")
        return stripped.trimmingCharacters(in: .whitespaces)
    }

    public static func parseStory(_ content: String?) -> StoryReply? {
        guard let content = content, !content.isEmpty else { return nil }
        guard let regex = try? NSRegularExpression(pattern: "\u{2060}\\[storyReply:([^\\]]*)\\]"),
              let match = regex.firstMatch(in: content, range: NSRange(content.startIndex..., in: content)),
              let range = Range(match.range(at: 1), in: content) else {
            return nil
        }
        let raw = String(content[range])
        let parts = raw.components(separatedBy: sep)
        guard let storyId = parts.first?.trimmingCharacters(in: .whitespaces), !storyId.isEmpty else { return nil }
        return StoryReply(
            storyId: storyId,
            userId: parts.count > 1 ? parts[1].trimmingCharacters(in: .whitespaces) : "",
            author: parts.count > 2 ? parts[2].trimmingCharacters(in: .whitespaces) : "",
            summary: parts.count > 3 ? parts[3].trimmingCharacters(in: .whitespaces) : "",
            mediaUrl: parts.count > 4 ? parts[4].trimmingCharacters(in: .whitespaces) : ""
        )
    }

    public static func parseContact(_ content: String?) -> SharedContact? {
        guard let content = content, !content.isEmpty else { return nil }
        guard let regex = try? NSRegularExpression(pattern: "\u{2060}\\[contact:([^\\]]*)\\]"),
              let match = regex.firstMatch(in: content, range: NSRange(content.startIndex..., in: content)),
              let range = Range(match.range(at: 1), in: content) else {
            return nil
        }
        let raw = String(content[range])
        let parts = raw.components(separatedBy: sep)
        let name = parts.first?.trimmingCharacters(in: .whitespaces) ?? ""
        let phone = parts.count > 1 ? parts[1].trimmingCharacters(in: .whitespaces) : ""
        if name.isEmpty && phone.isEmpty { return nil }
        return SharedContact(name: name, phone: phone)
    }
}
