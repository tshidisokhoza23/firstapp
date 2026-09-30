import SwiftUI

@main
struct TJApp: App {
    @StateObject private var globalCallState = GlobalCallState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(globalCallState)
        }
    }
}
