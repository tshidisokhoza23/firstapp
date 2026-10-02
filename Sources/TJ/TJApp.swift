import SwiftUI
import UserNotifications
import UIKit

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        PushNotificationHandler.shared.setupNotificationCategories()
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        CallBackgroundKeepAlive.begin()
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        AppRuntime.connectRealtime()
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushTokenRegistrar.shared.registerDeviceToken(deviceToken)
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        PushNotificationHandler.shared.handleRemoteNotificationPayload(userInfo)
        completionHandler(.newData)
    }
}

@main
struct TJApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
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
