import Foundation

struct DeviceSession: Identifiable, Codable, Hashable {
    let id: String
    let deviceName: String
    let osVersion: String
    let appVersion: String
    let ipAddress: String
    let location: String
    let lastActiveText: String
    let isCurrent: Bool

    static let samples = [
        DeviceSession(id: "s1", deviceName: "iPhone 15 Pro", osVersion: "iOS 17.5", appVersion: "v1.0.4", ipAddress: "102.165.32.14", location: "Johannesburg, South Africa", lastActiveText: "Active Now", isCurrent: true),
        DeviceSession(id: "s2", deviceName: "Samsung Galaxy A54", osVersion: "Android 14", appVersion: "v1.0.3", ipAddress: "197.221.14.88", location: "Cape Town, South Africa", lastActiveText: "06 Oct 2026, 16:57", isCurrent: false),
        DeviceSession(id: "s3", deviceName: "Chrome on Linux", osVersion: "Ubuntu 24.04", appVersion: "Web v1.0.0", ipAddress: "41.13.20.101", location: "Durban, South Africa", lastActiveText: "29 Sep 2026, 17:13", isCurrent: false)
    ]
}
