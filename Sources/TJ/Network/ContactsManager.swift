import Foundation
import Contacts
import Combine

struct ContactItem: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let phoneNumber: String
    let avatarUrl: String
    let isRegistered: Bool
    let isOnline: Bool
}

class ContactsManager: ObservableObject {
    static let shared = ContactsManager()

    @Published var contacts: [ContactItem] = []
    @Published var permissionGranted: Bool = false

    private let contactStore = CNContactStore()

    init() {
        requestContactsAccess()
    }

    func requestContactsAccess() {
        contactStore.requestAccess(for: .contacts) { [weak self] granted, error in
            DispatchQueue.main.async {
                self?.permissionGranted = granted
                if granted {
                    self?.fetchAndNormalizeDeviceContacts()
                }
            }
        }
    }

    func fetchAndNormalizeDeviceContacts() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            var fetched: [ContactItem] = []

            let keysToFetch = [CNContactGivenNameKey as CNKeyDescriptor,
                               CNContactFamilyNameKey as CNKeyDescriptor,
                               CNContactPhoneNumbersKey as CNKeyDescriptor]

            let fetchRequest = CNContactFetchRequest(keysToFetch: keysToFetch)

            try? self.contactStore.enumerateContacts(with: fetchRequest) { contact, _ in
                let name = "\(contact.givenName) \(contact.familyName)".trimmingCharacters(in: .whitespaces)
                for phoneNumber in contact.phoneNumbers {
                    let rawNumber = phoneNumber.value.stringValue
                    let normalizedNumber = self.normalizePhoneNumber(rawNumber)
                    if !normalizedNumber.isEmpty {
                        let item = ContactItem(
                            id: normalizedNumber,
                            name: name.isEmpty ? normalizedNumber : name,
                            phoneNumber: normalizedNumber,
                            avatarUrl: "https://i.pravatar.cc/150?u=\(normalizedNumber)",
                            isRegistered: true,
                            isOnline: false
                        )
                        fetched.append(item)
                    }
                }
            }

            DispatchQueue.main.async {
                self.contacts = fetched
            }
        }
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

    func addContactFromStory(id: String, name: String, avatarUrl: String) {
        let newContact = ContactItem(
            id: id,
            name: name,
            phoneNumber: "+1 555-0999",
            avatarUrl: avatarUrl,
            isRegistered: true,
            isOnline: true
        )
        if !contacts.contains(where: { $0.id == id }) {
            contacts.insert(newContact, at: 0)
        }
    }
}
