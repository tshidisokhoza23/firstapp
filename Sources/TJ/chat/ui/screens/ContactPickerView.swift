import SwiftUI

public struct ContactPickerView: View {
    @StateObject public var viewModel: ContactPickerViewModel = ContactPickerViewModel()
    public var initialIsGroupMode: Bool = false
    public var onBack: () -> Void = {}
    public var onContactSelected: (Contact) -> Void = { _ in }
    public var onInviteClicked: () -> Void = {}
    public var onGroupCreate: ([Contact]) -> Void = { _ in }

    @State private var isGroupMode: Bool = false
    @State private var searchQuery: String = ""
    @State private var isSearchActive: Bool = false
    @State private var selectedContacts: Set<Contact> = []

    @MainActor public init(
        viewModel: ContactPickerViewModel = ContactPickerViewModel(),
        initialIsGroupMode: Bool = false,
        onBack: @escaping () -> Void = {},
        onContactSelected: @escaping (Contact) -> Void = { _ in },
        onInviteClicked: @escaping () -> Void = {},
        onGroupCreate: @escaping ([Contact]) -> Void = { _ in }
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.initialIsGroupMode = initialIsGroupMode
        self.onBack = onBack
        self.onContactSelected = onContactSelected
        self.onInviteClicked = onInviteClicked
        self.onGroupCreate = onGroupCreate
        self._isGroupMode = State(initialValue: initialIsGroupMode)
    }

    private var contactsList: [Contact] {
        if isSearchActive && !searchQuery.isEmpty {
            return viewModel.searchResults
        }
        return viewModel.contacts.data ?? []
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                }

                if isSearchActive {
                    TextField("Search contacts...", text: $searchQuery)
                        .textFieldStyle(PlainTextFieldStyle())
                        .onChange(of: searchQuery) { query in
                            viewModel.searchContacts(query: query)
                        }

                    Button(action: {
                        isSearchActive = false
                        searchQuery = ""
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(isGroupMode ? "Add Participants" : "Select Contact")
                            .font(.system(size: 18, weight: .bold))
                        Text("\(contactsList.count) contacts")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Button(action: { isSearchActive = true }) {
                        Image(systemName: "magnifyingglass")
                    }

                    Button(action: { viewModel.fetchContacts() }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()

            List {
                if !isGroupMode {
                    Button(action: { isGroupMode = true }) {
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(Color.accentColor)
                                    .frame(width: 40, height: 40)
                                Image(systemName: "person.3.fill")
                                    .foregroundColor(.white)
                            }
                            Text("New Group")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.primary)
                        }
                    }

                    Button(action: {}) {
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(Color.accentColor)
                                    .frame(width: 40, height: 40)
                                Image(systemName: "person.badge.plus")
                                    .foregroundColor(.white)
                            }
                            Text("New Contact")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.primary)
                        }
                    }
                }

                ForEach(contactsList) { contact in
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(contact.isRegistered ? Color.accentColor : Color.gray.opacity(0.3))
                                .frame(width: 40, height: 40)
                            Text(String((contact.displayName.isEmpty ? contact.phoneNumber : contact.displayName).prefix(1)).uppercased())
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(contact.isRegistered ? .white : .primary)
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(contact.displayName.isEmpty ? contact.phoneNumber : contact.displayName)
                                .font(.system(size: 15, weight: .bold))
                            Text(contact.isRegistered ? (contact.bio ?? "Available") : contact.phoneNumber)
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        if isGroupMode && contact.isRegistered {
                            Image(systemName: selectedContacts.contains(contact) ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 22))
                                .foregroundColor(selectedContacts.contains(contact) ? .accentColor : .gray)
                        } else if !contact.isRegistered {
                            Text("INVITE")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.accentColor)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if isGroupMode {
                            if contact.isRegistered {
                                if selectedContacts.contains(contact) {
                                    selectedContacts.remove(contact)
                                } else {
                                    selectedContacts.insert(contact)
                                }
                            }
                        } else {
                            if contact.isRegistered {
                                onContactSelected(contact)
                            } else {
                                onInviteClicked()
                            }
                        }
                    }
                }
            }
            .listStyle(PlainListStyle())

            if isGroupMode && !selectedContacts.isEmpty {
                Button(action: {
                    onGroupCreate(Array(selectedContacts))
                }) {
                    Text("Create Group (\(selectedContacts.count))")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.accentColor)
                        .cornerRadius(12)
                        .padding()
                }
            }
        }
    }
}
