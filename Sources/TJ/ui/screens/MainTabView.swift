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
        case .chat: return "message.fill"
        case .social: return "globe"
        case .scroll: return "video.fill"
        case .mingle: return "sparkles"
        case .stream: return "music.note"
        case .ride: return "car.fill"
        case .more: return "ellipsis"
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
    @State private var showDirectMessages: Bool = false
    @State private var showNewChat: Bool = false
    @State private var showArchived: Bool = false

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
                                showNewChat = true
                            },
                            onArchivedClick: {
                                showArchived = true
                            },
                            onProfileClick: {
                                router.navigate(to: .profile(userId: "me"))
                            }
                        )

                    case .social:
                        ConnectedSocialFeedView(
                            onProfileClick: { id in
                                router.navigate(to: .profile(userId: id))
                            },
                            onDirectMessagesClick: {
                                showDirectMessages = true
                            }
                        )

                    case .scroll:
                        ConnectedShortsFeedView(
                            onProfileClick: { id in
                                router.navigate(to: .profile(userId: id))
                            }
                        )

                    case .mingle:
                        ConnectedMingleFeedView(
                            onProfileClick: { id in
                                router.navigate(to: .profile(userId: id))
                            },
                            onChatClick: { id in
                                router.navigate(to: .chatDetail(conversationId: id, participantName: "Mingle Contact"))
                            }
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

                // Custom Bottom Navigation Bar matching Android Screenshot 1
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
                    RootActiveCallOverlayBanner(callState: globalCallState)
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(10)
            }
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .sheet(isPresented: $showDirectMessages) {
            DirectMessagesView(
                onConversationClick: { item in
                    showDirectMessages = false
                    router.navigate(to: .chatDetail(conversationId: item.id, participantName: item.contactName))
                },
                onNewChatClick: {
                    showDirectMessages = false
                    showNewChat = true
                }
            )
        }
        .sheet(isPresented: $showNewChat) {
            ContactPickerView(
                onBack: { showNewChat = false },
                onContactSelected: { contact in
                    showNewChat = false
                    router.navigate(to: .chatDetail(conversationId: contact.id, participantName: contact.displayName))
                },
                onGroupCreate: { contacts in
                    showNewChat = false
                    chatViewModel.createGroup(name: "New Group", contactIds: contacts.map(\.id)) { chat in
                        router.navigate(to: .chatDetail(conversationId: chat.id, participantName: chat.senderName))
                    }
                }
            )
        }
        .sheet(isPresented: $showArchived) {
            ArchivedChatsView(
                viewModel: chatViewModel,
                onBack: { showArchived = false },
                onChatClick: { chat in
                    showArchived = false
                    router.navigate(to: .chatDetail(conversationId: chat.id, participantName: chat.displayName))
                }
            )
        }
        .sheet(isPresented: $showMoreSheet) {
            MoreShortcutsSheetView(
                onDismiss: { showMoreSheet = false },
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
            .presentationDetents([.height(340)])
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

// MARK: - Custom Bottom Navigation Bar Component matching Screenshot 1

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
                        ZStack {
                            if isSelected {
                                Capsule()
                                    .fill(Color.appBottomNavPill) // #D0E1FB
                                    .frame(width: 58, height: 32)
                            }

                            Image(systemName: item.iconName)
                                .font(.system(size: 18, weight: isSelected ? .bold : .regular))
                                .foregroundColor(isSelected ? Color.appPrimary : Color.secondary)
                        }
                        .frame(height: 32)

                        Text(item.title)
                            .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                            .foregroundColor(isSelected ? Color.appPrimary : Color.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.appBottomNavBg) // #EBF1FB
        .overlay(
            Rectangle()
                .frame(height: 0.5)
                .foregroundColor(Color.gray.opacity(0.2)),
            alignment: .top
        )
    }
}

// MARK: - More Shortcuts Sheet View matching Screenshot 4

public enum MoreShortcutRoute: String, Identifiable {
    case stream, ride, jobs, vocab, wallet, profile, settings
    public var id: String { rawValue }
}

public struct MoreShortcutsSheetView: View {
    public var onDismiss: () -> Void
    public var onSelectRoute: (MoreShortcutRoute) -> Void

    public init(onDismiss: @escaping () -> Void = {}, onSelectRoute: @escaping (MoreShortcutRoute) -> Void) {
        self.onDismiss = onDismiss
        self.onSelectRoute = onSelectRoute
    }

    private let shortcuts: [(route: MoreShortcutRoute, title: String, icon: String, color: Color, isEnabled: Bool)] = [
        (.wallet, "Wallet", "creditcard.fill", Color(red: 1.0, green: 0.8, blue: 0.4), true),
        (.vocab, "Vocab (Soon)", "book.fill", Color.gray.opacity(0.3), false),
        (.stream, "Stream (Soon)", "music.note", Color.gray.opacity(0.3), false),
        (.ride, "Ride (Soon)", "car.fill", Color.gray.opacity(0.3), false)
    ]

    public var body: some View {
        VStack(spacing: 20) {
            // Header: Shortcuts Title + X Close Button
            HStack {
                Text("Shortcuts")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.primary)

                Spacer()

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.secondary)
                        .padding(8)
                        .background(Color(UIColor.secondarySystemBackground))
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 20)

            // 3-Column Shortcuts Grid matching Screenshot 4
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 20) {
                ForEach(shortcuts, id: \.route) { item in
                    Button(action: {
                        if item.isEnabled {
                            onSelectRoute(item.route)
                        }
                    }) {
                        VStack(spacing: 10) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 18)
                                    .fill(item.isEnabled ? item.color.opacity(0.3) : Color(UIColor.secondarySystemBackground))
                                    .frame(width: 68, height: 68)

                                Image(systemName: item.icon)
                                    .font(.system(size: 26))
                                    .foregroundColor(item.isEnabled ? Color.orange : Color.gray.opacity(0.6))
                            }

                            Text(item.title)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(item.isEnabled ? .primary : .secondary)
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
