import SwiftUI

public struct MessageInfoView: View {
    public let messageId: String
    @StateObject public var viewModel: ChatViewModel
    public var onBack: () -> Void = {}

    @State private var readers: [ApiReader] = []
    @State private var isLoading: Bool = true

    @MainActor public init(
        messageId: String,
        viewModel: ChatViewModel? = nil,
        onBack: @escaping () -> Void = {}
    ) {
        self.messageId = messageId
        self._viewModel = StateObject(wrappedValue: viewModel ?? ChatViewModel())
        self.onBack = onBack
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                }
                Spacer()
                Text("Message Info")
                    .font(.system(size: 18, weight: .bold))
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()

            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if readers.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "checkmark.seal")
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                    Text("No read receipts yet")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    Section(header: Text("Read By").font(.system(size: 12, weight: .bold)).foregroundColor(.accentColor)) {
                        ForEach(readers, id: \.userId) { reader in
                            HStack {
                                Circle()
                                    .fill(Color.accentColor.opacity(0.15))
                                    .frame(width: 40, height: 40)
                                    .overlay(
                                        Text(String(reader.userId.prefix(1)).uppercased())
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(.accentColor)
                                    )

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(reader.userId)
                                        .font(.system(size: 15, weight: .bold))
                                    Text(formatFullDate(reader.readAt))
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
        }
        .onAppear {
            // Load readers
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                self.isLoading = false
            }
        }
    }

    private func formatFullDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, HH:mm"
        return formatter.string(from: date)
    }
}
