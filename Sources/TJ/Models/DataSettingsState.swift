import Foundation

enum AutoPlayMode: String, Codable, CaseIterable, Identifiable {
    case always = "Always"
    case wifiOnly = "Wi-Fi Only"
    case never = "Never"

    var id: String { rawValue }
}

struct AutoDownloadRules: Codable, Hashable {
    var photos: Bool = true
    var audio: Bool = false
    var videos: Bool = false
    var documents: Bool = false
}

struct DataSettingsState: Codable, Hashable {
    var mobileDownload: AutoDownloadRules = AutoDownloadRules(photos: true, audio: false, videos: false, documents: false)
    var wifiDownload: AutoDownloadRules = AutoDownloadRules(photos: true, audio: true, videos: true, documents: true)
    var roamingDownload: AutoDownloadRules = AutoDownloadRules(photos: false, audio: false, videos: false, documents: false)
    var autoPlayVideos: AutoPlayMode = .wifiOnly
    var autoPlayGifs: AutoPlayMode = .always
    var smartStreaming: Bool = true
    var cacheSizeBytes: Int64 = 0
    var networkUsageBytes: Int64 = 0
}
