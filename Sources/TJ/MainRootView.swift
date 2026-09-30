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
    @StateObject private var globalCallState = GlobalCallState.shared

    @State private var navigationPath = NavigationPath()
    @State private var isAuthenticated: Bool = false
    @State private var isProfileComplete: Bool = true
    @State private var isBiometricLocked: Bool = false
    @State private var selectedTab: Int = 0

    public init() {}

    public var body: some View {
        ZStack {
            if isBiometricLocked {
                BiometricLockOverlayView(onUnlockSuccess: {
                    isBiometricLocked = false
                })
            } else {
                if isAuthenticated {
                    AppNavigation()
                        .environmentObject(globalCallState)
                } else {
                    LoginView(
                        viewModel: authViewModel,
                        onLoginSuccess: { response in
                            handleLoginSuccess(response: response)
                        },
                        onNavigateToOtp: { phone, _, _, _, _, _ in
                            authViewModel.requestOTP(phoneNumber: phone)
                        }
                    )
                }
            }

            // Global Active/Incoming Call Overlay Banner
            if globalCallState.isCallActive {
                RootActiveCallOverlayBanner(callState: globalCallState)
            }
        }
        .onOpenURL { url in
            handleDeepLink(url: url)
        }
        .onAppear {
            checkAuthAndLockStatus()
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

// MARK: - Biometric Lock Overlay View

struct BiometricLockOverlayView: View {
    let onUnlockSuccess: () -> Void
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 64))
                .foregroundColor(.accentColor)

            Text("TJ is Locked")
                .font(.title)
                .bold()

            Text("Authenticate with Face ID or Touch ID to continue")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button(action: authenticateWithBiometrics) {
                Label("Unlock with Biometrics", systemImage: "faceid")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
        }
        .padding()
        .onAppear {
            authenticateWithBiometrics()
        }
    }

    private func authenticateWithBiometrics() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Unlock TJ application"
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        onUnlockSuccess()
                    } else {
                        errorMessage = authenticationError?.localizedDescription ?? "Biometric authentication failed"
                    }
                }
            }
        } else {
            errorMessage = error?.localizedDescription ?? "Biometrics not available on this device"
        }
    }
}

// MARK: - Active Call Overlay Banner

struct RootActiveCallOverlayBanner: View {
    @ObservedObject var callState: GlobalCallState

    var body: some View {
        VStack {
            HStack {
                Image(systemName: "phone.fill")
                    .foregroundColor(.green)
                VStack(alignment: .leading) {
                    Text(callState.activeParticipantId ?? "Active Call")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text("\(callState.callDurationSeconds) seconds")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer()
                Button(action: {
                    if let sid = callState.activeSessionId {
                        callState.onCallEnded(sessionId: sid)
                    }
                }) {
                    Image(systemName: "phone.down.fill")
                        .padding()
                        .background(Color.red)
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
