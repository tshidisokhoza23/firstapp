import SwiftUI

struct CompleteProfileView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authViewModel: AuthViewModel

    @State private var fullName: String = "Alex Morgan"
    @State private var username: String = "alex_morgan"
    @State private var bio: String = "Tech enthusiast & traveler 🚀"

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Profile Picture")) {
                    HStack {
                        Spacer()
                        ZStack {
                            Circle()
                                .fill(AppColors.brandPrimary.opacity(0.15))
                                .frame(width: 90, height: 90)
                            Image(systemName: "camera.fill")
                                .font(.title)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 8)
                }

                Section(header: Text("Personal Details")) {
                    TextField("Full Name", text: $fullName)
                    TextField("Username handle", text: $username)
                    TextField("Bio", text: $bio)
                }
            }
            .navigationTitle("Complete Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        authViewModel.currentUser.name = fullName
                        authViewModel.currentUser.bio = bio
                        dismiss()
                    }
                    .font(.headline)
                    .disabled(fullName.isEmpty)
                }
            }
        }
    }
}
