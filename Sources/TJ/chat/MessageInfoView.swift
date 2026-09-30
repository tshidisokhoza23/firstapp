import SwiftUI

struct MessageInfoView: View {
    let message: ChatMessage
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Message Content")) {
                    Text(message.content)
                        .font(.body)
                        .padding(.vertical, 4)
                }

                Section(header: Text("Status Details")) {
                    HStack {
                        Label("Delivered", systemImage: "checkmark.circle.fill")
                            .foregroundColor(.blue)
                        Spacer()
                        Text("10:32 AM")
                            .foregroundColor(AppColors.greyMedium)
                    }
                    HStack {
                        Label("Read", systemImage: "checkmark.circle.fill")
                            .foregroundColor(.green)
                        Spacer()
                        Text("10:35 AM")
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
            }
            .navigationTitle("Message Info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
