import SwiftUI

struct SecuritySettingsForm: View {
    @State private var lastSeen = "everyone"
    @State private var online = "everyone"
    @State private var stories = "my_contacts"
    @State private var readReceipts = true
    @State private var blocked: [ApiUser] = []
    @State private var pin = ""
    @State private var message = ""
    @State private var showPhone = false
    @State private var phoneAudience: String?
    @State private var photoAudience: String?
    @State private var ready = false
    @State private var loadedLastSeen = "everyone"
    @State private var loadedOnline = "everyone"
    @State private var loadedStories = "my_contacts"
    @State private var loadedReceipts = true
    private let pinKey = "tj_passcode"

    var body: some View {
        Form {
            Section("Last seen") {
                Picker("Who can see last seen", selection: $lastSeen) {
                    Text("Everyone").tag("everyone")
                    Text("My contacts").tag("my_contacts")
                    Text("Nobody").tag("nobody")
                }
                .onChange(of: lastSeen) { _ in Task { await saveIfReady() } }
            }
            Section("Online") {
                Picker("Who can see online", selection: $online) {
                    Text("Everyone").tag("everyone")
                    Text("My contacts").tag("my_contacts")
                    Text("Nobody").tag("nobody")
                }
                .onChange(of: online) { _ in Task { await saveIfReady() } }
            }
            Section("Stories") {
                Picker("Who can see stories", selection: $stories) {
                    Text("My contacts").tag("my_contacts")
                    Text("My contacts except").tag("my_contacts_except")
                }
                .onChange(of: stories) { _ in Task { await saveIfReady() } }
            }
            Toggle("Read receipts", isOn: $readReceipts)
                .onChange(of: readReceipts) { _ in Task { await saveIfReady() } }
            Section("Phone number") {
                if let phoneAudience {
                    Picker("Who can see your phone number", selection: Binding(get: { phoneAudience }, set: { self.phoneAudience = $0; Task { await save() } })) {
                        Text("Everyone").tag("everyone")
                        Text("My contacts").tag("my_contacts")
                        Text("Nobody").tag("nobody")
                    }
                } else {
                    Button("Your phone number") { showPhone = true }
                }
            }
            if let photoAudience {
                Section("Profile photo") {
                    Picker("Who can see your profile photo", selection: Binding(get: { photoAudience }, set: { self.photoAudience = $0; Task { await save() } })) {
                        Text("Everyone").tag("everyone")
                        Text("My contacts").tag("my_contacts")
                        Text("Nobody").tag("nobody")
                    }
                }
            } else {
                Text("Profile photo audience cannot be changed yet.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Section("Passcode lock") {
                SecureField("4-digit PIN", text: $pin)
                    .keyboardType(.numberPad)
                Button("Save PIN on this phone") { savePin() }
            }
            if BiometricHelper.isBiometricAvailable() {
                Section("Unlock") {
                    Button("Unlock with biometrics") {
                        BiometricHelper.showBiometricPrompt(onSuccess: {
                            message = "Unlocked"
                        }, onError: { _, text in
                            message = text
                        }, onFailed: {
                            message = "Biometrics did not match"
                        })
                    }
                }
            }
            Section("Blocked users") {
                if blocked.isEmpty {
                    Text("No blocked users")
                } else {
                    ForEach(blocked) { user in
                        HStack {
                            Text(name(user))
                            Spacer()
                            Button("Unblock") { Task { await unblock(user.id) } }
                        }
                    }
                }
            }
            if !message.isEmpty {
                Text(message).font(.caption).foregroundColor(.secondary)
            }
        }
        .navigationTitle("Security")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .alert("Phone number", isPresented: $showPhone) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Your phone number is used to sign in. Who can see it cannot be changed yet.")
        }
    }

    private func load() async {
        if let data = try? await API.data("users/privacy"),
           let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            let seen = LiveJSON.string(json["show_last_seen"])
            if !seen.isEmpty { lastSeen = seen }
            let who = LiveJSON.string(json["show_online_status"])
            if !who.isEmpty { online = who }
            let story = LiveJSON.string(json["who_can_see_stories"])
            if story == "my_contacts" || story == "my_contacts_except" { stories = story }
            if json["show_read_receipts"] != nil { readReceipts = LiveJSON.bool(json["show_read_receipts"]) }
            let phone = LiveJSON.string(json["who_can_see_phone"])
            phoneAudience = phone.isEmpty ? nil : phone
            let photo = LiveJSON.string(json["who_can_see_photo"])
            photoAudience = photo.isEmpty ? nil : photo
        }
        loadedLastSeen = lastSeen
        loadedOnline = online
        loadedStories = stories
        loadedReceipts = readReceipts
        pin = UserDefaults.standard.string(forKey: pinKey) ?? ""
        ready = true
        await loadBlocked()
    }

    private func saveIfReady() async {
        guard ready else { return }
        guard lastSeen != loadedLastSeen || online != loadedOnline || stories != loadedStories || readReceipts != loadedReceipts else { return }
        await save()
        loadedLastSeen = lastSeen
        loadedOnline = online
        loadedStories = stories
        loadedReceipts = readReceipts
    }

    private func save() async {
        var body: [String: Any] = [
            "show_last_seen": lastSeen,
            "show_online_status": online,
            "show_read_receipts": readReceipts,
            "who_can_see_stories": stories
        ]
        if let phoneAudience { body["who_can_see_phone"] = phoneAudience }
        if let photoAudience { body["who_can_see_photo"] = photoAudience }
        do {
            _ = try await API.data("users/privacy", method: "PUT", json: body)
            message = "Saved"
        } catch {
            message = error.localizedDescription
        }
    }

    private func loadBlocked() async {
        guard let data = try? await API.data("users/blocked") else { return }
        blocked = LiveJSON.list(data, keys: ["users", "blocked", "items"]).compactMap { row in
            let id = LiveJSON.string(row["id"]).nilIfEmpty ?? LiveJSON.string(row["user_id"])
            let label = ChannelJSON.visibleName(
                LiveJSON.string(row["display_name"]).nilIfEmpty ?? LiveJSON.string(row["username"]),
                id: id
            )
            guard !id.isEmpty, !label.isEmpty else { return nil }
            return ApiUser(id: id, username: LiveJSON.string(row["username"]), displayName: label, avatarUrl: LiveJSON.string(row["avatar_url"]).nilIfEmpty)
        }
    }

    private func unblock(_ id: String) async {
        do {
            _ = try await API.data("users/\(id)/unblock", method: "POST")
            await loadBlocked()
        } catch {
            message = error.localizedDescription
        }
    }

    private func savePin() {
        let digits = pin.filter(\.isNumber)
        guard digits.count == 4 else {
            message = "PIN must be 4 digits"
            return
        }
        UserDefaults.standard.set(digits, forKey: pinKey)
        pin = digits
        message = "PIN saved on this phone"
    }

    private func name(_ user: ApiUser) -> String {
        let label = ChannelJSON.visibleName(user.displayName.isEmpty ? user.username : user.displayName, id: user.id)
        return label
    }
}

struct StorageSettingsForm: View {
    @AppStorage("autoplay_gifs") private var autoplayGifs = true
    @AppStorage("autoplay_videos") private var autoplayVideos = true
    @State private var cache = "Measuring..."

    var body: some View {
        Form {
            Section("Cache") {
                Text(cache)
                Button("Clear cache") { clearCache() }
            }
            Toggle("Autoplay GIFs", isOn: $autoplayGifs)
            Toggle("Autoplay videos", isOn: $autoplayVideos)
        }
        .navigationTitle("Storage & Data")
        .navigationBarTitleDisplayMode(.inline)
        .task { measure() }
    }

    private func measure() {
        let bytes = URLCache.shared.currentDiskUsage + URLCache.shared.currentMemoryUsage
        cache = ByteCountFormatter.string(fromByteCount: Int64(bytes), countStyle: .file)
    }

    private func clearCache() {
        URLCache.shared.removeAllCachedResponses()
        measure()
    }
}

struct HelpSettingsForm: View {
    var body: some View {
        Form {
            Section {
                Link("Help Center", destination: mail("Help Center"))
                Link("Contact us", destination: mail("Contact us"))
                Link("Terms", destination: URL(string: "https://firstappserver.fly.dev/privacy")!)
                Link("Privacy", destination: URL(string: "https://firstappserver.fly.dev/privacy")!)
            }
            Section("App info") {
                Text(version)
            }
        }
        .navigationTitle("Help Center")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var version: String {
        let short = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? ""
        if short.isEmpty && build.isEmpty { return "Version not set" }
        return build.isEmpty ? short : "\(short) (\(build))"
    }

    private func mail(_ subject: String) -> URL {
        let encoded = subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? subject
        return URL(string: "mailto:support@firstapp.com?subject=\(encoded)")!
    }
}
