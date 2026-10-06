import Foundation

enum MediaType {
    case photo
    case audio
    case video
    case document
    case gif
}

enum StreamQuality {
    case minimal   // 360p, 64 kbps audio
    case dataSaver // 480p, 96 kbps audio
    case high      // 1080p, 256 kbps audio

    var maxVideoResolution: Int {
        switch self {
        case .minimal: return 360
        case .dataSaver: return 480
        case .high: return 1080
        }
    }

    var audioBitrateKbps: Int {
        switch self {
        case .minimal: return 64
        case .dataSaver: return 96
        case .high: return 256
        }
    }
}

class MediaPolicyEngine {
    static let shared = MediaPolicyEngine()

    private let networkMonitor = NetworkMonitor.shared

    func canAutoDownload(type: MediaType, settings: DataSettingsState) -> Bool {
        let status = networkMonitor.status
        guard status.isConnected else { return false }

        let ruleSet: AutoDownloadRules = {
            if status.isRoaming {
                return settings.roamingDownload
            } else if status.interfaceType == .cellular || status.isMetered {
                return settings.mobileDownload
            } else {
                return settings.wifiDownload
            }
        }()

        switch type {
        case .photo, .gif:
            return ruleSet.photos
        case .audio:
            return ruleSet.audio
        case .video:
            return ruleSet.videos
        case .document:
            return ruleSet.documents
        }
    }

    func shouldAutoPlay(isGif: Bool, settings: DataSettingsState) -> Bool {
        let status = networkMonitor.status
        guard status.isConnected, !status.isRoaming else { return false }

        let mode = isGif ? settings.autoPlayGifs : settings.autoPlayVideos

        switch mode {
        case .always:
            return true
        case .wifiOnly:
            return status.interfaceType == .wifi && !status.isMetered
        case .never:
            return false
        }
    }

    func resolveStreamQuality(settings: DataSettingsState) -> StreamQuality {
        let status = networkMonitor.status
        guard settings.smartStreaming, status.isConnected else {
            return .high
        }

        if status.isRoaming {
            return .minimal
        } else if status.interfaceType == .cellular || status.isMetered {
            return .dataSaver
        } else {
            return .high
        }
    }
}
