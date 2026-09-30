import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showingOtpStep: Bool = false
    @State private var showingCountryPicker: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            AppLogo(size: 48)

            Text("Welcome to TJ")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(AppColors.brandPrimary)

            Text("Enter your phone number to get started")
                .font(.subheadline)
                .foregroundColor(AppColors.greyMedium)

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

            if showingOtpStep {
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
                if showingOtpStep {
                    authViewModel.login()
                } else {
                    showingOtpStep = true
                }
            }) {
                Text(showingOtpStep ? "Verify & Continue" : "Send OTP Code")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.brandPrimary)
                    .cornerRadius(16)
            }
            .padding(.horizontal)

            Spacer()
        }
        .sheet(isPresented: $showingCountryPicker) {
            CountryPickerView { selectedCountry in
                authViewModel.selectedCountryCode = selectedCountry.code
            }
        }
    }
}
