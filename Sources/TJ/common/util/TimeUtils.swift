import Foundation

public enum TimeUtils {

    private static let isoParsers: [DateFormatter] = {
        let patterns = [
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss"
        ]
        return patterns.map { pattern in
            let df = DateFormatter()
            df.dateFormat = pattern
            df.locale = Locale(identifier: "en_US_POSIX")
            df.timeZone = TimeZone(secondsFromGMT: 0)
            return df
        }
    }()

    private static let iso8601Formatter = ISO8601DateFormatter()

    public static func parseIso(_ s: String?) -> Date? {
        guard let s = s?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else {
            return nil
        }

        if let milliTime = Double(s) {
            return Date(timeIntervalSince1970: milliTime > 1e11 ? milliTime / 1000.0 : milliTime)
        }

        if let date = iso8601Formatter.date(from: s) {
            return date
        }

        for df in isoParsers {
            if let date = df.date(from: s) {
                return date
            }
        }
        return nil
    }

    public static func formatWhatsAppTime(_ timestamp: Int64) -> String {
        return formatWhatsAppTime(Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0))
    }

    public static func formatWhatsAppTime(_ isoTimestamp: String?) -> String {
        guard let isoTimestamp = isoTimestamp, !isoTimestamp.isEmpty else { return "" }
        guard let date = parseIso(isoTimestamp) else { return isoTimestamp }
        return formatWhatsAppTime(date)
    }

    public static func formatWhatsAppTime(_ date: Date) -> String {
        let calendar = Calendar.current
        let now = Date()

        if calendar.isDateInToday(date) {
            let df = DateFormatter()
            df.dateFormat = "h:mm a"
            return df.string(from: date)
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday"
        } else {
            let components = calendar.dateComponents([.day], from: date, to: now)
            if let days = components.day, days < 7 {
                let df = DateFormatter()
                df.dateFormat = "EEEE"
                return df.string(from: date)
            } else {
                let df = DateFormatter()
                df.dateFormat = "yyyy/MM/dd"
                return df.string(from: date)
            }
        }
    }

    public static func formatChatDateHeader(_ timestamp: Int64) -> String {
        return formatChatDateHeader(Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0))
    }

    public static func formatChatDateHeader(_ isoTimestamp: String) -> String {
        guard let date = parseIso(isoTimestamp) else { return isoTimestamp }
        return formatChatDateHeader(date)
    }

    public static func formatChatDateHeader(_ date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Today"
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday"
        } else {
            let df = DateFormatter()
            df.dateFormat = "MMMM d, yyyy"
            return df.string(from: date)
        }
    }

    public static func formatMessageTime(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let df = DateFormatter()
        df.dateFormat = "h:mm a"
        return df.string(from: date)
    }

    public static func formatMessageTime(_ isoTimestamp: String?) -> String {
        guard let isoTimestamp = isoTimestamp, !isoTimestamp.isEmpty else { return "" }
        guard let date = parseIso(isoTimestamp) else { return isoTimestamp }
        let df = DateFormatter()
        df.dateFormat = "h:mm a"
        return df.string(from: date)
    }

    public static func formatPostedTime(_ timestamp: Int64) -> String {
        guard timestamp > 0 else { return "" }
        let diffSec = Date().timeIntervalSince1970 - (Double(timestamp) / 1000.0)
        let mins = Int(diffSec / 60.0)
        let hours = mins / 60
        let days = hours / 24
        let weeks = days / 7

        if mins < 60 {
            return "Posted \(max(0, mins))m ago"
        } else if hours < 24 {
            return "Posted \(hours)h ago"
        } else if days < 7 {
            return "Posted \(days)d ago"
        } else if weeks < 4 {
            return "Posted \(weeks)w ago"
        } else {
            return "Posted \(days / 30)mo ago"
        }
    }

    public static func formatPostedTime(_ isoTimestamp: String?) -> String {
        guard let isoTimestamp = isoTimestamp, !isoTimestamp.isEmpty else { return "" }
        guard let date = parseIso(isoTimestamp) else { return "" }
        return formatPostedTime(Int64(date.timeIntervalSince1970 * 1000.0))
    }

    public static func formatTwitterTime(_ isoTimestamp: String?) -> String {
        guard let isoTimestamp = isoTimestamp, !isoTimestamp.isEmpty else { return "" }
        guard let date = parseIso(isoTimestamp) else { return "" }

        let diffSec = Date().timeIntervalSince(date)
        if diffSec < 0 { return "now" }

        let mins = Int(diffSec / 60.0)
        let hours = mins / 60
        let days = hours / 24

        if mins < 1 {
            return "now"
        } else if mins < 60 {
            return "\(mins)m"
        } else if hours < 24 {
            return "\(hours)h"
        } else if days < 7 {
            return "\(days)d"
        } else {
            let df = DateFormatter()
            df.dateFormat = "MMM d"
            return df.string(from: date)
        }
    }
}
