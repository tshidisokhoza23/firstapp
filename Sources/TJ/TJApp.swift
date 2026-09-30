import SwiftUI

@main
struct TJApp: App {
    @StateObject private var mainViewModel = MainViewModel()
    @StateObject private var chatViewModel = ChatViewModel()
    @StateObject private var socialViewModel = SocialViewModel()
    @StateObject private var shortsViewModel = ShortsViewModel()
    @StateObject private var mingleViewModel = MingleViewModel()
    @StateObject private var streamViewModel = StreamViewModel()
    @StateObject private var driveViewModel = DriveViewModel()
    @StateObject private var authViewModel = AuthViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(mainViewModel)
                .environmentObject(chatViewModel)
                .environmentObject(socialViewModel)
                .environmentObject(shortsViewModel)
                .environmentObject(mingleViewModel)
                .environmentObject(streamViewModel)
                .environmentObject(driveViewModel)
                .environmentObject(authViewModel)
        }
    }
}
