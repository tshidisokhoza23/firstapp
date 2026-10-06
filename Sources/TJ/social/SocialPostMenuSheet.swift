import SwiftUI

struct SocialPostMenuSheet: View {
    let post: SocialPost
    let isOwner: Bool
    @Environment(\.dismiss) var dismiss

    var onEdit: () -> Void
    var onPin: () -> Void
    var onDelete: () -> Void

    var onBookmark: () -> Void
    var onCopyLink: () -> Void
    var onShare: () -> Void
    var onNotInterested: () -> Void
    var onBlockAuthor: () -> Void
    var onReport: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 12)

            Text("Post Options")
                .font(.headline)

            VStack(spacing: 12) {
                if isOwner {
                    Button(action: { onEdit(); dismiss() }) {
                        Label("Edit Post", systemImage: "pencil")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onPin(); dismiss() }) {
                        Label("Pin to Profile", systemImage: "pin.fill")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onDelete(); dismiss() }) {
                        Label("Delete Post", systemImage: "trash.fill")
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    Button(action: { onBookmark(); dismiss() }) {
                        Label("Bookmark", systemImage: "bookmark.fill")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onCopyLink(); dismiss() }) {
                        Label("Copy Link", systemImage: "doc.on.doc.fill")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onShare(); dismiss() }) {
                        Label("Share Post", systemImage: "square.and.arrow.up")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onNotInterested(); dismiss() }) {
                        Label("Not interested in this post", systemImage: "eye.slash.fill")
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onBlockAuthor(); dismiss() }) {
                        Label("Block \(post.authorName)", systemImage: "person.crop.circle.badge.xmark")
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Divider()
                    Button(action: { onReport(); dismiss() }) {
                        Label("Report Content", systemImage: "flag.fill")
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
        .frame(height: isOwner ? 280 : 380)
    }
}
