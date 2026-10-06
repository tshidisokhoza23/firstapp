import SwiftUI

struct ShortOptionsBottomSheet: View {
    let video: ShortVideoItem
    let isOwner: Bool
    @Environment(\.dismiss) var dismiss

    var onMessageAuthor: () -> Void
    var onCopyLink: () -> Void
    var onSaveToGallery: () -> Void
    var onNotInterested: () -> Void
    var onBlockAuthor: () -> Void
    var onReport: () -> Void
    var onDelete: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 12)

            Text("Short Options")
                .font(.headline)

            VStack(spacing: 12) {
                if isOwner {
                    Button(action: { onCopyLink(); dismiss() }) {
                        Label("Copy link", systemImage: "link")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onSaveToGallery(); dismiss() }) {
                        Label("Save to device", systemImage: "arrow.down.circle")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onDelete(); dismiss() }) {
                        Label("Delete short", systemImage: "trash.fill")
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    Button(action: { onMessageAuthor(); dismiss() }) {
                        Label("Message \(video.creatorName)", systemImage: "envelope.fill")
                            .foregroundColor(AppColors.brandPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onCopyLink(); dismiss() }) {
                        Label("Copy link", systemImage: "link")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onNotInterested(); dismiss() }) {
                        Label("Not interested", systemImage: "eye.slash.fill")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onBlockAuthor(); dismiss() }) {
                        Label("Block \(video.creatorName)", systemImage: "person.crop.circle.badge.xmark")
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onReport(); dismiss() }) {
                        Label("Report", systemImage: "flag.fill")
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16)
            .padding(.horizontal)

            Spacer()
        }
        .frame(height: isOwner ? 260 : 360)
    }
}
