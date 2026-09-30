import SwiftUI

@main
struct TJApp: App {
    @StateObject private var globalCallState = GlobalCallState()

    init() {
        TJAPIProtocol.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(globalCallState)
                .onOpenURL { url in
                    NotificationCenter.default.post(name: Notification.Name("tj.openURL"), object: url)
                }
        }
    }
}
