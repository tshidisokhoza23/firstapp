import SwiftUI

// MARK: - Bottom Navigation Item Enum (Matching firstapp's BottomNavItem)

public enum BottomNavItem: Int, CaseIterable, Identifiable {
    case chat = 0
    case social = 1
    case scroll = 2
    case mingle = 3
    case stream = 4
    case ride = 5
    case more = 6

    public var id: Int { rawValue }

    public var title: String {
        switch self {
        case .chat: return "Chat"
        case .social: return "Social"
        case .scroll: return "Scroll"
        case .mingle: return "Court"
        case .stream: return "Stream"
        case .ride: return "Ride"
        case .more: return "More"
        }
    }

    public var iconName: String {
        switch self {
        case .chat: return "bubble.left.and.bubble.right.fill"
        case .social: return "globe"
        case .scroll: return "play.tv.fill"
        case .mingle: return "sparkles"
        case .stream: return "music.note"
        case .ride: return "car.fill"
        case .more: return "ellipsis.circle.fill"
        }
    }

    public static var barItems: [BottomNavItem] {
        [.chat, .social, .scroll, .mingle, .more]
    }

    public static func fromIndex(_ index: Int) -> BottomNavItem {
        return BottomNavItem(rawValue: index) ?? .chat
    }
}

// MARK: - Main Tab View

public struct MainTabView: View {
    @ObservedObject var router: NavigationRouter
    @StateObject private var globalCallState = GlobalCallState.shared
    @StateObject private var chatViewModel = ChatViewModel()
    @StateObject private var rewardViewModel = RewardViewModel()

    @State private var selectedIndex: Int = 0
    @State private var lastActiveIndex: Int = 0
    @State private var showMoreSheet: Bool = false
    @State private var activeSheetRoute: MoreShortcutRoute? = nil

    public init(router: NavigationRouter, initialTab: String? = nil, initialSubTab: String? = nil) {
        self.router = router
    }

    private var currentTab: BottomNavItem {
        BottomNavItem.fromIndex(selectedIndex)
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                // Main Tab Content
                Group {
                    switch currentTab {
                    case .chat:
                        ChatTabView(
                            viewModel: chatViewModel,
                            rewardViewModel: rewardViewModel,
                            onChatSelected: { chat in
                                router.navigate(to: .chatDetail(conversationId: chat.id, participantName: chat.displayName))
                            },
                            onNewChatClick: {
                                // New chat action
                            },
                            onArchivedClick: {
                                // Archived chats
                            },
                            onProfileClick: {
                                router.navigate(to: .profile(userId: "me"))
                            }
                        )

                    case .social:
                        DirectMessagesView(
                            onConversationClick: { item in
                                router.navigate(to: .chatDetail(conversationId: item.id, participantName: item.contactName))
                            },
                            onNewChatClick: {}
                        )

                    case .scroll:
                        StoryViewer(
                            sampleStoryGroup: UserStory(
                                id: "story_feed",
                                userId: "user_feed",
                                userName: "Stories Feed",
                                stories: []
                            ),
                            onClose: {
                                selectedIndex = lastActiveIndex
                            }
                        )

                    case .mingle:
                        MingleView(
                            onMatchClick: { _ in },
                            onUpgradeClick: {}
                        )

                    case .stream:
                        MusicPlayerView()

                    case .ride:
                        DriveView()

                    case .more:
                        Color.clear
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Custom Bottom Navigation Bar matching firstapp
                CustomMainBottomBar(
                    selectedItem: currentTab,
                    onSelect: { item in
                        if item == .more {
                            showMoreSheet = true
                        } else {
                            lastActiveIndex = item.rawValue
                            selectedIndex = item.rawValue
                        }
                    }
                )
            }

            // Global Active Call Overlay Banner
            if globalCallState.isCallActive {
                VStack {
                    ActiveCallOverlayBanner()
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(10)
            }
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .sheet(isPresented: $showMoreSheet) {
            MoreShortcutsSheetView(
                onSelectRoute: { route in
                    showMoreSheet = false
                    switch route {
                    case .stream:
                        selectedIndex = BottomNavItem.stream.rawValue
                    case .ride:
                        selectedIndex = BottomNavItem.ride.rawValue
                    case .jobs:
                        activeSheetRoute = .jobs
                    case .vocab:
                        activeSheetRoute = .vocab
                    case .wallet:
                        activeSheetRoute = .wallet
                    case .profile:
                        router.navigate(to: .profile(userId: "me"))
                    case .settings:
                        router.navigate(to: .settings)
                    }
                }
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(item: $activeSheetRoute) { route in
            switch route {
            case .stream:
                MusicPlayerView()
            case .ride:
                DriveView()
            case .jobs:
                JobProfileView(onClose: { activeSheetRoute = nil })
            case .vocab:
                VocabView()
            case .wallet:
                WalletView()
            case .profile:
                ProfileView(userId: "me", router: router)
            case .settings:
                SettingsView(router: router)
            }
        }
    }
}

// MARK: - Custom Bottom Navigation Bar Component

public struct CustomMainBottomBar: View {
    public let selectedItem: BottomNavItem
    public let onSelect: (BottomNavItem) -> Void

    public init(selectedItem: BottomNavItem, onSelect: @escaping (BottomNavItem) -> Void) {
        self.selectedItem = selectedItem
        self.onSelect = onSelect
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(BottomNavItem.barItems) { item in
                let isSelected = selectedItem == item
                Button(action: { onSelect(item) }) {
                    VStack(spacing: 4) {
                        Image(systemName: item.iconName)
                            .font(.system(size: 20, weight: isSelected ? .bold : .regular))

                        Text(item.title)
                            .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .foregroundColor(isSelected ? .accentColor : .secondary)
                }
            }
        }
        .background(Color(uiColor: .systemBackground))
        .overlay(
            Rectangle()
                .frame(height: 0.5)
                .foregroundColor(Color.gray.opacity(0.3)),
            alignment: .top
        )
    }
}

// MARK: - More Shortcuts Sheet View & Routes

public enum MoreShortcutRoute: String, Identifiable {
    case stream, ride, jobs, vocab, wallet, profile, settings
    public var id: String { rawValue }
}

public struct MoreShortcutsSheetView: View {
    public var onSelectRoute: (MoreShortcutRoute) -> Void

    public init(onSelectRoute: @escaping (MoreShortcutRoute) -> Void) {
        self.onSelectRoute = onSelectRoute
    }

    private let shortcuts: [(route: MoreShortcutRoute, title: String, icon: String, color: Color)] = [
        (.stream, "Stream", "music.note", .purple),
        (.ride, "Ride / Drive", "car.fill", .blue),
        (.jobs, "Jobs Portal", "briefcase.fill", .indigo),
        (.vocab, "Vocab & Cards", "book.fill", .green),
        (.wallet, "Wallet & Rewards", "creditcard.fill", .orange),
        (.profile, "My Profile", "person.circle.fill", .teal),
        (.settings, "Settings", "gearshape.fill", .gray)
    ]

    public var body: some View {
        VStack(spacing: 16) {
            Text("More Services")
                .font(.system(size: 18, weight: .bold))
                .padding(.top, 16)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                ForEach(shortcuts, id: \.route) { item in
                    Button(action: { onSelectRoute(item.route) }) {
                        VStack(spacing: 8) {
                            ZStack {
                                Circle()
                                    .fill(item.color.opacity(0.15))
                                    .frame(width: 56, height: 56)
                                Image(systemName: item.icon)
                                    .font(.system(size: 24))
                                    .foregroundColor(item.color)
                            }
                            Text(item.title)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.primary)
                                .lineLimit(1)
                        }
                    }
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
    }
}
