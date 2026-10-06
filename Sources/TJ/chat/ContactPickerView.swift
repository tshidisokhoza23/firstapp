import SwiftUI

struct ContactPickerView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatViewModel: ChatViewModel
    @ObservedObject var contactsManager = ContactsManager.shared
    @State private var searchText: String = ""

    var body: some View {
        NavigationView {
            VStack {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(AppColors.greyMedium)
                    TextField("Search contacts...", text: $searchText)
                }
                .padding(10)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
                .padding(.horizontal)

                List {
                    Section(header: Text("Registered TJ Contacts")) {
                        ForEach(contactsManager.contacts.filter { $0.isRegistered && (searchText.isEmpty || $0.displayName.localizedCaseInsensitiveContains(searchText) || $0.phoneNumber.contains(searchText)) }) { contact in
                            Button(action: {
                                Task { @MainActor in
                                    let _ = await NetworkClient.shared.initiateDM(contactId: contact.id)
                                    let newChat = ChatItem(
                                        id: contact.id,
                                        name: contact.displayName,
                                        lastMessage: "Conversation active",
                                        timestamp: "Just now",
                                        unreadCount: 0,
                                        avatarUrl: contact.avatarUrl,
                                        isOnline: true
                                    )
                                    if let existingIndex = chatViewModel.chats.firstIndex(where: { $0.id == contact.id }) {
                                        chatViewModel.chats.remove(at: existingIndex)
                                    }
                                    chatViewModel.chats.insert(newChat, at: 0)
                                    chatViewModel.activeChat = newChat
                                    dismiss()
                                }
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: "person.crop.circle.fill")
                                        .font(.system(size: 40))
                                        .foregroundColor(AppColors.greyMedium)

                                    VStack(alignment: .leading, spacing: 2) {
                                        HStack {
                                            Text(contact.displayName)
                                                .font(.headline)
                                                .foregroundColor(.primary)
                                            Image(systemName: "checkmark.seal.fill")
                                                .font(.caption)
                                                .foregroundColor(AppColors.brandPrimary)
                                        }
                                        Text(contact.phoneNumber)
                                            .font(.caption)
                                            .foregroundColor(AppColors.greyMedium)
                                    }

                                    Spacer()

                                    Text("TJ User")
                                        .font(.caption2)
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(AppColors.brandPrimary)
                                        .cornerRadius(8)
                                }
                            }
                        }
                    }

                    Section(header: Text("Device Contacts")) {
                        ForEach(contactsManager.contacts.filter { !$0.isRegistered && (searchText.isEmpty || $0.displayName.localizedCaseInsensitiveContains(searchText) || $0.phoneNumber.contains(searchText)) }) { contact in
                            HStack(spacing: 12) {
                                Image(systemName: "person.crop.circle")
                                    .font(.system(size: 40))
                                    .foregroundColor(AppColors.greyMedium)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(contact.displayName)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Text(contact.phoneNumber)
                                        .font(.caption)
                                        .foregroundColor(AppColors.greyMedium)
                                }

                                Spacer()

                                Button("Invite") { }
                                    .buttonStyle(.bordered)
                                    .tint(AppColors.brandPrimary)
                            }
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Contacts")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
