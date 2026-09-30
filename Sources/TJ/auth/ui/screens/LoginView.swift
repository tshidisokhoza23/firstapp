import SwiftUI

public struct LoginView: View {
    @StateObject private var viewModel: AuthViewModel
    private var onLoginSuccess: (AuthResponse?) -> Void
    private var onNavigateToOtp: (String, String?, String?, String?, String?, String?) -> Void

    @State private var username = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var displayName = ""
    @State private var phoneNumber = ""
    @State private var isPasswordVisible = false
    @State private var isConfirmPasswordVisible = false
    @State private var isSignUp = false
    @State private var showForgotPassword = false
    @State private var forgotPasswordId = ""
    @State private var alertMessage: String? = nil
    @State private var showAlert = false

    @MainActor public init(
        viewModel: AuthViewModel = AuthViewModel(),
        onLoginSuccess: @escaping (AuthResponse?) -> Void,
        onNavigateToOtp: @escaping (String, String?, String?, String?, String?, String?) -> Void
    ) {
        _viewModel = StateObject(wrappedValue: viewModel)
        self.onLoginSuccess = onLoginSuccess
        self.onNavigateToOtp = onNavigateToOtp
    }

    public var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: [Color.appBackgroundDarkTop, Color.appBackgroundDarkBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 40)

                    // App Logo Container
                    AppLogo(size: 90)
                        .shadow(color: .black.opacity(0.3), radius: 12, x: 0, y: 6)

                    VStack(spacing: 4) {
                        Text(isSignUp ? "Create Account" : "Welcome Back")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)

                        Text(isSignUp ? "Join TJ today" : "Login to continue")
                            .font(.system(size: 15))
                            .foregroundColor(.gray)

                        Text("by TPSK")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.gray.opacity(0.6))
                    }

                    // Info Card for Google Sign In
                    Button(action: {
                        alertMessage = "Manual login & phone registration is temporarily disabled. Please tap 'Continue with Google'."
                        showAlert = true
                    }) {
                        HStack(spacing: 12) {
                            Image(systemName: "person.circle.fill")
                                .foregroundColor(.appAccent)
                                .font(.system(size: 20))

                            Text("Sign in & sign up are currently supported via Google. Tap below to continue.")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.leading)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity)
                        .background(Color.appAccent.opacity(0.15))
                        .cornerRadius(12)
                    }

                    // Phone/Username Field (Disabled)
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Image(systemName: isSignUp ? "person" : "phone")
                                .foregroundColor(.gray)
                            TextField(isSignUp ? "Username" : "Phone Number", text: $username)
                                .disabled(true)
                                .foregroundColor(.gray)
                        }
                        .padding()
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(16)

                        if let err = viewModel.fieldErrors["username"] {
                            Text(err).font(.caption).foregroundColor(.red).padding(.leading, 12)
                        }
                    }

                    // Password Field (Disabled)
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Image(systemName: "lock")
                                .foregroundColor(.gray)
                            if isPasswordVisible {
                                TextField("Password", text: $password)
                                    .disabled(true)
                                    .foregroundColor(.gray)
                            } else {
                                SecureField("Password", text: $password)
                                    .disabled(true)
                                    .foregroundColor(.gray)
                            }
                            Button(action: { isPasswordVisible.toggle() }) {
                                Image(systemName: isPasswordVisible ? "eye" : "eye.slash")
                                    .foregroundColor(.gray)
                            }
                        }
                        .padding()
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(16)
                    }

                    if !isSignUp {
                        HStack {
                            Spacer()
                            Button("Forgot Password?") {
                                showForgotPassword = true
                            }
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.appAccent)
                        }
                    }

                    Spacer().frame(height: 12)

                    // Continue with Google Button
                    Button(action: {
                        viewModel.googleSignIn(idToken: "sample_token")
                    }) {
                        HStack(spacing: 12) {
                            if case .loading = viewModel.authState {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Image(systemName: "g.circle.fill")
                                    .font(.system(size: 22))
                                    .foregroundColor(.white)
                                Text("Continue with Google")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            LinearGradient(
                                colors: [Color.logoBlueDark, Color.appAccent],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(16)
                    }

                    HStack(spacing: 4) {
                        Text("Registration & phone login disabled.")
                            .font(.system(size: 13))
                            .foregroundColor(.gray)
                        Button("Use Google") {
                            alertMessage = "Registration and login are managed automatically via Google. Please tap 'Continue with Google'."
                            showAlert = true
                        }
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.appAccent)
                    }

                    Spacer().frame(height: 32)
                }
                .padding(.horizontal, 32)
            }
        }
        .onReceive(viewModel.$authState) { state in
            if case .success(let response) = state {
                onLoginSuccess(response)
            }
        }
        .alert("Notice", isPresented: $showAlert) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(alertMessage ?? "")
        }
        .sheet(isPresented: $showForgotPassword) {
            VStack(spacing: 20) {
                Text("Reset Password")
                    .font(.title2.bold())

                Text("Enter your email or username and we'll send you instructions.")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .multilineTextAlignment(.center)

                TextField("Email or Username", text: $forgotPasswordId)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding()

                HStack {
                    Button("Cancel") {
                        showForgotPassword = false
                    }
                    Spacer()
                    Button("Send") {
                        if !forgotPasswordId.isEmpty {
                            viewModel.forgotPassword(emailOrUsername: forgotPasswordId)
                            showForgotPassword = false
                        }
                    }
                    .font(.headline)
                }
                .padding(.horizontal)
            }
            .padding()
            .presentationDetents([.height(280)])
        }
    }
}
