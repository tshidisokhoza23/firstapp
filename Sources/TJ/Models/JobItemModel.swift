import Foundation

struct JobItemModel: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let company: String
    let location: String
    let salaryRange: String
    let jobType: String
    let description: String

    static let samples = [
        JobItemModel(id: "j1", title: "Senior iOS Engineer", company: "TPSK Labs", location: "Remote / San Francisco", salaryRange: "$140,000 - $180,000", jobType: "Full-time", description: "Join TPSK Labs to build next-generation iOS applications using SwiftUI, Combine, and modern architecture patterns."),
        JobItemModel(id: "j2", title: "Android Architect", company: "Tech Flow", location: "New York, NY", salaryRange: "$150,000 - $190,000", jobType: "Full-time", description: "Lead Android platform development with Jetpack Compose, Kotlin Coroutines, and Hilt."),
        JobItemModel(id: "j3", title: "Product Designer", company: "Studio X", location: "Remote", salaryRange: "$110,000 - $140,000", jobType: "Contract", description: "Design intuitive user interfaces and experiences for mobile and web products.")
    ]
}
