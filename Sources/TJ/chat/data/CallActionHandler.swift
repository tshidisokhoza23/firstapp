import Foundation
import UserNotifications

public final class CallActionHandler {

    public static let shared = CallActionHandler()

    public var onCallAccepted: ((_ sessionId: String, _ senderId: String, _ callType: CallType) -> Void)?
    public var onCallDeclined: ((_ sessionId: String, _ senderId: String) -> Void)?
    public var onTextMessageReplied: ((_ conversationId: String, _ text: String) -> Void)?

    private init() {}

    public func handleNotificationResponse(
        actionIdentifier: String,
        userInfo: [AnyHashable: Any],
        response: UNNotificationResponse? = nil
    ) {
        let sessionId = userInfo["session_id"] as? String ?? ""
        let senderId = userInfo["sender_id"] as? String ?? userInfo["caller_id"] as? String ?? ""
        let callTypeStr = userInfo["call_type"] as? String ?? "voice"
        let callType: CallType = (callTypeStr == "video") ? .video : .voice

        switch actionIdentifier {
        case PushNotificationHandler.actionAcceptCall:
            CallKitService.shared.reportConnected(sessionId: sessionId)
            onCallAccepted?(sessionId, senderId, callType)

        case PushNotificationHandler.actionDeclineCall:
            CallKitService.shared.endCall(sessionId: sessionId, reason: "user_declined")
            onCallDeclined?(sessionId, senderId)

        case PushNotificationHandler.actionReplyMessage:
            if let textResponse = response as? UNTextInputNotificationResponse {
                let text = textResponse.userText
                let conversationId = userInfo["conversation_id"] as? String ?? userInfo["contact_id"] as? String ?? "0"
                onTextMessageReplied?(conversationId, text)
            }

        default:
            break
        }
    }
}
