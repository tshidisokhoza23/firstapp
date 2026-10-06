import Foundation
import Contacts
import Combine

struct AccountContactDTO: Codable {
    let name: String
    let phone_number: String
}

struct SyncContactsRequest: Codable {
    let numbers: [String]
}

struct SyncAccountContactsRequest: Codable {
    let contacts: [AccountContactDTO]
}

struct APIContactUser: Codable {
    let id: String
    let username: String
    let display_name: String
    let avatar_url: String?
    let phone_number: String?
}

struct SyncContactsResponse: Codable {
    let contacts: [APIContactUser]
}

struct ContactItem: Identifiable, Codable, Hashable {
    let id: String
    let username: String
    let displayName: String
    let avatarUrl: String
    let phoneNumber: String
    let isRegistered: Bool

    func matchesPhoneNumber(_ target: String) -> Bool {
        let cleanSelf = phoneNumber.replacingOccurrences(of: " ", with: "")
        let cleanTarget = target.replacingOccurrences(of: " ", with: "")
        if cleanSelf == cleanTarget { return true }
        if cleanSelf.count >= 9 && cleanTarget.count >= 9 {
            return cleanSelf.suffix(9) == cleanTarget.suffix(9)
        }
        return false
    }
}

class ContactsManager: ObservableObject {
    static let shared = ContactsManager()

    @Published var contacts: [ContactItem] = []
    @Published var permissionGranted: Bool = false
    @Published var isSyncing: Bool = false

    private let contactStore = CNContactStore()

    init() {
        requestContactsAccessAndSync()
    }

    func requestContactsAccessAndSync() {
        contactStore.requestAccess(for: .contacts) { [weak self] granted, error in
            DispatchQueue.main.async {
                self?.permissionGranted = granted
                if granted {
                    self?.syncAndQueryContacts()
                }
            }
        }
    }

    func syncAndQueryContacts() {
        guard !isSyncing else { return }
        isSyncing = true

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }

            let deviceContacts = self.fetchPhoneContactsWithNames()
            let localByPhone = Dictionary(uniqueKeysWithValues: deviceContacts.map { ($0.phone_number, $0.name) })

            let phoneNumbers = deviceContacts.map { $0.phone_number }

            Task { @MainActor in
                let _ = await self.syncAccountContactsWithBackend(deviceContacts)

                let registeredUsers = await self.syncContactsWithBackend(phoneNumbers: phoneNumbers)

                let registeredItems: [ContactItem] = registeredUsers.map { apiUser in
                    let phone = apiUser.phone_number ?? ""
                    let localName = localByPhone[phone]
                    return ContactItem(
                        id: apiUser.id,
                        username: apiUser.username,
                        displayName: (localName != nil && !localName!.isEmpty) ? localName! : apiUser.display_name,
                        avatarUrl: apiUser.avatar_url ?? "",
                        phoneNumber: phone,
                        isRegistered: true
                    )
                }

                let registeredPhones = Set(registeredUsers.compactMap { $0.phone_number })

                let nonRegisteredItems: [ContactItem] = deviceContacts
                    .filter { !$0.phone_number.isEmpty && !registeredPhones.contains($0.phone_number) }
                    .map { contact in
                        ContactItem(
                            id: "phone:\(contact.phone_number)",
                            username: contact.phone_number,
                            displayName: contact.name,
                            avatarUrl: "",
                            phoneNumber: contact.phone_number,
                            isRegistered: false
                        )
                    }

                let allMerged = (registeredItems + nonRegisteredItems).sorted { c1, c2 in
                    if c1.isRegistered != c2.isRegistered {
                        return c1.isRegistered && !c2.isRegistered
                    }
                    return c1.displayName.localizedCaseInsensitiveCompare(c2.displayName) == .orderedAscending
                }

                self.contacts = allMerged
                self.isSyncing = false
            }
        }
    }

    private func fetchPhoneContactsWithNames() -> [AccountContactDTO] {
        var results: [AccountContactDTO] = []
        var seenNumbers = Set<String>()

        let keysToFetch = [CNContactGivenNameKey as CNKeyDescriptor,
                           CNContactFamilyNameKey as CNKeyDescriptor,
                           CNContactPhoneNumbersKey as CNKeyDescriptor]

        let request = CNContactFetchRequest(keysToFetch: keysToFetch)

        try? contactStore.enumerateContacts(with: request) { contact, _ in
            let name = "\(contact.givenName) \(contact.familyName)".trimmingCharacters(in: .whitespaces)
            for phoneNumber in contact.phoneNumbers {
                let rawNumber = phoneNumber.value.stringValue
                let normalized = self.normalizePhoneNumber(rawNumber)
                if normalized.count >= 7 && !seenNumbers.contains(normalized) {
                    seenNumbers.insert(normalized)
                    results.append(AccountContactDTO(name: name.isEmpty ? normalized : name, phone_number: normalized))
                }
            }
        }
        return results
    }

    func normalizePhoneNumber(_ rawNumber: String) -> String {
        let cleaned = rawNumber.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
        if rawNumber.hasPrefix("+") {
            return "+\(cleaned)"
        } else if cleaned.count == 10 {
            return "+1\(cleaned)"
        }
        return "+\(cleaned)"
    }

    private func syncAccountContactsWithBackend(_ contacts: [AccountContactDTO]) async -> Bool {
        guard let url = URL(string: "\(NetworkClient.shared.baseURL)/contacts/account/sync") else { return false }
        do {
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            if let token = NetworkClient.shared.authToken {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            let body = SyncAccountContactsRequest(contacts: contacts)
            request.httpBody = try? JSONEncoder().encode(body)
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
    }

    private func syncContactsWithBackend(phoneNumbers: [String]) async -> [APIContactUser] {
        guard let url = URL(string: "\(NetworkClient.shared.baseURL)/contacts/sync") else { return [] }
        do {
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            if let token = NetworkClient.shared.authToken {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            let body = SyncContactsRequest(numbers: phoneNumbers)
            request.httpBody = try? JSONEncoder().encode(body)

            let (data, response) = try await URLSession.shared.data(for: request)
            if (response as? HTTPURLResponse)?.statusCode == 200 {
                let decoded = try JSONDecoder().decode(SyncContactsResponse.self, from: data)
                return decoded.contacts
            }
            return []
        } catch {
            return []
        }
    }

    func addContactFromStory(id: String, name: String, avatarUrl: String) {
        let newContact = ContactItem(
            id: id,
            username: name.lowercased().replacingOccurrences(of: " ", with: "_"),
            displayName: name,
            avatarUrl: avatarUrl,
            phoneNumber: "+1 555-0999",
            isRegistered: true
        )
        if !contacts.contains(where: { $0.id == id }) {
            contacts.insert(newContact, at: 0)
        }
    }
}
