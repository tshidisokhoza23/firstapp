import SwiftUI

public struct MessageBubbleView: View {
    public let message: Message
    public var isSelected: Bool = false
    public var referencedMessage: Message? = nil
    public var fontSize: CGFloat = 16.0
    public var showReadReceipts: Bool = true
    public var playingAudioUri: String? = nil
    public var onPlayAudio: (String?) -> Void = { _ in }
    public var onReply: (Message) -> Void = { _ in }
    public var onMediaClick: (String, String) -> Void = { _, _ in }
    public var onQuoteClick: (Message) -> Void = { _ in }
    public var onLongClick: () -> Void = {}

    public init(
        message: Message,
        isSelected: Bool = false,
        referencedMessage: Message? = nil,
        fontSize: CGFloat = 16.0,
        showReadReceipts: Bool = true,
        playingAudioUri: String? = nil,
        onPlayAudio: @escaping (String?) -> Void = { _ in },
        onReply: @escaping (Message) -> Void = { _ in },
        onMediaClick: @escaping (String, String) -> Void = { _, _ in },
        onQuoteClick: @escaping (Message) -> Void = { _ in },
        onLongClick: @escaping () -> Void = {}
    ) {
        self.message = message
        self.isSelected = isSelected
        self.referencedMessage = referencedMessage
        self.fontSize = fontSize
        self.showReadReceipts = showReadReceipts
        self.playingAudioUri = playingAudioUri
        self.onPlayAudio = onPlayAudio
        self.onReply = onReply
        self.onQuoteClick = onQuoteClick
        self.onMediaClick = onMediaClick
        self.onLongClick = onLongClick
    }

    private var formattedTime: String {
        let date = Date(timeIntervalSince1970: TimeInterval(message.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private var visibleBody: String {
        let stripped = MessageMarkers.strip(message.content)
        if stripped.hasPrefix("http://") || stripped.hasPrefix("https://") || stripped.contains("[mediaUrl:") {
            return MessageMarkers.parseStory(message.content) == nil ? stripped : "Story"
        }
        return stripped
    }

    private var isStoryReply: Bool {
        message.replyToId?.hasPrefix("story_") == true
            || message.replyToText?.localizedCaseInsensitiveContains("[Story]") == true
            || message.replyToText?.localizedCaseInsensitiveContains("[mediaUrl:") == true
            || MessageMarkers.parseStory(message.content) != nil
    }

    public var body: some View {
        if message.type == .system {
            HStack {
                Spacer()
                Text(message.content)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(Capsule())
                Spacer()
            }
            .padding(.vertical, 4)
        } else {
            HStack {
                if message.isFromMe { Spacer() }

                VStack(alignment: message.isFromMe ? .trailing : .leading, spacing: 4) {
                    if isStoryReply {
                        Button(action: { onQuoteClick(message) }) {
                            StoryReplyCard(replyText: message.replyToText ?? message.content, isFromMe: message.isFromMe)
                        }
                        .buttonStyle(.plain)
                    } else if let replySender = message.replyToSenderName, let replyText = message.replyToText {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(replySender)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.accentColor)
                            Text(replyText)
                                .font(.system(size: 12))
                                .lineLimit(1)
                                .foregroundColor(.secondary)
                        }
                        .padding(6)
                        .background(Color.primary.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                    }

                    if message.type != .audioMessage && message.type != .voiceCall && !message.mediaUrls.isEmpty {
                        ForEach(message.mediaUrls, id: \.self) { mediaUrl in
                            let resolved = FeedPost.resolve(mediaUrl) ?? URL(string: mediaUrl)
                            if message.type == .video {
                                Button(action: { onMediaClick(resolved?.absoluteString ?? mediaUrl, message.type.rawValue) }) {
                                    ZStack {
                                        Rectangle().fill(Color.black.opacity(0.35))
                                        Image(systemName: "play.circle.fill")
                                            .font(.system(size: 42))
                                            .foregroundColor(.white)
                                    }
                                    .frame(width: 220, height: 140)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                                .buttonStyle(.plain)
                            } else if let url = resolved {
                                AsyncImage(url: url) { phase in
                                    if let image = phase.image {
                                        image.resizable().scaledToFill()
                                    } else {
                                        Rectangle().fill(Color.gray.opacity(0.2))
                                    }
                                }
                                .frame(maxWidth: 240, maxHeight: 200)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .onTapGesture {
                                    onMediaClick(url.absoluteString, message.type.rawValue)
                                }
                            }
                        }
                    }

                    if message.type == .audioMessage || message.type == .voiceCall {
                        HStack(spacing: 8) {
                            Button(action: {
                                if playingAudioUri == message.mediaUrls.first {
                                    onPlayAudio(nil)
                                } else {
                                    onPlayAudio(message.mediaUrls.first)
                                }
                            }) {
                                Image(systemName: playingAudioUri == message.mediaUrls.first ? "pause.circle.fill" : "play.circle.fill")
                                    .font(.system(size: 28))
                                    .foregroundColor(message.isFromMe ? .white : .accentColor)
                            }

                            Text(message.duration ?? "0:00")
                                .font(.system(size: 12))
                                .foregroundColor(message.isFromMe ? .white : .primary)
                        }
                    } else if !message.content.isEmpty {
                        Text(visibleBody)
                            .font(.system(size: fontSize))
                            .foregroundColor(message.isFromMe ? .white : .primary)
                    }

                    HStack(spacing: 4) {
                        if message.isStarred {
                            Image(systemName: "star.fill")
                                .font(.system(size: 10))
                                .foregroundColor(.yellow)
                        }

                        Text(formattedTime)
                            .font(.system(size: 10))
                            .foregroundColor(message.isFromMe ? .white.opacity(0.8) : .secondary)

                        if message.isFromMe && showReadReceipts {
                            StatusTickView(status: message.status)
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(message.isFromMe ? Color.accentColor : Color(UIColor.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .onLongPressGesture {
                    onLongClick()
                }

                if !message.isFromMe { Spacer() }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
        }
    }
}

private struct StoryReplyCard: View {
    let replyText: String?
    let isFromMe: Bool

    private var media: String {
        let text = replyText ?? ""
        guard let start = text.range(of: "[mediaUrl:"), let end = text[start.upperBound...].firstIndex(of: "]") else { return "" }
        return String(text[start.upperBound..<end]).trimmingCharacters(in: .whitespaces)
    }

    private var caption: String {
        let text = (replyText ?? "")
            .replacingOccurrences(of: "\\[mediaUrl:.*?\\]", with: "", options: .regularExpression)
            .replacingOccurrences(of: "[Story]", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty || text.hasPrefix("http://") || text.hasPrefix("https://") || text.hasPrefix("uploads/") { return "Story" }
        return text
    }

    var body: some View {
        HStack(spacing: 8) {
            if let url = FeedPost.resolve(media) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.white.opacity(0.2)
                }
                .frame(width: 42, height: 42)
                .clipped()
                .cornerRadius(8)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Story")
                    .font(.system(size: 11, weight: .bold))
                Text(caption)
                    .font(.system(size: 12))
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .foregroundColor(isFromMe ? .white : .primary)
        .padding(6)
        .background(isFromMe ? Color.white.opacity(0.15) : Color.black.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
