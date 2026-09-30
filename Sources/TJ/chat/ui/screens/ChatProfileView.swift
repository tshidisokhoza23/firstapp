import SwiftUI

public struct ChatProfileView: View {
    public let chat: ChatMessage
    @StateObject public var viewModel: ChatViewModel = ChatViewModel()

    public var onBack: () -> Void = {}
    public var onOpenProfile: (String) -> Void = { _ in }
    public var onMediaClick: (String, String) -> Void = { _, _ in }
    public var onNavigateToSharedMedia: (String) -> Void = { _ in }

    @State private var isMuted: Bool = false
    @State private var isPinned: Bool = false
    @State private var showBlockAlert: Bool = false
    @State private var showReportAlert: Bool = false
    @State private var showEncryptionInfo: Bool = false

    @MainActor public init(
        chat: ChatMessage,
        viewModel: ChatViewModel = ChatViewModel(),
        onBack: @escaping () -> Void = {},
        onOpenProfile: @escaping (String) -> Void = { _ in },
        onMediaClick: @escaping (String, String) -> Void = { _, _ in },
        onNavigateToSharedMedia: @escaping (String) -> Void = { _ in }
    ) {
        self.chat = chat
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.onBack = onBack
        self.onOpenProfile = onOpenProfile
        self.onMediaClick = onMediaClick
        self.onNavigateToSharedMedia = onNavigateToSharedMedia
    }

    private var titleName: String {
        let name = chat.consistentName()
        return name.isEmpty ? "Profile" : name
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Top Navigation Bar
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                    }
                    Spacer()
                    Text("Contact Info")
                        .font(.system(size: 18, weight: .bold))
                    Spacer()
                    Button(action: { showReportAlert = true }) {
                        Image(systemName: "flag")
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                // Avatar and Name Section
                VStack(spacing: 12) {
                    if let urlString = chat.avatarUrl, let url = URL(string: urlString) {
                        AsyncImage(url: url) { phase in
                            if let image = phase.image {
                                image.resizable().scaledToFill()
                            } else {
                                avatarPlaceholder
                            }
                        }
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                    } else {
                        avatarPlaceholder
                    }

                    Text(titleName)
                        .font(.system(size: 22, weight: .bold))

                    if let phone = chat.phoneNumber, !phone.isEmpty {
                        Text(phone)
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                    }

                    // Call Action Buttons
                    HStack(spacing: 32) {
                        Button(action: {
                            viewModel.startVoiceOrVideoCall(receiverId: chat.otherUserId ?? chat.id, callType: .voice, name: titleName)
                        }) {
                            VStack {
                                Image(systemName: "phone.fill")
                                    .font(.system(size: 20))
                                Text("Voice")
                                    .font(.system(size: 12))
                            }
                            .foregroundColor(.accentColor)
                        }

                        Button(action: {
                            viewModel.startVoiceOrVideoCall(receiverId: chat.otherUserId ?? chat.id, callType: .video, name: titleName)
                        }) {
                            VStack {
                                Image(systemName: "video.fill")
                                    .font(.system(size: 20))
                                Text("Video")
                                    .font(.system(size: 12))
                            }
                            .foregroundColor(.accentColor)
                        }
                    }
                    .padding(.top, 8)
                }

                Divider()

                // Settings & Options List
                VStack(spacing: 0) {
                    Toggle(isOn: $isMuted) {
                        HStack {
                            Image(systemName: "bell.slash")
                            Text("Mute Notifications")
                        }
                    }
                    .onChange(of: isMuted) { newValue in
                        viewModel.muteChat(chatId: chat.id, isMuted: newValue)
                    }
                    .padding()

                    Divider()

                    Toggle(isOn: $isPinned) {
                        HStack {
                            Image(systemName: "pin")
                            Text("Pin Conversation")
                        }
                    }
                    .onChange(of: isPinned) { newValue in
                        viewModel.pinChat(chatId: chat.id, isPinned: newValue)
                    }
                    .padding()

                    Divider()

                    Button(action: {
                        onNavigateToSharedMedia(chat.id)
                    }) {
                        HStack {
                            Image(systemName: "photo.on.rectangle")
                            Text("Shared Media & Documents")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                        }
                        .foregroundColor(.primary)
                        .padding()
                    }

                    Divider()

                    Button(action: { showEncryptionInfo = true }) {
                        HStack {
                            Image(systemName: "lock.shield")
                            Text("Encryption Info")
                            Spacer()
                        }
                        .foregroundColor(.primary)
                        .padding()
                    }
                }
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(12)
                .padding(.horizontal, 16)

                // Danger Actions
                VStack(spacing: 12) {
                    Button(action: { showBlockAlert = true }) {
                        HStack {
                            Image(systemName: "hand.raised.fill")
                            Text("Block Contact")
                        }
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                    }

                    Button(action: {
                        viewModel.clearChat(chatId: chat.id)
                    }) {
                        HStack {
                            Image(systemName: "trash.fill")
                            Text("Clear Chat History")
                        }
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.bottom, 24)
        }
        .alert("Block Contact", isPresented: $showBlockAlert) {
            Button("Block", role: .destructive) {
                viewModel.blockUser(userId: chat.otherUserId ?? chat.id)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to block this contact?")
        }
        .alert("Report Contact", isPresented: $showReportAlert) {
            Button("Report", role: .destructive) {
                viewModel.reportUser(userId: chat.otherUserId ?? chat.id, reason: "Spam")
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Report spam or inappropriate behavior to support.")
        }
        .alert("End-to-End Encryption", isPresented: $showEncryptionInfo) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Messages and calls are end-to-end encrypted. No one outside of this chat can read or listen to them.")
        }
    }

    private var avatarPlaceholder: some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.15))
                .frame(width: 100, height: 100)
            Text(String(titleName.prefix(1)).uppercased())
                .font(.system(size: 36, weight: .bold))
                .foregroundColor(.accentColor)
        }
    }
}
