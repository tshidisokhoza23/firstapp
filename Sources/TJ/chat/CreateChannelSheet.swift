import SwiftUI

struct CreateChannelSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var channelName: String = ""
    @State private var channelDescription: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Channel Details")) {
                    TextField("Channel Name", text: $channelName)
                    TextField("Description", text: $channelDescription)
                }
            }
            .navigationTitle("New Channel")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") { dismiss() }
                        .font(.headline)
                        .disabled(channelName.isEmpty)
                }
            }
        }
    }
}
