import Foundation
import UIKit

public protocol PushTokenApiProtocol {
    func upsertPushToken(token: String, deviceId: String, platform: String) async throws
}

public final class PushTokenRegistrar {

    public static let shared = PushTokenRegistrar()

    private var apiProvider: (() -> PushTokenApiProtocol?)?
    private var isLoggedInProvider: (() -> Bool)?
    private var pending: [(token: String, voip: Bool)] = []

    private init() {}

    public func configure(
        apiProvider: @escaping () -> PushTokenApiProtocol?,
        isLoggedInProvider: @escaping () -> Bool
    ) {
        self.apiProvider = apiProvider
        self.isLoggedInProvider = isLoggedInProvider
    }

    public func registerDeviceToken(_ deviceTokenData: Data, voip: Bool = false) {
        let tokenString = deviceTokenData.map { String(format: "%02.2hhx", $0) }.joined()
        remember(tokenString, voip: voip)
        uploadPending()
    }

    public func uploadToken(_ token: String) {
        remember(token, voip: false)
        uploadPending()
    }

    public func uploadPending() {
        let loggedIn = isLoggedInProvider?() ?? SessionStore.shared.isLoggedIn
        guard loggedIn else { return }
        let queued = pending
        pending.removeAll()
        for item in queued {
            upload(item.token, voip: item.voip)
        }
    }

    private func remember(_ token: String, voip: Bool) {
        let clean = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        pending.removeAll { $0.voip == voip }
        pending.append((clean, voip))
    }

    private func upload(_ token: String, voip: Bool) {
        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? "ios-device"
        let bundleId = Bundle.main.bundleIdentifier ?? "com.tpsk.TJ"
        let platform = voip ? "ios_voip" : "ios"
        Task {
            do {
                _ = try await API.data("devices/push-token", method: "POST", json: [
                    "token": token,
                    "device_id": deviceId,
                    "platform": platform,
                    "bundle_id": bundleId
                ])
            } catch {
                remember(token, voip: voip)
                print("[PushTokenRegistrar] Failed to upload token: \(error.localizedDescription)")
            }
        }
    }
}
