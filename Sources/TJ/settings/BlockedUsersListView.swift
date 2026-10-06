import SwiftUI

struct BlockedUsersListView: View {
    @Environment(\.dismiss) var dismiss
    @State private var blockedUsers: [String] = ["Spam Bot #104", "Unknown Marketer"]

    var body: some View {
        NavigationView {
            List {
                if blockedUsers.isEmpty {
                    Text("No blocked contacts")
                        .foregroundColor(AppColors.greyMedium)
                } else {
                    ForEach(blockedUsers, id: \.self) { user in
                        HStack {
                            Image(systemName: "person.crop.circle.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.greyMedium)
                            Text(user)
                                .font(.headline)
                            Spacer()
                            Button("Unblock") {
                                blockedUsers.removeAll(where: { $0 == user })
                            }
                            .buttonStyle(.bordered)
                            .tint(.red)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .navigationTitle("Blocked Users")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
