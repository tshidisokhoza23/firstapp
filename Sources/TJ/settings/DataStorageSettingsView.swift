import SwiftUI

struct AutoDownloadRulesSheet: View {
    let title: String
    @Binding var rules: AutoDownloadRules
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Types of media")) {
                    Toggle("Photos", isOn: $rules.photos)
                    Toggle("Voice Notes & Audio", isOn: $rules.audio)
                    Toggle("Videos", isOn: $rules.videos)
                    Toggle("Documents & Files", isOn: $rules.documents)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct DataStorageSettingsView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var storageManager = StorageManager.shared
    @StateObject private var networkMonitor = NetworkMonitor.shared

    @State private var settings = DataSettingsState()
    @State private var editingRulesType: String? = nil
    @State private var showingCacheClearedToast: Bool = false

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Active Connection")) {
                    HStack {
                        Image(systemName: networkMonitor.status.interfaceType == .wifi ? "wifi" : "antenna.radiowaves.left.and.right")
                            .foregroundColor(AppColors.brandPrimary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(networkMonitor.status.isConnected ? "Connected (\(networkMonitor.status.interfaceType == .wifi ? "Wi-Fi" : "Cellular"))" : "Offline")
                                .font(.headline)
                            if networkMonitor.status.isMetered {
                                Text("Metered Connection (Hotspot / Limited Data)")
                                    .font(.caption)
                                    .foregroundColor(.orange)
                            }
                        }
                    }
                }

                Section(header: Text("Media Auto-Download")) {
                    Button(action: { editingRulesType = "Mobile Data" }) {
                        HStack {
                            Text("On Mobile Data")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(formatRulesSummary(settings.mobileDownload))
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { editingRulesType = "Wi-Fi" }) {
                        HStack {
                            Text("On Wi-Fi")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(formatRulesSummary(settings.wifiDownload))
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }

                    Button(action: { editingRulesType = "Roaming" }) {
                        HStack {
                            Text("On Roaming")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(formatRulesSummary(settings.roamingDownload))
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                    }
                }

                Section(header: Text("Auto-Play Media")) {
                    Picker("Auto-play Videos", selection: $settings.autoPlayVideos) {
                        ForEach(AutoPlayMode.allCases) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Picker("Auto-play GIFs", selection: $settings.autoPlayGifs) {
                            ForEach(AutoPlayMode.allCases) { mode in
                                Text(mode.rawValue).tag(mode)
                            }
                        }
                        Text("Loop GIFs automatically in feeds and chats")
                            .font(.caption2)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }

                Section(header: Text("Streaming Quality")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Toggle("Smart Streaming", isOn: $settings.smartStreaming)
                        Text("Adjust video & audio quality automatically based on connection speed")
                            .font(.caption2)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }

                Section(header: Text("Storage & Cache Analytics")) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Cached Media Size")
                                .font(.headline)
                            Text("Images, video chunks, and document cache")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                        Spacer()
                        Text(storageManager.formattedCacheSize())
                            .font(.headline)
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {
                        storageManager.clearCache {
                            showingCacheClearedToast = true
                        }
                    }) {
                        HStack {
                            Image(systemName: "trash.fill")
                                .foregroundColor(.red)
                            Text("Clear Local Cache")
                                .foregroundColor(.red)
                                .fontWeight(.semibold)
                        }
                    }
                }
            }
            .navigationTitle("Data & Storage Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(item: $editingRulesType) { type in
                if type == "Mobile Data" {
                    AutoDownloadRulesSheet(title: "On Mobile Data", rules: $settings.mobileDownload)
                } else if type == "Wi-Fi" {
                    AutoDownloadRulesSheet(title: "On Wi-Fi", rules: $settings.wifiDownload)
                } else {
                    AutoDownloadRulesSheet(title: "On Roaming", rules: $settings.roamingDownload)
                }
            }
            .alert("Cache Cleared", isPresented: $showingCacheClearedToast) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("All cached media files and temporary buffers have been purged.")
            }
        }
    }

    private func formatRulesSummary(_ rules: AutoDownloadRules) -> String {
        var items: [String] = []
        if rules.photos { items.append("Photos") }
        if rules.audio { items.append("Audio") }
        if rules.videos { items.append("Videos") }
        if rules.documents { items.append("Docs") }
        return items.isEmpty ? "Disabled" : items.joined(separator: ", ")
    }
}
