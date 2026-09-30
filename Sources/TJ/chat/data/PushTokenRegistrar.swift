import Foundation
import UIKit

public protocol PushTokenApiProtocol {
    func upsertPushToken(token: String, deviceId: String, platform: String) async throws
}

public final class PushTokenRegistrar {

    public static let shared = PushTokenRegistrar()

    private var apiProvider: (() -> PushTokenApiProtocol?)?
    private var isLoggedInProvider: (() -> Bool)?

    private init() {}

    public func configure(
        apiProvider: @escaping () -> PushTokenApiProtocol?,
        isLoggedInProvider: @escaping () -> Bool
    ) {
        self.apiProvider = apiProvider
        self.isLoggedInProvider = isLoggedInProvider
    }

    public func registerDeviceToken(_ deviceTokenData: Data) {
        let tokenString = deviceTokenData.map { String(format: "%02.2hhx", $0) }.joined()
        uploadToken(tokenString)
    }

    public func uploadToken(_ token: String) {
        let cleanToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanToken.isEmpty else { return }

        guard let isLoggedIn = isLoggedInProvider?(), isLoggedIn else { return }

        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? "ios-device"

        Task {
            do {
                if let api = apiProvider?() {
                    try await api.upsertPushToken(
                        token: cleanToken,
                        deviceId: deviceId,
                        platform: "ios"
                    )
                }
            } catch {
                print("[PushTokenRegistrar] Failed to upload token: \(error.localizedDescription)")
            }
        }
    }
}
