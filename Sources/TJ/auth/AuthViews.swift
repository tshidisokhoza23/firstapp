import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.colorScheme) var colorScheme

    @State private var usernamePhone: String = ""
    @State private var passwordText: String = ""
    @State private var isSignUp: Bool = false
    @State private var showForgotPassword: Bool = false
    @State private var forgotPasswordId: String = ""
    @State private var isPasswordVisible: Bool = false

    // Gradient Brush matching Android LoginScreen
    let gradientBrush = LinearGradient(
        colors: [Color(red: 0.10, green: 0.14, blue: 0.49), Color(red: 0.0, green: 0.82, blue: 1.0)],
        startPoint: .leading,
        endPoint: .trailing
    )

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 60)

                // Logo Container Card
                ZStack {
                    RoundedRectangle(cornerRadius: 28)
                        .fill(Color(UIColor.systemBackground))
                        .frame(width: 120, height: 120)
                        .shadow(color: Color.black.opacity(0.15), radius: 12, x: 0, y: 6)

                    AppLogo(size: 40)
                }

                Spacer().frame(height: 32)

                // Headings
                Text(isSignUp ? "Create Account" : "Welcome Back")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.primary)

                Text(isSignUp ? "Join TJ today" : "Login to continue")
                    .font(.system(size: 15))
                    .foregroundColor(AppColors.greyMedium)
                    .padding(.top, 4)

                Text("by TPSK")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(AppColors.greyMedium.opacity(0.6))
                    .padding(.top, 2)

                Spacer().frame(height: 36)

                // Info Banner for Google Sign-In
                HStack(spacing: 12) {
                    Image(systemName: "person.fill")
                        .font(.system(size: 18))
                        .foregroundColor(AppColors.brandPrimary)

                    Text("Sign in & sign up are currently supported via Google. Tap below to continue.")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.primary)
                }
                .padding(14)
                .background(AppColors.brandPrimary.opacity(0.1))
                .cornerRadius(12)
                .padding(.horizontal, 24)

                Spacer().frame(height: 24)

                // Disabled Username / Phone Field
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: isSignUp ? "person.fill" : "phone.fill")
                            .foregroundColor(AppColors.greyMedium.opacity(0.6))
                        TextField(isSignUp ? "Username" : "Phone Number", text: $usernamePhone)
                            .disabled(true)
                    }
                    .padding()
                    .background(Color(UIColor.tertiarySystemBackground))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(AppColors.greyMedium.opacity(0.2), lineWidth: 1)
                    )
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 16)

                // Disabled Password Field
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: "lock.fill")
                            .foregroundColor(AppColors.greyMedium.opacity(0.6))

                        if isPasswordVisible {
                            TextField("Password", text: $passwordText)
                                .disabled(true)
                        } else {
                            SecureField("Password", text: $passwordText)
                                .disabled(true)
                        }

                        Button(action: { isPasswordVisible.toggle() }) {
                            Image(systemName: isPasswordVisible ? "eye.fill" : "eye.slash.fill")
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                    .padding()
                    .background(Color(UIColor.tertiarySystemBackground))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(AppColors.greyMedium.opacity(0.2), lineWidth: 1)
                    )
                }
                .padding(.horizontal, 24)

                if !isSignUp {
                    HStack {
                        Spacer()
                        Button(action: { showForgotPassword = true }) {
                            Text("Forgot Password?")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 8)
                }

                Spacer().frame(height: 32)

                // Primary Gradient Google Sign In Button
                Button(action: {
                    authViewModel.loginWithGoogle()
                }) {
                    HStack(spacing: 12) {
                        if authViewModel.isLoggingIn {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "g.circle.fill")
                                .font(.system(size: 24))
                                .foregroundColor(.white)

                            Text("Continue with Google")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(gradientBrush)
                    .cornerRadius(16)
                    .shadow(color: AppColors.brandAccent.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .padding(.horizontal, 24)
                .disabled(authViewModel.isLoggingIn)

                Spacer().frame(height: 24)

                // Footer Text
                HStack(spacing: 4) {
                    Text("Registration & phone login disabled. ")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.greyMedium)

                    Button(action: {
                        authViewModel.loginWithGoogle()
                    }) {
                        Text("Use Google")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }

                Spacer().frame(height: 40)
            }
        }
        .alert("Reset Password", isPresented: $showForgotPassword) {
            TextField("Email or Username", text: $forgotPasswordId)
            Button("Send", action: {})
            Button("Cancel", role: .cancel, action: {})
        } message: {
            Text("Enter your email or username and we'll send you instructions.")
        }
    }
}
