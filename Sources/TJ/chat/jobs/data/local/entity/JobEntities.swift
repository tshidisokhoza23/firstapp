import Foundation

public struct JobEntity: Codable, Identifiable, Hashable {
    public let id: String
    public var title: String
    public var companyName: String
    public var companyLogoUrl: String?
    public var location: String
    public var salaryRange: String
    public var jobType: String
    public var postedTime: Int64
    public var description: String
    public var requirements: [String]
    public var isVerified: Bool
    public var isApplied: Bool
    public var isSaved: Bool
    public var isExternal: Bool
    public var sourcePartner: String?
    public var trackingApplyUrl: String?
    public var hasAppliedInternal: Bool
    public var hasClickedExternal: Bool

    public init(
        id: String,
        title: String,
        companyName: String,
        companyLogoUrl: String?,
        location: String,
        salaryRange: String,
        jobType: String,
        postedTime: Int64,
        description: String,
        requirements: [String],
        isVerified: Bool,
        isApplied: Bool,
        isSaved: Bool,
        isExternal: Bool = false,
        sourcePartner: String? = nil,
        trackingApplyUrl: String? = nil,
        hasAppliedInternal: Bool = false,
        hasClickedExternal: Bool = false
    ) {
        self.id = id
        self.title = title
        self.companyName = companyName
        self.companyLogoUrl = companyLogoUrl
        self.location = location
        self.salaryRange = salaryRange
        self.jobType = jobType
        self.postedTime = postedTime
        self.description = description
        self.requirements = requirements
        self.isVerified = isVerified
        self.isApplied = isApplied
        self.isSaved = isSaved
        self.isExternal = isExternal
        self.sourcePartner = sourcePartner
        self.trackingApplyUrl = trackingApplyUrl
        self.hasAppliedInternal = hasAppliedInternal
        self.hasClickedExternal = hasClickedExternal
    }
}

public struct JobApplicationEntity: Codable, Identifiable, Hashable {
    public let id: String
    public var jobId: String
    public var jobTitle: String
    public var companyName: String
    public var location: String
    public var appliedDate: Int64
    public var status: ApplicationStatus
    public var conversationId: String?

    public init(
        id: String,
        jobId: String,
        jobTitle: String,
        companyName: String,
        location: String,
        appliedDate: Int64,
        status: ApplicationStatus,
        conversationId: String? = nil
    ) {
        self.id = id
        self.jobId = jobId
        self.jobTitle = jobTitle
        self.companyName = companyName
        self.location = location
        self.appliedDate = appliedDate
        self.status = status
        self.conversationId = conversationId
    }
}
