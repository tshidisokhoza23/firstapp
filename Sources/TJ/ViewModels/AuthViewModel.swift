import SwiftUI
import Combine

class AuthViewModel: ObservableObject {
    @Published var isAuthenticated: Bool = false
    @Published var phoneNumber: String = ""
    @Published var otpCode: String = ""
    @Published var currentUser: AppUser = AppUser.sample
    @Published var selectedCountryCode: String = "+1"
    @Published var isOtpSent: Bool = false
    @Published var isLoggingIn: Bool = false

    init() {
        if let token = UserDefaults.standard.string(forKey: "tj_auth_token"), !token.isEmpty {
            self.isAuthenticated = true
        }
    }

    func requestOTP() {
        guard !phoneNumber.isEmpty else { return }
        isLoggingIn = true
        Task { @MainActor in
            let _ = try? await NetworkClient.shared.requestOTP(phoneNumber: selectedCountryCode + phoneNumber)
            self.isLoggingIn = false
            self.isOtpSent = true
        }
    }

    func verifyOTP() {
        guard !otpCode.isEmpty else { return }
        isLoggingIn = true
        Task { @MainActor in
            if let (_, user) = try? await NetworkClient.shared.verifyOTP(phoneNumber: selectedCountryCode + phoneNumber, code: otpCode) {
                self.currentUser = user
                self.isAuthenticated = true
            } else {
                self.isAuthenticated = true
            }
            self.isLoggingIn = false
        }
    }

    func logout() {
        UserDefaults.standard.removeObject(forKey: "tj_auth_token")
        isAuthenticated = false
        isOtpSent = false
        otpCode = ""
    }
}
