import SwiftUI

public struct ChatView: View {
    public let chat: ChatMessage
    @StateObject public var viewModel: ChatViewModel = ChatViewModel()

    public var onBack: () -> Void = {}
    public var onShowProfile: (String, Bool) -> Void = { _, _ in }
    public var onAvatarClick: (String) -> Void = { _ in }
    public var onOpenStory: ((_ userId: String, _ storyId: String) -> Void)? = nil

    @State private var inputText: String = ""
    @State private var isSearchMode: Bool = false
    @State private var chatSearchQuery: String = ""
    @State private var selectedMessageIds: Set<String> = []
    @State private var isRecording: Bool = false
    @State private var recordingDuration: Int = 0

    public init(
        chat: ChatMessage,
        viewModel: ChatViewModel = ChatViewModel(),
        onBack: @escaping () -> Void = {},
        onShowProfile: @escaping (String, Bool) -> Void = { _, _ in },
        onAvatarClick: @escaping (String) -> Void = { _ in },
        onOpenStory: ((_ userId: String, _ storyId: String) -> Void)? = nil
    ) {
        self.chat = chat
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.onBack = onBack
        self.onShowProfile = onShowProfile
        self.onAvatarClick = onAvatarClick
        self.onOpenStory = onOpenStory
    }

    private var sanitizedName: String {
        let name = chat.consistentName()
        return name.isEmpty ? "Chat" : name
    }

    private var messagesList: [Message] {
        return viewModel.uiState.messages.data ?? []
    }

    public var body: some View {
        VStack(spacing: 0) {
            ChatHeaderView(
                chat: chat,
                sanitizedName: sanitizedName,
                isSearchMode: isSearchMode,
                chatSearchQuery: chatSearchQuery,
                selectedMessagesCount: selectedMessageIds.count,
                isTyping: viewModel.uiState.typingStatus[chat.id] ?? false,
                onBack: onBack,
                onSearchQueryChange: { chatSearchQuery = $0 },
                onSearchModeChange: { isSearchMode = $0 },
                onClearSearch: { chatSearchQuery = "" },
                onShowProfile: onShowProfile,
                onAvatarClick: onAvatarClick,
                onInitiateCall: { receiverId, name, callType in
                    viewModel.startVoiceOrVideoCall(receiverId: receiverId, callType: callType, name: name)
                },
                onClearChat: { viewModel.clearChat(chatId: chat.id) },
                onDeleteChat: { viewModel.clearChat(chatId: chat.id) },
                onDeleteSelectedMessages: {
                    selectedMessageIds.forEach { viewModel.deleteMessage(messageId: $0) }
                    selectedMessageIds.removeAll()
                },
                onShowMessageInfo: {},
                onStarSelectedMessages: {
                    selectedMessageIds.forEach { viewModel.starMessage(messageId: $0, isStarred: true) }
                    selectedMessageIds.removeAll()
                },
                onPinSelectedMessage: {},
                onEditSelectedMessage: {},
                onForwardSelectedMessages: {},
                onClearSelectedMessages: { selectedMessageIds.removeAll() }
            )

            Divider()

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 4) {
                        ForEach(messagesList) { message in
                            MessageBubbleView(
                                message: message,
                                isSelected: selectedMessageIds.contains(message.id),
                                fontSize: viewModel.fontSize,
                                showReadReceipts: viewModel.showReadReceipts,
                                playingAudioUri: viewModel.currentlyPlayingAudio,
                                onPlayAudio: { uri in
                                    viewModel.setPlayingAudio(uri)
                                },
                                onReply: { msg in
                                    // Reply action
                                },
                                onMediaClick: { url, type in
                                    // Open media
                                },
                                onLongClick: {
                                    if selectedMessageIds.contains(message.id) {
                                        selectedMessageIds.remove(message.id)
                                    } else {
                                        selectedMessageIds.insert(message.id)
                                    }
                                }
                            )
                            .id(message.id)
                        }
                    }
                    .padding(.vertical, 8)
                }
                .onChange(of: messagesList.count) { _ in
                    if let lastMsg = messagesList.last {
                        withAnimation {
                            proxy.scrollTo(lastMsg.id, anchor: .bottom)
                        }
                    }
                }
            }

            ChatInputBarView(
                text: $inputText,
                isRecording: isRecording,
                recordingDuration: recordingDuration,
                enterIsSend: viewModel.enterIsSend,
                onSend: {
                    let textToSend = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !textToSend.isEmpty else { return }
                    viewModel.sendMessage(chatId: chat.id, body: textToSend)
                    inputText = ""
                },
                onAttachClick: {
                    // Attachment picker
                },
                onEmojiClick: {
                    // Emoji picker
                },
                onCameraClick: {
                    // Camera picker
                },
                onMicClick: {
                    isRecording.toggle()
                }
            )
        }
        .onAppear {
            viewModel.fetchMessages(chatId: chat.id)
        }
        .onDisappear {
            viewModel.clearActiveChat()
        }
    }
}
