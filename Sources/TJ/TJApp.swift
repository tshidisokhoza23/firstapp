import SwiftUI
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

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
                    #if canImport(GoogleSignIn)
                    GIDSignIn.sharedInstance.handle(url)
                    #endif
                    NotificationCenter.default.post(name: Notification.Name("tj.openURL"), object: url)
                }
        }
    }
}
