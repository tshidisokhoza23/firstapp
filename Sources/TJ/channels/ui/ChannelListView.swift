import SwiftUI

public struct ChannelListView: View {
    public var channels: [Channel]
    public var topBarHeight: CGFloat
    public var currentUserId: String?
    public var onChannelClick: (Channel) -> Void
    public var onSubscribe: (String) -> Void

    public init(
        channels: [Channel],
        topBarHeight: CGFloat = 0,
        currentUserId: String? = nil,
        onChannelClick: @escaping (Channel) -> Void,
        onSubscribe: @escaping (String) -> Void = { _ in }
    ) {
        self.channels = channels
        self.topBarHeight = topBarHeight
        self.currentUserId = currentUserId
        self.onChannelClick = onChannelClick
        self.onSubscribe = onSubscribe
    }

    private var followedChannels: [Channel] {
        channels.filter { $0.isSubscribed || $0.isAdmin }
    }

    private var topChannels: [Channel] {
        channels.filter { $0.isTopInCountry && !$0.isSubscribed && !$0.isAdmin }
    }

    public var body: some View {
        if channels.isEmpty {
            VStack(spacing: 8) {
                Spacer()
                Text("No channels yet")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)

                Text("Channels you follow show up here. The most followed channels in your country appear in this list too.")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                Spacer()
            }
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: topBarHeight)

                    ForEach(followedChannels) { ch in
                        ChannelItemView(
                            channel: ch,
                            currentUserId: currentUserId,
                            onClick: { onChannelClick(ch) },
                            onSubscribe: onSubscribe
                        )
                    }

                    if !topChannels.isEmpty {
                        Text("Most followed in your country")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 16)
                            .padding(.top, 18)
                            .padding(.bottom, 4)

                        ForEach(topChannels) { ch in
                            ChannelItemView(
                                channel: ch,
                                currentUserId: currentUserId,
                                onClick: { onChannelClick(ch) },
                                onSubscribe: onSubscribe
                            )
                        }
                    }

                    Spacer().frame(height: 160)
                }
            }
        }
    }
}
