import SwiftUI
import PhotosUI
import UserNotifications

enum TJEvents {
    static let openChat = Notification.Name("tj.openChat")
    static let openPost = Notification.Name("tj.openPost")
    static let openMingle = Notification.Name("tj.openMingle")
    static let clearLocalChats = Notification.Name("tj.clearLocalChats")
}

enum ChatPrefs {
    static let font = "tj_chat_font_size"
    static let enter = "tj_enter_is_send"
    static let media = "tj_chat_media_visible"
    static let animations = "tj_chat_animations"

    static func fontSize() -> CGFloat {
        let stored = UserDefaults.standard.double(forKey: font)
        return stored > 0 ? CGFloat(stored) : 16
    }

    static func enterIsSend() -> Bool {
        guard UserDefaults.standard.object(forKey: enter) != nil else { return false }
        return UserDefaults.standard.bool(forKey: enter)
    }
}

enum AlertPrefs {
    static let privateChats = "tj_alert_private"
    static let groups = "tj_alert_groups"
    static let channels = "tj_alert_channels"
    static let sounds = "tj_alert_sounds"
    static let vibrate = "tj_alert_vibrate"
    static let previews = "tj_alert_previews"

    static func on(_ key: String) -> Bool {
        if UserDefaults.standard.object(forKey: key) == nil { return true }
        return UserDefaults.standard.bool(forKey: key)
    }

    static func allowsChat(type: String) -> Bool {
        let value = type.lowercased()
        if value.contains("group") { return on(groups) }
        if value.contains("channel") { return on(channels) }
        return on(privateChats)
    }
}

enum AvatarUpload {
    static func putJPEG(_ data: Data) async throws -> String {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        func append(_ text: String) { body.append(Data(text.utf8)) }
        append("--\(boundary)\r\nContent-Disposition: form-data; name=\"avatar\"; filename=\"avatar.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
        body.append(data)
        append("\r\n--\(boundary)--\r\n")
        guard var req = API.request("users/avatar", method: "PUT", body: body) else { throw APIError.badURL }
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        let (responseData, response) = try await URLSession.shared.data(for: req)
        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
        guard (200...299).contains(code),
              let json = try? JSONSerialization.jsonObject(with: responseData) as? [String: Any] else {
            throw APIError.status(code, String(data: responseData, encoding: .utf8) ?? "avatar upload failed")
        }
        return LiveJSON.string(json["avatar_url"])
    }
}

struct FollowListView: View {
    let userId: String
    let title: String
    var onOpen: (String) -> Void
    @State private var people: [(id: String, name: String, subtitle: String)] = []
    @State private var status = "Loading..."

    var body: some View {
        NavigationStack {
            Group {
                if people.isEmpty {
                    Text(status).foregroundColor(.secondary)
                } else {
                    List(people, id: \.id) { person in
                        Button {
                            onOpen(person.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(person.name).foregroundColor(.primary)
                                if !person.subtitle.isEmpty {
                                    Text(person.subtitle).font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await load() }
    }

    private func load() async {
        let path = title == "Following" ? "users/\(userId)/following" : "users/\(userId)/followers"
        guard let data = try? await API.data(path) else {
            status = "Could not load \(title.lowercased())"
            return
        }
        let rows = LiveJSON.list(data, keys: ["contacts", "users", "followers", "following"])
        people = rows.compactMap { row in
            let id = LiveJSON.string(row["id"])
            guard !id.isEmpty else { return nil }
            let name = personName(row)
            guard !name.isEmpty else { return nil }
            let handle = LiveJSON.string(row["username"])
            return (id, name, handle.isEmpty ? "" : "@\(handle)")
        }
        status = people.isEmpty ? "No one here yet" : ""
    }

    private func personName(_ row: [String: Any]) -> String {
        let display = LiveJSON.string(row["display_name"])
        if !display.isEmpty && display.lowercased() != "user" { return display }
        let username = LiveJSON.string(row["username"])
        if !username.isEmpty { return username }
        return LiveJSON.string(row["phone_number"])
    }
}

struct MessageReadersView: View {
    let messageId: String
    let sentByMe: Bool
    @State private var rows: [(id: String, name: String, when: String)] = []
    @State private var status = "Loading..."

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Message info").font(.headline)
            Text(sentByMe ? "Sent by you" : "Received").font(.caption).foregroundColor(.secondary)
            if rows.isEmpty {
                Text(status).font(.subheadline).foregroundColor(.secondary)
            } else {
                Text("Read").font(.subheadline.bold())
                ForEach(rows, id: \.id) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        if !row.name.isEmpty { Text(row.name) }
                        Text(row.when).font(.caption).foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .task { await load() }
    }

    private func load() async {
        guard !messageId.isEmpty, let data = try? await API.data("messages/\(messageId)/readers") else {
            status = "No read receipts yet"
            return
        }
        let parsed = LiveJSON.list(data, keys: ["readers", "items"])
        var built: [(id: String, name: String, when: String)] = []
        for row in parsed {
            let userId = LiveJSON.string(row["user_id"])
            let millis = LiveJSON.int64(row["read_at"])
            let when = millis > 0 ? Self.format(millis) : ""
            var name = ""
            if !userId.isEmpty, let profile = try? await API.data("users/\(userId)"),
               let json = try? JSONSerialization.jsonObject(with: profile) as? [String: Any] {
                let user = (json["user"] as? [String: Any]) ?? json
                let display = LiveJSON.string(user["display_name"])
                if !display.isEmpty && display.lowercased() != "user" {
                    name = display
                } else {
                    name = LiveJSON.string(user["username"])
                    if name.isEmpty { name = LiveJSON.string(user["phone_number"]) }
                }
            }
            built.append((userId.isEmpty ? UUID().uuidString : userId, name, when))
        }
        rows = built
        status = rows.isEmpty ? "No read receipts yet" : ""
    }

    private static func format(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, HH:mm"
        return formatter.string(from: date)
    }
}

struct ChatSettingsForm: View {
    @State private var fontSize = ChatPrefs.fontSize()
    @State private var enterIsSend = ChatPrefs.enterIsSend()
    @State private var mediaVisible = AlertPrefs.on(ChatPrefs.media)
    @State private var animations = AlertPrefs.on(ChatPrefs.animations)
    @State private var note = ""

    var body: some View {
        Form {
            Stepper(value: $fontSize, in: 12...24, step: 1) {
                Text("Font size \(Int(fontSize))")
            }
            .onChange(of: fontSize) { value in
                UserDefaults.standard.set(Double(value), forKey: ChatPrefs.font)
            }
            Toggle("Enter is send", isOn: $enterIsSend)
                .onChange(of: enterIsSend) { value in
                    UserDefaults.standard.set(value, forKey: ChatPrefs.enter)
                }
            Toggle("Media visibility", isOn: $mediaVisible)
                .onChange(of: mediaVisible) { value in
                    UserDefaults.standard.set(value, forKey: ChatPrefs.media)
                }
            Toggle("Animations", isOn: $animations)
                .onChange(of: animations) { value in
                    UserDefaults.standard.set(value, forKey: ChatPrefs.animations)
                }
            Button("Clear chats on this phone") {
                Task {
                    await LocalDatabase.shared.clearAll()
                    URLCache.shared.removeAllCachedResponses()
                    NotificationCenter.default.post(name: TJEvents.clearLocalChats, object: nil)
                    note = "Local chats cleared. Messages on the server remain."
                }
            }
            if !note.isEmpty { Text(note).font(.caption).foregroundColor(.secondary) }
        }
        .navigationTitle("Chats")
    }
}

struct AlertSettingsForm: View {
    @State private var privateChats = AlertPrefs.on(AlertPrefs.privateChats)
    @State private var groups = AlertPrefs.on(AlertPrefs.groups)
    @State private var channels = AlertPrefs.on(AlertPrefs.channels)
    @State private var sounds = AlertPrefs.on(AlertPrefs.sounds)
    @State private var vibrate = AlertPrefs.on(AlertPrefs.vibrate)
    @State private var previews = AlertPrefs.on(AlertPrefs.previews)

    var body: some View {
        Form {
            Toggle("Private Chats", isOn: $privateChats).onChange(of: privateChats) { UserDefaults.standard.set($0, forKey: AlertPrefs.privateChats) }
            Toggle("Groups", isOn: $groups).onChange(of: groups) { UserDefaults.standard.set($0, forKey: AlertPrefs.groups) }
            Toggle("Channels", isOn: $channels).onChange(of: channels) { UserDefaults.standard.set($0, forKey: AlertPrefs.channels) }
            Toggle("Notification Sounds", isOn: $sounds).onChange(of: sounds) { UserDefaults.standard.set($0, forKey: AlertPrefs.sounds) }
            Toggle("Vibrate", isOn: $vibrate).onChange(of: vibrate) { UserDefaults.standard.set($0, forKey: AlertPrefs.vibrate) }
            Toggle("Message Previews", isOn: $previews).onChange(of: previews) { UserDefaults.standard.set($0, forKey: AlertPrefs.previews) }
        }
        .navigationTitle("Alerts")
    }
}

struct LanguageSettingsForm: View {
    private let languages: [(String, String)] = [
        ("English", "en"), ("Afrikaans", "af"), ("isiZulu", "zu"), ("isiXhosa", "xh"),
        ("Sepedi", "nso"), ("Setswana", "tn"), ("Sesotho", "st"), ("Xitsonga", "ts"),
        ("siSwati", "ss"), ("Tshivenda", "ve"), ("isiNdebele", "nr")
    ]
    @State private var selected = UserDefaults.standard.string(forKey: "tj_language") ?? "en"

    var body: some View {
        List(languages, id: \.1) { item in
            Button {
                selected = item.1
                UserDefaults.standard.set(item.1, forKey: "tj_language")
                UserDefaults.standard.set([item.1], forKey: "AppleLanguages")
            } label: {
                HStack {
                    Text(item.0).foregroundColor(.primary)
                    Spacer()
                    if selected == item.1 { Image(systemName: "checkmark") }
                }
            }
        }
        .navigationTitle("Language")
        .safeAreaInset(edge: .bottom) {
            Text("This stores the same language choice as Android. The iOS screens are not translated yet.")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding()
        }
    }
}

struct CreateShortSheet: View {
    var onPosted: () -> Void
    @State private var item: PhotosPickerItem?
    @State private var caption = ""
    @State private var status = ""
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                PhotosPicker("Choose video", selection: $item, matching: .videos)
                TextField("Caption", text: $caption, axis: .vertical)
                Button(busy ? "Posting..." : "Post short") { Task { await post() } }
                    .disabled(item == nil || busy)
                if !status.isEmpty { Text(status).font(.caption).foregroundColor(.secondary) }
            }
            .navigationTitle("Create Short")
        }
    }

    private func post() async {
        guard let item else { return }
        busy = true
        defer { busy = false }
        guard let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty else {
            status = "Could not read that video"
            return
        }
        do {
            let url = try await MediaActions.upload(data: data, filename: "short.mp4", mime: "video/mp4", folder: "posts")
            try await MediaActions.create(content: caption, type: "short", mediaURLs: [url])
            onPosted()
        } catch {
            status = error.localizedDescription
        }
    }
}

struct LiveSearchView: View {
    var onOpenProfile: (String) -> Void
    var onOpenChat: (String, String) -> Void
    @State private var query = ""
    @State private var category: SearchCategory = .all
    @State private var results: [SearchResultItem] = []
    @State private var status = ""
    @State private var job: SearchResultItem?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                TextField("Search", text: $query)
                    .textFieldStyle(.roundedBorder)
                    .padding()
                    .onSubmit { Task { await search() } }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(SearchCategory.allCases) { item in
                            Button(item.rawValue) {
                                category = item
                                Task { await search() }
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(category == item ? Color.blue : Color(.tertiarySystemBackground))
                            .foregroundColor(category == item ? .white : .primary)
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal)
                }
                if results.isEmpty {
                    Text(status.isEmpty ? "Search contacts, chats, channels, and jobs" : status)
                        .foregroundColor(.secondary)
                        .padding()
                    Spacer()
                } else {
                    List(results) { item in
                        Button {
                            open(item)
                        } label: {
                            VStack(alignment: .leading) {
                                Text(item.title).foregroundColor(.primary)
                                if !item.subtitle.isEmpty {
                                    Text(item.subtitle).font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Search")
            .sheet(item: $job) { item in
                VStack(alignment: .leading, spacing: 8) {
                    Text(item.title).font(.headline)
                    if !item.subtitle.isEmpty { Text(item.subtitle) }
                    Button("Close") { job = nil }
                }
                .padding()
                .presentationDetents([.medium])
            }
        }
    }

    private func search() async {
        let text = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            results = []
            status = ""
            return
        }
        let type: String
        switch category {
        case .all: type = "global"
        case .contacts, .messages: type = "chats"
        case .channels: type = "channels"
        case .jobs: type = "jobs"
        }
        guard let data = try? await API.data("search", query: ["query": text, "type": type]) else {
            results = []
            status = "Search failed"
            return
        }
        results = Self.parse(data, category: category)
        status = results.isEmpty ? "No matching results" : ""
    }

    private func open(_ item: SearchResultItem) {
        switch item.kind {
        case "profile":
            onOpenProfile(item.id)
        case "job":
            job = item
        default:
            onOpenChat(item.id, item.title)
        }
    }

    static func parse(_ data: Data, category: SearchCategory) -> [SearchResultItem] {
        let obj = LiveJSON.object(data)
        var items: [SearchResultItem] = []
        if category == .all || category == .contacts {
            items.append(contentsOf: people(obj["contacts"]))
        }
        if category == .all || category == .messages {
            items.append(contentsOf: chats(obj["chats"]))
        }
        if category == .all || category == .channels {
            items.append(contentsOf: channels(obj["channels"]))
        }
        if category == .all || category == .jobs {
            items.append(contentsOf: jobs(obj["jobs"]))
        }
        return items
    }

    private static func people(_ value: Any?) -> [SearchResultItem] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            guard let json = row as? [String: Any] else { return nil }
            let id = LiveJSON.string(json["id"])
            let name = display(json)
            guard !id.isEmpty, !name.isEmpty else { return nil }
            let handle = LiveJSON.string(json["username"])
            return SearchResultItem(id: id, title: name, subtitle: handle.isEmpty ? "" : "@\(handle)", category: .contacts, kind: "profile")
        }
    }

    private static func chats(_ value: Any?) -> [SearchResultItem] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            guard let json = row as? [String: Any] else { return nil }
            let id = LiveJSON.string(json["conversation_id"]).nilIfEmpty ?? LiveJSON.string(json["id"])
            let rawType = LiveJSON.string(json["type"]).lowercased()
            if rawType.contains("mingle") || id.hasPrefix("mingle_") { return nil }
            let title = display(json).nilIfEmpty ?? LiveJSON.string(json["title"])
            guard !id.isEmpty, !title.isEmpty else { return nil }
            return SearchResultItem(id: id, title: title, subtitle: LiveJSON.string(json["last_message"]), category: .messages, kind: "chat")
        }
    }

    private static func channels(_ value: Any?) -> [SearchResultItem] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            guard let json = row as? [String: Any] else { return nil }
            let conversation = LiveJSON.string(json["conversation_id"])
            let id = conversation.isEmpty ? LiveJSON.string(json["id"]) : conversation
            let title = LiveJSON.string(json["name"]).nilIfEmpty ?? LiveJSON.string(json["title"])
            guard !id.isEmpty, !title.isEmpty else { return nil }
            return SearchResultItem(id: id, title: title, subtitle: LiveJSON.string(json["description"]), category: .channels, kind: conversation.isEmpty ? "channel" : "chat")
        }
    }

    private static func jobs(_ value: Any?) -> [SearchResultItem] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            guard let json = row as? [String: Any] else { return nil }
            let id = LiveJSON.string(json["id"])
            let title = LiveJSON.string(json["title"])
            guard !id.isEmpty, !title.isEmpty else { return nil }
            let company = LiveJSON.string(json["company_name"])
            let location = LiveJSON.string(json["location"])
            let subtitle = [company, location].filter { !$0.isEmpty }.joined(separator: " • ")
            return SearchResultItem(id: id, title: title, subtitle: subtitle, category: .jobs, kind: "job")
        }
    }

    private static func display(_ json: [String: Any]) -> String {
        let display = LiveJSON.string(json["display_name"])
        if !display.isEmpty && display.lowercased() != "user" { return display }
        let contact = LiveJSON.string(json["contact_name"])
        if !contact.isEmpty && contact.lowercased() != "user" { return contact }
        let username = LiveJSON.string(json["username"])
        if !username.isEmpty { return username }
        return LiveJSON.string(json["phone_number"])
    }
}
