import SwiftUI

struct ChangePhoneWizardView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authViewModel: AuthViewModel

    @State private var step: Int = 1 // 1: Warning, 2: New Number, 3: OTP, 4: Done
    @State private var newPhoneNumber: String = ""
    @State private var otpCode: String = ""
    @State private var showingCountryPicker: Bool = false

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()

                if step == 1 {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(.orange)

                    Text("Change Phone Number")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Your account, messages, media, and contacts will move to your new phone number.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.greyMedium)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    Button("Next") { step = 2 }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppColors.brandPrimary)
                        .cornerRadius(16)
                        .padding(.horizontal)
                } else if step == 2 {
                    Text("Enter New Phone Number")
                        .font(.title2)
                        .fontWeight(.bold)

                    HStack(spacing: 12) {
                        Button(action: { showingCountryPicker = true }) {
                            Text(authViewModel.selectedCountryCode)
                                .font(.headline)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 14)
                                .background(Color(UIColor.secondarySystemBackground))
                                .cornerRadius(12)
                        }

                        TextField("New phone number", text: $newPhoneNumber)
                            .keyboardType(.phonePad)
                            .font(.headline)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 14)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(12)
                    }
                    .padding(.horizontal)

                    Button("Send OTP Code") { step = 3 }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppColors.brandPrimary)
                        .cornerRadius(16)
                        .padding(.horizontal)
                        .disabled(newPhoneNumber.isEmpty)
                } else if step == 3 {
                    Text("Verify SMS Code")
                        .font(.title2)
                        .fontWeight(.bold)

                    TextField("6-digit OTP", text: $otpCode)
                        .keyboardType(.numberPad)
                        .font(.title)
                        .multilineTextAlignment(.center)
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        .padding(.horizontal)

                    Button("Confirm Number Change") {
                        authViewModel.currentUser.phoneNumber = authViewModel.selectedCountryCode + newPhoneNumber
                        step = 4
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.brandPrimary)
                    .cornerRadius(16)
                    .padding(.horizontal)
                } else {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 64))
                        .foregroundColor(.green)

                    Text("Phone Number Updated!")
                        .font(.title2)
                        .fontWeight(.bold)

                    Button("Done") { dismiss() }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.brandPrimary)
                }

                Spacer()
            }
            .navigationTitle("Change Number")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if step < 4 {
                        Button("Cancel") { dismiss() }
                    }
                }
            }
            .sheet(isPresented: $showingCountryPicker) {
                CountryPickerView { country in
                    authViewModel.selectedCountryCode = country.code
                }
            }
        }
    }
}
