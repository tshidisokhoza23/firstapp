import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showingCountryPicker: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            AppLogo(size: 48)

            Text("Welcome to TJ")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(AppColors.brandPrimary)

            Text(authViewModel.isOtpSent ? "Enter the 6-digit verification code" : "Enter your phone number to log in")
                .font(.subheadline)
                .foregroundColor(AppColors.greyMedium)

            if !authViewModel.isOtpSent {
                HStack(spacing: 12) {
                    Button(action: { showingCountryPicker = true }) {
                        Text(authViewModel.selectedCountryCode)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 14)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(12)
                    }

                    TextField("Phone number", text: $authViewModel.phoneNumber)
                        .keyboardType(.phonePad)
                        .font(.headline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 14)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                }
                .padding(.horizontal)
            } else {
                TextField("6-digit OTP code", text: $authViewModel.otpCode)
                    .keyboardType(.numberPad)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 14)
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)
            }

            Button(action: {
                if authViewModel.isOtpSent {
                    authViewModel.verifyOTP()
                } else {
                    authViewModel.requestOTP()
                }
            }) {
                HStack {
                    if authViewModel.isLoggingIn {
                        ProgressView().tint(.white)
                    }
                    Text(authViewModel.isOtpSent ? "Verify & Sign In" : "Send OTP Code")
                        .font(.headline)
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(AppColors.brandPrimary)
                .cornerRadius(16)
            }
            .padding(.horizontal)
            .disabled(authViewModel.isLoggingIn)

            if authViewModel.isOtpSent {
                Button("Change Phone Number") {
                    authViewModel.isOtpSent = false
                }
                .font(.subheadline)
                .foregroundColor(AppColors.brandPrimary)
            }

            Spacer()
        }
        .sheet(isPresented: $showingCountryPicker) {
            CountryPickerView { selectedCountry in
                authViewModel.selectedCountryCode = selectedCountry.code
            }
        }
    }
}
