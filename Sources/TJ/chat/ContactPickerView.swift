import SwiftUI

struct ContactPickerView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var searchText: String = ""

    let contacts = [
        ("Alice Smith", "c1", "Online"),
        ("Bob Johnson", "c2", "Offline"),
        ("Charlie Brown", "c3", "Online"),
        ("Diana Prince", "c4", "Offline"),
        ("Evan Wright", "c5", "Online")
    ]

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
                    Section(header: Text("Frequently Contacted")) {
                        ForEach(contacts.filter { searchText.isEmpty || $0.0.localizedCaseInsensitiveContains(searchText) }, id: \.1) { contact in
                            Button(action: {
                                Task { @MainActor in
                                    let _ = await NetworkClient.shared.initiateDM(contactId: contact.1)
                                    let newChat = ChatItem(
                                        id: contact.1,
                                        name: contact.0,
                                        lastMessage: "Conversation active",
                                        timestamp: "Just now",
                                        unreadCount: 0,
                                        avatarUrl: "https://i.pravatar.cc/150?u=\(contact.1)",
                                        isOnline: contact.2 == "Online"
                                    )
                                    if let existingIndex = chatViewModel.chats.firstIndex(where: { $0.id == contact.1 }) {
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
                                        Text(contact.0)
                                            .font(.headline)
                                            .foregroundColor(.primary)
                                        Text(contact.2)
                                            .font(.caption)
                                            .foregroundColor(contact.2 == "Online" ? .green : AppColors.greyMedium)
                                    }

                                    Spacer()

                                    if contact.2 == "Online" {
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
            .navigationTitle("New Chat / Restore")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
