import Foundation
import Contacts

public func isIdString(_ text: String?, id: String? = nil) -> Bool {
    guard let text = text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return true }
    let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
    let lower = trimmed.lowercased()

    if ["unknown", "null", "undefined", "none", "new chat", "user", "mingle user"].contains(lower) {
        return true
    }

    if let id = id, trimmed.caseInsensitiveCompare(id.trimmingCharacters(in: .whitespacesAndNewlines)) == .orderedSame {
        return true
    }

    // Hex ID / MongoDB ObjectId (16-64 hex chars)
    let hexRegex = "^[0-9a-fA-F]{16,64}$"
    if NSPredicate(format: "SELF MATCHES %@", hexRegex).evaluate(with: trimmed) {
        return true
    }

    // UUID pattern
    let uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    if NSPredicate(format: "SELF MATCHES %@", uuidRegex).evaluate(with: trimmed) {
        return true
    }

    return false
}

public func sanitizeDisplayName(name: String?, id: String? = nil, phone: String? = nil) -> String {
    if !isIdString(name, id: id), let name = name {
        return name.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    if let phone = phone, !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return phone.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    return ""
}

public func looksLikePhone(_ value: String?) -> Bool {
    guard let value = value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
    let digits = value.filter { $0.isNumber }.count
    let compact = value.filter { !$0.isWhitespace }.count
    return digits >= 8 && compact > 0 && digits * 10 >= compact * 7
}

public struct UserProfileInfo {
    public let id: String
    public let displayName: String
    public let username: String
    public let phoneNumber: String

    public init(id: String, displayName: String, username: String, phoneNumber: String) {
        self.id = id
        self.displayName = displayName
        self.username = username
        self.phoneNumber = phoneNumber
    }
}

public final class DisplayNameResolver {

    private var contactCache = NSCache<NSString, NSString>()

    public init() {}

    public func resolve(user: UserProfileInfo?, phoneNumber: String) -> String {
        let cleanPhone = phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? (user?.phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines) ?? "")
            : phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines)

        // 1. Local device contact lookup via ContactSyncManager
        if !cleanPhone.isEmpty {
            let nsPhone = cleanPhone as NSString
            if let cached = contactCache.object(forKey: nsPhone) {
                return cached as String
            }
            if let localName = ContactSyncManager.shared.getContactNameByNumber(cleanPhone) {
                contactCache.setObject(localName as NSString, forKey: nsPhone)
                return localName
            }
        }

        // 2. User profile display_name or username
        if let user = user {
            let displayName = user.displayName.trimmingCharacters(in: .whitespacesAndNewlines)
            if !isIdString(displayName, id: user.id) {
                return displayName
            }
            let username = user.username.trimmingCharacters(in: .whitespacesAndNewlines)
            if !isIdString(username, id: user.id) {
                return username
            }
        }

        // 3. Fallback to phone number or user ID
        if !cleanPhone.isEmpty {
            return CountryUtils.formatDisplayPhone(cleanPhone)
        }

        if let user = user, !user.id.isEmpty {
            return user.id
        }

        return "User"
    }

    public func clearCache() {
        contactCache.removeAllObjects()
    }
}
