import Foundation
import Combine
import SwiftUI

@MainActor
public final class AuthViewModel: ObservableObject {
    @Published public var authState: Resource<AuthResponse>? = nil
    @Published public var toastMessage: String? = nil
    @Published public var otpState: Resource<String>? = nil
    @Published public var fieldErrors: [String: String] = [:]

    public private(set) var lastRequestedPhone: String = ""
    private var pendingAuthResponse: AuthResponse? = nil

    public init() {}

    public func login(username: String, password: String) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            let user = User(id: "1", username: username, displayName: username)
            let response = AuthResponse(token: "token_abc", refreshToken: "refresh_xyz", user: user)
            self.authState = .success(response)
        }
    }

    public func requestOTP(phoneNumber: String, countryCode: String? = nil) {
        lastRequestedPhone = phoneNumber
        otpState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            self.otpState = .success("OTP sent to \(phoneNumber)")
        }
    }

    public func verifyOTP(phoneNumber: String, code: String, onVerified: @escaping () -> Void) {
        otpState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            if code.count == 6 {
                self.otpState = .success("Verified")
                onVerified()
            } else {
                self.otpState = .error("Verification failed. Invalid code.")
            }
        }
    }

    public func register(
        username: String,
        email: String,
        displayName: String,
        phoneNumber: String,
        password: String,
        countryCode: String? = nil
    ) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            let user = User(id: UUID().uuidString, username: username, email: email, displayName: displayName, phoneNumber: phoneNumber)
            let response = AuthResponse(token: "token_reg", refreshToken: "refresh_reg", user: user)
            self.saveToken(response)
            self.authState = .success(response)
        }
    }

    public func googleSignIn(idToken: String) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            let user = User(id: "google_123", username: "google_user", email: "user@gmail.com", displayName: "Google User")
            let response = AuthResponse(token: "google_token", refreshToken: "google_refresh", user: user)
            self.saveToken(response)
            self.authState = .success(response)
        }
    }

    public func completeProfile(
        displayName: String?,
        username: String?,
        phoneNumber: String?,
        categories: [String]? = nil
    ) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            let user = User(
                id: "1",
                username: username ?? "user",
                displayName: displayName ?? "User",
                phoneNumber: phoneNumber ?? ""
            )
            let response = AuthResponse(token: "session_token", refreshToken: "session_refresh", user: user)
            self.authState = .success(response)
        }
    }

    public func forgotPassword(emailOrUsername: String) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            self.toastMessage = "Reset link sent!"
            self.authState = nil
        }
    }

    public func clearToastMessage() {
        toastMessage = nil
    }

    public func commitLogin() {
        if let pending = pendingAuthResponse {
            saveToken(pending)
            authState = .success(pending)
            pendingAuthResponse = nil
        }
    }

    private func saveToken(_ authResponse: AuthResponse?) {
        // SessionManager persistence logic
    }

    public func resetState() {
        authState = nil
        otpState = nil
    }

    public func resetOtpState() {
        otpState = nil
    }

    public func setFieldError(field: String, error: String?) {
        if let err = error {
            fieldErrors[field] = err
        } else {
            fieldErrors.removeValue(forKey: field)
        }
    }

    public func clearFieldErrors() {
        fieldErrors.removeAll()
    }

    public func checkAvailability(
        username: String? = nil,
        email: String? = nil,
        phone: String? = nil,
        onResult: @escaping (Bool) -> Void = { _ in }
    ) {
        authState = .loading()
        Task {
            try? await Task.sleep(nanoseconds: 300_000_000)
            self.authState = nil
            onResult(true)
        }
    }
}
