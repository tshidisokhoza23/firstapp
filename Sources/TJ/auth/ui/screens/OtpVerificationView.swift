import SwiftUI

public struct OtpVerificationView: View {
    @ObservedObject private var viewModel: AuthViewModel
    private var phoneNumber: String
    private var username: String?
    private var email: String?
    private var displayName: String?
    private var password: String?
    private var countryCode: String?
    private var onVerificationSuccess: () -> Void
    private var onBack: () -> Void

    @State private var otpCode: String = ""

    private var isRegistration: Bool {
        username != nil && password != nil
    }

    public init(
        phoneNumber: String,
        username: String? = nil,
        email: String? = nil,
        displayName: String? = nil,
        password: String? = nil,
        countryCode: String? = nil,
        viewModel: AuthViewModel,
        onVerificationSuccess: @escaping () -> Void,
        onBack: @escaping () -> Void
    ) {
        self.phoneNumber = phoneNumber
        self.username = username
        self.email = email
        self.displayName = displayName
        self.password = password
        self.countryCode = countryCode
        self.viewModel = viewModel
        self.onVerificationSuccess = onVerificationSuccess
        self.onBack = onBack
    }

    public var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Text("Verify Phone")
                .font(.system(size: 32, weight: .bold))

            Text("Enter the 6-digit code sent to your registered number (\(phoneNumber))")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            if let msg = viewModel.otpState?.message ?? viewModel.authState?.message {
                Text(msg)
                    .foregroundColor(.red)
                    .font(.caption)
            }

            TextField("6-Digit Code", text: $otpCode)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.system(size: 24, weight: .bold, design: .monospaced))
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)
                .onChange(of: otpCode) { newValue in
                    if newValue.count > 6 {
                        otpCode = String(newValue.prefix(6))
                    }
                    if otpCode.count == 6 {
                        verify()
                    }
                }

            Button(action: {
                verify()
            }) {
                HStack {
                    if viewModel.otpState?.isLoading == true {
                        ProgressView()
                    } else {
                        Text(isRegistration ? "Verify & Sign Up" : "Verify & Login")
                            .font(.system(size: 18, weight: .bold))
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(otpCode.count == 6 ? Color.appPrimary : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(otpCode.count != 6)

            HStack {
                Spacer()
                Button("Resend Code") {
                    viewModel.requestOTP(phoneNumber: phoneNumber, countryCode: countryCode)
                }
                .font(.system(size: 14))
                .foregroundColor(.appPrimary)
            }

            Button("Edit Phone Number") {
                onBack()
            }
            .font(.system(size: 14))
            .foregroundColor(.appPrimary)

            Spacer()
        }
        .padding(24)
        .onReceive(viewModel.$authState) { state in
            if case .success = state {
                onVerificationSuccess()
            }
        }
    }

    private func verify() {
        guard otpCode.count == 6 else { return }
        viewModel.verifyOTP(phoneNumber: phoneNumber, code: otpCode) {
            if isRegistration {
                viewModel.register(
                    username: username!,
                    email: email ?? "",
                    displayName: displayName ?? "",
                    phoneNumber: phoneNumber,
                    password: password!,
                    countryCode: countryCode
                )
            } else {
                viewModel.commitLogin()
            }
        }
    }
}
