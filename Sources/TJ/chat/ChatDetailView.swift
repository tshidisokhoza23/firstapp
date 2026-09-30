import SwiftUI

struct ChatDetailView: View {
    let chat: ChatItem
    @EnvironmentObject var chatViewModel: ChatViewModel
    @Environment(\.dismiss) var dismiss

    @State private var showingContactProfile: Bool = false
    @State private var showingVoiceCall: Bool = false
    @State private var showingVideoCall: Bool = false
    @State private var showingAttachments: Bool = false
    @State private var showingExpressions: Bool = false
    @State private var isRecordingVoiceNote: Bool = false
    @State private var fullScreenMediaUrl: String? = nil
    @State private var searchInChatQuery: String = ""
    @State private var isSearchingChat: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            // Navigation Header
            HStack(spacing: 12) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.title2)
                        .foregroundColor(.primary)
                }

                Button(action: { showingContactProfile = true }) {
                    HStack(spacing: 10) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 40, height: 40)
                            .foregroundColor(AppColors.greyMedium)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(chat.name)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.primary)
                            Text(chat.isOnline ? "Online" : "Last seen recently")
                                .font(.system(size: 12))
                                .foregroundColor(chat.isOnline ? .green : AppColors.greyMedium)
                        }
                    }
                }

                Spacer()

                HStack(spacing: 16) {
                    Button(action: { isSearchingChat.toggle() }) {
                        Image(systemName: "magnifyingglass")
                            .font(.title3)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                    Button(action: { showingVoiceCall = true }) {
                        Image(systemName: "phone.fill")
                            .font(.title3)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                    Button(action: { showingVideoCall = true }) {
                        Image(systemName: "video.fill")
                            .font(.title3)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
            .background(Color(UIColor.secondarySystemBackground))

            if isSearchingChat {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(AppColors.greyMedium)
                    TextField("Search in chat...", text: $searchInChatQuery)
                    Button("Done") { isSearchingChat = false }
                        .font(.caption)
                }
                .padding(8)
                .background(Color(UIColor.tertiarySystemBackground))
            }

            Divider()

            // Messages Conversation Stream
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(chatViewModel.activeChatMessages.filter { searchInChatQuery.isEmpty || $0.content.localizedCaseInsensitiveContains(searchInChatQuery) }) { message in
                        MessageBubbleView(
                            message: message,
                            onReply: { },
                            onReact: { _ in },
                            onTapMedia: { mediaUrl in
                                fullScreenMediaUrl = mediaUrl
                            }
                        )
                    }
                }
                .padding()
            }

            Divider()

            // Bottom Message Input Bar / Voice Recorder Bar
            if isRecordingVoiceNote {
                VoiceNoteRecorderView(
                    onCancel: { isRecordingVoiceNote = false },
                    onSendAudio: {
                        let audioMsg = ChatMessage(
                            id: UUID().uuidString,
                            chatId: chat.id,
                            senderName: "Alex",
                            content: "Voice Note (0:14)",
                            timestamp: "Just now",
                            isFromMe: true,
                            isAudio: true,
                            mediaUrl: nil
                        )
                        chatViewModel.activeChatMessages.append(audioMsg)
                        isRecordingVoiceNote = false
                    }
                )
                .padding()
            } else {
                HStack(spacing: 12) {
                    Button(action: { showingExpressions = true }) {
                        Image(systemName: "face.smiling.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.greyMedium)
                    }

                    Button(action: { showingAttachments = true }) {
                        Image(systemName: "plus")
                            .font(.title2)
                            .foregroundColor(AppColors.greyMedium)
                    }

                    TextField("Message...", text: $chatViewModel.messageInputText)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Color(UIColor.tertiarySystemBackground))
                        .cornerRadius(20)

                    if chatViewModel.messageInputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Button(action: { isRecordingVoiceNote = true }) {
                            Image(systemName: "mic.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    } else {
                        Button(action: {
                            chatViewModel.sendMessage()
                        }) {
                            Image(systemName: "paperplane.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.brandPrimary)
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 10)
                .background(Color(UIColor.secondarySystemBackground))
            }
        }
        .sheet(isPresented: $showingContactProfile) {
            ChatProfileView(chat: chat)
        }
        .sheet(isPresented: $showingAttachments) {
            ChatAttachmentSheetView { attachmentType in
                let attachmentMsg = ChatMessage(
                    id: UUID().uuidString,
                    chatId: chat.id,
                    senderName: "Alex",
                    content: "Shared \(attachmentType)",
                    timestamp: "Just now",
                    isFromMe: true,
                    isAudio: false,
                    mediaUrl: "sample_media"
                )
                chatViewModel.activeChatMessages.append(attachmentMsg)
            }
        }
        .sheet(isPresented: $showingExpressions) {
            GifStickerPickerView { selectedExpression in
                chatViewModel.messageInputText += selectedExpression
            }
        }
        .fullScreenCover(isPresented: $showingVoiceCall) {
            ActiveCallOverlayView(contactName: chat.name, isVideoCall: false)
        }
        .fullScreenCover(isPresented: $showingVideoCall) {
            ActiveCallOverlayView(contactName: chat.name, isVideoCall: true)
        }
        .fullScreenCover(item: $fullScreenMediaUrl) { mediaUrl in
            FullScreenMediaViewer(mediaUrl: mediaUrl)
        }
    }
}

extension String: Identifiable {
    public var id: String { self }
}
