import SwiftUI

public struct Job: Identifiable, Hashable, Codable {
    public let id: String
    public var title: String
    public var companyName: String
    public var companyLogoUrl: String?
    public var location: String
    public var logoHex: String
    public var salaryRange: String
    public var jobType: String
    public var postedTime: Int64
    public var description: String
    public var requirements: [String]
    public var isVerified: Bool
    public var isApplied: Bool
    public var isSaved: Bool
    public var createdBy: String?
    public var isExternal: Bool
    public var sourcePartner: String?
    public var trackingApplyUrl: String?
    public var hasAppliedInternal: Bool
    public var hasClickedExternal: Bool

    public var logoColor: Color {
        Color(hex: logoHex) ?? Color.blue
    }

    public init(
        id: String,
        title: String,
        companyName: String,
        companyLogoUrl: String? = nil,
        location: String,
        logoHex: String = "#2196F3",
        salaryRange: String = "",
        jobType: String = "Full-time",
        postedTime: Int64 = 0,
        description: String = "",
        requirements: [String] = [],
        isVerified: Bool = true,
        isApplied: Bool = false,
        isSaved: Bool = false,
        createdBy: String? = nil,
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
        self.logoHex = logoHex
        self.salaryRange = salaryRange
        self.jobType = jobType
        self.postedTime = postedTime
        self.description = description
        self.requirements = requirements
        self.isVerified = isVerified
        self.isApplied = isApplied
        self.isSaved = isSaved
        self.createdBy = createdBy
        self.isExternal = isExternal
        self.sourcePartner = sourcePartner
        self.trackingApplyUrl = trackingApplyUrl
        self.hasAppliedInternal = hasAppliedInternal
        self.hasClickedExternal = hasClickedExternal
    }
}

extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        let r, g, b, a: Double
        if hexSanitized.count == 6 {
            r = Double((rgb & 0xFF0000) >> 16) / 255.0
            g = Double((rgb & 0x00FF00) >> 8) / 255.0
            b = Double(rgb & 0x0000FF) / 255.0
            a = 1.0
        } else if hexSanitized.count == 8 {
            a = Double((rgb & 0xFF000000) >> 24) / 255.0
            r = Double((rgb & 0x00FF0000) >> 16) / 255.0
            g = Double((rgb & 0x0000FF00) >> 8) / 255.0
            b = Double(rgb & 0x000000FF) / 255.0
        } else {
            return nil
        }

        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
