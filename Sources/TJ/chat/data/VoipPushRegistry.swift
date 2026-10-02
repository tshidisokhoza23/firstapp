import Foundation
import PushKit

/// Registers the VoIP token and reports a CallKit call as soon as Apple wakes the process.
public final class VoipPushRegistry: NSObject, PKPushRegistryDelegate {
    public static let shared = VoipPushRegistry()

    private var registry: PKPushRegistry?

    private override init() {
        super.init()
    }

    public func start() {
        if registry != nil { return }
        let registry = PKPushRegistry(queue: .main)
        registry.delegate = self
        registry.desiredPushTypes = [.voIP]
        self.registry = registry
    }

    public func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        guard type == .voIP else { return }
        PushTokenRegistrar.shared.registerDeviceToken(pushCredentials.token, voip: true)
    }

    public func pushRegistry(_ registry: PKPushRegistry, didInvalidatePushTokenFor type: PKPushType) {}

    public func pushRegistry(
        _ registry: PKPushRegistry,
        didReceiveIncomingPushWith payload: PKPushPayload,
        for type: PKPushType,
        completion: @escaping () -> Void
    ) {
        let info = payload.dictionaryPayload
        let eventType = (info["type"] as? String) ?? ""
        if eventType == "call_invite" {
            PushNotificationHandler.shared.handleRemoteNotificationPayload(info)
        } else {
            // Apple requires every VoIP push to report a call. End it immediately
            // when the payload is not an invite so the phone is not left ringing.
            let sessionId = (info["session_id"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? UUID().uuidString
            CallKitService.shared.reportIncomingCall(
                sessionId: sessionId,
                participantId: (info["sender_id"] as? String) ?? "",
                participantName: "Incoming call",
                callType: .voice
            ) { _ in
                CallKitService.shared.endCall(sessionId: sessionId, reason: eventType.isEmpty ? "invalid_voip" : eventType)
            }
        }
        completion()
    }
}
