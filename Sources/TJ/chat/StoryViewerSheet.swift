import SwiftUI

struct StoryViewerSheet: View {
    let story: StoryItem
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var replyText: String = ""
    @State private var progress: Double = 0.0
    let timer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(colors: [AppColors.brandPrimary, AppColors.darkBackgroundTop], startPoint: .topLeading, endPoint: .bottomTrailing)
                .edgesIgnoringSafeArea(.all)

            VStack {
                ProgressView(value: progress, total: 1.0)
                    .tint(.white)
                    .padding(.horizontal)
                    .padding(.top, 12)

                HStack(spacing: 12) {
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .frame(width: 40, height: 40)
                        .foregroundColor(.white)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(story.userName)
                            .font(.headline)
                            .foregroundColor(.white)
                        Text("2h ago")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                    }

                    Spacer()

                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(.white)
                    }
                }
                .padding(.horizontal)

                Spacer()

                Text("Sharing moments on TJ Stories ✨")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding()

                Spacer()

                HStack(spacing: 12) {
                    TextField("Reply to \(story.userName)...", text: $replyText)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Color.white.opacity(0.2))
                        .cornerRadius(20)
                        .foregroundColor(.white)

                    Button(action: sendStoryReply) {
                        Image(systemName: "paperplane.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.brandAccent)
                    }
                    .disabled(replyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .padding(.horizontal)
                .padding(.bottom, 24)
            }
        }
        .onReceive(timer) { _ in
            if progress < 1.0 {
                progress += 0.02
            } else {
                dismiss()
            }
        }
    }

    private func sendStoryReply() {
        let replyContent = "Replied to story: \(replyText)"
        let targetId = story.id.isEmpty ? "c1" : story.id

        Task { @MainActor in
            let _ = await NetworkClient.shared.initiateDM(contactId: targetId)

            let chatCard = ChatItem(
                id: targetId,
                name: story.userName,
                lastMessage: replyContent,
                timestamp: "Just now",
                unreadCount: 0,
                avatarUrl: story.avatarUrl,
                isOnline: true
            )

            if let index = chatViewModel.chats.firstIndex(where: { $0.id == targetId }) {
                chatViewModel.chats.remove(at: index)
            }
            chatViewModel.chats.insert(chatCard, at: 0)

            let replyMessage = ChatMessage(
                id: UUID().uuidString,
                chatId: targetId,
                senderName: "Alex",
                content: replyContent,
                timestamp: "Just now",
                isFromMe: true,
                isAudio: false,
                mediaUrl: nil
            )
            chatViewModel.activeChatMessages.append(replyMessage)
            chatViewModel.activeChat = chatCard

            WebSocketManager.shared.send(type: "chat_message", payload: ["content": replyContent, "chatId": targetId, "conversation_id": targetId])

            dismiss()
        }
    }
}
