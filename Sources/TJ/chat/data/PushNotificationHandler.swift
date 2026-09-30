import Foundation
import UserNotifications
import UIKit

public final class PushNotificationHandler: NSObject, UNUserNotificationCenterDelegate {

    public static let shared = PushNotificationHandler()

    public static let categoryIncomingCall = "INCOMING_CALL"
    public static let categoryNewMessage = "NEW_MESSAGE"

    public static let actionAcceptCall = "ACTION_ACCEPT_CALL"
    public static let actionDeclineCall = "ACTION_DECLINE_CALL"
    public static let actionReplyMessage = "ACTION_REPLY_MESSAGE"

    public var onOpenChat: ((_ conversationId: String, _ chatType: String) -> Void)?
    public var onOpenStory: ((_ storyId: String?, _ userId: String) -> Void)?
    public var onOpenSocial: ((_ postId: String?, _ actorId: String?) -> Void)?
    public var onOpenMingle: ((_ isRequest: Bool, _ actorId: String?) -> Void)?

    private override init() {
        super.init()
    }

    public func setupNotificationCategories() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self

        // Incoming call actions
        let acceptAction = UNNotificationAction(
            identifier: PushNotificationHandler.actionAcceptCall,
            title: "Accept",
            options: [.foreground]
        )
        let declineAction = UNNotificationAction(
            identifier: PushNotificationHandler.actionDeclineCall,
            title: "Decline",
            options: [.destructive]
        )
        let callCategory = UNNotificationCategory(
            identifier: PushNotificationHandler.categoryIncomingCall,
            actions: [acceptAction, declineAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        // Reply action for messages
        let replyAction = UNTextInputNotificationAction(
            identifier: PushNotificationHandler.actionReplyMessage,
            title: "Reply",
            options: [.foreground],
            textInputButtonTitle: "Send",
            textInputPlaceholder: "Type a message..."
        )
        let messageCategory = UNNotificationCategory(
            identifier: PushNotificationHandler.categoryNewMessage,
            actions: [replyAction],
            intentIdentifiers: [],
            options: []
        )

        center.setNotificationCategories([callCategory, messageCategory])
    }

    // MARK: - UNUserNotificationCenterDelegate

    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        let type = userInfo["type"] as? String ?? ""

        if type == "call_invite" {
            // Handled via CallKit
            completionHandler([.banner, .sound, .list])
        } else {
            completionHandler([.banner, .sound, .badge, .list])
        }
    }

    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let actionIdentifier = response.actionIdentifier

        CallActionHandler.shared.handleNotificationResponse(
            actionIdentifier: actionIdentifier,
            userInfo: userInfo,
            response: response
        )

        if actionIdentifier == UNNotificationDefaultActionIdentifier {
            handleNotificationTap(userInfo: userInfo)
        }

        completionHandler()
    }

    // MARK: - Remote Payload Handler

    public func handleRemoteNotificationPayload(_ userInfo: [AnyHashable: Any]) {
        guard let type = userInfo["type"] as? String else { return }

        switch type {
        case "call_invite":
            let sessionId = userInfo["session_id"] as? String ?? ""
            let senderId = userInfo["caller_id"] as? String ?? userInfo["sender_id"] as? String ?? ""
            let callerName = userInfo["caller_name"] as? String ?? "Incoming Call"
            let callTypeStr = userInfo["call_type"] as? String ?? "voice"
            let callType: CallType = (callTypeStr == "video") ? .video : .voice

            CallKitService.shared.reportIncomingCall(
                sessionId: sessionId,
                participantId: senderId,
                participantName: callerName,
                callType: callType
            )

        case "call_end", "call_reject":
            if let sessionId = userInfo["session_id"] as? String {
                CallKitService.shared.endCall(sessionId: sessionId, reason: type)
            }

        case "new_message":
            showLocalMessageNotification(userInfo: userInfo)

        case "new_story":
            showLocalStoryNotification(userInfo: userInfo)

        case "new_chat":
            showLocalChatNotification(userInfo: userInfo)

        case "mingle_like", "mingle_match", "mingle", "mingle_intro":
            showMingleNotification(userInfo: userInfo)

        case "social_like", "social_comment", "social_follow", "social_notification", "social":
            showSocialNotification(userInfo: userInfo)

        default:
            break
        }
    }

    // MARK: - Private Builders

    private func showLocalMessageNotification(userInfo: [AnyHashable: Any]) {
        let senderName = userInfo["sender_name"] as? String ?? "New Message"
        let body = userInfo["body"] as? String ?? ""
        let conversationId = userInfo["conversation_id"] as? String ?? userInfo["contact_id"] as? String ?? "0"
        let chatType = userInfo["chat_type"] as? String ?? "direct"

        let content = UNMutableNotificationContent()
        content.title = senderName
        content.body = body
        content.sound = .default
        content.categoryIdentifier = PushNotificationHandler.categoryNewMessage
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: "msg_\(conversationId)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func showLocalStoryNotification(userInfo: [AnyHashable: Any]) {
        let title = userInfo["title"] as? String ?? "New Story"
        let body = userInfo["body"] as? String ?? "Someone posted a new story"

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: "story_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func showLocalChatNotification(userInfo: [AnyHashable: Any]) {
        let title = userInfo["title"] as? String ?? "New Group"
        let body = userInfo["body"] as? String ?? "You were added to a chat"

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: "chat_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func showMingleNotification(userInfo: [AnyHashable: Any]) {
        let isRequest = (userInfo["type"] as? String == "mingle_intro")
        let title = userInfo["title"] as? String ?? (isRequest ? "Mingle" : "Mingle Notification")
        let body = userInfo["body"] as? String ?? (isRequest ? "New message request on Mingle" : "Interaction on Mingle")

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: "mingle_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func showSocialNotification(userInfo: [AnyHashable: Any]) {
        let title = userInfo["title"] as? String ?? "Social Notification"
        let body = userInfo["body"] as? String ?? "New activity on your post"

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = userInfo

        let request = UNNotificationRequest(
            identifier: "social_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func handleNotificationTap(userInfo: [AnyHashable: Any]) {
        guard let type = userInfo["type"] as? String else { return }

        switch type {
        case "new_message", "new_chat":
            let conversationId = userInfo["conversation_id"] as? String ?? userInfo["contact_id"] as? String ?? "0"
            let chatType = userInfo["chat_type"] as? String ?? "direct"
            onOpenChat?(conversationId, chatType)

        case "new_story":
            let userId = userInfo["user_id"] as? String ?? userInfo["actor_id"] as? String ?? ""
            let storyId = userInfo["story_id"] as? String
            onOpenStory?(storyId, userId)

        case "mingle_like", "mingle_match", "mingle", "mingle_intro":
            let isRequest = (type == "mingle_intro")
            let actorId = userInfo["actor_id"] as? String
            onOpenMingle?(isRequest, actorId)

        case "social_like", "social_comment", "social_follow", "social_notification", "social":
            let postId = userInfo["post_id"] as? String
            let actorId = userInfo["actor_id"] as? String
            onOpenSocial?(postId, actorId)

        default:
            break
        }
    }
}
