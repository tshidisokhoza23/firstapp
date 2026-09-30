import SwiftUI

struct ChatAttachmentItemView: View {
    let title: String
    let iconName: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 60, height: 60)
                    .overlay(
                        Image(systemName: iconName)
                            .font(.title2)
                            .foregroundColor(color)
                    )

                Text(title)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.primary)
            }
        }
    }
}

struct ChatAttachmentSheetView: View {
    @Environment(\.dismiss) var dismiss
    var onSelectOption: (String) -> Void

    var body: some View {
        VStack(spacing: 20) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 12)

            Text("Share Content")
                .font(.headline)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 16), count: 3), spacing: 24) {
                ChatAttachmentItemView(title: "Camera", iconName: "camera.fill", color: .purple) {
                    onSelectOption("Camera Photo")
                    dismiss()
                }
                ChatAttachmentItemView(title: "Photos & Video", iconName: "photo.fill", color: .blue) {
                    onSelectOption("Gallery Photo")
                    dismiss()
                }
                ChatAttachmentItemView(title: "Document", iconName: "doc.fill", color: .indigo) {
                    onSelectOption("PDF Document")
                    dismiss()
                }
                ChatAttachmentItemView(title: "Location", iconName: "location.fill", color: .green) {
                    onSelectOption("Live Location")
                    dismiss()
                }
                ChatAttachmentItemView(title: "Contact", iconName: "person.crop.square.fill", color: .cyan) {
                    onSelectOption("Contact Card")
                    dismiss()
                }
                ChatAttachmentItemView(title: "Audio", iconName: "headphones", color: .orange) {
                    onSelectOption("Audio Track")
                    dismiss()
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 30)
        }
        .frame(height: 280)
    }
}
