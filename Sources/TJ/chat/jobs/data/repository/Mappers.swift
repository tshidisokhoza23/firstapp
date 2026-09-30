import SwiftUI

public extension Job {
    func toEntity() -> JobEntity {
        JobEntity(
            id: id,
            title: title,
            companyName: companyName,
            companyLogoUrl: companyLogoUrl,
            location: location,
            salaryRange: salaryRange,
            jobType: jobType,
            postedTime: postedTime,
            description: description,
            requirements: requirements,
            isVerified: isVerified,
            isApplied: isApplied,
            isSaved: isSaved,
            isExternal: isExternal,
            sourcePartner: sourcePartner,
            trackingApplyUrl: trackingApplyUrl,
            hasAppliedInternal: hasAppliedInternal,
            hasClickedExternal: hasClickedExternal
        )
    }
}

public extension JobEntity {
    func toModel() -> Job {
        Job(
            id: id,
            title: title,
            companyName: companyName,
            companyLogoUrl: companyLogoUrl,
            location: location,
            logoColorHex: "#2196F3",
            salaryRange: salaryRange,
            jobType: jobType,
            postedTime: postedTime,
            description: description,
            requirements: requirements,
            isVerified: isVerified,
            isApplied: isApplied || hasAppliedInternal,
            isSaved: isSaved,
            createdBy: nil,
            isExternal: isExternal,
            sourcePartner: sourcePartner,
            trackingApplyUrl: trackingApplyUrl,
            hasAppliedInternal: hasAppliedInternal,
            hasClickedExternal: hasClickedExternal
        )
    }
}

public extension JobApplication {
    func toEntity() -> JobApplicationEntity {
        JobApplicationEntity(
            id: id,
            jobId: jobId,
            jobTitle: jobTitle,
            companyName: companyName,
            location: location,
            appliedDate: appliedDate,
            status: status,
            conversationId: conversationId
        )
    }
}

public extension JobApplicationEntity {
    func toModel() -> JobApplication {
        JobApplication(
            id: id,
            jobId: jobId,
            jobTitle: jobTitle,
            companyName: companyName,
            location: location,
            appliedDate: appliedDate,
            status: status,
            logoColorHex: "#81C784",
            conversationId: conversationId
        )
    }
}
