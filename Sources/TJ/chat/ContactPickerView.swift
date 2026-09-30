import SwiftUI

struct ContactPickerView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var searchText: String = ""

    let contacts = [
        ("Alice Smith", "+1 555-0123", "Online"),
        ("Bob Johnson", "+1 555-0124", "Offline"),
        ("Charlie Brown", "+1 555-0125", "Online"),
        ("Diana Prince", "+1 555-0126", "Offline"),
        ("Evan Wright", "+1 555-0127", "Online")
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
                                let newChat = ChatItem(
                                    id: UUID().uuidString,
                                    name: contact.0,
                                    lastMessage: "Started a new conversation",
                                    timestamp: "Just now",
                                    unreadCount: 0,
                                    avatarUrl: "https://i.pravatar.cc/150?u=\(contact.1)",
                                    isOnline: contact.2 == "Online"
                                )
                                chatViewModel.chats.insert(newChat, at: 0)
                                chatViewModel.activeChat = newChat
                                dismiss()
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: "person.crop.circle.fill")
                                        .font(.system(size: 40))
                                        .foregroundColor(AppColors.greyMedium)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(contact.0)
                                            .font(.headline)
                                            .foregroundColor(.primary)
                                        Text(contact.1)
                                            .font(.caption)
                                            .foregroundColor(AppColors.greyMedium)
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
            .navigationTitle("New Chat")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
