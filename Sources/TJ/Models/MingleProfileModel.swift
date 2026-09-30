import Foundation

struct MingleProfileModel: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let age: Int
    let location: String
    let occupation: String
    let bio: String
    let imageUrls: [String]
    let distanceKm: Double

    static let samples = [
        MingleProfileModel(id: "m1", name: "Chloe", age: 25, location: "San Francisco, CA", occupation: "UX Designer", bio: "Coffee lover ☕️, hiker 🏔️, and iOS enthusiast.", imageUrls: ["https://picsum.photos/500/700?random=1"], distanceKm: 3.2),
        MingleProfileModel(id: "m2", name: "Daniel", age: 28, location: "Oakland, CA", occupation: "Software Engineer", bio: "Building cool apps & exploring local food spots.", imageUrls: ["https://picsum.photos/500/700?random=2"], distanceKm: 7.5)
    ]
}
