import Foundation
import Combine
import SwiftUI
import Contacts

@MainActor
public class ContactPickerViewModel: ObservableObject {
    @Published public var contacts: Resource<[Contact]> = .loading()
    @Published public var searchResults: [Contact] = []

    private var allContacts: [Contact] = []

    public init() {
        fetchContacts()
    }

    public func fetchContacts() {
        contacts = .loading(allContacts)
        Task { await syncDeviceContacts() }
    }

    private func syncDeviceContacts() async {
        let allowed = await ContactSyncManager.shared.requestContactsAccess()
        guard allowed else {
            contacts = .error("Contacts permission is required to find people on TJ", allContacts)
            return
        }
        let device = ContactSyncManager.shared.fetchAllContacts()
        do {
            let data = try await API.data("users/sync-contacts", method: "POST", json: [
                "phone_numbers": device.map(\.phoneNumber)
            ])
            let rows = (LiveJSON.object(data)["contacts"] as? [[String: Any]]) ?? []
            let matched: [Contact] = rows.compactMap { row in
                let id = LiveJSON.string(row["id"])
                guard !id.isEmpty else { return nil }
                let phone = LiveJSON.string(row["phone_number"])
                let local = device.first { $0.phoneNumber == phone }?.displayName ?? ""
                let serverName = LiveJSON.string(row["display_name"])
                let username = LiveJSON.string(row["username"])
                let name = !local.isEmpty ? local : (!serverName.isEmpty ? serverName : (!username.isEmpty ? username : phone))
                return Contact(
                    id: id,
                    username: username,
                    displayName: name,
                    avatarUrl: LiveJSON.string(row["avatar_url"]).nilIfEmpty,
                    phoneNumber: phone,
                    isRegistered: true,
                    isVerified: LiveJSON.bool(row["is_verified"])
                )
            }
            allContacts = matched
            contacts = .success(matched)
        } catch {
            contacts = .error(error.localizedDescription, allContacts)
        }
    }

    public func searchContacts(query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            searchResults = []
            return
        }
        searchResults = allContacts.filter {
            $0.displayName.localizedCaseInsensitiveContains(trimmed) ||
            $0.phoneNumber.contains(trimmed) ||
            $0.username.localizedCaseInsensitiveContains(trimmed)
        }
    }
}
