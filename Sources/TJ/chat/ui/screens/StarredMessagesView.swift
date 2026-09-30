import SwiftUI

public struct StarredMessagesView: View {
    @StateObject public var viewModel: StarredMessagesViewModel
    public var onBack: () -> Void = {}
    public var onMessageClick: (Message) -> Void = { _ in }

    @State private var selectedMessage: Message? = nil
    @State private var showOptionsAlert: Bool = false

    @MainActor public init(
        viewModel: StarredMessagesViewModel = StarredMessagesViewModel(),
        onBack: @escaping () -> Void = {},
        onMessageClick: @escaping (Message) -> Void = { _ in }
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.onBack = onBack
        self.onMessageClick = onMessageClick
    }

    private var messagesList: [Message] {
        return viewModel.starredMessages.data ?? []
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
                Text("Starred Messages")
                    .font(.system(size: 18, weight: .bold))
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()

            if messagesList.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "star")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No starred messages")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(messagesList) { message in
                        HStack(spacing: 12) {
                            Image(systemName: "star.fill")
                                .foregroundColor(.yellow)
                                .font(.system(size: 20))

                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(message.senderName)
                                        .font(.system(size: 15, weight: .bold))
                                    Spacer()
                                    Text(formatTime(message.timestamp))
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }

                                Text(message.content)
                                    .font(.system(size: 14))
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            onMessageClick(message)
                        }
                        .onLongPressGesture {
                            selectedMessage = message
                            showOptionsAlert = true
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
        }
        .confirmationDialog("Message Options", isPresented: $showOptionsAlert, titleVisibility: .visible) {
            Button("Unstar Message") {
                if let msg = selectedMessage {
                    viewModel.unstarMessage(messageId: msg.id)
                }
            }
            Button("Delete Message", role: .destructive) {
                if let msg = selectedMessage {
                    viewModel.deleteMessage(messageId: msg.id)
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    private func formatTime(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}
