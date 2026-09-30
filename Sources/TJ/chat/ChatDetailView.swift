import SwiftUI

struct ChatDetailView: View {
    let chat: ChatItem
    @EnvironmentObject var chatViewModel: ChatViewModel
    @Environment(\.dismiss) var dismiss

    @State private var showingContactProfile: Bool = false
    @State private var showingVoiceCall: Bool = false
    @State private var showingVideoCall: Bool = false

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

            Divider()

            // Messages Conversation List
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(chatViewModel.activeChatMessages) { message in
                        HStack {
                            if message.isFromMe { Spacer() }

                            VStack(alignment: message.isFromMe ? .trailing : .leading, spacing: 4) {
                                Text(message.content)
                                    .font(.system(size: 15))
                                    .foregroundColor(message.isFromMe ? .white : .primary)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 10)
                                    .background(message.isFromMe ? AppColors.brandPrimary : Color(UIColor.tertiarySystemBackground))
                                    .cornerRadius(18)

                                Text(message.timestamp)
                                    .font(.system(size: 10))
                                    .foregroundColor(AppColors.greyMedium)
                                    .padding(.horizontal, 4)
                            }

                            if !message.isFromMe { Spacer() }
                        }
                    }
                }
                .padding()
            }

            Divider()

            // Bottom Message Input Bar
            HStack(spacing: 12) {
                Button(action: {}) {
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
                    Button(action: {}) {
                        Image(systemName: "mic.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.greyMedium)
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
        .sheet(isPresented: $showingContactProfile) {
            ChatProfileView(chat: chat)
        }
        .fullScreenCover(isPresented: $showingVoiceCall) {
            ActiveCallOverlayView(contactName: chat.name, isVideoCall: false)
        }
        .fullScreenCover(isPresented: $showingVideoCall) {
            ActiveCallOverlayView(contactName: chat.name, isVideoCall: true)
        }
    }
}
