import Foundation
import Combine
import SwiftUI

@MainActor
public class ContactPickerViewModel: ObservableObject {
    @Published public var contacts: Resource<[Contact]> = .loading()
    @Published public var searchResults: [Contact] = []

    private var allContacts: [Contact] = []

    public init() {
        fetchContacts()
    }

    public func fetchContacts() {
        contacts = .loading(data: allContacts)
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
