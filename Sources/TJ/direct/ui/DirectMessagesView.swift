import SwiftUI

// MARK: - Direct Message Models

public struct DirectConversationItem: Identifiable, Hashable {
    public let id: String
    public let contactName: String
    public let contactHandle: String
    public let lastMessage: String
    public let timestamp: String
    public var unreadCount: Int
    public var isOnline: Bool
    public var isPinned: Bool
    public var avatarGradient: [Color]

    public init(id: String, contactName: String, contactHandle: String, lastMessage: String, timestamp: String, unreadCount: Int = 0, isOnline: Bool = false, isPinned: Bool = false, avatarGradient: [Color] = [.blue, .purple]) {
        self.id = id
        self.contactName = contactName
        self.contactHandle = contactHandle
        self.lastMessage = lastMessage
        self.timestamp = timestamp
        self.unreadCount = unreadCount
        self.isOnline = isOnline
        self.isPinned = isPinned
        self.avatarGradient = avatarGradient
    }
}

// MARK: - DirectMessagesView

public struct DirectMessagesView: View {
    @State private var searchText: String = ""
    @State private var showNewChatSheet: Bool = false
    @State private var selectedContactForQuickMsg: String = ""
    @State private var quickMessageText: String = ""
    @State private var alertToastMessage: String? = nil
    @State private var showAlertToast: Bool = false

    @State private var conversations: [DirectConversationItem] = []
    private var onConversationClick: ((DirectConversationItem) -> Void)?
    private var onNewChatClick: (() -> Void)?

    public init(
        onConversationClick: ((DirectConversationItem) -> Void)? = nil,
        onNewChatClick: (() -> Void)? = nil
    ) {
        self.onConversationClick = onConversationClick
        self.onNewChatClick = onNewChatClick
    }

    private var filteredConversations: [DirectConversationItem] {
        conversations.filter { conv in
            searchText.isEmpty ||
            conv.contactName.localizedCaseInsensitiveContains(searchText) ||
            conv.contactHandle.localizedCaseInsensitiveContains(searchText) ||
            conv.lastMessage.localizedCaseInsensitiveContains(searchText)
        }
    }

    private var pinnedConversations: [DirectConversationItem] {
        filteredConversations.filter(\.isPinned)
    }

    private var otherConversations: [DirectConversationItem] {
        filteredConversations.filter { !$0.isPinned }
    }

    public var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    // Search Bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField("Search direct messages & contacts...", text: $searchText)
                            .textFieldStyle(.plain)
                        if !searchText.isEmpty {
                            Button(action: { searchText = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(10)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .padding(.bottom, 8)

                    // Conversation List
                    List {
                        if !pinnedConversations.isEmpty {
                            Section(header: Label("PINNED CHATS", systemImage: "pin.fill").font(.caption.bold())) {
                                ForEach(pinnedConversations) { item in
                                    conversationRowView(item)
                                }
                            }
                        }

                        Section(header: Text("RECENT MESSAGES").font(.caption.bold())) {
                            ForEach(otherConversations) { item in
                                conversationRowView(item)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }

                // New Direct Message Floating Button
                Button(action: {
                    onNewChatClick?()
                    showNewChatSheet = true
                }) {
                    Image(systemName: "square.and.pencil")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.blue)
                        .clipShape(Circle())
                        .shadow(radius: 6)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
            .navigationTitle("Direct Messages")
            .alert("Direct Message Notice", isPresented: $showAlertToast) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(alertToastMessage ?? "")
            }
            .sheet(isPresented: $showNewChatSheet) {
                newDirectMessageSheet
            }
            .task { await loadDirectChats() }
        }
    }

    private func loadDirectChats() async {
        _ = await AppRuntime.chatRepository.syncChats()
        let loaded = await AppRuntime.chatRepository.getChats()
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        conversations = (loaded.data ?? [])
            .filter { $0.type == .direct && !$0.isMingle }
            .map { chat in
                DirectConversationItem(
                    id: chat.otherUserId?.isEmpty == false ? chat.otherUserId! : chat.id,
                    contactName: chat.senderName.isEmpty ? "Direct" : chat.senderName,
                    contactHandle: chat.phoneNumber ?? "",
                    lastMessage: chat.lastMessage,
                    timestamp: chat.timestamp > 0 ? formatter.string(from: Date(timeIntervalSince1970: Double(chat.timestamp) / 1000)) : "",
                    unreadCount: chat.unreadCount,
                    isOnline: chat.isOnline,
                    isPinned: chat.isPinned
                )
            }
    }

    // MARK: - Subviews

    private func conversationRowView(_ item: DirectConversationItem) -> some View {
        HStack(spacing: 12) {
            ZStack(alignment: .bottomTrailing) {
                Circle()
                    .fill(LinearGradient(colors: item.avatarGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 48, height: 48)
                    .overlay(
                        Text(String(item.contactName.prefix(1)))
                            .font(.title3.bold())
                            .foregroundColor(.white)
                    )

                if item.isOnline {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 12, height: 12)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                }
            }

            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(item.contactName)
                        .font(.subheadline.bold())
                        .lineLimit(1)

                    if item.isPinned {
                        Image(systemName: "pin.fill")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Text(item.timestamp)
                        .font(.caption2)
                        .foregroundColor(item.unreadCount > 0 ? .blue : .secondary)
                }

                HStack {
                    Text(item.lastMessage)
                        .font(.subheadline)
                        .foregroundColor(item.unreadCount > 0 ? .primary : .secondary)
                        .fontWeight(item.unreadCount > 0 ? .semibold : .regular)
                        .lineLimit(1)

                    Spacer()

                    if item.unreadCount > 0 {
                        Text("\(item.unreadCount)")
                            .font(.caption2.bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Color.blue)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { onConversationClick?(item) }
        .padding(.vertical, 4)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                conversations.removeAll { $0.id == item.id }
            } label: {
                Label("Delete", systemImage: "trash")
            }

            Button {
                if let idx = conversations.firstIndex(where: { $0.id == item.id }) {
                    conversations[idx].isPinned.toggle()
                }
            } label: {
                Label(item.isPinned ? "Unpin" : "Pin", systemImage: "pin")
            }
            .tint(.orange)
        }
    }

    private var newDirectMessageSheet: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                Text("Start New Direct Message")
                    .font(.title2.bold())
                    .padding(.top)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Recipient Handle or Name")
                        .font(.caption.bold())
                        .foregroundColor(.secondary)

                    TextField("e.g. @alexmorgan or Sarah", text: $selectedContactForQuickMsg)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Initial Message")
                        .font(.caption.bold())
                        .foregroundColor(.secondary)

                    TextField("Type message here...", text: $quickMessageText)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                }

                Spacer()

                Button(action: {
                    if !selectedContactForQuickMsg.isEmpty && !quickMessageText.isEmpty {
                        conversations.insert(
                            DirectConversationItem(
                                id: UUID().uuidString,
                                contactName: selectedContactForQuickMsg.replacingOccurrences(of: "@", with: "").capitalized,
                                contactHandle: selectedContactForQuickMsg.hasPrefix("@") ? selectedContactForQuickMsg : "@\(selectedContactForQuickMsg)",
                                lastMessage: quickMessageText,
                                timestamp: "Just now",
                                unreadCount: 0,
                                isOnline: true,
                                isPinned: false,
                                avatarGradient: [.blue, .purple]
                            ),
                            at: 0
                        )
                        showNewChatSheet = false
                        alertToastMessage = "Direct message sent to \(selectedContactForQuickMsg)."
                        showAlertToast = true
                    }
                }) {
                    Text("Send Direct Message")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .navigationBarItems(trailing: Button("Cancel") { showNewChatSheet = false })
        }
        .presentationDetents([.height(380)])
    }
}

#Preview {
    DirectMessagesView()
}
