import SwiftUI

public struct ArchivedChatsView: View {
    @StateObject public var viewModel: ChatViewModel
    public var onBack: () -> Void = {}
    public var onChatClick: (ChatMessage) -> Void = { _ in }

    @MainActor public init(
        viewModel: ChatViewModel? = nil,
        onBack: @escaping () -> Void = {},
        onChatClick: @escaping (ChatMessage) -> Void = { _ in }
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel ?? ChatViewModel())
        self.onBack = onBack
        self.onChatClick = onChatClick
    }

    private var archivedList: [ChatMessage] {
        return viewModel.uiState.archivedChats.data ?? []
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                }
                Spacer()
                Text("Archived Chats")
                    .font(.system(size: 18, weight: .bold))
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()

            if archivedList.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "archivebox")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No archived chats")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(archivedList) { chat in
                        HStack {
                            ChatItemRow(chat: chat)
                                .onTapGesture {
                                    onChatClick(chat)
                                }

                            Spacer()

                            Button(action: {
                                viewModel.archiveChat(chatId: chat.id, isArchived: false)
                            }) {
                                Image(systemName: "tray.and.arrow.up")
                                    .font(.system(size: 18))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
        }
    }
}
