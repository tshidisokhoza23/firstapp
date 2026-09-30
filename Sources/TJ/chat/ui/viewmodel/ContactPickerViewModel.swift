import Foundation
import Combine
import SwiftUI

public struct Contact: Identifiable, Hashable, Codable {
    public var id: String
    public var username: String
    public var displayName: String
    public var phoneNumber: String
    public var bio: String?
    public var avatarUrl: String?
    public var isRegistered: Bool

    public init(
        id: String = UUID().uuidString,
        username: String = "",
        displayName: String = "",
        phoneNumber: String = "",
        bio: String? = nil,
        avatarUrl: String? = nil,
        isRegistered: Bool = false
    ) {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.bio = bio
        self.avatarUrl = avatarUrl
        self.isRegistered = isRegistered
    }
}

@MainActor
public class ContactPickerViewModel: ObservableObject {
    @Published public var contacts: ResourceState<[Contact]> = .loading([])
    @Published public var searchResults: [Contact] = []

    private var allContacts: [Contact] = []

    public init() {
        fetchContacts()
    }

    public func fetchContacts() {
        contacts = .loading(allContacts)
        // Simulated local or remote contact fetching
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            guard let self = self else { return }
            self.contacts = .success(self.allContacts)
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
