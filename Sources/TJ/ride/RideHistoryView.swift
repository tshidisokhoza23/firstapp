import SwiftUI

struct RideHistoryView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Past Rides")) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Financial District → Airport")
                                .font(.headline)
                            Spacer()
                            Text("$32.50")
                                .font(.headline)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                        HStack {
                            Text("Yesterday • TJ Economy")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Spacer()
                            Text("Completed")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Ride History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
