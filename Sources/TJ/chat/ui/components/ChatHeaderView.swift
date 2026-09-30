import SwiftUI

public struct ChatHeaderView: View {
    public let chat: ChatMessage
    public let sanitizedName: String
    public var isSearchMode: Bool = false
    public var chatSearchQuery: String = ""
    public var selectedMessagesCount: Int = 0
    public var isTyping: Bool = false

    public var onBack: () -> Void = {}
    public var onSearchQueryChange: (String) -> Void = { _ in }
    public var onSearchModeChange: (Bool) -> Void = { _ in }
    public var onClearSearch: () -> Void = {}
    public var onShowProfile: (String, Bool) -> Void = { _, _ in }
    public var onAvatarClick: (String) -> Void = { _ in }
    public var onInitiateCall: (String, String, CallType) -> Void = { _, _, _ in }
    public var onClearChat: () -> Void = {}
    public var onDeleteChat: () -> Void = {}
    public var onDeleteSelectedMessages: () -> Void = {}
    public var onShowMessageInfo: () -> Void = {}
    public var onStarSelectedMessages: () -> Void = {}
    public var onPinSelectedMessage: () -> Void = {}
    public var onEditSelectedMessage: () -> Void = {}
    public var onForwardSelectedMessages: () -> Void = {}
    public var onClearSelectedMessages: () -> Void = {}

    public init(
        chat: ChatMessage,
        sanitizedName: String,
        isSearchMode: Bool = false,
        chatSearchQuery: String = "",
        selectedMessagesCount: Int = 0,
        isTyping: Bool = false,
        onBack: @escaping () -> Void = {},
        onSearchQueryChange: @escaping (String) -> Void = { _ in },
        onSearchModeChange: @escaping (Bool) -> Void = { _ in },
        onClearSearch: @escaping () -> Void = {},
        onShowProfile: @escaping (String, Bool) -> Void = { _, _ in },
        onAvatarClick: @escaping (String) -> Void = { _ in },
        onInitiateCall: @escaping (String, String, CallType) -> Void = { _, _, _ in },
        onClearChat: @escaping () -> Void = {},
        onDeleteChat: @escaping () -> Void = {},
        onDeleteSelectedMessages: @escaping () -> Void = {},
        onShowMessageInfo: @escaping () -> Void = {},
        onStarSelectedMessages: @escaping () -> Void = {},
        onPinSelectedMessage: @escaping () -> Void = {},
        onEditSelectedMessage: @escaping () -> Void = {},
        onForwardSelectedMessages: @escaping () -> Void = {},
        onClearSelectedMessages: @escaping () -> Void = {}
    ) {
        self.chat = chat
        self.sanitizedName = sanitizedName
        self.isSearchMode = isSearchMode
        self.chatSearchQuery = chatSearchQuery
        self.selectedMessagesCount = selectedMessagesCount
        self.isTyping = isTyping
        self.onBack = onBack
        self.onSearchQueryChange = onSearchQueryChange
        self.onSearchModeChange = onSearchModeChange
        self.onClearSearch = onClearSearch
        self.onShowProfile = onShowProfile
        self.onAvatarClick = onAvatarClick
        self.onInitiateCall = onInitiateCall
        self.onClearChat = onClearChat
        self.onDeleteChat = onDeleteChat
        self.onDeleteSelectedMessages = onDeleteSelectedMessages
        self.onShowMessageInfo = onShowMessageInfo
        self.onStarSelectedMessages = onStarSelectedMessages
        self.onPinSelectedMessage = onPinSelectedMessage
        self.onEditSelectedMessage = onEditSelectedMessage
        self.onForwardSelectedMessages = onForwardSelectedMessages
        self.onClearSelectedMessages = onClearSelectedMessages
    }

    public var body: some View {
        HStack(spacing: 12) {
            if isSearchMode {
                Button(action: {
                    onSearchModeChange(false)
                    onSearchQueryChange("")
                }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                }

                TextField("Search...", text: Binding(
                    get: { chatSearchQuery },
                    set: { onSearchQueryChange($0) }
                ))
                .textFieldStyle(PlainTextFieldStyle())

                if !chatSearchQuery.isEmpty {
                    Button(action: onClearSearch) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                }
            } else if selectedMessagesCount > 0 {
                Button(action: onClearSelectedMessages) {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .semibold))
                }

                Text("\(selectedMessagesCount) Selected")
                    .font(.system(size: 18, weight: .bold))

                Spacer()

                if selectedMessagesCount == 1 {
                    Button(action: onShowMessageInfo) { Image(systemName: "info.circle") }
                    Button(action: onEditSelectedMessage) { Image(systemName: "pencil") }
                    Button(action: onPinSelectedMessage) { Image(systemName: "pin") }
                }
                Button(action: onStarSelectedMessages) { Image(systemName: "star") }
                Button(action: onDeleteSelectedMessages) { Image(systemName: "trash") }
                Button(action: onForwardSelectedMessages) { Image(systemName: "arrowshape.turn.up.right") }
            } else {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                }

                HStack(spacing: 8) {
                    if let urlString = chat.avatarUrl, let url = URL(string: urlString) {
                        AsyncImage(url: url) { phase in
                            if let image = phase.image {
                                image.resizable().scaledToFill()
                            } else {
                                avatarPlaceholder
                            }
                        }
                        .frame(width: 36, height: 36)
                        .clipShape(Circle())
                        .onTapGesture {
                            let targetId = chat.type == .direct ? (chat.otherUserId ?? chat.id) : chat.id
                            onAvatarClick(targetId)
                        }
                    } else {
                        avatarPlaceholder
                            .onTapGesture {
                                let targetId = chat.type == .direct ? (chat.otherUserId ?? chat.id) : chat.id
                                onAvatarClick(targetId)
                            }
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(sanitizedName)
                            .font(.system(size: 16, weight: .bold))
                            .lineLimit(1)

                        Text(isTyping ? "typing..." : (chat.isOnline ? "online" : "offline"))
                            .font(.system(size: 11))
                            .foregroundColor(isTyping || chat.isOnline ? .accentColor : .gray)
                    }
                    .onTapGesture {
                        let targetId = chat.type == .direct ? (chat.otherUserId ?? chat.id) : chat.id
                        onShowProfile(targetId, chat.isGroup)
                    }
                }

                Spacer()

                let targetId = chat.otherUserId ?? chat.id
                Button(action: { onInitiateCall(targetId, sanitizedName, .video) }) {
                    Image(systemName: "video")
                }
                Button(action: { onInitiateCall(targetId, sanitizedName, .voice) }) {
                    Image(systemName: "phone")
                }

                Menu {
                    Button("View Profile") {
                        let targetId = chat.type == .direct ? (chat.otherUserId ?? chat.id) : chat.id
                        onShowProfile(targetId, chat.isGroup)
                    }
                    Button("Search Chat") { onSearchModeChange(true) }
                    Button("Clear Messages") { onClearChat() }
                    Button("Delete Conversation", role: .destructive) { onDeleteChat() }
                } label: {
                    Image(systemName: "ellipsis")
                        .rotationEffect(.degrees(90))
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(UIColor.systemBackground))
    }

    private var avatarPlaceholder: some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.15))
                .frame(width: 36, height: 36)
            Text(String(sanitizedName.prefix(1)).uppercased())
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.accentColor)
        }
    }
}
