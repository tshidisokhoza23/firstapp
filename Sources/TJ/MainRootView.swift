import SwiftUI
import LocalAuthentication
import Contacts
import UserNotifications

// MARK: - Root Navigation Routes
public enum RootRoute: Hashable {
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
}

/// Root View managing authentication state, navigation hierarchy, active call banners, deep links, and local biometric security.
public struct MainRootView: View {
    @StateObject private var authViewModel = AuthViewModel()
    @ObservedObject private var globalCallState = GlobalCallState.shared

    @State private var navigationPath = NavigationPath()
    @State private var isAuthenticated: Bool = false
    @State private var isProfileComplete: Bool = true
    @State private var isBiometricLocked: Bool = false
    @State private var otpPhone: String? = nil
    @State private var selectedTab: Int = 0
    @State private var storyLaunch: StoryLaunch?
    @State private var joinCode: String?
    @State private var joinError = ""

    public init() {}

    public var body: some View {
        ZStack {
            if isBiometricLocked {
                BiometricLockOverlayView(onUnlockSuccess: {
                    isBiometricLocked = false
                })
            } else if isAuthenticated && !isProfileComplete {
                CompleteProfileView(
                    viewModel: authViewModel,
                    onCompleteSuccess: { isProfileComplete = true },
                    onNavigateToTerms: {}
                )
            } else if isAuthenticated {
                AppNavigation()
                    .environmentObject(globalCallState)
            } else if let phone = otpPhone {
                OtpVerificationView(
                    phoneNumber: phone,
                    viewModel: authViewModel,
                    onVerificationSuccess: {
                        otpPhone = nil
                        if SessionStore.shared.isLoggedIn {
                            handleLoginSuccess(response: AuthResponse(token: SessionStore.shared.token ?? "", user: SessionStore.shared.user))
                        }
                    },
                    onBack: { otpPhone = nil }
                )
            } else {
                LoginView(
                    viewModel: authViewModel,
                    onLoginSuccess: { response in
                        handleLoginSuccess(response: response)
                    },
                    onNavigateToOtp: { phone, _, _, _, _, _ in
                        otpPhone = phone
                    }
                )
            }

            if let incoming = globalCallState.incomingCall {
                IncomingCallView(
                    callInfo: incoming,
                    onAccept: { _ in CallSocketBridge.shared.acceptIncoming() },
                    onDecline: { _ in CallSocketBridge.shared.declineIncoming() }
                )
                .zIndex(100)
            } else if globalCallState.isCallActive {
                CallBannerOverlay()
                    .environmentObject(globalCallState)
                    .zIndex(100)
            }
        }
        .onOpenURL { url in
            handleDeepLink(url: url)
        }
        .onReceive(NotificationCenter.default.publisher(for: SessionStore.didChange)) { _ in
            DispatchQueue.main.async {
                self.checkAuthAndLockStatus()
            }
        }
        .onAppear {
            checkAuthAndLockStatus()
            PushNotificationHandler.shared.onOpenStory = { storyId, userId in
                guard !userId.isEmpty else { return }
                storyLaunch = StoryLaunch(userId: userId, storyId: storyId)
            }
            PushNotificationHandler.shared.onOpenChat = { conversationId, _ in
                guard !conversationId.isEmpty, conversationId != "0" else { return }
                NotificationCenter.default.post(name: TJEvents.openChat, object: nil, userInfo: ["id": conversationId])
            }
            PushNotificationHandler.shared.onOpenSocial = { postId, actorId in
                NotificationCenter.default.post(name: TJEvents.openPost, object: nil, userInfo: [
                    "id": postId ?? "",
                    "actor": actorId ?? ""
                ])
            }
            PushNotificationHandler.shared.onOpenMingle = { _, _ in
                NotificationCenter.default.post(name: TJEvents.openMingle, object: nil)
            }
            CallActionHandler.shared.onCallAccepted = { sessionId, senderId, callType in
                if GlobalCallState.shared.incomingCall == nil {
                    GlobalCallState.shared.incomingCall = IncomingCallInfo(id: sessionId, senderId: senderId, callerName: "", callType: callType)
                }
                CallSocketBridge.shared.acceptIncoming()
            }
            CallActionHandler.shared.onCallDeclined = { _, _ in
                CallSocketBridge.shared.declineIncoming()
            }
        }
        .fullScreenCover(item: $storyLaunch) { launch in
            StoryRowPlayer(startUserId: launch.userId, startStoryId: launch.storyId, onClose: { storyLaunch = nil })
        }
        .alert("Join group?", isPresented: Binding(get: { joinCode != nil }, set: { if !$0 { joinCode = nil } })) {
            Button("Join") { Task { await joinPendingGroup() } }
            Button("Cancel", role: .cancel) { joinCode = nil }
        } message: {
            Text("This opens the group from the invite link.")
        }
        .alert("Invite", isPresented: Binding(get: { !joinError.isEmpty }, set: { if !$0 { joinError = "" } })) {
            Button("OK", role: .cancel) { joinError = "" }
        } message: {
            Text(joinError)
        }
    }

    private func handleLoginSuccess(response: AuthResponse?) {
        isAuthenticated = true
        if let user = response?.user, user.displayName.isEmpty {
            isProfileComplete = false
        } else {
            isProfileComplete = true
        }
    }

    private func checkAuthAndLockStatus() {
        if SessionStore.shared.isLoggedIn {
            isAuthenticated = true
            if let user = SessionStore.shared.user, user.displayName.isEmpty {
                isProfileComplete = false
            } else {
                isProfileComplete = true
            }
            AppRuntime.connectRealtime()
            Task { await SessionActions.syncContacts() }
        } else {
            isAuthenticated = false
        }
        if UserDefaults.standard.bool(forKey: "enable_biometric_lock") {
            isBiometricLocked = true
        }
    }

    private func handleDeepLink(url: URL) {
        let scheme = url.scheme ?? ""
        let host = url.host ?? ""
        let path = url.path
        guard scheme == "firstapp" || scheme == "tj" else { return }
        switch host {
        case "chat":
            let conversationId = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            guard !conversationId.isEmpty else { return }
            NotificationCenter.default.post(name: TJEvents.openChat, object: nil, userInfo: ["id": conversationId])
        case "join":
            let inviteCode = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            if !inviteCode.isEmpty { joinCode = inviteCode }
        case "group":
            let parts = path.split(separator: "/").map(String.init)
            if parts.count >= 2, parts[0] == "invite", !parts[1].isEmpty {
                joinCode = parts[1]
            }
        default:
            break
        }
    }

    private func joinPendingGroup() async {
        guard let code = joinCode?.trimmingCharacters(in: .whitespacesAndNewlines), !code.isEmpty else { return }
        joinCode = nil
        do {
            let data = try await API.data("chats/groups/join/\(code)", method: "POST")
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            let conversation = LiveJSON.string(json["conversation_id"])
            let group = LiveJSON.string(json["group_id"])
            let id = conversation.isEmpty ? group : conversation
            guard !id.isEmpty else {
                joinError = "The server did not return the group"
                return
            }
            NotificationCenter.default.post(name: TJEvents.openChat, object: nil, userInfo: ["id": id])
        } catch {
            joinError = "Could not join this group"
        }
    }
}

public struct RootActiveCallOverlayBanner: View {
    @ObservedObject var callState: GlobalCallState

    public var body: some View {
        HStack {
            Text("Active Call")
                .font(.subheadline.bold())
                .foregroundColor(.white)
            Spacer()
            Button("End") {
                if let sid = callState.activeSessionId {
                    callState.onCallEnded(sessionId: sid)
                }
            }
            .font(.caption.bold())
            .foregroundColor(.red)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.white)
            .cornerRadius(12)
        }
        .padding()
        .background(Color.green)
        .cornerRadius(16)
        .padding(.horizontal)
        .padding(.top, 40)
    }
}

public struct BiometricLockOverlayView: View {
    public var onUnlockSuccess: () -> Void

    public var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "faceid")
                .font(.system(size: 64))
                .foregroundColor(.blue)

            Text("TJ is Locked")
                .font(.title2.bold())

            Text("Authenticate to access your conversations and security settings.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: authenticateWithBiometrics) {
                Text("Unlock with Face ID / Touch ID")
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.blue)
                    .cornerRadius(12)
                    .padding(.horizontal, 32)
            }
        }
    }

    private func authenticateWithBiometrics() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Unlock TJ App"
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, _ in
                if success {
                    DispatchQueue.main.async {
                        onUnlockSuccess()
                    }
                }
            }
        } else {
            onUnlockSuccess()
        }
    }
}
