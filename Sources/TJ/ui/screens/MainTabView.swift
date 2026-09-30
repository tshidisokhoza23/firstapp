import SwiftUI

// MARK: - Tab Items

public enum TabItem: Int, CaseIterable, Identifiable {
    case home = 0
    case calls = 1
    case channels = 2
    case profile = 3
    case settings = 4

    public var id: Int { rawValue }

    public var title: String {
        switch self {
        case .home: return "Chats"
        case .calls: return "Calls"
        case .channels: return "Channels"
        case .profile: return "Profile"
        case .settings: return "Settings"
        }
    }

    public var iconName: String {
        switch self {
        case .home: return "message.fill"
        case .calls: return "phone.fill"
        case .channels: return "megaphone.fill"
        case .profile: return "person.crop.circle.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

// MARK: - Main Tab View

public struct MainTabView: View {
    @ObservedObject var router: NavigationRouter
    @StateObject private var globalCallState = GlobalCallState.shared
    @State private var selectedTab: TabItem = .home

    let initialTab: String?
    let initialSubTab: String?

    public init(router: NavigationRouter, initialTab: String? = nil, initialSubTab: String? = nil) {
        self.router = router
        self.initialTab = initialTab
        self.initialSubTab = initialSubTab
    }

    public var body: some View {
        ZStack(alignment: .top) {
            TabView(selection: $selectedTab) {
                HomeView(router: router)
                    .tabItem {
                        Label(TabItem.home.title, systemImage: TabItem.home.iconName)
                    }
                    .tag(TabItem.home)

                CallsHistoryTabView()
                    .tabItem {
                        Label(TabItem.calls.title, systemImage: TabItem.calls.iconName)
                    }
                    .tag(TabItem.calls)

                ChannelsTabView()
                    .tabItem {
                        Label(TabItem.channels.title, systemImage: TabItem.channels.iconName)
                    }
                    .tag(TabItem.channels)

                ProfileView(userId: "currentUser", router: router)
                    .tabItem {
                        Label(TabItem.profile.title, systemImage: TabItem.profile.iconName)
                    }
                    .tag(TabItem.profile)

                SettingsView(router: router)
                    .tabItem {
                        Label(TabItem.settings.title, systemImage: TabItem.settings.iconName)
                    }
                    .tag(TabItem.settings)
            }
            .tint(.accentColor)

            // Global Active Call Overlay Banner
            if globalCallState.isCallActive {
                ActiveCallOverlayBanner()
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(10)
            }
        }
        .onAppear {
            if let tabName = initialTab?.lowercased() {
                switch tabName {
                case "calls": selectedTab = .calls
                case "channels": selectedTab = .channels
                case "profile": selectedTab = .profile
                case "settings": selectedTab = .settings
                default: selectedTab = .home
                }
            }
        }
    }
}

// MARK: - Sub-Tab Views & Overlays

struct CallsHistoryTabView: View {
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("Recent Calls")) {
                    HStack(spacing: 12) {
                        Image(systemName: "phone.arrow.down.left.fill")
                            .foregroundColor(.green)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Alice Smith")
                                .fontWeight(.semibold)
                            Text("Incoming • Today, 10:14 AM")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "info.circle")
                            .foregroundColor(.blue)
                    }

                    HStack(spacing: 12) {
                        Image(systemName: "phone.arrow.up.right.fill")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Bob Jones")
                                .fontWeight(.semibold)
                            Text("Outgoing • Yesterday, 4:30 PM")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "info.circle")
                            .foregroundColor(.blue)
                    }
                }
            }
            .navigationTitle("Calls")
        }
    }
}

struct ChannelsTabView: View {
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("Subscribed Channels")) {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(LinearGradient(colors: [.purple, .blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 44, height: 44)
                            .overlay(Image(systemName: "bolt.fill").foregroundColor(.white))
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Tech Updates & News")
                                .fontWeight(.semibold)
                            Text("12.4K subscribers")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Channels")
        }
    }
}

struct ActiveCallOverlayBanner: View {
    @ObservedObject var globalCallState = GlobalCallState.shared

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color.green)
                .frame(width: 10, height: 10)

            Text("Active Call • \(globalCallState.callDurationSeconds)s")
                .font(.footnote)
                .fontWeight(.bold)
                .foregroundColor(.white)

            Spacer()

            Button("Return") {
                // Return to active call screen
            }
            .font(.caption)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(Color.white.opacity(0.2))
            .cornerRadius(12)
            .foregroundColor(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.green)
        .cornerRadius(16)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .shadow(radius: 4)
    }
}
