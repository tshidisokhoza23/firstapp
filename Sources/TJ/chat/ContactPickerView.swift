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
                    Section(header: Text("Synced Contacts & Story Creators")) {
                        ForEach(contactsManager.contacts.filter { searchText.isEmpty || $0.name.localizedCaseInsensitiveContains(searchText) || $0.phoneNumber.contains(searchText) }) { contact in
                            Button(action: {
                                Task { @MainActor in
                                    let _ = await NetworkClient.shared.initiateDM(contactId: contact.id)
                                    let newChat = ChatItem(
                                        id: contact.id,
                                        name: contact.name,
                                        lastMessage: "Conversation active",
                                        timestamp: "Just now",
                                        unreadCount: 0,
                                        avatarUrl: contact.avatarUrl,
                                        isOnline: contact.isOnline
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
                                        Text(contact.name)
                                            .font(.headline)
                                            .foregroundColor(.primary)
                                        Text(contact.phoneNumber)
                                            .font(.caption)
                                            .foregroundColor(AppColors.greyMedium)
                                    }

                                    Spacer()

                                    if contact.isOnline {
                                        Circle()
                                            .fill(Color.green)
                                            .frame(width: 10, height: 10)
                                    }
                                }
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
