import Foundation
import UIKit
import UserNotifications
import BackgroundTasks

/// Application-wide state, dependency manager, lifecycle management, push registration, and background tasks.
@MainActor
public final class TJApplicationState: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    public static let shared = TJApplicationState()

    // Dependency references or managers
    @Published public var isNetworkConnected: Bool = true
    @Published public var deviceToken: String?

    private override init() {
        super.init()
        setupNotificationCenter()
        setupURLCache()
        registerBackgroundTasks()
    }

    public func configureOnLaunch() {
        print("[TJApplicationState] Initializing application dependencies...")
        setupMemoryWarningObserver()
        scheduleBackgroundSync()
    }

    // MARK: - Push Notifications Registration
    public func registerForPushNotifications() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error = error {
                print("[TJApplicationState] Push registration error: \(error.localizedDescription)")
                return
            }
            guard granted else {
                print("[TJApplicationState] Push notification authorization denied")
                return
            }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    private func setupNotificationCenter() {
        UNUserNotificationCenter.current().delegate = self
    }

    // UNUserNotificationCenterDelegate methods
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("[TJApplicationState] Received push notification payload: \(userInfo)")
        completionHandler()
    }

    // MARK: - Image / Disk Caching Setup (Coil Equivalent in iOS)
    private func setupURLCache() {
        let memoryCapacity = 50 * 1024 * 1024 // 50 MB
        let diskCapacity = 250 * 1024 * 1024 // 250 MB
        let urlCache = URLCache(memoryCapacity: memoryCapacity, diskCapacity: diskCapacity, diskPath: "image_cache")
        URLCache.shared = urlCache
    }

    // MARK: - Memory Warning Handling (onTrimMemory Equivalent)
    private func setupMemoryWarningObserver() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleMemoryWarning),
            name: UIApplication.didReceiveMemoryWarningNotification,
            object: nil
        )
    }

    @objc private func handleMemoryWarning() {
        print("[TJApplicationState] Memory warning received. Clearing caches...")
        URLCache.shared.removeAllCachedResponses()
    }

    // MARK: - Background Tasks (WorkManager / Periodic Sync Equivalent)
    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.tpsk.tj.message_sync", using: nil) { task in
            self.handleMessageSyncTask(task: task as! BGAppRefreshTask)
        }
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.tpsk.tj.contact_sync", using: nil) { task in
            self.handleContactSyncTask(task: task as! BGAppRefreshTask)
        }
    }

    public func scheduleBackgroundSync() {
        let messageSyncRequest = BGAppRefreshTaskRequest(identifier: "com.tpsk.tj.message_sync")
        messageSyncRequest.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // 15 minutes

        let contactSyncRequest = BGAppRefreshTaskRequest(identifier: "com.tpsk.tj.contact_sync")
        contactSyncRequest.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60) // 1 hour

        do {
            try BGTaskScheduler.shared.submit(messageSyncRequest)
            try BGTaskScheduler.shared.submit(contactSyncRequest)
        } catch {
            print("[TJApplicationState] Failed to schedule background tasks: \(error.localizedDescription)")
        }
    }

    private func handleMessageSyncTask(task: BGAppRefreshTask) {
        scheduleBackgroundSync() // Reschedule next

        task.expirationHandler = {
            // Cancel operations
        }

        Task {
            // Perform background sync logic
            task.setTaskCompleted(success: true)
        }
    }

    private func handleContactSyncTask(task: BGAppRefreshTask) {
        scheduleBackgroundSync()

        task.expirationHandler = {
            // Cancel
        }

        Task {
            // Perform contact sync
            task.setTaskCompleted(success: true)
        }
    }
}
