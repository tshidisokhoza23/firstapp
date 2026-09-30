import SwiftUI

public enum ApplicationStatus: String, Codable, CaseIterable {
    case pending = "PENDING"
    case reviewing = "REVIEWING"
    case interview = "INTERVIEW"
    case rejected = "REJECTED"
    case offered = "OFFERED"

    public var label: String {
        switch self {
        case .pending: return "Pending"
        case .reviewing: return "Reviewing"
        case .interview: return "Interview"
        case .rejected: return "Rejected"
        case .offered: return "Offered"
        }
    }

    public var color: Color {
        switch self {
        case .pending: return Color(hex: "#FFB74D") ?? .orange
        case .reviewing: return Color(hex: "#64B5F6") ?? .blue
        case .interview: return Color(hex: "#81C784") ?? .green
        case .rejected: return Color(hex: "#E57373") ?? .red
        case .offered: return Color(hex: "#BA68C8") ?? .purple
        }
    }
}

public struct JobApplication: Identifiable, Hashable, Codable {
    public let id: String
    public var jobId: String
    public var jobTitle: String
    public var companyName: String
    public var location: String
    public var appliedDate: Int64
    public var status: ApplicationStatus
    public var logoHex: String
    public var conversationId: String?

    public var logoColor: Color {
        Color(hex: logoHex) ?? Color.blue
    }

    public init(
        id: String,
        jobId: String,
        jobTitle: String,
        companyName: String,
        location: String,
        appliedDate: Int64,
        status: ApplicationStatus,
        logoHex: String = "#2196F3",
        conversationId: String? = nil
    ) {
        self.id = id
        self.jobId = jobId
        self.jobTitle = jobTitle
        self.companyName = companyName
        self.location = location
        self.appliedDate = appliedDate
        self.status = status
        self.logoHex = logoHex
        self.conversationId = conversationId
    }
}
