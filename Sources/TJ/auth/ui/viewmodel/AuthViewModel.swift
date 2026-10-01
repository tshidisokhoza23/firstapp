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
    private let api = AuthApi()

    public init() {}

    public func login(username: String, password: String) {
        authState = .loading()
        Task {
            do {
                let response = try await api.login(request: LoginRequest(username: username, password: password))
                self.saveToken(response)
                self.authState = .success(response)
            } catch {
                self.authState = .error(error.localizedDescription)
            }
        }
    }

    public func requestOTP(phoneNumber: String, countryCode: String? = nil) {
        lastRequestedPhone = phoneNumber
        otpState = .loading()
        Task {
            do {
                var body = ["phone_number": phoneNumber]
                if let countryCode, !countryCode.isEmpty { body["country_code"] = countryCode }
                let response = try await api.requestOTP(body: body)
                self.otpState = .success(response.message ?? response.status)
            } catch {
                self.otpState = .error(error.localizedDescription)
            }
        }
    }

    public func verifyOTP(phoneNumber: String, code: String, onVerified: @escaping () -> Void) {
        otpState = .loading()
        Task {
            do {
                _ = try await api.verifyOTP(body: ["phone_number": phoneNumber, "code": code])
                self.otpState = .success("Verified")
                onVerified()
            } catch {
                self.otpState = .error(error.localizedDescription)
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
            do {
                let response = try await api.register(request: RegisterRequest(
                    username: username,
                    email: email,
                    displayName: displayName,
                    phoneNumber: phoneNumber,
                    password: password,
                    countryCode: countryCode
                ))
                self.saveToken(response)
                self.authState = .success(response)
            } catch {
                self.authState = .error(error.localizedDescription)
            }
        }
    }

    public func googleSignIn(idToken: String) {
        authState = .loading()
        Task {
            do {
                let response = try await api.googleSignIn(idToken: idToken)
                self.saveToken(response)
                self.authState = .success(response)
            } catch {
                self.authState = .error(error.localizedDescription)
            }
        }
    }

    public func signInWithGoogle() {
        if authState?.isLoading == true { return }
        authState = .loading()
        Task {
            do {
                let idToken = try await GoogleSignInService.shared.signIn()
                let response = try await api.googleSignIn(idToken: idToken)
                self.saveToken(response)
                self.authState = .success(response)
            } catch {
                self.authState = .error(error.localizedDescription)
            }
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
            do {
                let user = try await api.completeProfile(
                    displayName: displayName,
                    username: username,
                    phoneNumber: phoneNumber,
                    categories: categories
                )
                SessionStore.shared.updateUser(user)
                let response = AuthResponse(
                    token: SessionStore.shared.token ?? "",
                    refreshToken: SessionStore.shared.refreshToken,
                    user: user
                )
                self.authState = .success(response)
            } catch {
                self.authState = .error(error.localizedDescription)
            }
        }
    }

    public func forgotPassword(emailOrUsername: String) {
        authState = .loading()
        Task {
            do {
                _ = try await api.forgotPassword(request: ForgotPasswordRequest(emailOrUsername: emailOrUsername))
                self.toastMessage = "Reset link sent!"
                self.authState = nil
            } catch {
                self.authState = .error(error.localizedDescription)
            }
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
        guard let authResponse else { return }
        SessionStore.shared.save(token: authResponse.token, refreshToken: authResponse.refreshToken, user: authResponse.user)
        AppRuntime.connectRealtime()
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
            do {
                let availability = try await api.checkAvailability(username: username, email: email, phone: phone)
                var allAvailable = !availability.isEmpty
                for (field, isAvailable) in availability {
                    if !isAvailable {
                        allAvailable = false
                        setFieldError(field: field, error: "\(field) is already taken")
                    } else {
                        setFieldError(field: field, error: nil)
                    }
                }
                self.authState = allAvailable ? nil : .error("Some details are already taken")
                onResult(allAvailable)
            } catch {
                self.authState = .error(error.localizedDescription)
                onResult(false)
            }
        }
    }
}
