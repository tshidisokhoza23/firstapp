import SwiftUI

public struct ChannelItemView: View {
    public var channel: Channel
    public var currentUserId: String?
    public var onClick: () -> Void
    public var onSubscribe: (String) -> Void

    public init(
        channel: Channel,
        currentUserId: String? = nil,
        onClick: @escaping () -> Void = {},
        onSubscribe: @escaping (String) -> Void = { _ in }
    ) {
        self.channel = channel
        self.currentUserId = currentUserId
        self.onClick = onClick
        self.onSubscribe = onSubscribe
    }

    private var isOwnChannel: Bool {
        let myId = currentUserId ?? ""
        return channel.isAdmin || (!myId.isEmpty && channel.createdBy == myId)
    }

    private var followerLabel: String {
        return channel.followers == 1 ? "1 follower" : "\(channel.followers) followers"
    }

    public var body: some View {
        HStack(spacing: 16) {
            // Avatar
            ZStack {
                Circle()
                    .fill(channel.logoColor)
                    .frame(width: 56, height: 56)

                if let urlString = channel.avatarUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        ProgressView()
                    }
                    .frame(width: 56, height: 56)
                    .clipShape(Circle())
                } else {
                    Text(String(channel.name.prefix(1)).uppercased())
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Text(channel.name)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.primary)

                    if channel.isVerified {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.appPrimary)
                    }
                }

                Text(channel.lastPostText.isEmpty ? "No updates yet" : channel.lastPostText)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .lineLimit(1)

                Text(followerLabel)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()

            if !channel.isSubscribed && !isOwnChannel {
                Button(action: {
                    onSubscribe(channel.id)
                }) {
                    Text("Follow")
                        .font(.system(size: 12, weight: .bold))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(Color.appPrimary.opacity(0.1))
                        .foregroundColor(.appPrimary)
                        .cornerRadius(16)
                }
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .contentShape(Rectangle())
        .onTapGesture {
            onClick()
        }
    }
}
