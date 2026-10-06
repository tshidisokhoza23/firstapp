import SwiftUI

struct SideNavMenuSheet: View {
    @Environment(\.dismiss) private var dismiss

    var onSelectViewProfile: () -> Void
    var onSelectCreatePost: () -> Void
    var onSelectNotifications: () -> Void
    var onSelectDirectMessages: () -> Void
    var onSelectSettings: () -> Void
    var onSelectSavedPosts: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Capsule()
                .fill(Color.secondary.opacity(0.3))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
                .padding(.bottom, 12)

            MenuRow(icon: "person.fill", text: "View Profile") {
                dismiss()
                onSelectViewProfile()
            }
            MenuRow(icon: "plus", text: "Create Post") {
                dismiss()
                onSelectCreatePost()
            }
            MenuRow(icon: "bell.fill", text: "Notifications") {
                dismiss()
                onSelectNotifications()
            }
            MenuRow(icon: "envelope.fill", text: "Direct Messages") {
                dismiss()
                onSelectDirectMessages()
            }
            MenuRow(icon: "gearshape.fill", text: "Settings") {
                dismiss()
                onSelectSettings()
            }
            MenuRow(icon: "bookmark.fill", text: "Saved Posts") {
                dismiss()
                onSelectSavedPosts()
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .presentationDetents([.fraction(0.45)])
    }
}

private struct MenuRow: View {
    let icon: String
    let text: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .frame(width: 24)
                    .foregroundColor(.primary)
                Text(text)
                    .font(.body)
                    .foregroundColor(.primary)
                Spacer()
            }
            .padding(.vertical, 10)
        }
    }
}
