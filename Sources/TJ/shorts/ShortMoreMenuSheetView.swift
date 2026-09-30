import SwiftUI

struct ShortMoreMenuSheetView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        VStack(spacing: 20) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 12)

            VStack(spacing: 12) {
                Button(action: { dismiss() }) {
                    Label("Not Interested", systemImage: "eye.slash.fill")
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Divider()
                Button(action: { dismiss() }) {
                    Label("Save Video", systemImage: "bookmark.fill")
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Divider()
                Button(action: { dismiss() }) {
                    Label("Copy Link", systemImage: "doc.on.doc.fill")
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Divider()
                Button(action: { dismiss() }) {
                    Label("Report Content", systemImage: "flag.fill")
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16)
            .padding(.horizontal)

            Spacer()
        }
        .frame(height: 280)
    }
}
