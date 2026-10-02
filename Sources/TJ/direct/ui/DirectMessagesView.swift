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

    @State private var callLogs: [CallLog] = []
    @State private var callPeople: [ChatMessage] = []
    @State private var callError = ""

    public var body: some View {
        NavigationStack {
            Group {
                if visibleCalls.isEmpty && callPeople.isEmpty {
                    Text(callError.isEmpty ? "No calls yet" : callError)
                        .foregroundColor(.secondary)
                } else {
                    List {
                        ForEach(callPeople) { person in
                            Button {
                                ChatViewModel().startVoiceOrVideoCall(receiverId: person.otherUserId ?? person.id, callType: .voice, name: person.consistentName())
                            } label: {
                                Text(person.consistentName().isEmpty ? (person.phoneNumber ?? "") : person.consistentName())
                                    .lineLimit(1)
                            }
                        }
                        ForEach(visibleCalls) { call in
                        Button {
                            let receiver = call.contactId.isEmpty ? (call.calleeId.isEmpty ? call.callerId : call.calleeId) : call.contactId
                            ChatViewModel().startVoiceOrVideoCall(
                                receiverId: receiver,
                                callType: call.callType.lowercased() == "video" ? .video : .voice,
                                name: resolvedCallName(call)
                            )
                        } label: {
                            HStack {
                                AsyncImage(url: FeedPost.resolve(call.contactAvatar)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Circle().fill(Color.gray.opacity(0.3))
                                        .overlay(Text(String(resolvedCallName(call).prefix(1)).uppercased()).foregroundColor(.white))
                                }
                                .frame(width: 44, height: 44)
                                .clipShape(Circle())
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(resolvedCallName(call))
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(.primary)
                                        .lineLimit(1)
                                    Text(call.status.lowercased())
                                        .font(.caption)
                                        .foregroundColor(call.status.lowercased() == "missed" ? .red : .green)
                                }
                                Spacer()
                                Image(systemName: call.callType.lowercased() == "video" ? "video.fill" : "phone.fill")
                                    .foregroundColor(.accentColor)
                            }
                        }
                        }
                    }
                }
            }
            .navigationTitle("Calls")
            .searchable(text: $searchText, prompt: "Search calls and people")
            .onChange(of: searchText) { query in
                Task { await searchPeople(query) }
            }
            .task { await loadCallLogs() }
        }
    }

    @MainActor
    private func resolvedCallName(_ call: CallLog) -> String {
        let cached = GlobalCallState.shared.contactNames[call.contactId]
        let phone = GlobalCallState.shared.contactPhones[call.contactId] ?? ""
        let local = phone.isEmpty ? nil : ContactSyncManager.shared.getContactNameByNumber(phone)
        if let local, !local.isEmpty { return local }
        if !isIdString(call.contactName, id: call.contactId), !call.contactName.isEmpty, call.contactName.lowercased() != "chat", call.contactName.lowercased() != "direct" {
            return call.contactName
        }
        if let cached, !cached.isEmpty, !isIdString(cached, id: call.contactId), cached.lowercased() != "chat", cached.lowercased() != "direct" { return cached }
        if !phone.isEmpty { return phone }
        return ""
    }

    private var visibleCalls: [CallLog] {
        callLogs.filter { searchText.isEmpty || resolvedCallName($0).localizedCaseInsensitiveContains(searchText) || $0.status.localizedCaseInsensitiveContains(searchText) }
    }

    private func searchPeople(_ query: String) async {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            callPeople = []
            return
        }
        guard let data = try? await API.data("search", query: ["query": trimmed, "type": "chats"]) else { return }
        let rows = (LiveJSON.object(data)["contacts"] as? [[String: Any]]) ?? []
        callPeople = rows.compactMap { row in
            let id = LiveJSON.string(row["id"])
            guard !id.isEmpty else { return nil }
            let phone = LiveJSON.string(row["phone_number"])
            let name = LiveJSON.string(row["display_name"])
            return ChatMessage(id: id, senderName: name.isEmpty ? phone : name, avatarUrl: LiveJSON.string(row["avatar_url"]).nilIfEmpty, phoneNumber: phone, otherUserId: id)
        }
    }

    private func loadCallLogs() async {
        do {
            let data = try await API.data("calls")
            let rows = LiveJSON.list(data, keys: ["calls", "data"])
            callLogs = rows.map { json in
                CallLog(
                    id: LiveJSON.string(json["id"]).isEmpty ? LiveJSON.string(json["session_id"]) : LiveJSON.string(json["id"]),
                    sessionId: LiveJSON.string(json["session_id"]),
                    callerId: LiveJSON.string(json["caller_id"]),
                    calleeId: LiveJSON.string(json["callee_id"]),
                    contactId: LiveJSON.string(json["contact_id"]),
                    contactName: LiveJSON.string(json["contact_name"]),
                    contactAvatar: LiveJSON.string(json["contact_avatar"]).isEmpty ? nil : LiveJSON.string(json["contact_avatar"]),
                    callType: LiveJSON.string(json["call_type"]).isEmpty ? "voice" : LiveJSON.string(json["call_type"]),
                    status: LiveJSON.string(json["status"]),
                    durationSeconds: Int(LiveJSON.int64(json["duration_seconds"])),
                    timestamp: LiveJSON.int64(json["started_at"])
                )
            }
        } catch {
            callError = error.localizedDescription
        }
    }

    private var unusedDirectBody: some View {
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

        let allChats = loaded.data ?? []
        let filtered = allChats.filter { $0.type == .direct && !$0.isMingle }

        var items: [DirectConversationItem] = []
        for chat in filtered {
            let cid = chat.otherUserId?.isEmpty == false ? chat.otherUserId! : chat.id
            let name = chat.senderName.isEmpty ? "Direct" : chat.senderName
            let phone = chat.phoneNumber ?? ""
            let timeStr = chat.timestamp > 0 ? formatter.string(from: Date(timeIntervalSince1970: Double(chat.timestamp) / 1000)) : ""
            let item = DirectConversationItem(
                id: cid,
                contactName: name,
                contactHandle: phone,
                lastMessage: chat.lastMessage,
                timestamp: timeStr,
                unreadCount: chat.unreadCount,
                isOnline: chat.isOnline,
                isPinned: chat.isPinned
            )
            items.append(item)
        }
        self.conversations = items
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
