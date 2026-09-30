import SwiftUI

struct ChannelsListView: View {
    @StateObject private var channelsViewModel = ChannelsViewModel()

    var body: some View {
        List(channelsViewModel.channels) { channel in
            HStack(spacing: 12) {
                Image(systemName: channel.iconUrl)
                    .font(.title2)
                    .foregroundColor(AppColors.brandPrimary)
                    .frame(width: 44, height: 44)
                    .background(AppColors.brandPrimary.opacity(0.1))
                    .cornerRadius(12)

                VStack(alignment: .leading, spacing: 4) {
                    Text(channel.name)
                        .font(.system(size: 16, weight: .semibold))
                    Text(channel.description)
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.greyMedium)
                        .lineLimit(1)
                }
                Spacer()
                Button(action: {
                    channelsViewModel.toggleJoin(channel: channel)
                }) {
                    Text(channel.isJoined ? "Joined" : "Join")
                        .font(.system(size: 13, weight: .semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(channel.isJoined ? AppColors.greyMedium : AppColors.brandPrimary)
            }
            .padding(.vertical, 4)
        }
        .listStyle(PlainListStyle())
    }
}
