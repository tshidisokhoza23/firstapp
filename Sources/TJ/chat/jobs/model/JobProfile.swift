import Foundation

public struct JobProfileData: Codable, Hashable {
    public var fullName: String
    public var email: String
    public var phone: String
    public var location: String
    public var city: String
    public var country: String
    public var education: String
    public var experience: String
    public var skills: String
    public var skillsList: [String]
    public var aboutMe: String
    public var linkedinUrl: String
    public var portfolioUrl: String
    public var workPreference: String
    public var availability: String
    public var expectedSalary: String
    public var currency: String
    public var headline: String
    public var targetJobTitles: [String]
    public var isRemoteOpen: Bool
    public var noFormalExperience: Bool
    public var experienceLevel: String
    public var minSalaryTarget: Double
    public var resumeFileName: String?

    public init(
        fullName: String = "",
        email: String = "",
        phone: String = "",
        location: String = "",
        city: String = "",
        country: String = "ZA",
        education: String = "",
        experience: String = "",
        skills: String = "",
        skillsList: [String] = [],
        aboutMe: String = "",
        linkedinUrl: String = "",
        portfolioUrl: String = "",
        workPreference: String = "Remote",
        availability: String = "",
        expectedSalary: String = "",
        currency: String = "ZAR",
        headline: String = "",
        targetJobTitles: [String] = [],
        isRemoteOpen: Bool = true,
        noFormalExperience: Bool = false,
        experienceLevel: String = "mid",
        minSalaryTarget: Double = 0.0,
        resumeFileName: String? = nil
    ) {
        self.fullName = fullName
        self.email = email
        self.phone = phone
        self.location = location
        self.city = city
        self.country = country
        self.education = education
        self.experience = experience
        self.skills = skills
        self.skillsList = skillsList
        self.aboutMe = aboutMe
        self.linkedinUrl = linkedinUrl
        self.portfolioUrl = portfolioUrl
        self.workPreference = workPreference
        self.availability = availability
        self.expectedSalary = expectedSalary
        self.currency = currency
        self.headline = headline
        self.targetJobTitles = targetJobTitles
        self.isRemoteOpen = isRemoteOpen
        self.noFormalExperience = noFormalExperience
        self.experienceLevel = experienceLevel
        self.minSalaryTarget = minSalaryTarget
        self.resumeFileName = resumeFileName
    }

    public var isCompleteForEasyApply: Bool {
        !headline.trimmingCharacters(in: .whitespaces).isEmpty ||
        !skills.trimmingCharacters(in: .whitespaces).isEmpty ||
        !skillsList.isEmpty ||
        !(resumeFileName?.trimmingCharacters(in: .whitespaces).isEmpty ?? true)
    }

    public func getParsedSkills() -> [String] {
        if !skillsList.isEmpty { return skillsList }
        if skills.trimmingCharacters(in: .whitespaces).isEmpty { return [] }
        let components = skills.components(separatedBy: CharacterSet(charactersIn: ",;"))
        return components.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }
}
