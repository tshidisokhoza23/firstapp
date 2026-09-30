import SwiftUI
import Combine

enum BottomNavItem: Int, CaseIterable, Identifiable {
    case chat = 0
    case social = 1
    case scroll = 2
    case mingle = 3
    case stream = 4
    case ride = 5
    case more = 6

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .chat: return "Chat"
        case .social: return "Social"
        case .scroll: return "Scroll"
        case .mingle: return "Court"
        case .stream: return "Stream"
        case .ride: return "Ride"
        case .more: return "More"
        }
    }

    var iconName: String {
        switch self {
        case .chat: return "message.fill"
        case .social: return "globe"
        case .scroll: return "video.fill"
        case .mingle: return "sparkles"
        case .stream: return "music.note"
        case .ride: return "car.fill"
        case .more: return "ellipsis"
        }
    }

    static var mainItems: [BottomNavItem] {
        [.chat, .social, .scroll, .mingle, .more]
    }
}

class MainViewModel: ObservableObject {
    @Published var selectedTab: BottomNavItem = .chat
    @Published var previousTab: BottomNavItem = .chat
    @Published var isMoreSheetVisible: Bool = false

    func selectTab(_ tab: BottomNavItem) {
        if tab == .more {
            isMoreSheetVisible = true
        } else {
            previousTab = selectedTab
            selectedTab = tab
            isMoreSheetVisible = false
        }
    }

    func dismissMoreSheet() {
        isMoreSheetVisible = false
    }
}
