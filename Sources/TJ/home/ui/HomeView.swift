import SwiftUI

// MARK: - Local Data Models

struct HomeStoryItem: Identifiable {
    let id: String
    let name: String
    let avatarName: String
    let hasUnread: Bool
}

struct ChatSummaryItem: Identifiable {
    let id: String
    let name: String
    let lastMessage: String
    let timestamp: String
    let unreadCount: Int
    let isGroup: Bool
    let isPinned: Bool
}

// MARK: - Home View

public struct HomeView: View {
    @ObservedObject var router: NavigationRouter
    @State private var searchText: String = ""
    @State private var selectedFilter: ChatFilter = .all
    @State private var showNewChatSheet: Bool = false

    enum ChatFilter: String, CaseIterable, Identifiable {
        case all = "All"
        case unread = "Unread"
        case groups = "Groups"
        case channels = "Channels"

        var id: String { rawValue }
    }

    private let sampleStories: [HomeStoryItem] = [
        HomeStoryItem(id: "s1", name: "Alice", avatarName: "person.circle.fill", hasUnread: true),
        HomeStoryItem(id: "s2", name: "Bob", avatarName: "person.circle.fill", hasUnread: true),
        HomeStoryItem(id: "s3", name: "Charlie", avatarName: "person.circle.fill", hasUnread: false),
        HomeStoryItem(id: "s4", name: "Diana", avatarName: "person.circle.fill", hasUnread: true)
    ]

    private let sampleChats: [ChatSummaryItem] = [
        ChatSummaryItem(id: "c1", name: "Alice Smith", lastMessage: "Hey, are we still meeting today?", timestamp: "10:42 AM", unreadCount: 2, isGroup: false, isPinned: true),
        ChatSummaryItem(id: "c2", name: "Dev Team Lead", lastMessage: "Build deployed to production successfully.", timestamp: "9:15 AM", unreadCount: 0, isGroup: true, isPinned: true),
        ChatSummaryItem(id: "c3", name: "Bob Jones", lastMessage: "Sent the document link.", timestamp: "Yesterday", unreadCount: 0, isGroup: false, isPinned: false),
        ChatSummaryItem(id: "c4", name: "Product Announcements", lastMessage: "Version 2.0 release candidate is live!", timestamp: "Oct 12", unreadCount: 5, isGroup: false, isPinned: false)
    ]

    public init(router: NavigationRouter) {
        self.router = router
    }

    public var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    // Search Bar & Filters
                    VStack(spacing: 12) {
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.secondary)
                            TextField("Search chats, contacts, messages...", text: $searchText)
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

                        // Filter Pills
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(ChatFilter.allCases) { filter in
                                    Button(action: { selectedFilter = filter }) {
                                        Text(filter.rawValue)
                                            .font(.footnote)
                                            .fontWeight(selectedFilter == filter ? .bold : .medium)
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 6)
                                            .background(selectedFilter == filter ? Color.accentColor : Color(.tertiarySystemBackground))
                                            .foregroundColor(selectedFilter == filter ? .white : .primary)
                                            .cornerRadius(16)
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 8)

                    ScrollView {
                        VStack(spacing: 16) {
                            // Stories Horizontal Row
                            HomeStoriesRowView(stories: sampleStories)

                            Divider()
                                .padding(.horizontal, 16)

                            // Chat List
                            LazyVStack(spacing: 0) {
                                ForEach(filteredChats) { chat in
                                    ChatItemRowView(chat: chat) {
                                        router.navigate(to: .chatDetail(conversationId: chat.id, participantName: chat.name))
                                    }
                                    Divider()
                                        .padding(.leading, 72)
                                }
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }

                // Floating Action Button (FAB)
                Button(action: { showNewChatSheet = true }) {
                    Image(systemName: "square.and.pencil")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.accentColor)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.2), radius: 6, x: 0, y: 3)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
            .navigationTitle("TJ Messages")
            .navigationBarTitleDisplayMode(.large)
            .sheet(isPresented: $showNewChatSheet) {
                NewChatSheetView()
            }
        }
    }

    private var filteredChats: [ChatSummaryItem] {
        sampleChats.filter { chat in
            let matchesSearch = searchText.isEmpty || chat.name.localizedCaseInsensitiveContains(searchText) || chat.lastMessage.localizedCaseInsensitiveContains(searchText)
            switch selectedFilter {
            case .all: return matchesSearch
            case .unread: return matchesSearch && chat.unreadCount > 0
            case .groups: return matchesSearch && chat.isGroup
            case .channels: return matchesSearch && !chat.isGroup
            }
        }
    }
}

// MARK: - Subcomponents

struct HomeStoriesRowView: View {
    let stories: [HomeStoryItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Stories")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .padding(.horizontal, 16)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    // Add Story Button
                    VStack(spacing: 6) {
                        ZStack(alignment: .bottomTrailing) {
                            Circle()
                                .fill(Color.blue.opacity(0.15))
                                .frame(width: 58, height: 58)
                                .overlay(Image(systemName: "person.fill").foregroundColor(.blue))

                            Circle()
                                .fill(Color.accentColor)
                                .frame(width: 20, height: 20)
                                .overlay(Image(systemName: "plus").font(.caption).foregroundColor(.white))
                        }
                        Text("Your Story")
                            .font(.caption2)
                            .foregroundColor(.primary)
                    }
                    .padding(.leading, 16)

                    // Story Items
                    ForEach(stories) { story in
                        VStack(spacing: 6) {
                            Circle()
                                .fill(Color(.tertiarySystemBackground))
                                .frame(width: 58, height: 58)
                                .overlay(Image(systemName: story.avatarName).resizable().padding(8).foregroundColor(.gray))
                                .overlay(
                                    Circle()
                                        .stroke(story.hasUnread ? LinearGradient(colors: [.purple, .orange], startPoint: .topLeading, endPoint: .bottomTrailing) : LinearGradient(colors: [.gray.opacity(0.3)], startPoint: .center, endPoint: .center), lineWidth: 2.5)
                                )
                            Text(story.name)
                                .font(.caption2)
                                .lineLimit(1)
                        }
                    }
                }
            }
        }
    }
}

struct ChatItemRowView: View {
    let chat: ChatSummaryItem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Circle()
                    .fill(chat.isGroup ? Color.purple.opacity(0.2) : Color.blue.opacity(0.2))
                    .frame(width: 50, height: 50)
                    .overlay(Image(systemName: chat.isGroup ? "person.3.fill" : "person.fill").foregroundColor(chat.isGroup ? .purple : .blue))

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(chat.name)
                            .font(.body)
                            .fontWeight(chat.unreadCount > 0 ? .bold : .semibold)
                            .foregroundColor(.primary)

                        if chat.isPinned {
                            Image(systemName: "pin.fill")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        Text(chat.timestamp)
                            .font(.caption2)
                            .foregroundColor(chat.unreadCount > 0 ? .accentColor : .secondary)
                    }

                    HStack {
                        Text(chat.lastMessage)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(1)

                        Spacer()

                        if chat.unreadCount > 0 {
                            Text("\(chat.unreadCount)")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(Color.accentColor)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
    }
}

struct NewChatSheetView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Label("New Group", systemImage: "person.3.fill")
                    Label("New Contact", systemImage: "person.crop.circle.badge.plus")
                    Label("New Channel", systemImage: "megaphone.fill")
                }
            }
            .navigationTitle("New Message")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
