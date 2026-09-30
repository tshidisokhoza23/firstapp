import SwiftUI

public enum MessageStatus: String, Codable {
    case pending = "PENDING"
    case sending = "SENDING"
    case sent = "SENT"
    case delivered = "DELIVERED"
    case read = "READ"
    case failed = "FAILED"
}

public enum MessageType: String, Codable {
    case text = "TEXT"
    case image = "IMAGE"
    case video = "VIDEO"
    case audio = "AUDIO"
    case audioMessage = "AUDIO_MESSAGE"
    case document = "DOCUMENT"
    case location = "LOCATION"
    case contact = "CONTACT"
    case sticker = "STICKER"
    case system = "SYSTEM"
}

public struct Message: Identifiable, Hashable, Codable {
    public var id: String
    public var conversationId: String
    public var content: String
    public var senderId: String
    public var senderName: String
    public var timestamp: Int64
    public var isFromMe: Bool
    public var status: MessageStatus
    public var type: MessageType
    public var mediaUrls: [String]
    public var replyToId: String?
    public var replyToText: String?
    public var replyToSenderName: String?
    public var isStarred: Bool
    public var reactions: [String: String] // userId: emoji
    public var duration: String?
    public var fileName: String?
    public var fileSize: String?
    public var contactName: String?
    public var contactPhone: String?
    public var latitude: Double?
    public var longitude: Double?
    public var audioAmplitudes: [Int]?

    public init(
        id: String = UUID().uuidString,
        conversationId: String = "",
        content: String = "",
        senderId: String = "",
        senderName: String = "",
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        isFromMe: Bool = false,
        status: MessageStatus = .sent,
        type: MessageType = .text,
        mediaUrls: [String] = [],
        replyToId: String? = nil,
        replyToText: String? = nil,
        replyToSenderName: String? = nil,
        isStarred: Bool = false,
        reactions: [String: String] = [:],
        duration: String? = nil,
        fileName: String? = nil,
        fileSize: String? = nil,
        contactName: String? = nil,
        contactPhone: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        audioAmplitudes: [Int]? = nil
    ) {
        self.id = id
        self.conversationId = conversationId
        self.content = content
        self.senderId = senderId
        self.senderName = senderName
        self.timestamp = timestamp
        self.isFromMe = isFromMe
        self.status = status
        self.type = type
        self.mediaUrls = mediaUrls
        self.replyToId = replyToId
        self.replyToText = replyToText
        self.replyToSenderName = replyToSenderName
        self.isStarred = isStarred
        self.reactions = reactions
        self.duration = duration
        self.fileName = fileName
        self.fileSize = fileSize
        self.contactName = contactName
        self.contactPhone = contactPhone
        self.latitude = latitude
        self.longitude = longitude
        self.audioAmplitudes = audioAmplitudes
    }
}

public struct MessageBubbleView: View {
    public let message: Message
    public var isSelected: Bool = false
    public var referencedMessage: Message? = nil
    public var fontSize: CGFloat = 16.0
    public var showReadReceipts: Bool = true
    public var playingAudioUri: String? = nil
    public var onPlayAudio: (String?) -> Void = { _ in }
    public var onReply: (Message) -> Void = { _ in }
    public var onMediaClick: (String, String) -> Void = { _, _ -> }
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
        self.onMediaClick = onMediaClick
        self.onLongClick = onLongClick
    }

    private var formattedTime: String {
        let date = Date(timeIntervalSince1970: TimeInterval(message.timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
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
                    if let replySender = message.replyToSenderName, let replyText = message.replyToText {
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

                    if !message.mediaUrls.isEmpty {
                        ForEach(message.mediaUrls, id: \.self) { mediaUrl in
                            if let url = URL(string: mediaUrl) {
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
                                    onMediaClick(mediaUrl, message.type.rawValue)
                                }
                            }
                        }
                    }

                    if message.type == .audioMessage || message.type == .audio {
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
                        Text(message.content)
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
