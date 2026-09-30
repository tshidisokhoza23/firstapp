import SwiftUI

struct ArchivedChatsView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            VStack {
                Spacer()
                Image(systemName: "archivebox.fill")
                    .font(.system(size: 64))
                    .foregroundColor(AppColors.greyMedium)
                Text("Archived Conversations")
                    .font(.title2)
                    .fontWeight(.bold)
                Text("Conversations you archive will stay stored here.")
                    .font(.subheadline)
                    .foregroundColor(AppColors.greyMedium)
                Spacer()
            }
            .navigationTitle("Archived Chats")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
