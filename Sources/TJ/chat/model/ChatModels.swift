import Foundation
import SwiftUI

public enum MessageStatus: String, Codable, Sendable {
    case pending = "PENDING"
    case sending = "SENDING"
    case sent = "SENT"
    case delivered = "DELIVERED"
    case read = "READ"
    case failed = "FAILED"
}

public enum MessageType: String, Codable, Sendable {
    case text = "TEXT"
    case system = "SYSTEM"
    case image = "IMAGE"
    case video = "VIDEO"
    case audioMessage = "AUDIO_MESSAGE"
    case location = "LOCATION"
    case contact = "CONTACT"
    case document = "DOCUMENT"
    case shortShare = "SHORT_SHARE"
    case jobPost = "JOB_POST"
    case voiceCall = "VOICE_CALL"
    case applicationLink = "APPLICATION_LINK"
}

public struct ApplicantProfile: Codable, Equatable, Sendable {
    public var fullName: String
    public var bio: String?
    public var cvUrl: String?

    enum CodingKeys: String, CodingKey {
        case fullName = "full_name"
        case bio
        case cvUrl = "cv_url"
    }

    public init(fullName: String, bio: String? = nil, cvUrl: String? = nil) {
        self.fullName = fullName
        self.bio = bio
        self.cvUrl = cvUrl
    }
}

public struct JobDetails: Codable, Equatable, Sendable {
    public var title: String

    public init(title: String) {
        self.title = title
    }
}

public struct LinkPreview: Codable, Equatable, Sendable {
    public var url: String
    public var title: String?
    public var description: String?
    public var thumbnailUrl: String?
    public var siteName: String?

    public init(
        url: String,
        title: String? = nil,
        description: String? = nil,
        thumbnailUrl: String? = nil,
        siteName: String? = nil
    ) {
        self.url = url
        self.title = title
        self.description = description
        self.thumbnailUrl = thumbnailUrl
        self.siteName = siteName
    }
}

public struct Message: Identifiable, Codable, Equatable, Sendable {
    public var id: String
    public var conversationId: String
    public var content: String
    public var senderId: String
    public var senderName: String
    public var receiverId: String?
    public var timestamp: Int64
    public var isFromMe: Bool
    public var status: MessageStatus
    public var type: MessageType
    public var mediaUrls: [String]
    public var fileName: String?
    public var fileSize: String?
    public var duration: String?
    public var contactName: String?
    public var contactPhone: String?
    public var latitude: Double?
    public var longitude: Double?
    public var isRead: Bool
    public var isDelivered: Bool
    public var isStarred: Bool
    public var isViewOnce: Bool
    public var expiresAt: Int64?
    public var isEncrypted: Bool
    public var replyToId: String?
    public var replyToText: String?
    public var replyToSenderName: String?
    public var reactions: [String: Int]
    public var clientId: String?
    public var applicantProfile: ApplicantProfile?
    public var jobDetails: JobDetails?
    public var linkPreview: LinkPreview?
    public var audioAmplitudes: [Int]?
    public var scheduledAt: Int64?

    public init(
        id: String = "",
        conversationId: String = "",
        content: String = "",
        senderId: String = "",
        senderName: String = "",
        receiverId: String? = nil,
        timestamp: Int64 = 0,
        isFromMe: Bool = false,
        status: MessageStatus = .sent,
        type: MessageType = .text,
        mediaUrls: [String] = [],
        fileName: String? = nil,
        fileSize: String? = nil,
        duration: String? = nil,
        contactName: String? = nil,
        contactPhone: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        isRead: Bool = false,
        isDelivered: Bool = false,
        isStarred: Bool = false,
        isViewOnce: Bool = false,
        expiresAt: Int64? = nil,
        isEncrypted: Bool = false,
        replyToId: String? = nil,
        replyToText: String? = nil,
        replyToSenderName: String? = nil,
        reactions: [String: Int] = [:],
        clientId: String? = nil,
        applicantProfile: ApplicantProfile? = nil,
        jobDetails: JobDetails? = nil,
        linkPreview: LinkPreview? = nil,
        audioAmplitudes: [Int]? = nil,
        scheduledAt: Int64? = nil
    ) {
        self.id = id
        self.conversationId = conversationId
        self.content = content
        self.senderId = senderId
        self.senderName = senderName
        self.receiverId = receiverId
        self.timestamp = timestamp
        self.isFromMe = isFromMe
        self.status = status
        self.type = type
        self.mediaUrls = mediaUrls
        self.fileName = fileName
        self.fileSize = fileSize
        self.duration = duration
        self.contactName = contactName
        self.contactPhone = contactPhone
        self.latitude = latitude
        self.longitude = longitude
        self.isRead = isRead
        self.isDelivered = isDelivered
        self.isStarred = isStarred
        self.isViewOnce = isViewOnce
        self.expiresAt = expiresAt
        self.isEncrypted = isEncrypted
        self.replyToId = replyToId
        self.replyToText = replyToText
        self.replyToSenderName = replyToSenderName
        self.reactions = reactions
        self.clientId = clientId
        self.applicantProfile = applicantProfile
        self.jobDetails = jobDetails
        self.linkPreview = linkPreview
        self.audioAmplitudes = audioAmplitudes
        self.scheduledAt = scheduledAt
    }
}
