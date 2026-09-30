import SwiftUI

// MARK: - Account Models

public struct ActiveSession: Identifiable {
    public let id: String
    public let deviceName: String
    public let location: String
    public let lastActive: String
    public let isCurrent: Bool

    public init(id: String, deviceName: String, location: String, lastActive: String, isCurrent: Bool = false) {
        self.id = id
        self.deviceName = deviceName
        self.location = location
        self.lastActive = lastActive
        self.isCurrent = isCurrent
    }
}

// MARK: - AccountView

public struct AccountView: View {
    // State Variables
    @State private var email: String = "user@example.com"
    @State private var isEmailVerified: Bool = false
    @State private var phoneNumber: String = "+1 (555) 019-2834"
    @State private var isTwoFactorEnabled: Bool = true

    // Sheet States
    @State private var showChangePhoneSheet: Bool = false
    @State private var showChangePasswordSheet: Bool = false
    @State private var showDeleteAccountAlert: Bool = false
    @State private var showVerificationSentAlert: Bool = false

    // Phone Change Form
    @State private var newPhoneNumber: String = ""
    @State private var phoneOtpCode: String = ""
    @State private var isOtpSent: Bool = false

    // Password Form
    @State private var currentPassword: String = ""
    @State private var newPassword: String = ""
    @State private var confirmPassword: String = ""

    // Toast / Banner
    @State private var alertMessage: String? = nil
    @State private var showAlert: Bool = false

    @State private var activeSessions: [ActiveSession] = [
        ActiveSession(id: "s1", deviceName: "iPhone 15 Pro (This Device)", location: "San Francisco, CA", lastActive: "Active Now", isCurrent: true),
        ActiveSession(id: "s2", deviceName: "MacBook Pro 16\"", location: "San Francisco, CA", lastActive: "2 hours ago"),
        ActiveSession(id: "s3", deviceName: "iPad Air 5th Gen", location: "San Jose, CA", lastActive: "3 days ago")
    ]

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header Status Card
                    statusHeaderCard

                    // Section 1: Contact & Verification
                    contactInfoSection

                    // Section 2: Security & Authentication
                    securitySection

                    // Section 3: Linked Accounts
                    linkedAccountsSection

                    // Section 4: Active Sessions
                    activeSessionsSection

                    // Section 5: Account Danger Zone
                    dangerZoneSection
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .navigationTitle("Account Settings")
            .navigationBarTitleDisplayMode(.inline)
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
            .alert("Account Notice", isPresented: $showAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(alertMessage ?? "")
            }
            .sheet(isPresented: $showChangePhoneSheet) {
                changePhoneSheetView
            }
            .sheet(isPresented: $showChangePasswordSheet) {
                changePasswordSheetView
            }
            .alert("Delete Account?", isPresented: $showDeleteAccountAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Delete", role: .destructive) {
                    alertMessage = "Account deletion request submitted. You will receive a confirmation email."
                    showAlert = true
                }
            } message: {
                Text("This action is permanent and will wipe all your data, chats, and files.")
            }
        }
    }

    // MARK: - Subviews

    private var statusHeaderCard: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 60, height: 60)
                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 36))
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Account Status")
                    .font(.headline)

                HStack(spacing: 6) {
                    Circle()
                        .fill(isEmailVerified ? Color.green : Color.orange)
                        .frame(width: 8, height: 8)
                    Text(isEmailVerified ? "Fully Verified Account" : "Action Needed: Verify Email")
                        .font(.subheadline)
                        .foregroundColor(isEmailVerified ? .green : .orange)
                }
            }
            Spacer()
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }

    private var contactInfoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("CONTACT INFORMATION")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .padding(.leading, 8)

            VStack(spacing: 0) {
                // Email Row
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Email Address")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(email)
                            .font(.body)
                    }
                    Spacer()

                    if isEmailVerified {
                        Label("Verified", systemImage: "checkmark.seal.fill")
                            .font(.caption)
                            .foregroundColor(.green)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green.opacity(0.12))
                            .cornerRadius(8)
                    } else {
                        Button("Verify Now") {
                            isEmailVerified = true
                            alertMessage = "A verification email has been sent to \(email)."
                            showAlert = true
                        }
                        .font(.caption.bold())
                        .foregroundColor(.blue)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.blue.opacity(0.12))
                        .cornerRadius(8)
                    }
                }
                .padding()

                Divider().padding(.leading, 16)

                // Phone Row
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Phone Number")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(phoneNumber)
                            .font(.body)
                    }
                    Spacer()

                    Button("Change") {
                        newPhoneNumber = ""
                        phoneOtpCode = ""
                        isOtpSent = false
                        showChangePhoneSheet = true
                    }
                    .font(.subheadline.bold())
                    .foregroundColor(.blue)
                }
                .padding()
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var securitySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SECURITY & LOGINS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .padding(.leading, 8)

            VStack(spacing: 0) {
                Toggle(isOn: $isTwoFactorEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Two-Factor Authentication")
                            .font(.body)
                        Text("Secure your account with SMS or Authenticator App")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding()

                Divider().padding(.leading, 16)

                Button(action: { showChangePasswordSheet = true }) {
                    HStack {
                        Text("Change Password")
                            .font(.body)
                            .foregroundColor(.primary)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var linkedAccountsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("LINKED ACCOUNTS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .padding(.leading, 8)

            VStack(spacing: 0) {
                HStack {
                    Label("Google Account", systemImage: "g.circle.fill")
                        .font(.body)
                    Spacer()
                    Text("Connected")
                        .font(.caption)
                        .foregroundColor(.green)
                }
                .padding()

                Divider().padding(.leading, 16)

                HStack {
                    Label("Apple ID", systemImage: "applelogo")
                        .font(.body)
                    Spacer()
                    Button("Connect") {
                        alertMessage = "Apple ID account linking initiated."
                        showAlert = true
                    }
                    .font(.caption.bold())
                    .foregroundColor(.blue)
                }
                .padding()
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var activeSessionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ACTIVE SESSIONS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .padding(.leading, 8)

            VStack(spacing: 0) {
                ForEach(activeSessions) { session in
                    HStack(spacing: 12) {
                        Image(systemName: session.deviceName.contains("iPhone") ? "iphone" : session.deviceName.contains("Mac") ? "laptopcomputer" : "ipad")
                            .font(.title3)
                            .foregroundColor(.blue)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(session.deviceName)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                            Text("\(session.location) • \(session.lastActive)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        if !session.isCurrent {
                            Button("Revoke") {
                                activeSessions.removeAll { $0.id == session.id }
                            }
                            .font(.caption)
                            .foregroundColor(.red)
                        }
                    }
                    .padding()

                    if session.id != activeSessions.last?.id {
                        Divider().padding(.leading, 16)
                    }
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var dangerZoneSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("DANGER ZONE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.red)
                .padding(.leading, 8)

            VStack(spacing: 0) {
                Button(action: {
                    alertMessage = "Account deactivated temporarily. Log back in to reactivate."
                    showAlert = true
                }) {
                    HStack {
                        Text("Deactivate Account")
                            .foregroundColor(.orange)
                        Spacer()
                        Image(systemName: "pause.circle")
                            .foregroundColor(.orange)
                    }
                    .padding()
                }

                Divider().padding(.leading, 16)

                Button(action: { showDeleteAccountAlert = true }) {
                    HStack {
                        Text("Delete Account Permanently")
                            .foregroundColor(.red)
                            .fontWeight(.semibold)
                        Spacer()
                        Image(systemName: "trash.fill")
                            .foregroundColor(.red)
                    }
                    .padding()
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    // MARK: - Change Phone Sheet

    private var changePhoneSheetView: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("Change Phone Number")
                    .font(.title2.bold())
                    .padding(.top)

                Text("Enter your new phone number below. We'll send an SMS verification code.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                VStack(spacing: 12) {
                    TextField("New Phone Number", text: $newPhoneNumber)
                        .keyboardType(.phonePad)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)

                    if isOtpSent {
                        TextField("6-Digit OTP Code", text: $phoneOtpCode)
                            .keyboardType(.numberPad)
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal)

                Button(action: {
                    if !isOtpSent {
                        if !newPhoneNumber.isEmpty {
                            isOtpSent = true
                        }
                    } else {
                        if !phoneOtpCode.isEmpty {
                            phoneNumber = newPhoneNumber
                            showChangePhoneSheet = false
                            alertMessage = "Phone number updated successfully."
                            showAlert = true
                        }
                    }
                }) {
                    Text(isOtpSent ? "Verify & Save" : "Send Verification Code")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                }
                .padding(.horizontal)

                Spacer()
            }
            .navigationBarItems(trailing: Button("Cancel") {
                showChangePhoneSheet = false
            })
        }
        .presentationDetents([.height(380)])
    }

    // MARK: - Change Password Sheet

    private var changePasswordSheetView: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Change Password")
                    .font(.title2.bold())
                    .padding(.top)

                SecureField("Current Password", text: $currentPassword)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)

                SecureField("New Password", text: $newPassword)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)

                SecureField("Confirm New Password", text: $confirmPassword)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)

                Button(action: {
                    if newPassword == confirmPassword && !newPassword.isEmpty {
                        showChangePasswordSheet = false
                        alertMessage = "Password changed successfully."
                        showAlert = true
                    }
                }) {
                    Text("Update Password")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                }

                Spacer()
            }
            .padding(.horizontal)
            .navigationBarItems(trailing: Button("Cancel") {
                showChangePasswordSheet = false
            })
        }
        .presentationDetents([.height(420)])
    }
}

#Preview {
    AccountView()
}
