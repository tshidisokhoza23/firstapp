import SwiftUI
import LocalAuthentication
import Contacts
import UserNotifications

// MARK: - App Navigation Routes
public enum AppRoute: Hashable {
    case login
    case completeProfile
    case mainTab(selectedTab: String? = nil)
    case chat(id: String, type: String = "direct")
    case postDetail(id: String)
    case advertDetail(id: String)
    case channelDetail(id: String)
    case groupDetail(id: String)
    case joinGroup(inviteCode: String)
    case userProfile(id: String)
    case story(userId: String?, storyId: String?)
    case mingleRequests
    case mingleNotification
    case socialNotification
}

/// Root SwiftUI View managing authentication state, tab navigation, call banners, deep links, and security locks.
struct MainRootView: View {
    @EnvironmentObject var appState: TJApplicationState

    @State private var isLoggedIn: Bool = false
    @State private var isAppUnlocked: Bool = false
    @State private var pinLockEnabled: Bool = false
    @State private var biometricLockEnabled: Bool = false
    @State private var appPin: String? = nil

    @State private var navigationPath = NavigationPath()
    @State private var activeTab: Int = 0
    @State private var currentUser: User? = nil
    @State private var isLoadingUser: Bool = false
    @State private var showOfflineOption: Bool = false

    // Incoming / Active Call state
    @State private var activeCallSessionId: String? = nil
    @State private var incomingCallSenderName: String? = nil
    @State private var isCallOverlayVisible: Bool = false

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                Color(uiColor: .systemBackground)
                    .ignoresSafeArea()

                if !isAppUnlocked && pinLockEnabled && appPin != nil {
                    PinLockView(correctPin: appPin!) {
                        isAppUnlocked = true
                    }
                } else if !isLoggedIn {
                    LoginRootView(onLoginSuccess: {
                        isLoggedIn = true
                        isAppUnlocked = true
                    })
                } else if currentUser == nil && isLoadingUser {
                    VStack(spacing: 16) {
                        ProgressView()
                            .scaleEffect(1.2)
                        if showOfflineOption {
                            Text("Connecting to server...")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Button("Continue Offline") {
                                // Proceed with cached data
                                isLoadingUser = false
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                } else {
                    // Main content tab view
                    MainTabView(selectedTab: $activeTab)
                        .environment(\.currentUser, currentUser)
                }

                // Call Banner / Call Overlay at top/bottom
                if isCallOverlayVisible, let callerName = incomingCallSenderName {
                    CallBannerOverlayView(callerName: callerName) {
                        // Accept
                        isCallOverlayVisible = false
                    } onDecline: {
                        // Decline
                        isCallOverlayVisible = false
                    }
                }
            }
            .navigationDestination(for: AppRoute.self) { route in
                destinationView(for: route)
            }
            .onOpenURL { url in
                handleDeepLink(url)
            }
            .onAppear {
                checkAuthAndLoadUser()
                requestAppPermissions()
            }
        }
    }

    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .login:
            Text("Login Screen")
        case .completeProfile:
            Text("Complete Profile Screen")
        case .mainTab:
            MainTabView(selectedTab: $activeTab)
        case .chat(let id, let type):
            Text("Chat Screen: \(id), Type: \(type)")
        case .postDetail(let id):
            Text("Post Detail: \(id)")
        case .advertDetail(let id):
            Text("Advert Detail: \(id)")
        case .channelDetail(let id):
            Text("Channel Detail: \(id)")
        case .groupDetail(let id):
            Text("Group Detail: \(id)")
        case .joinGroup(let inviteCode):
            Text("Join Group: \(inviteCode)")
        case .userProfile(let id):
            Text("User Profile: \(id)")
        case .story(let userId, let storyId):
            Text("Story View: User \(userId ?? "none"), Story \(storyId ?? "none")")
        case .mingleRequests:
            Text("Mingle Message Requests")
        case .mingleNotification:
            Text("Mingle Notification")
        case .socialNotification:
            Text("Social Notifications")
        }
    }

    // MARK: - Deep Link Parser
    private func handleDeepLink(_ url: URL) {
        print("[MainRootView] Handling deep link: \(url)")

        // 1. Custom scheme: firstapp://
        if url.scheme == "firstapp" {
            guard let host = url.host else { return }
            let pathComponents = url.pathComponents.filter { $0 != "/" }

            switch host {
            case "chat":
                if let id = pathComponents.first {
                    navigationPath.append(AppRoute.chat(id: id))
                }
            case "post":
                if let id = pathComponents.first {
                    navigationPath.append(AppRoute.postDetail(id: id))
                }
            case "song":
                activeTab = 1 // Stream / Music tab
            case "advert":
                if let id = pathComponents.first {
                    navigationPath.append(AppRoute.advertDetail(id: id))
                }
            case "channel":
                if let id = pathComponents.first {
                    navigationPath.append(AppRoute.channelDetail(id: id))
                }
            case "group":
                if pathComponents.count >= 2 && pathComponents[0] == "invite" {
                    navigationPath.append(AppRoute.joinGroup(inviteCode: pathComponents[1]))
                } else if let id = pathComponents.first {
                    navigationPath.append(AppRoute.groupDetail(id: id))
                }
            default:
                break
            }
        }

        // 2. Universal links: https://firstapp.com/...
        if url.scheme == "https" && url.host == "firstapp.com" {
            let path = url.path
            if path.hasPrefix("/post/") {
                if let id = url.pathComponents.last {
                    navigationPath.append(AppRoute.postDetail(id: id))
                }
            }
        }
    }

    // MARK: - Permissions Request
    private func requestAppPermissions() {
        // Notifications
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }

        // Contacts
        let store = CNContactStore()
        store.requestAccess(for: .contacts) { _, _ in }
    }

    private func checkAuthAndLoadUser() {
        // Mock auth check or token check from UserDefaults/Keychain
        let token = UserDefaults.standard.string(forKey: "auth_token")
        if let token = token, !token.isEmpty {
            isLoggedIn = true
            isLoadingUser = true

            DispatchQueue.main.asyncAfter(deadline: .now() + 8.0) {
                if isLoadingUser {
                    showOfflineOption = true
                }
            }
        } else {
            isLoggedIn = false
            isAppUnlocked = true
        }
    }
}

// MARK: - LocalCurrentUser Environment Key
private struct CurrentUserKey: EnvironmentKey {
    static let defaultValue: User? = nil
}

extension EnvironmentValues {
    var currentUser: User? {
        get { self[CurrentUserKey.self] }
        set { self[CurrentUserKey.self] = newValue }
    }
}

// MARK: - Helper Views (PIN Lock View & Call Overlay View)
struct PinLockView: View {
    let correctPin: String
    let onUnlocked: () -> Void

    @State private var enteredPin: String = ""
    @State private var showError: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Text("Enter PIN")
                .font(.title2)
                .bold()

            HStack(spacing: 16) {
                ForEach(0..<4, id: \.self) { index in
                    Circle()
                        .fill(index < enteredPin.count ? Color.blue : Color.gray.opacity(0.3))
                        .frame(width: 16, height: 16)
                }
            }

            if showError {
                Text("Incorrect PIN")
                    .foregroundColor(.red)
                    .font(.caption)
            }

            LazyVGrid(columns: Array(repeating: GridItem(.fixed(70)), count: 3), spacing: 16) {
                ForEach(["1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK"], id: \.self) { key in
                    Button(action: {
                        handleKey(key)
                    }) {
                        Text(key)
                            .font(.title3)
                            .bold()
                            .frame(width: 64, height: 64)
                            .background(Color(uiColor: .secondarySystemBackground))
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding()
        .background(Color(uiColor: .systemBackground))
    }

    private func handleKey(_ key: String) {
        if key == "C" {
            if !enteredPin.isEmpty { enteredPin.removeLast() }
        } else if key == "OK" {
            if enteredPin == correctPin {
                onUnlocked()
            } else {
                showError = true
                enteredPin = ""
            }
        } else if enteredPin.count < 4 {
            enteredPin.append(key)
            showError = false
        }
    }
}

struct CallBannerOverlayView: View {
    let callerName: String
    let onAccept: () -> Void
    let onDecline: () -> Void

    var body: some View {
        VStack {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(callerName)
                        .font(.headline)
                        .foregroundColor(.white)
                    Text("Incoming Call...")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer()
                Button(action: onDecline) {
                    Image(systemName: "phone.down.fill")
                        .padding()
                        .background(Color.red)
                        .clipShape(Circle())
                        .foregroundColor(.white)
                }
                Button(action: onAccept) {
                    Image(systemName: "phone.fill")
                        .padding()
                        .background(Color.green)
                        .clipShape(Circle())
                        .foregroundColor(.white)
                }
            }
            .padding()
            .background(Color.black.opacity(0.9))
            .cornerRadius(16)
            .shadow(radius: 10)
            .padding()

            Spacer()
        }
    }
}

struct MainTabView: View {
    @Binding var selectedTab: Int

    var body: some View {
        TabView(selection: $selectedTab) {
            Text("Chats Tab").tabItem { Label("Chats", systemImage: "message") }.tag(0)
            Text("Stream Tab").tabItem { Label("Stream", systemImage: "music.note") }.tag(1)
            Text("Calls Tab").tabItem { Label("Calls", systemImage: "phone") }.tag(2)
            Text("Settings Tab").tabItem { Label("Settings", systemImage: "gear") }.tag(3)
        }
    }
}

struct LoginRootView: View {
    let onLoginSuccess: () -> Void
    var body: some View {
        VStack {
            Text("Welcome to TJ")
                .font(.largeTitle)
                .bold()
            Button("Log In") {
                onLoginSuccess()
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
