import SwiftUI
import Combine

class ChannelsViewModel: ObservableObject {
    @Published var channels: [ChannelItem] = ChannelItem.samples

    func toggleJoin(channel: ChannelItem) {
        if let index = channels.firstIndex(where: { $0.id == channel.id }) {
            channels[index].isJoined.toggle()
        }
    }
}
