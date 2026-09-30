import SwiftUI
import Combine

class AuthViewModel: ObservableObject {
    @Published var isAuthenticated: Bool = true
    @Published var phoneNumber: String = ""
    @Published var otpCode: String = ""
    @Published var currentUser: AppUser = AppUser.sample
    @Published var selectedCountryCode: String = "+1"

    func login() {
        Task { @MainActor in
            let _ = try? await NetworkClient.shared.login(phoneNumber: phoneNumber)
            self.isAuthenticated = true
        }
    }

    func logout() {
        isAuthenticated = false
    }
}
