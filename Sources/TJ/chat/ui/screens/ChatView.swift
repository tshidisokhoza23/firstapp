import SwiftUI
import PhotosUI
import AVFoundation
import UIKit

public struct ChatView: View {
    public let chat: ChatMessage
    @StateObject public var viewModel: ChatViewModel
    @State private var openedMedia: String?

    public var onBack: () -> Void = {}
    public var onShowProfile: (String, Bool) -> Void = { _, _ in }
    public var onAvatarClick: (String) -> Void = { _ in }
    public var onOpenStory: ((_ userId: String, _ storyId: String) -> Void)? = nil

    @State private var inputText: String = ""
    @State private var isSearchMode: Bool = false
    @State private var chatSearchQuery: String = ""
    @State private var selectedMessageIds: Set<String> = []
    @StateObject private var voice = VoiceNoteController()
    @State private var replyTo: Message?
    @State private var editingMessage: Message?
    @State private var infoMessage: Message?
    @State private var showEmoji = false
    @State private var attachItem: PhotosPickerItem?
    @State private var cameraItem: PhotosPickerItem?
    @State private var showAttach = false
    @State private var showCamera = false
    @State private var note = ""
    @State private var storyLaunch: StoryLaunch?

    @MainActor public init(
        chat: ChatMessage,
        viewModel: ChatViewModel? = nil,
        onBack: @escaping () -> Void = {},
        onShowProfile: @escaping (String, Bool) -> Void = { _, _ in },
        onAvatarClick: @escaping (String) -> Void = { _ in },
        onOpenStory: ((_ userId: String, _ storyId: String) -> Void)? = nil
    ) {
        self.chat = chat
        self._viewModel = StateObject(wrappedValue: viewModel ?? ChatViewModel())
        self.onBack = onBack
        self.onShowProfile = onShowProfile
        self.onAvatarClick = onAvatarClick
        self.onOpenStory = onOpenStory
    }

    private var sanitizedName: String {
        let name = chat.consistentName()
        if !name.isEmpty { return name }
        return chat.phoneNumber ?? ""
    }

    private var messagesList: [Message] {
        return viewModel.uiState.messages.data ?? []
    }

    public var body: some View {
        VStack(spacing: 0) {
            chatHeader
            Divider()
            messageList
            composer
        }
        .safeAreaInset(edge: .bottom) {
            emojiStrip
        }
        .onAppear {
            viewModel.fetchMessages(chatId: chat.id)
        }
        .onDisappear {
            viewModel.clearActiveChat()
        }
        .photosPicker(isPresented: $showAttach, selection: $attachItem, matching: .any(of: [.images, .videos]))
        .photosPicker(isPresented: $showCamera, selection: $cameraItem, matching: .images)
        .onChange(of: attachItem) { item in
            guard let item else { return }
            Task { await sendPicked(item) }
        }
        .onChange(of: voice.error) { message in
            if let message, !message.isEmpty { note = message }
        }
        .onChange(of: cameraItem) { item in
            guard let item else { return }
            Task { await sendPicked(item) }
        }
        .sheet(item: $infoMessage) { message in
            VStack(alignment: .leading, spacing: 12) {
                Text("Message info").font(.headline)
                Text(message.content).font(.body)
                Text(message.isFromMe ? "Sent by you" : "Received").font(.caption).foregroundColor(.secondary)
                Button("Close") { infoMessage = nil }
            }
            .padding()
            .presentationDetents([.medium])
        }
        .alert("Chat", isPresented: Binding(get: { !note.isEmpty }, set: { if !$0 { note = "" } })) {
            Button("OK", role: .cancel) { note = "" }
        } message: {
            Text(note)
        }
        .fullScreenCover(isPresented: Binding(get: { openedMedia != nil }, set: { if !$0 { openedMedia = nil } })) {
            if let openedMedia {
                InAppMediaView(urlString: openedMedia, onClose: { self.openedMedia = nil })
            }
        }
        .fullScreenCover(item: $storyLaunch) { launch in
            StoryRowPlayer(startUserId: launch.userId, startStoryId: launch.storyId, onClose: { storyLaunch = nil })
        }
    }

    private var chatHeader: some View {
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
            onClearSearch: {
                chatSearchQuery = ""
                isSearchMode = false
            },
            onShowProfile: onShowProfile,
            onAvatarClick: onAvatarClick,
            onInitiateCall: { receiverId, name, callType in
                viewModel.startVoiceOrVideoCall(receiverId: receiverId, callType: callType, name: name)
            },
            onClearChat: { clearConversation() },
            onDeleteChat: { deleteConversation() },
            onDeleteSelectedMessages: {
                selectedMessageIds.forEach { viewModel.deleteMessage(messageId: $0) }
                selectedMessageIds.removeAll()
            },
            onShowMessageInfo: { infoMessage = selectedMessage() },
            onStarSelectedMessages: {
                selectedMessageIds.forEach { viewModel.starMessage(messageId: $0, isStarred: true) }
                selectedMessageIds.removeAll()
            },
            onPinSelectedMessage: { pinSelected() },
            onEditSelectedMessage: { editSelected() },
            onForwardSelectedMessages: { forwardSelected() },
            onClearSelectedMessages: { selectedMessageIds.removeAll() }
        )
    }

    private var messageList: some View {
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
                                    replyTo = msg
                                    if inputText.isEmpty { inputText = "" }
                                },
                                onMediaClick: { url, _ in
                                    openedMedia = url
                                },
                                onQuoteClick: { message in
                                    openQuoted(message)
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
    }

    private func openQuoted(_ message: Message) {
        let parsed = MessageMarkers.parseStory(message.content) ?? MessageMarkers.parseStoryReplyId(replyToId: message.replyToId)
        let storyId = parsed?.storyId.nilIfEmpty
            ?? message.replyToId?.replacingOccurrences(of: "story_", with: "").components(separatedBy: "|").first
        let userId = parsed?.userId.nilIfEmpty ?? chat.otherUserId ?? chat.id
        let isStory = message.replyToId?.hasPrefix("story_") == true
            || MessageMarkers.parseStory(message.content) != nil
            || message.replyToText?.localizedCaseInsensitiveContains("[Story]") == true
            || message.replyToText?.localizedCaseInsensitiveContains("[mediaUrl:") == true
        if isStory {
            if userId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || (storyId ?? "").isEmpty {
                note = "This story has expired or is no longer available."
            } else {
                storyLaunch = StoryLaunch(userId: userId, storyId: storyId)
            }
            return
        }
    }

    private var composer: some View {
        ChatInputBarView(
            text: $inputText,
            isRecording: voice.isRecording,
            recordingDuration: voice.duration,
            isReviewingVoiceNote: voice.isReviewing,
            isPlayingReview: voice.isPlayingReview,
            onSend: { sendCurrentText() },
            onAttachClick: { showAttach = true },
            onEmojiClick: { showEmoji.toggle() },
            onCameraClick: { showCamera = true },
            onMicClick: { voice.toggle() },
            onSendVoiceNote: { sendVoiceNote() },
            onDiscardVoiceNote: { voice.discard() },
            onTogglePlayReview: { voice.toggleReviewPlayback() }
        )
    }

    @ViewBuilder
    private var emojiStrip: some View {
        if showEmoji {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(["😀", "😂", "❤️", "👍", "🔥", "🙏", "😢", "🎉"], id: \.self) { emoji in
                        Button(emoji) { inputText += emoji }
                            .font(.title2)
                    }
                }
                .padding(8)
            }
            .background(.ultraThinMaterial)
        }
    }

    private func selectedMessage() -> Message? {
        guard let id = selectedMessageIds.first else { return messagesList.last }
        return messagesList.first { $0.id == id }
    }

    private func sendCurrentText() {
        let textToSend = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !textToSend.isEmpty else { return }
        if let editing = editingMessage {
            let id = editing.id
            editingMessage = nil
            inputText = ""
            Task {
                _ = try? await API.data("conversations/\(chat.id)/messages/\(id)", method: "PATCH", json: ["message_text": textToSend])
                viewModel.fetchMessages(chatId: chat.id)
            }
            return
        }
        viewModel.sendMessage(
            chatId: chat.id,
            body: textToSend,
            replyToId: replyTo?.id,
            replyToText: replyTo?.content,
            replyToSenderName: replyTo?.senderName
        )
        inputText = ""
        replyTo = nil
    }

    private func sendVoiceNote() {
        guard let picked = voice.takeForSend() else { return }
        let label = String(format: "%d:%02d", picked.seconds / 60, picked.seconds % 60)
        let chatId = chat.id
        Task {
            do {
                let data = try Data(contentsOf: picked.url)
                let uploaded = try await MediaActions.upload(
                    data: data,
                    filename: picked.url.lastPathComponent,
                    mime: "audio/mp4",
                    folder: "chat/audio_message"
                )
                let url = FeedPost.resolve(uploaded)?.absoluteString ?? uploaded
                viewModel.sendMessage(
                    chatId: chatId,
                    body: "Voice Note",
                    messageType: "audio_message",
                    mediaUrls: [url],
                    duration: label,
                    fileName: picked.url.lastPathComponent,
                    fileSize: String(data.count)
                )
                try? FileManager.default.removeItem(at: picked.url)
            } catch {
                note = error.localizedDescription
            }
        }
    }

    private func sendPicked(_ item: PhotosPickerItem) async {
        defer { attachItem = nil; cameraItem = nil }
        guard let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty else {
            note = "Could not read that file"
            return
        }
        let type = item.supportedContentTypes.first
        let mime = type?.preferredMIMEType ?? "application/octet-stream"
        let kind = mime.hasPrefix("video") ? "video" : (mime.hasPrefix("image") ? "image" : "document")
        var payload = data
        var sendMime = mime
        var filename = kind == "video" ? "chat_attachment.mp4" : (kind == "image" ? "chat_attachment.jpg" : "chat_attachment")
        if kind == "image", let image = UIImage(data: data), let jpeg = image.jpegData(compressionQuality: 0.82), !jpeg.isEmpty {
            payload = jpeg
            sendMime = "image/jpeg"
            filename = "chat_attachment.jpg"
        }
        do {
            let uploaded = try await MediaActions.upload(
                data: payload,
                filename: filename,
                mime: sendMime,
                folder: "chat/\(kind)"
            )
            let url = FeedPost.resolve(uploaded)?.absoluteString ?? uploaded
            viewModel.sendMessage(
                chatId: chat.id,
                body: inputText.isEmpty ? kind.capitalized : inputText,
                messageType: kind,
                mediaUrls: [url],
                fileName: filename,
                fileSize: String(payload.count)
            )
        } catch {
            note = error.localizedDescription
            return
        }
        inputText = ""
    }

    private func pinSelected() {
        guard let id = selectedMessageIds.first else { return }
        Task {
            _ = try? await API.data("conversations/\(chat.id)/pinned-message", method: "PATCH", json: ["message_id": id])
            selectedMessageIds.removeAll()
        }
    }

    private func editSelected() {
        guard let message = selectedMessage() else { return }
        inputText = message.content
        editingMessage = message
        selectedMessageIds.removeAll()
    }

    private func forwardSelected() {
        let text = selectedMessageIds.compactMap { id in messagesList.first { $0.id == id }?.content }.joined(separator: "\n")
        guard !text.isEmpty else { return }
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = scene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
        selectedMessageIds.removeAll()
    }

    private func clearConversation() {
        viewModel.clearChat(chatId: chat.id)
        Task { _ = try? await API.data("conversations/\(chat.id)/clear", method: "POST") }
    }

    private func deleteConversation() {
        Task {
            _ = try? await API.data("conversations/\(chat.id)", method: "DELETE")
            onBack()
        }
    }
}