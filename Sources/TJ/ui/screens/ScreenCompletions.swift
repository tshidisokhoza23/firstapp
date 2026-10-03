import SwiftUI
import AVFoundation
import UIKit

final class VoicePlayback: ObservableObject {
    @Published var playing: String?
    private var player: AVPlayer?
    private var endObserver: NSObjectProtocol?

    func toggle(_ remote: String) {
        if playing == remote {
            stop()
            return
        }
        stop()
        playing = remote
        Task {
            guard let url = FeedPost.resolve(remote) ?? URL(string: remote),
                  let file = await AuthenticatedMedia.localFile(for: url) else {
                await MainActor.run { if self.playing == remote { self.stop() } }
                return
            }
            await MainActor.run {
                guard self.playing == remote else { return }
                let item = AVPlayerItem(url: file)
                let player = AVPlayer(playerItem: item)
                self.player = player
                self.endObserver = NotificationCenter.default.addObserver(
                    forName: .AVPlayerItemDidPlayToEndTime,
                    object: item,
                    queue: .main
                ) { [weak self] _ in
                    self?.stop()
                }
                player.play()
            }
        }
    }

    func stop() {
        player?.pause()
        player = nil
        if let endObserver {
            NotificationCenter.default.removeObserver(endObserver)
        }
        endObserver = nil
        playing = nil
    }
}

struct LiveGroupInfoView: View {
    let conversationId: String
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
                        Button { onOpen(person.id) } label: {
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
            .navigationTitle(title.isEmpty ? "Group" : title)
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await load() }
    }

    private func load() async {
        guard let data = try? await API.data("conversations/\(conversationId)/members") else {
            status = "Could not load members"
            return
        }
        people = LiveJSON.list(data, keys: ["users", "members", "participants"]).compactMap { row in
            let id = LiveJSON.string(row["id"])
            guard !id.isEmpty else { return nil }
            let name = displayName(row)
            guard !name.isEmpty else { return nil }
            let handle = LiveJSON.string(row["username"])
            return (id, name, handle.isEmpty ? "" : "@\(handle)")
        }
        status = people.isEmpty ? "No members returned" : ""
    }
}

struct SharedFilesView: View {
    @State private var items: [SharedFileItem] = []
    @State private var status = "Loading..."
    @State private var filter = "All"
    @State private var opened: OpenedMedia?
    private let filters = ["All", "Images", "Videos", "Documents", "Audio"]

    var body: some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(filters, id: \.self) { name in
                        Button(name) { filter = name }
                            .font(.footnote.bold())
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(filter == name ? Color.accentColor : Color(.secondarySystemBackground))
                            .foregroundColor(filter == name ? .white : .primary)
                            .clipShape(Capsule())
                    }
                }
                .padding()
            }
            if visible.isEmpty {
                Spacer()
                Text(status).foregroundColor(.secondary)
                Spacer()
            } else {
                List(visible) { item in
                    Button { opened = OpenedMedia(url: item.url) } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title).foregroundColor(.primary)
                            Text(item.kind).font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("Files")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $opened) { item in
            InAppMediaView(urlString: item.url) { opened = nil }
        }
        .task { await load() }
    }

    private var visible: [SharedFileItem] {
        items.filter { item in
            switch filter {
            case "Images": return item.kind.contains("image")
            case "Videos": return item.kind.contains("video")
            case "Documents": return item.kind.contains("file") || item.kind.contains("document")
            case "Audio": return item.kind.contains("audio") || item.kind.contains("voice")
            default: return true
            }
        }
    }

    private func load() async {
        guard let data = try? await API.data("conversations") else {
            status = "Could not load files"
            return
        }
        var found: [SharedFileItem] = []
        for chat in LiveJSON.list(data, keys: ["chats", "conversations"]).prefix(25) {
            let id = LiveJSON.string(chat["conversation_id"])
            let conversationId = id.isEmpty ? LiveJSON.string(chat["id"]) : id
            let rawType = LiveJSON.string(chat["type"]).lowercased()
            if rawType.contains("mingle") || conversationId.hasPrefix("mingle_") { continue }
            if !ChatExpiry.mediaVisible(conversationId) { continue }
            guard !conversationId.isEmpty,
                  let messages = try? await API.data("conversations/\(conversationId)/messages") else { continue }
            for row in LiveJSON.list(messages, keys: ["messages"]) {
                let type = LiveJSON.string(row["media_type"]).lowercased()
                var media = LiveJSON.string(row["media_url"])
                if media.isEmpty, let urls = row["media_urls"] as? [Any] {
                    media = urls.compactMap { $0 as? String }.first ?? ""
                }
                guard !media.isEmpty, !type.isEmpty, type != "text" else { continue }
                let messageId = LiveJSON.string(row["message_id"])
                let name = LiveJSON.string(row["file_name"])
                found.append(SharedFileItem(
                    id: messageId.isEmpty ? media : messageId,
                    title: name.isEmpty ? type.replacingOccurrences(of: "_", with: " ") : name,
                    url: media,
                    kind: type
                ))
            }
        }
        items = found
        status = found.isEmpty ? "No files yet" : ""
    }
}

struct SharedFileItem: Identifiable {
    let id: String
    let title: String
    let url: String
    let kind: String
}

struct OpenedMedia: Identifiable {
    let url: String
    var id: String { url }
}

struct MonthCalendarView: View {
    @State private var month = Date()
    @State private var events: [LocalCalendarEvent] = LocalCalendarEvent.load()
    @State private var showAdd = false
    @State private var draft = ""

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Button { shift(-1) } label: { Image(systemName: "chevron.left") }
                Spacer()
                Text(month, format: .dateTime.month(.wide).year()).font(.headline)
                Spacer()
                Button { shift(1) } label: { Image(systemName: "chevron.right") }
            }
            .padding(.horizontal)
            let days = daysInMonth()
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 8) {
                ForEach(Array(["S", "M", "T", "W", "T", "F", "S"].enumerated()), id: \.offset) { _, label in
                    Text(label).font(.caption).foregroundColor(.secondary)
                }
                ForEach(0..<leadingBlanks(), id: \.self) { _ in
                    Text(" ").font(.body)
                }
                ForEach(days, id: \.self) { day in
                    Text("\(day)")
                        .frame(maxWidth: .infinity)
                        .padding(6)
                }
            }
            .padding(.horizontal)
            if events.isEmpty {
                Text("No events on this phone.")
                    .foregroundColor(.secondary)
            } else {
                List {
                    ForEach(events) { event in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(event.title)
                            Text(event.day).font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .onDelete { offsets in
                        events.remove(atOffsets: offsets)
                        LocalCalendarEvent.save(events)
                    }
                }
            }
            Spacer()
        }
        .padding(.top)
        .navigationTitle("Calendar")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Add") { draft = ""; showAdd = true }
            }
        }
        .alert("Add event", isPresented: $showAdd) {
            TextField("What and when", text: $draft)
            Button("Save") { addEvent() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This stays on this phone. There is no shared calendar service.")
        }
    }

    private func addEvent() {
        let title = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else { return }
        let day = DateFormatter.localizedString(from: month, dateStyle: .medium, timeStyle: .none)
        events.append(LocalCalendarEvent(id: UUID().uuidString, title: title, day: day))
        LocalCalendarEvent.save(events)
        draft = ""
    }

    private func shift(_ value: Int) {
        month = Calendar.current.date(byAdding: .month, value: value, to: month) ?? month
    }

    private func daysInMonth() -> [Int] {
        let range = Calendar.current.range(of: .day, in: .month, for: month) ?? 1..<1
        return Array(range)
    }

    private func leadingBlanks() -> Int {
        let parts = Calendar.current.dateComponents([.year, .month], from: month)
        let start = Calendar.current.date(from: parts) ?? month
        return Calendar.current.component(.weekday, from: start) - 1
    }
}

struct LocalCalendarEvent: Identifiable, Codable {
    let id: String
    let title: String
    let day: String

    static func load() -> [LocalCalendarEvent] {
        guard let data = UserDefaults.standard.data(forKey: "tj_local_events"),
              let rows = try? JSONDecoder().decode([LocalCalendarEvent].self, from: data) else { return [] }
        return rows
    }

    static func save(_ rows: [LocalCalendarEvent]) {
        if let data = try? JSONEncoder().encode(rows) {
            UserDefaults.standard.set(data, forKey: "tj_local_events")
        }
    }
}

struct SocialPostSearch: View {
    var onOpen: (FeedPost) -> Void
    @State private var query = ""
    @State private var posts: [FeedPost] = []
    @State private var note = ""

    var body: some View {
        NavigationStack {
            VStack {
                TextField("Search posts", text: $query)
                    .textFieldStyle(.roundedBorder)
                    .padding()
                    .submitLabel(.search)
                    .onSubmit { Task { await search() } }
                if posts.isEmpty {
                    Text(note.isEmpty ? "Search posts" : note).foregroundColor(.secondary)
                    Spacer()
                } else {
                    List(posts) { post in
                        Button { onOpen(post) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(post.authorName).font(.headline).foregroundColor(.primary)
                                Text(post.content).lineLimit(3).foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Search")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func search() async {
        let text = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        guard let data = try? await API.data("search", query: ["query": text, "type": "posts"]) else {
            posts = []
            note = "Search failed"
            return
        }
        posts = LiveJSON.posts(from: data).filter { !$0.authorName.isEmpty || !$0.content.isEmpty }
        note = posts.isEmpty ? "No matching posts" : ""
    }
}

private func displayName(_ row: [String: Any]) -> String {
    let display = LiveJSON.string(row["display_name"])
    if !display.isEmpty && display != "User" && display != "Someone" { return display }
    let username = LiveJSON.string(row["username"])
    if !username.isEmpty && username != "User" { return username }
    return LiveJSON.string(row["phone_number"])
}
