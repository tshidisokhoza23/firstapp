import SwiftUI

struct MessageReactionItem: Identifiable, Codable, Hashable {
    let id = UUID()
    let emoji: String
    let count: Int
}

struct MessageBubbleView: View {
    let message: ChatMessage
    var onReply: () -> Void
    var onReact: (String) -> Void
    var onTapMedia: (String) -> Void

    @State private var showingReactionsBar: Bool = false

    var body: some View {
        HStack {
            if message.isFromMe { Spacer(minLength: 60) }

            VStack(alignment: message.isFromMe ? .trailing : .leading, spacing: 4) {
                // Quoted Reply Preview if replying
                HStack(spacing: 6) {
                    Rectangle()
                        .fill(message.isFromMe ? Color.white.opacity(0.6) : AppColors.brandPrimary)
                        .frame(width: 3, height: 24)
                    Text("Replying to message...")
                        .font(.caption)
                        .italic()
                        .lineLimit(1)
                }
                .padding(6)
                .background(Color.black.opacity(0.08))
                .cornerRadius(8)

                // Image or Video Media Attachment
                if let mediaUrl = message.mediaUrl, !mediaUrl.isEmpty {
                    Button(action: { onTapMedia(mediaUrl) }) {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(AppColors.brandPrimary.opacity(0.15))
                            .frame(width: 220, height: 140)
                            .overlay(
                                Image(systemName: "photo.fill")
                                    .font(.title)
                                    .foregroundColor(AppColors.brandPrimary)
                            )
                    }
                }

                // Audio Voice Note
                if message.isAudio {
                    VoiceNotePlayerView(duration: "0:14")
                } else {
                    Text(message.content)
                        .font(.system(size: 15))
                        .foregroundColor(message.isFromMe ? .white : .primary)
                }

                HStack(spacing: 4) {
                    Text(message.timestamp)
                        .font(.system(size: 10))
                        .foregroundColor(message.isFromMe ? .white.opacity(0.8) : AppColors.greyMedium)

                    if message.isFromMe {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.system(size: 10))
                            .foregroundColor(.white.opacity(0.9))
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(message.isFromMe ? AppColors.brandPrimary : Color(UIColor.tertiarySystemBackground))
            .cornerRadius(18)
            .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
            .onLongPressGesture {
                showingReactionsBar.toggle()
            }
            .overlay(
                // Reaction Bar Overlay
                Group {
                    if showingReactionsBar {
                        HStack(spacing: 8) {
                            ForEach(["👍", "❤️", "😂", "😮", "😢", "🙏"], id: \.self) { emoji in
                                Button(emoji) {
                                    onReact(emoji)
                                    showingReactionsBar = false
                                }
                                .font(.system(size: 20))
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(20)
                        .shadow(radius: 4)
                        .offset(y: -40)
                    }
                }
            )

            if !message.isFromMe { Spacer(minLength: 60) }
        }
    }
}
