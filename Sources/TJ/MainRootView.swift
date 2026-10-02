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
        print("[MainRootView] Handling deep link URL: \(url.absoluteString)")
        let scheme = url.scheme ?? ""
        let host = url.host ?? ""
        let path = url.path

        if scheme == "firstapp" || scheme == "tj" {
            switch host {
            case "chat":
                let conversationId = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                navigationPath.append(RootRoute.chat(id: conversationId))
            case "join":
                let inviteCode = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                navigationPath.append(RootRoute.joinGroup(inviteCode: inviteCode))
            default:
                break
            }
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
