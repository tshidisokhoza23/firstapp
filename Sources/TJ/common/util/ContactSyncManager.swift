import Foundation
import Contacts

public struct ContactInfo: Codable, Sendable {
    public let displayName: String
    public let phoneNumber: String

    public enum CodingKeys: String, CodingKey {
        case displayName = "display_name"
        case phoneNumber = "phone_number"
    }

    public init(displayName: String, phoneNumber: String) {
        self.displayName = displayName
        self.phoneNumber = phoneNumber
    }
}

public protocol ChatRepositoryContactSyncProtocol {
    func syncContacts(phoneNumbers: [String]) async throws
}

public final class ContactSyncManager {

    public static let shared = ContactSyncManager()

    private let contactStore = CNContactStore()
    private var observerRegistered = false
    public var onContactsChanged: (() -> Void)?

    private init() {}

    // MARK: - Permission Check

    public func canReadContacts() -> Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        return status.rawValue == 3 || status.rawValue == 4
    }

    public func requestContactsAccess() async -> Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        if canReadContacts() { return true }
        switch status {
        case .authorized:
            return true
        case .notDetermined:
            do {
                return try await contactStore.requestAccess(for: .contacts)
            } catch {
                return false
            }
        default:
            return false
        }
    }

    // MARK: - Fetch Contacts

    public func fetchAllContacts() -> [ContactInfo] {
        guard canReadContacts() else {
            return []
        }

        var contacts: [ContactInfo] = []
        var seenNumbers = Set<String>()
        let defaultPrefix = getDefaultPhonePrefix()

        let keysToFetch: [CNKeyDescriptor] = [
            CNContactGivenNameKey as CNKeyDescriptor,
            CNContactFamilyNameKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor
        ]

        let fetchRequest = CNContactFetchRequest(keysToFetch: keysToFetch)

        do {
            try contactStore.enumerateContacts(with: fetchRequest) { contact, _ in
                let name = "\(contact.givenName) \(contact.familyName)".trimmingCharacters(in: .whitespacesAndNewlines)
                for phoneNumberValue in contact.phoneNumbers {
                    let rawNumber = phoneNumberValue.value.stringValue
                    let formattedNumber = self.formatPhoneNumber(rawNumber, defaultPrefix: defaultPrefix)
                    if self.isValidPhoneNumber(formattedNumber) && !seenNumbers.contains(formattedNumber) {
                        seenNumbers.insert(formattedNumber)
                        contacts.append(ContactInfo(displayName: name, phoneNumber: formattedNumber))
                    }
                }
            }
        } catch {
            print("[ContactSyncManager] Failed to fetch contacts: \(error.localizedDescription)")
        }

        return contacts
    }

    public func getContactNameByNumber(_ phoneNumber: String) -> String? {
        guard CNContactStore.authorizationStatus(for: .contacts) == .authorized else {
            return nil
        }

        let defaultPrefix = getDefaultPhonePrefix()
        let cleanInput = formatPhoneNumber(phoneNumber, defaultPrefix: defaultPrefix)

        let predicate = CNContact.predicateForContacts(matching: CNPhoneNumber(stringValue: cleanInput))
        let keysToFetch = [CNContactGivenNameKey as CNKeyDescriptor, CNContactFamilyNameKey as CNKeyDescriptor]

        do {
            let matches = try contactStore.unifiedContacts(matching: predicate, keysToFetch: keysToFetch)
            if let first = matches.first {
                let fullName = "\(first.givenName) \(first.familyName)".trimmingCharacters(in: .whitespacesAndNewlines)
                return fullName.isEmpty ? nil : fullName
            }
        } catch {
            print("[ContactSyncManager] Lookup error: \(error.localizedDescription)")
        }

        return nil
    }

    // MARK: - Observer

    public func startObserving() {
        guard !observerRegistered else { return }
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleContactsDidChange(_:)),
            name: .CNContactStoreDidChange,
            object: nil
        )
        observerRegistered = true
    }

    public func stopObserving() {
        guard observerRegistered else { return }
        NotificationCenter.default.removeObserver(self, name: .CNContactStoreDidChange, object: nil)
        observerRegistered = false
    }

    @objc private func handleContactsDidChange(_ notification: Notification) {
        onContactsChanged?()
    }

    // MARK: - Background Sync

    public func syncContacts(repository: ChatRepositoryContactSyncProtocol) async {
        guard await requestContactsAccess() else { return }

        let contacts = fetchAllContacts()
        let phoneNumbers = contacts.map { $0.phoneNumber }

        do {
            try await repository.syncContacts(phoneNumbers: phoneNumbers)
            print("[ContactSyncManager] Background contact sync successful (\(phoneNumbers.count) contacts)")
        } catch {
            print("[ContactSyncManager] Contact sync error: \(error.localizedDescription)")
        }
    }

    // MARK: - Formatting Helpers

    public func formatPhoneNumber(_ number: String, defaultPrefix: String) -> String {
        let clean = number.components(separatedBy: CharacterSet(charactersIn: "0123456789+").inverted).joined()

        if clean.hasPrefix("+") {
            return clean
        }
        if clean.hasPrefix("00") {
            return "+" + String(clean.dropFirst(2))
        }
        if clean.hasPrefix("0") {
            return "+\(defaultPrefix)" + String(clean.dropFirst(1))
        }
        if clean.count <= 9 {
            return "+\(defaultPrefix)\(clean)"
        }
        if clean.hasPrefix(defaultPrefix) {
            return "+\(clean)"
        }
        return "+\(clean)"
    }

    public func formatDisplayPhone(_ number: String?) -> String {
        guard let number = number, !number.isEmpty else { return "" }
        let clean = number.components(separatedBy: CharacterSet(charactersIn: "0123456789+").inverted).joined()
        guard clean.hasPrefix("+") else { return clean }

        let digits = String(clean.dropFirst())
        if digits.count == 11 {
            let c = digits.map { String($0) }
            return "+\(c[0...1].joined()) \(c[2...3].joined()) \(c[4...6].joined()) \(c[7...10].joined())"
        } else if digits.count == 10 {
            let c = digits.map { String($0) }
            return "+\(c[0]) \(c[1...3].joined()) \(c[4...6].joined()) \(c[7...9].joined())"
        }
        return clean
    }

    private func isValidPhoneNumber(_ number: String) -> Bool {
        let regex = "^\\+[1-9]\\d{6,14}$"
        return NSPredicate(format: "SELF MATCHES %@", regex).evaluate(with: number)
    }

    private func getDefaultPhonePrefix() -> String {
        let regionCode = Locale.current.region?.identifier ?? "ZA"
        switch regionCode {
        case "US", "CA": return "1"
        case "GB": return "44"
        case "ZA": return "27"
        case "IN": return "91"
        case "AU": return "61"
        case "DE": return "49"
        case "FR": return "33"
        default: return "27"
        }
    }
}
