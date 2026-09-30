import SwiftUI

struct FilesView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Storage Overview")) {
                    HStack {
                        Image(systemName: "doc.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                        VStack(alignment: .leading) {
                            Text("Documents")
                                .font(.headline)
                            Text("12 Files • 4.2 MB")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                    HStack {
                        Image(systemName: "photo.fill")
                            .font(.title2)
                            .foregroundColor(.purple)
                        VStack(alignment: .leading) {
                            Text("Photos & Media")
                                .font(.headline)
                            Text("84 Files • 120 MB")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }
            }
            .navigationTitle("TJ Cloud Storage")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
