import SwiftUI

struct EditProfileView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authViewModel: AuthViewModel

    @State private var name: String = ""
    @State private var bio: String = ""
    @State private var phone: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Public Profile")) {
                    TextField("Name", text: $name)
                    TextField("Bio", text: $bio)
                }

                Section(header: Text("Account Phone")) {
                    TextField("Phone", text: $phone)
                }
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                name = authViewModel.currentUser.name
                bio = authViewModel.currentUser.bio ?? ""
                phone = authViewModel.currentUser.phoneNumber
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        authViewModel.currentUser.name = name
                        authViewModel.currentUser.bio = bio
                        dismiss()
                    }
                    .font(.headline)
                }
            }
        }
    }
}
