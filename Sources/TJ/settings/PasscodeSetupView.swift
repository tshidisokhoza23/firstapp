import SwiftUI

struct PasscodeSetupView: View {
    @Environment(\.dismiss) var dismiss
    var onSavePasscode: (String) -> Void

    @State private var pin: String = ""
    @State private var confirmPin: String = ""
    @State private var isConfirmStep: Bool = false
    @State private var errorMessage: String? = nil

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Spacer()

                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 64))
                    .foregroundColor(AppColors.brandPrimary)

                Text(isConfirmStep ? "Confirm 4-digit Passcode" : "Set 4-digit Passcode")
                    .font(.title2)
                    .fontWeight(.bold)

                SecureField("PIN", text: isConfirmStep ? $confirmPin : $pin)
                    .keyboardType(.numberPad)
                    .font(.title)
                    .multilineTextAlignment(.center)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 60)

                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }

                Button(action: {
                    if !isConfirmStep {
                        if pin.count == 4 {
                            isConfirmStep = true
                            errorMessage = nil
                        } else {
                            errorMessage = "Passcode must be exactly 4 digits."
                        }
                    } else {
                        if confirmPin == pin {
                            onSavePasscode(pin)
                            dismiss()
                        } else {
                            errorMessage = "Passcodes do not match. Try again."
                            confirmPin = ""
                        }
                    }
                }) {
                    Text(isConfirmStep ? "Save Passcode" : "Next")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppColors.brandPrimary)
                        .cornerRadius(16)
                }
                .padding(.horizontal, 40)

                Spacer()
            }
            .navigationTitle("Passcode Lock")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
