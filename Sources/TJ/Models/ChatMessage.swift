import Foundation

struct ChatMessage: Identifiable, Codable, Hashable {
    let id: String
    let chatId: String
    let senderName: String
    let content: String
    let timestamp: String
    let isFromMe: Bool
    let isAudio: Bool
    let mediaUrl: String?

    static let sampleConversation = [
        ChatMessage(id: "m1", chatId: "c1", senderName: "Sarah Connor", content: "Hey! Are you coming to the team tech talk today?", timestamp: "10:30 AM", isFromMe: false, isAudio: false, mediaUrl: nil),
        ChatMessage(id: "m2", chatId: "c1", senderName: "Alex", content: "Yes! Preparing the presentation slides right now.", timestamp: "10:32 AM", isFromMe: true, isAudio: false, mediaUrl: nil),
        ChatMessage(id: "m3", chatId: "c1", senderName: "Sarah Connor", content: "Awesome, let me know if you need help.", timestamp: "10:35 AM", isFromMe: false, isAudio: false, mediaUrl: nil),
        ChatMessage(id: "m4", chatId: "c1", senderName: "Sarah Connor", content: "See you at the meeting!", timestamp: "10:42 AM", isFromMe: false, isAudio: false, mediaUrl: nil)
    ]
}
