import Foundation

struct ChannelItem: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let description: String
    let subscriberCount: Int
    let category: String
    let iconUrl: String
    var isJoined: Bool = false

    static let samples = [
        ChannelItem(id: "ch1", name: "iOS Developers World", description: "SwiftUI, Swift, and iOS architecture news.", subscriberCount: 14200, category: "Tech", iconUrl: "applelogo", isJoined: true),
        ChannelItem(id: "ch2", name: "TJ Official Updates", description: "News and feature drops from the TJ team.", subscriberCount: 52000, category: "News", iconUrl: "sparkles", isJoined: false),
        ChannelItem(id: "ch3", name: "Design & UX Guild", description: "UI/UX trends, Figma tips, and design systems.", subscriberCount: 28400, category: "Design", iconUrl: "paintbrush.fill", isJoined: false)
    ]
}
