import SwiftUI
import PhotosUI

struct ConversationInfoView: View {
    let chat: ChatMessage
    var onOpenUser: (String) -> Void
    var onLeft: () -> Void

    var body: some View {
        if chat.isGroup || chat.type == .group {
            GroupInfoScreen(conversationId: chat.id, fallbackName: chat.consistentName(), onOpenUser: onOpenUser, onLeft: onLeft)
        } else if chat.type == .channel {
            ChannelChatInfoScreen(conversationId: chat.id, onLeft: onLeft)
        } else {
            DirectChatInfoScreen(chat: chat, onOpenUser: onOpenUser, onLeft: onLeft)
        }
    }
}

private struct InfoPerson: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let isAdmin: Bool
    let isCreator: Bool
}

private struct InfoMedia: Identifiable {
    let id: String
    let title: String
    let url: String
    let kind: String
}

private enum InfoJSON {
    static func object(_ data: Data) -> [String: Any] {
        (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }

    static func name(_ row: [String: Any], id: String) -> String {
        let raw = LiveJSON.string(row["display_name"])
        let username = LiveJSON.string(row["username"])
        let phone = LiveJSON.string(row["phone_number"])
        let blocked = ["user", "someone", "direct", "chat", "channel", "group chat", "mingle user", "mingle contact", "unknown caller"]
        if !raw.isEmpty, !blocked.contains(raw.lowercased()), !LiveJSON.isOpaqueId(raw) { return raw }
        if !username.isEmpty, !blocked.contains(username.lowercased()), !LiveJSON.isOpaqueId(username) { return username }
        if !phone.isEmpty { return phone }
        if LiveJSON.isOpaqueId(id) { return "" }
        return ""
    }
}

struct GroupInfoScreen: View {
    let conversationId: String
    let fallbackName: String
    var onOpenUser: (String) -> Void
    var onLeft: () -> Void

    @State private var groupId = ""
    @State private var name = ""
    @State private var details = ""
    @State private var avatarUrl = ""
    @State private var createdBy = ""
    @State private var inviteCode = ""
    @State private var people: [InfoPerson] = []
    @State private var media: [InfoMedia] = []
    @State private var contacts: [(id: String, name: String, phone: String)] = []
    @State private var query = ""
    @State private var note = ""
    @State private var muted = false
    @State private var showRename = false
    @State private var draftName = ""
    @State private var showAdd = false
    @State private var showLeave = false
    @State private var memberAction: InfoPerson?
    @State private var openedMedia: String?
    @State private var avatarItem: PhotosPickerItem?

    private var mine: String { SessionStore.shared.userId ?? "" }
    private var isAdmin: Bool { people.contains { $0.id == mine && ($0.isAdmin || $0.isCreator) } || createdBy == mine }
    private var visiblePeople: [InfoPerson] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return people }
        return people.filter { $0.name.localizedCaseInsensitiveContains(trimmed) || $0.subtitle.localizedCaseInsensitiveContains(trimmed) }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(spacing: 8) {
                        avatar
                        Text(name.isEmpty ? fallbackName : name)
                            .font(.title3.bold())
                        Text("\(people.count) participants")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if isAdmin {
                            Button("Tap the name to rename") { draftName = name; showRename = true }
                                .font(.caption)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                if !details.isEmpty {
                    Section("Group subject") { Text(details) }
                }
                Section {
                    Button(muted ? "Unmute notifications" : "Mute notifications") { toggleMute() }
                    Button("Media, links, and docs") {}
                    Text(media.isEmpty ? "No shared media yet" : "\(media.count)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    ForEach(media.prefix(12)) { item in
                        Button(item.title) { openedMedia = item.url }
                    }
                    if isAdmin {
                        Button("Add members") { Task { await loadContacts() } }
                        Button("Invite via link") { Task { await invite() } }
                    }
                    if !inviteCode.isEmpty {
                        Text("Invite code \(inviteCode)")
                            .font(.caption)
                        ShareLink(item: "Join \(name) on TJ\nfirstapp://group/invite/\(inviteCode)") {
                            Text("Share group")
                        }
                    }
                }
                Section("Search members") {
                    TextField("Search members", text: $query)
                }
                Section {
                    ForEach(visiblePeople) { person in
                        Button {
                            if person.id == mine { return }
                            if isAdmin { memberAction = person } else { onOpenUser(person.id) }
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(person.id == mine ? "You" : person.name)
                                Text(person.isCreator ? "Group creator" : (person.isAdmin ? "Group admin" : person.subtitle))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                Section {
                    Button("Exit group", role: .destructive) { showLeave = true }
                }
            }
            .navigationTitle("Group info")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await load() }
        .sheet(isPresented: $showAdd) { addSheet }
        .sheet(isPresented: $showRename) { renameSheet }
        .confirmationDialog("Member", isPresented: Binding(get: { memberAction != nil }, set: { if !$0 { memberAction = nil } })) {
            if let person = memberAction {
                Button("Message \(person.name)") { onOpenUser(person.id) }
                if person.isAdmin {
                    Button("Dismiss as admin") { Task { await demote(person.id) } }
                } else {
                    Button("Make group admin") { Task { await promote(person.id) } }
                }
                if person.id != createdBy {
                    Button("Remove \(person.name)", role: .destructive) { Task { await remove(person.id) } }
                }
                Button("Cancel", role: .cancel) { memberAction = nil }
            }
        }
        .alert("Exit group?", isPresented: $showLeave) {
            Button("Exit", role: .destructive) { Task { await leave() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(createdBy == mine ? "If you leave and no admin remains, the oldest member becomes admin" : "You will no longer receive messages from \(name).")
        }
        .alert("Group", isPresented: Binding(get: { !note.isEmpty }, set: { if !$0 { note = "" } })) {
            Button("OK", role: .cancel) { note = "" }
        } message: { Text(note) }
        .fullScreenCover(isPresented: Binding(get: { openedMedia != nil }, set: { if !$0 { openedMedia = nil } })) {
            if let openedMedia {
                InAppMediaView(urlString: openedMedia, onClose: { self.openedMedia = nil })
            }
        }
        .onChange(of: avatarItem) { item in
            guard let item else { return }
            Task { await uploadAvatar(item) }
        }
    }

    @ViewBuilder private var avatar: some View {
        let image = Group {
            if let url = FeedPost.resolve(avatarUrl) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image { image.resizable().scaledToFill() } else { placeholder }
                }
            } else {
                placeholder
            }
        }
        .frame(width: 84, height: 84)
        .clipShape(Circle())
        if isAdmin {
            PhotosPicker(selection: $avatarItem, matching: .images) { image }
        } else {
            image
        }
    }

    private var placeholder: some View {
        Circle().fill(Color.gray.opacity(0.3)).overlay(Image(systemName: "person.3.fill"))
    }

    private var addSheet: some View {
        NavigationStack {
            List(contacts.filter { contact in !people.contains { $0.id == contact.id } }, id: \.id) { contact in
                Button {
                    Task { await add(contact.id) }
                } label: {
                    VStack(alignment: .leading) {
                        Text(contact.name)
                        if !contact.phone.isEmpty { Text(contact.phone).font(.caption).foregroundColor(.secondary) }
                    }
                }
            }
            .overlay {
                if contacts.filter({ contact in !people.contains { $0.id == contact.id } }).isEmpty {
                    Text("No more contacts to add. They must be registered on TJ.")
                        .foregroundColor(.secondary)
                        .padding()
                }
            }
            .navigationTitle("Add members")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showAdd = false } } }
        }
    }

    private var renameSheet: some View {
        NavigationStack {
            Form {
                TextField("Group name", text: $draftName)
                Button("Save") { Task { await rename() } }
                    .disabled(draftName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("Group name")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showRename = false } } }
        }
    }

    private func load() async {
        let infoData = try? await API.data("chats/groups/\(conversationId)")
        let info = infoData.map(InfoJSON.object) ?? [:]
        groupId = LiveJSON.string(info["id"])
        if groupId.isEmpty { groupId = conversationId }
        let loadedName = LiveJSON.string(info["name"])
        name = loadedName.isEmpty ? fallbackName : loadedName
        details = LiveJSON.string(info["description"])
        avatarUrl = LiveJSON.string(info["avatar_url"])
        createdBy = LiveJSON.string(info["created_by"])
        inviteCode = LiveJSON.string(info["invite_code"])
        let adminIds = Set((info["admin_ids"] as? [Any])?.compactMap { $0 as? String } ?? [])
        let memberData = try? await API.data("conversations/\(conversationId)/members")
        let rows = memberData.map { LiveJSON.list($0, keys: ["users", "members", "participants"]) } ?? []
        people = rows.compactMap { row in
            let id = LiveJSON.string(row["id"])
            let personName = InfoJSON.name(row, id: id)
            guard !id.isEmpty, !personName.isEmpty else { return nil }
            let handle = LiveJSON.string(row["username"])
            return InfoPerson(id: id, name: personName, subtitle: handle.isEmpty ? "" : "@\(handle)", isAdmin: adminIds.contains(id), isCreator: id == createdBy)
        }
        media = await loadMedia(conversationId)
    }

    private func loadMedia(_ id: String) async -> [InfoMedia] {
        guard let data = try? await API.data("conversations/\(id)/messages") else { return [] }
        return LiveJSON.list(data, keys: ["messages"]).compactMap { row in
            let type = LiveJSON.string(row["media_type"]).lowercased()
            var url = LiveJSON.string(row["media_url"])
            if url.isEmpty, let urls = row["media_urls"] as? [Any] {
                url = urls.compactMap { $0 as? String }.first ?? ""
            }
            guard !url.isEmpty, !type.isEmpty, type != "text" else { return nil }
            let id = LiveJSON.string(row["message_id"])
            let file = LiveJSON.string(row["file_name"])
            return InfoMedia(id: id.isEmpty ? url : id, title: file.isEmpty ? type.replacingOccurrences(of: "_", with: " ") : file, url: url, kind: type)
        }
    }

    private func target() -> String { groupId.isEmpty ? conversationId : groupId }

    private func toggleMute() {
        muted.toggle()
        Task {
            do {
                _ = try await API.data("conversations/\(conversationId)/prefs", method: "PATCH", json: ["is_muted": muted])
            } catch {
                muted.toggle()
                note = error.localizedDescription
            }
        }
    }

    private func rename() async {
        let next = draftName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !next.isEmpty else { return }
        do {
            _ = try await API.data("chats/groups/\(target())", method: "PATCH", json: ["name": next])
            name = next
            showRename = false
        } catch {
            note = error.localizedDescription
        }
    }

    private func loadContacts() async {
        let phones = ContactSyncManager.shared.fetchAllContacts().map(\.phoneNumber)
        guard let data = try? await API.data("users/sync-contacts", method: "POST", json: ["phone_numbers": phones]) else {
            note = "Could not load contacts"
            return
        }
        contacts = LiveJSON.list(data, keys: ["users", "contacts"]).compactMap { row in
            let id = LiveJSON.string(row["id"])
            let personName = InfoJSON.name(row, id: id)
            guard !id.isEmpty, !personName.isEmpty else { return nil }
            return (id, personName, LiveJSON.string(row["phone_number"]))
        }
        showAdd = true
    }

    private func add(_ userId: String) async {
        do {
            _ = try await API.data("chats/groups/\(target())/members", method: "POST", json: ["user_id": userId])
            showAdd = false
            await load()
        } catch {
            note = error.localizedDescription
        }
    }

    private func remove(_ userId: String) async {
        do {
            _ = try await API.data("chats/groups/\(target())/members", method: "DELETE", json: ["user_id": userId])
            await load()
        } catch {
            note = error.localizedDescription
        }
    }

    private func promote(_ userId: String) async {
        do {
            _ = try await API.data("chats/groups/\(target())/admins", method: "POST", json: ["user_id": userId])
            await load()
        } catch {
            note = error.localizedDescription
        }
    }

    private func demote(_ userId: String) async {
        do {
            _ = try await API.data("chats/groups/\(target())/admins", method: "DELETE", json: ["user_id": userId])
            await load()
        } catch {
            note = error.localizedDescription
        }
    }

    private func invite() async {
        do {
            let data = try await API.data("chats/groups/\(target())/invite-code", method: "POST")
            inviteCode = LiveJSON.string(InfoJSON.object(data)["invite_code"])
            if inviteCode.isEmpty { note = "Only admins can create invite links" }
            else {
                UIPasteboard.general.string = inviteCode
                note = "Invite link copied"
            }
        } catch {
            note = error.localizedDescription
        }
    }

    private func leave() async {
        do {
            _ = try await API.data("chats/groups/\(target())/leave", method: "POST")
            onLeft()
        } catch {
            note = error.localizedDescription
        }
    }

    private func uploadAvatar(_ item: PhotosPickerItem) async {
        defer { avatarItem = nil }
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        do {
            let url = try await MediaActions.upload(data: data, filename: "group.jpg", mime: "image/jpeg", folder: "avatars")
            _ = try await API.data("chats/groups/\(target())/avatar", method: "PATCH", json: ["avatar_url": url])
            avatarUrl = url
        } catch {
            note = error.localizedDescription
        }
    }
}

struct DirectChatInfoScreen: View {
    let chat: ChatMessage
    var onOpenUser: (String) -> Void
    var onLeft: () -> Void
    @State private var muted: Bool
    @State private var pinned: Bool
    @State private var phone = ""
    @State private var username = ""
    @State private var media: [InfoMedia] = []
    @State private var openedMedia: String?
    @State private var note = ""
    @State private var showBlock = false
    @State private var showReport = false
    @State private var showClear = false
    @State private var showDelete = false
    @State private var reportReason = "Spam"

    init(chat: ChatMessage, onOpenUser: @escaping (String) -> Void, onLeft: @escaping () -> Void) {
        self.chat = chat
        self.onOpenUser = onOpenUser
        self.onLeft = onLeft
        _muted = State(initialValue: chat.isMuted)
        _pinned = State(initialValue: chat.isPinned)
        _phone = State(initialValue: chat.phoneNumber ?? "")
    }

    private var userId: String {
        let other = chat.otherUserId ?? ""
        return other.isEmpty ? "" : other
    }

    private var title: String {
        let name = chat.consistentName()
        if chat.isSecretMingleChat { return "Secret Chat info" }
        return name.isEmpty ? "Contact info" : name
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(spacing: 8) {
                        Text(title).font(.title3.bold())
                        if chat.isSecretMingleChat {
                            Text("Secret Mingle Chat. Profile and Mingle bio are kept private.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        } else {
                            if !phone.isEmpty { Text(phone).font(.subheadline) }
                            if !username.isEmpty { Text("@\(username)").font(.caption).foregroundColor(.secondary) }
                            if !userId.isEmpty {
                                Button("View profile") { onOpenUser(userId) }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                if !chat.isSecretMingleChat && !userId.isEmpty {
                    Section {
                        Button("Audio") { ChatViewModel().startVoiceOrVideoCall(receiverId: userId, callType: .voice, name: chat.consistentName()) }
                        Button("Video") { ChatViewModel().startVoiceOrVideoCall(receiverId: userId, callType: .video, name: chat.consistentName()) }
                    }
                }
                Section {
                    Toggle("Mute notifications", isOn: $muted)
                        .onChange(of: muted) { value in savePref("is_muted", value) }
                    Toggle("Pin chat", isOn: $pinned)
                        .onChange(of: pinned) { value in savePref("is_pinned", value) }
                }
                Section("Media, links, and docs") {
                    if media.isEmpty {
                        Text("No shared media yet").foregroundColor(.secondary)
                    }
                    ForEach(media) { item in
                        Button(item.title) { openedMedia = item.url }
                    }
                }
                Section {
                    Text("Messages and calls are end-to-end encrypted.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Section {
                    if !userId.isEmpty && !chat.isSecretMingleChat {
                        Button("Block", role: .destructive) { showBlock = true }
                        Button("Report", role: .destructive) { showReport = true }
                    }
                    Button("Clear Chat", role: .destructive) { showClear = true }
                    Button(chat.isGroup ? "Delete group" : "Delete Chat", role: .destructive) { showDelete = true }
                }
            }
            .navigationTitle(chat.isSecretMingleChat ? "Secret Chat info" : "Contact info")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await load() }
        .alert("Block \(title)?", isPresented: $showBlock) {
            Button("Block", role: .destructive) { Task { await block() } }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Report \(title)", isPresented: $showReport) {
            Button("Spam") { reportReason = "Spam"; Task { await report() } }
            Button("Harassment") { reportReason = "Harassment"; Task { await report() } }
            Button("Scam or fraud") { reportReason = "Scam or fraud"; Task { await report() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This report is sent to TJ. The contact is not notified.")
        }
        .alert("Clear Chat", isPresented: $showClear) {
            Button("Clear", role: .destructive) { Task { await clear() } }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Delete Chat", isPresented: $showDelete) {
            Button("Delete", role: .destructive) { Task { await deleteChat() } }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Chat", isPresented: Binding(get: { !note.isEmpty }, set: { if !$0 { note = "" } })) {
            Button("OK", role: .cancel) { note = "" }
        } message: { Text(note) }
        .fullScreenCover(isPresented: Binding(get: { openedMedia != nil }, set: { if !$0 { openedMedia = nil } })) {
            if let openedMedia {
                InAppMediaView(urlString: openedMedia, onClose: { self.openedMedia = nil })
            }
        }
    }

    private func load() async {
        if !userId.isEmpty, let data = try? await API.data("users/\(userId)") {
            let root = InfoJSON.object(data)
            let user = (root["user"] as? [String: Any]) ?? root
            let loadedPhone = LiveJSON.string(user["phone_number"])
            if !loadedPhone.isEmpty { phone = loadedPhone }
            username = LiveJSON.string(user["username"])
        }
        guard let data = try? await API.data("conversations/\(chat.id)/messages") else { return }
        media = LiveJSON.list(data, keys: ["messages"]).compactMap { row in
            let type = LiveJSON.string(row["media_type"]).lowercased()
            var url = LiveJSON.string(row["media_url"])
            if url.isEmpty, let urls = row["media_urls"] as? [Any] {
                url = urls.compactMap { $0 as? String }.first ?? ""
            }
            guard !url.isEmpty, !type.isEmpty, type != "text" else { return nil }
            let id = LiveJSON.string(row["message_id"])
            let file = LiveJSON.string(row["file_name"])
            return InfoMedia(id: id.isEmpty ? url : id, title: file.isEmpty ? type.replacingOccurrences(of: "_", with: " ") : file, url: url, kind: type)
        }
    }

    private func savePref(_ key: String, _ value: Bool) {
        Task {
            do { _ = try await API.data("conversations/\(chat.id)/prefs", method: "PATCH", json: [key: value]) }
            catch { note = error.localizedDescription }
        }
    }

    private func block() async {
        guard !userId.isEmpty else { return }
        do {
            _ = try await API.data("users/\(userId)/block", method: "POST")
            note = "Blocked"
        } catch { note = error.localizedDescription }
    }

    private func report() async {
        guard !userId.isEmpty else { return }
        do {
            _ = try await API.data("users/\(userId)/report", method: "POST", json: ["reason": reportReason])
            note = "Report sent"
        } catch { note = error.localizedDescription }
    }

    private func clear() async {
        do { _ = try await API.data("conversations/\(chat.id)/clear", method: "POST") }
        catch { note = error.localizedDescription }
    }

    private func deleteChat() async {
        do {
            _ = try await API.data("conversations/\(chat.id)", method: "DELETE")
            onLeft()
        } catch { note = error.localizedDescription }
    }
}

struct ChannelChatInfoScreen: View {
    let conversationId: String
    var onLeft: () -> Void
    @State private var name = ""
    @State private var details = ""
    @State private var followers = 0
    @State private var muted = false
    @State private var subscribed = false
    @State private var note = ""
    @State private var descriptionDraft = ""
    @State private var showDescription = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(name).font(.title3.bold())
                    Text("\(followers) followers").font(.caption).foregroundColor(.secondary)
                    if !details.isEmpty { Text(details) }
                }
                Section {
                    Button(muted ? "Unmute notifications" : "Mute notifications") { Task { await mute() } }
                    Button(subscribed ? "Unfollow channel" : "Follow channel") { Task { await follow() } }
                    Button("Add description") { showDescription = true }
                    Button("Share to my status") { Task { await shareStatus() } }
                }
            }
            .navigationTitle("Channel info")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await load() }
        .sheet(isPresented: $showDescription) {
            NavigationStack {
                Form {
                    TextField("Description", text: $descriptionDraft, axis: .vertical)
                    Button("Post to channel") { Task { await postDescription() } }
                        .disabled(descriptionDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .navigationTitle("Add description")
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showDescription = false } } }
            }
        }
        .alert("Channel", isPresented: Binding(get: { !note.isEmpty }, set: { if !$0 { note = "" } })) {
            Button("OK", role: .cancel) { note = "" }
        } message: { Text(note) }
    }

    private func load() async {
        guard let data = try? await API.data("chats/channels/\(conversationId)") else {
            note = "Channel was not found"
            return
        }
        let row = InfoJSON.object(data)
        name = LiveJSON.string(row["name"])
        details = LiveJSON.string(row["description"])
        followers = LiveJSON.int(row["followers"])
        muted = LiveJSON.bool(row["is_muted"])
        subscribed = LiveJSON.bool(row["is_subscribed"])
    }

    private func mute() async {
        do {
            _ = try await API.data("chats/channels/\(conversationId)/mute", method: "POST", json: ["is_muted": !muted])
            muted.toggle()
        } catch { note = error.localizedDescription }
    }

    private func follow() async {
        do {
            _ = try await API.data("chats/channels/\(conversationId)/subscribe", method: "POST")
            subscribed.toggle()
        } catch { note = error.localizedDescription }
    }

    private func shareStatus() async {
        let text = "Check out \(name) channel on FirstApp!"
        do {
            _ = try await StoryRepository.shared.createStory(content: text)
            note = "Posted to your status"
        } catch { note = error.localizedDescription }
    }

    private func postDescription() async {
        let text = descriptionDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        do {
            _ = try await API.data("conversations/\(conversationId)/messages", method: "POST", json: [
                "client_uuid": UUID().uuidString,
                "message_text": text,
                "media_type": "TEXT"
            ])
            showDescription = false
            note = "Description posted to the channel"
        } catch { note = error.localizedDescription }
    }
}

struct ChannelInfoSheet: View {
    let channel: Channel
    var onPostDescription: (String) -> Void
    var onShareStatus: () -> Void
    var onInviteAdmins: () -> Void
    var onClose: () -> Void
    @State private var draft = ""

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(channel.name).font(.title3.bold())
                    Text("\(channel.followers) followers").font(.caption).foregroundColor(.secondary)
                    if !channel.description.isEmpty { Text(channel.description) }
                }
                Section {
                    TextField("Add description", text: $draft, axis: .vertical)
                    Button("Post description to channel") {
                        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !text.isEmpty else { return }
                        onPostDescription(text)
                    }
                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Button("Share to my status", action: onShareStatus)
                    if channel.isAdmin { Button("Invite admins", action: onInviteAdmins) }
                }
            }
            .navigationTitle("Channel info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close", action: onClose) } }
        }
        .onAppear { draft = channel.description }
    }
}
