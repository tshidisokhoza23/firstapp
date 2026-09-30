import Foundation

struct AppUser: Identifiable, Codable {
    let id: String
    var name: String
    var phoneNumber: String
    var avatarUrl: String?
    var bio: String?
    var isVerified: Bool

    static let sample = AppUser(
        id: "usr_1",
        name: "Alex Morgan",
        phoneNumber: "+1 555-0199",
        avatarUrl: "https://i.pravatar.cc/150?u=usr_1",
        bio: "Tech enthusiast & traveler 🚀",
        isVerified: true
    )
}
