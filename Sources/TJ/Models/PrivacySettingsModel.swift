import Foundation

enum PrivacyScope: String, Codable, CaseIterable, Identifiable {
    case everyone = "Everyone"
    case myContacts = "My Contacts"
    case nobody = "Nobody"

    var id: String { rawValue }
}

struct PrivacySettingsModel: Codable, Hashable {
    var lastSeen: PrivacyScope = .everyone
    var phoneNumber: PrivacyScope = .myContacts
    var profilePhoto: PrivacyScope = .everyone
    var stories: PrivacyScope = .everyone
    var readReceipts: Bool = true
    var isPasscodeEnabled: Bool = false
    var isBiometricsEnabled: Bool = false
    var passcodePIN: String = ""
}
