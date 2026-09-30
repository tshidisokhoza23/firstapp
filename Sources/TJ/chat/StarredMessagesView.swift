import SwiftUI

struct StarredMessagesView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: "star.fill")
                            .foregroundColor(.yellow)
                        Text("Sarah Connor")
                            .font(.headline)
                        Spacer()
                        Text("10:42 AM")
                            .font(.caption)
                            .foregroundColor(AppColors.greyMedium)
                    }
                    Text("See you at the meeting!")
                        .font(.body)
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Starred Messages")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
