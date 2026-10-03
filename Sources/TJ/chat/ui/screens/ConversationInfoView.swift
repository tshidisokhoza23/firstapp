import SwiftUI
import UIKit
import PhotosUI

struct ConversationInfoView: View {
    let chat: ChatMessage
    var onOpenUser: (String) -> Void
    var onLeft: () -> Void

    var body: some View {
        if chat.isGroup || chat.type == .group {
            GroupInfoScreen(conversationId: chat.id, fallbackName: chat.consistentName(), fallbackAvatar: chat.avatarUrl ?? "", initialMuted: chat.isMuted, onOpenUser: onOpenUser, onLeft: onLeft)
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
    var fallbackAvatar: String = ""
    var initialMuted: Bool = false
    var onOpenUser: (String) -> Void
    var onLeft: () -> Void
    @Environment(\.dismiss) private var dismiss

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
    @State private var memberCount = 0
    @State private var appliedMute = false
    @State private var showRename = false
    @State private var draftName = ""
    @State private var showAdd = false
    @State private var showLeave = false
    @State private var memberAction: InfoPerson?
    @State private var openedMedia: String?
    @State private var avatarItem: PhotosPickerItem?
    @State private var showReport = false
    @State private var showDescription = false
    @State private var descriptionDraft = ""

    private var mine: String { SessionStore.shared.userId ?? "" }
    private var isAdmin: Bool { people.contains { $0.id == mine && ($0.isAdmin || $0.isCreator) } || createdBy == mine }
    private var visiblePeople: [InfoPerson] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return people }
        return people.filter { $0.name.localizedCaseInsensitiveContains(trimmed) || $0.subtitle.localizedCaseInsensitiveContains(trimmed) }
    }

    private var displayName: String { name.isEmpty ? fallbackName : name }
    private var participantCount: Int { max(memberCount, people.count, 1) }
    private var shownAvatar: String { avatarUrl.isEmpty ? fallbackAvatar : avatarUrl }
    private var inviteLink: String { "firstapp://group/invite/\(inviteCode)" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: 8) {
                        ZStack(alignment: .bottomTrailing) {
                            avatar
                            if isAdmin {
                                PhotosPicker(selection: $avatarItem, matching: .images) {
                                    Image(systemName: "camera.fill")
                                        .font(.system(size: 14))
                                        .foregroundColor(.white)
                                        .frame(width: 32, height: 32)
                                        .background(Color.appAccent)
                                        .clipShape(Circle())
                                }
                            }
                        }
                        Button {
                            guard isAdmin else { return }
                            draftName = displayName
                            showRename = true
                        } label: {
                            Text(displayName)
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(.primary)
                        }
                        .disabled(!isAdmin)
                        if isAdmin {
                            Button("Add description") { descriptionDraft = details; showDescription = true }
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(hex: "00A884"))
                            Text("Tap the name to rename")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        Text("\(participantCount) participants")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        if !details.isEmpty {
                            Text(details)
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.top, 4)
                        }
                        Button { dismiss() } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "bubble.left.fill")
                                Text("Open group chat").fontWeight(.bold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.appAccent)
                            .cornerRadius(12)
                        }
                        .padding(.top, 8)
                    }
                    .padding(24)
                    thickDivider
                    HStack(spacing: 16) {
                        Image(systemName: "bell.slash").foregroundColor(Color.appAccent).frame(width: 24)
                        Text("Mute notifications").foregroundColor(.primary)
                        Spacer()
                        Toggle("", isOn: Binding(get: { muted }, set: { setMuted($0) }))
                            .labelsHidden()
                            .tint(Color.appAccent)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    if isAdmin {
                        Button { Task { await loadContacts() } } label: {
                            infoLine(icon: "person.badge.plus", title: "Add members", subtitle: "Only admins can add people")
                        }
                        Button {
                            if inviteCode.isEmpty { Task { await invite() } } else { copyInvite() }
                        } label: {
                            infoLine(icon: "link", title: "Invite via link", subtitle: inviteCode.isEmpty ? "Create a link anyone can use to join" : inviteLink)
                        }
                    }
                    thickDivider
                    HStack {
                        Text("Media, links, and docs").foregroundColor(.primary)
                        Spacer()
                        Text("\(media.count)").foregroundColor(.secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    if !media.isEmpty {
                        HStack(spacing: 6) {
                            ForEach(Array(media.prefix(4))) { item in
                                Button { openedMedia = item.url } label: { mediaThumb(item.url) }
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 8)
                    }
                    thickDivider
                    HStack {
                        Text("\(max(memberCount, people.count)) participants").fontWeight(.bold)
                        Spacer()
                        Image(systemName: "magnifyingglass").foregroundColor(Color.appAccent)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    TextField("Search members", text: $query)
                        .padding(10)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.35)))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    ForEach(visiblePeople) { person in
                        Button {
                            if person.id != mine { memberAction = person }
                        } label: {
                            HStack(spacing: 12) {
                                personBubble(person.name, mine: person.id == mine)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(person.id == mine ? "You" : person.name).foregroundColor(.primary)
                                    Text(person.isCreator ? "Group creator" : (person.isAdmin ? "Group admin" : (person.subtitle.isEmpty ? "Member" : person.subtitle)))
                                        .font(.system(size: 12))
                                        .foregroundColor(person.isAdmin || person.isCreator ? Color.appAccent : .secondary)
                                }
                                Spacer()
                                if person.isAdmin || person.isCreator {
                                    Image(systemName: "shield.fill").font(.system(size: 14)).foregroundColor(Color.appAccent)
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                        }
                    }
                    thickDivider
                    Button { showLeave = true } label: {
                        HStack(alignment: .top, spacing: 16) {
                            Image(systemName: "rectangle.portrait.and.arrow.right").foregroundColor(.red)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Exit group").foregroundColor(.red).fontWeight(.bold)
                                Text(createdBy == mine ? "If you leave and no admin remains, the oldest member becomes admin" : "You will stop receiving messages")
                                    .font(.system(size: 12))
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }
                    Button { showReport = true } label: {
                        HStack(spacing: 16) {
                            Image(systemName: "trash").foregroundColor(.red)
                            Text("Report group").foregroundColor(.red)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 28)
                    }
                    .confirmationDialog("Report group", isPresented: $showReport, titleVisibility: .visible) {
                        Button("Spam") { Task { await reportGroup("Spam") } }
                        Button("Harassment") { Task { await reportGroup("Harassment") } }
                        Button("Inappropriate content") { Task { await reportGroup("Inappropriate content") } }
                        Button("Cancel", role: .cancel) {}
                    }
                }
            }
            .navigationTitle("Group info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 16) {
                        ShareLink(item: inviteCode.isEmpty ? "Join \(displayName) on TJ" : inviteLink) {
                            Image(systemName: "square.and.arrow.up")
                        }
                        Menu {
                            Button("Open chat") { dismiss() }
                            if isAdmin {
                                Button("Add members") { Task { await loadContacts() } }
                                Button("Invite via link") { Task { await invite() } }
                            }
                        } label: {
                            Image(systemName: "ellipsis")
                        }
                    }
        .sheet(isPresented: $showDescription) { descriptionSheet }
                }
            }
        }
        .task { await load() }
        .sheet(isPresented: $showAdd) { addSheet }
        .sheet(isPresented: $showRename) { renameSheet }
        .confirmationDialog("Member", isPresented: Binding(get: { memberAction != nil }, set: { if !$0 { memberAction = nil } })) {
            if let person = memberAction {
                Button("Message \(person.name)") { onOpenUser(person.id); memberAction = nil }
                if isAdmin {
                    if person.isAdmin {
                        Button("Dismiss as admin") { Task { await demote(person.id) } }
                    } else {
                        Button("Make group admin") { Task { await promote(person.id) } }
                    }
                    if person.id != createdBy {
                        Button("Remove \(person.name)", role: .destructive) { Task { await remove(person.id) } }
                    }
                }
                Button("Cancel", role: .cancel) { memberAction = nil }
            }
        }
        .alert("Exit group?", isPresented: $showLeave) {
            Button("Exit", role: .destructive) { Task { await leave() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You will no longer receive messages from \(displayName).")
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

    private var avatar: some View {
        Group {
            if let url = FeedPost.resolve(shownAvatar) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image { image.resizable().scaledToFill() } else { placeholder }
                }
            } else {
                placeholder
            }
        }
        .frame(width: 96, height: 96)
        .clipShape(Circle())
    }

    private var thickDivider: some View {
        Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
    }

    private func infoLine(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon).foregroundColor(Color.appAccent).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundColor(.primary)
                Text(subtitle).font(.system(size: 12)).foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func mediaThumb(_ raw: String) -> some View {
        Group {
            if let url = FeedPost.resolve(raw) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image { image.resizable().scaledToFill() } else { Color.gray.opacity(0.15) }
                }
            } else {
                Color.gray.opacity(0.15)
            }
        }
        .frame(width: 72, height: 72)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func personBubble(_ label: String, mine: Bool) -> some View {
        Circle()
            .fill(mine ? Color.appAccent : Color.gray)
            .frame(width: 40, height: 40)
            .overlay(Text(String(label.prefix(1)).uppercased()).foregroundColor(.white).font(.system(size: 16, weight: .bold)))
    }

    private func copyInvite() {
        UIPasteboard.general.string = inviteLink
        note = "Invite link copied"
    }

    private func setMuted(_ value: Bool) {
        let previous = muted
        muted = value
        Task {
            do {
                _ = try await API.data("conversations/\(conversationId)/prefs", method: "PATCH", json: ["is_muted": value])
            } catch {
                muted = previous
                note = error.localizedDescription
            }
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

    private var descriptionSheet: some View {
        NavigationStack {
            Form {
                TextField("Description", text: $descriptionDraft, axis: .vertical)
                Button("Save") { Task { await saveDescription() } }
                    .disabled(descriptionDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("Group description")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showDescription = false } } }
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
        let title = LiveJSON.string(info["title"])
        let loadedName = LiveJSON.string(info["name"])
        name = !title.isEmpty ? title : (loadedName.isEmpty ? fallbackName : loadedName)
        let ids = (info["member_ids"] as? [Any])?.compactMap { $0 as? String } ?? []
        memberCount = ids.count
        if !appliedMute {
            muted = initialMuted
            appliedMute = true
        }
        if avatarUrl.isEmpty { avatarUrl = fallbackAvatar }
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
        guard ChatExpiry.mediaVisible(id) else { return [] }
        guard let data = try? await API.data("conversations/\(id)/messages") else { return [] }
        return LiveJSON.list(data, keys: ["messages"]).compactMap { row in
            let type = LiveJSON.string(row["media_type"]).lowercased()
            var url = LiveJSON.string(row["media_url"])
            if url.isEmpty, let urls = row["media_urls"] as? [Any] {
                url = urls.compactMap { $0 as? String }.first ?? ""
            }
            guard !url.isEmpty, !type.isEmpty, type != "text" else { return nil }
            let messageId = LiveJSON.string(row["message_id"])
            let file = LiveJSON.string(row["file_name"])
            return InfoMedia(id: messageId.isEmpty ? url : messageId, title: file.isEmpty ? type.replacingOccurrences(of: "_", with: " ") : file, url: url, kind: type)
        }
    }

    private func reportGroup(_ reason: String) async {
        guard !createdBy.isEmpty else {
            note = "The group owner was not returned"
            return
        }
        do {
            _ = try await API.data("users/\(createdBy)/report", method: "POST", json: ["reason": reason])
            note = "Report sent"
        } catch {
            note = error.localizedDescription
        }
    }

    private func saveDescription() async {
        let text = descriptionDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        do {
            _ = try await API.data("chats/groups/\(target())/description", method: "PATCH", json: ["description": text])
            details = text
            showDescription = false
            note = "Description saved"
        } catch {
            do {
                _ = try await API.data("conversations/\(conversationId)/messages", method: "POST", json: [
                    "client_uuid": UUID().uuidString,
                    "message_text": text,
                    "media_type": "TEXT"
                ])
                showDescription = false
                note = "Description posted to the group. The server did not save it on the group yet."
            } catch {
                note = error.localizedDescription
            }
        }
    }

    private func target() -> String { groupId.isEmpty ? conversationId : groupId }

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
            else { copyInvite() }
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

    @Environment(\.dismiss) private var dismiss
    @State private var mediaVisible: Bool
    @State private var disappearSeconds: Int
    @State private var blocked = false
    @State private var showDisappear = false
    @State private var showEncryption = false

    init(chat: ChatMessage, onOpenUser: @escaping (String) -> Void, onLeft: @escaping () -> Void) {
        self.chat = chat
        self.onOpenUser = onOpenUser
        self.onLeft = onLeft
        _muted = State(initialValue: chat.isMuted)
        _pinned = State(initialValue: chat.isPinned)
        _phone = State(initialValue: chat.phoneNumber ?? "")
        let visibleKey = "tj_media_visible_\(chat.id)"
        let disappearKey = "tj_disappear_\(chat.id)"
        let visible = UserDefaults.standard.object(forKey: visibleKey) as? Bool ?? chat.mediaVisibility
        let seconds = UserDefaults.standard.object(forKey: disappearKey) as? Int ?? chat.disappearingMessagesDuration
        _mediaVisible = State(initialValue: visible)
        _disappearSeconds = State(initialValue: seconds)
    }

    private var userId: String {
        let other = chat.otherUserId ?? ""
        return other.isEmpty ? "" : other
    }

    private var title: String {
        let name = chat.consistentName()
        return name.isEmpty ? (chat.isSecretMingleChat ? "Secret Chat info" : "Contact info") : name
    }

    private var sectionTitle: String {
        if chat.isSecretMingleChat { return "Secret Chat info" }
        return "Contact info"
    }

    private var presence: String {
        if blocked { return "Blocked" }
        if chat.isOnline { return "online" }
        if chat.lastSeen > 0 { return Self.lastSeen(chat.lastSeen) }
        if !chat.isSecretMingleChat { return "offline" }
        return ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: 8) {
                        contactAvatar
                        Text(title)
                            .font(.system(size: 22, weight: .bold))
                        if !phone.isEmpty, phone != title {
                            Text(phone).font(.system(size: 15)).foregroundColor(.secondary)
                        }
                        if !presence.isEmpty {
                            HStack(spacing: 6) {
                                if chat.isOnline && !blocked {
                                    Circle().fill(Color.appAccent).frame(width: 8, height: 8)
                                }
                                Text(presence)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(chat.isOnline && !blocked ? Color.appAccent : .secondary)
                            }
                        }
                        HStack(spacing: 18) {
                            actionButton("phone.fill", "Audio") { placeCall(.voice) }
                            actionButton("video.fill", "Video") { placeCall(.video) }
                            actionButton(muted ? "speaker.slash.fill" : "speaker.wave.2.fill", muted ? "Unmute" : "Mute") {
                                muted.toggle()
                                savePref("is_muted", muted)
                            }
                            actionButton("magnifyingglass", "Search") { dismiss() }
                        }
                        .padding(.top, 8)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(
                        LinearGradient(colors: [Color.appAccent.opacity(0.22), Color(uiColor: .systemBackground)], startPoint: .top, endPoint: .bottom)
                    )

                    VStack(alignment: .leading, spacing: 8) {
                        Text(sectionTitle).font(.system(size: 14, weight: .bold)).foregroundColor(Color.appAccent)
                        if chat.isSecretMingleChat {
                            Text("Secret Mingle Chat - Profile and Mingle bio are kept private.")
                                .font(.system(size: 14))
                                .foregroundColor(.secondary)
                        } else {
                            if !phone.isEmpty {
                                labeledRow(icon: "phone.fill", title: phone, subtitle: "Mobile")
                            }
                            if !username.isEmpty, username.caseInsensitiveCompare(title) != .orderedSame {
                                Button { if !userId.isEmpty { onOpenUser(userId) } } label: {
                                    labeledRow(icon: "at", title: username, subtitle: "Username")
                                }
                            }
                            if phone.isEmpty && username.isEmpty {
                                Text(LiveJSON.isOpaqueId(chat.id) ? "No public contact details" : chat.id)
                                    .font(.system(size: 14))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)

                    Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
                    HStack {
                        Text("Media, links, and docs")
                        Spacer()
                        Text("\(media.count) >").foregroundColor(.secondary)
                    }
                    .padding(16)
                    if media.isEmpty {
                        Text("No shared media yet").font(.system(size: 13)).foregroundColor(.secondary).frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16).padding(.bottom, 12)
                    } else {
                        HStack(spacing: 4) {
                            ForEach(Array(media.prefix(4))) { item in
                                Button { openedMedia = item.url } label: {
                                    mediaTile(item.url, video: item.kind.contains("video"))
                                }
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                    }
                    Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
                    settingRow(icon: "bell.slash", title: "Mute notifications") {
                        Toggle("", isOn: Binding(get: { muted }, set: { muted = $0; savePref("is_muted", $0) })).labelsHidden().tint(Color.appAccent)
                    }
                    Button { pinned.toggle(); savePref("is_pinned", pinned) } label: {
                        settingLabel(icon: "pin.fill", title: pinned ? "Unpin chat" : "Pin chat", subtitle: "")
                    }
                    settingRow(icon: "photo", title: "Media visibility", subtitle: mediaVisible ? "Shown in gallery" : "Hidden from gallery") {
                        Toggle("", isOn: Binding(get: { mediaVisible }, set: { setMediaVisible($0) })).labelsHidden().tint(Color.appAccent)
                    }
                    Button { showDisappear = true } label: {
                        settingLabel(icon: "timer", title: "Disappearing messages", subtitle: disappearLabel)
                    }
                    Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
                    Button { showEncryption = true } label: {
                        settingLabel(icon: "lock.fill", title: "Encryption", subtitle: "Messages and calls are end-to-end encrypted. Tap for more info.")
                    }
                    Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
                    dangerRow(icon: "trash", title: "Clear Chat") { showClear = true }
                    dangerRow(icon: "trash.fill", title: "Delete Chat") { showDelete = true }
                    if !userId.isEmpty && !chat.isSecretMingleChat {
                        dangerRow(icon: "nosign", title: blocked ? "Unblock" : "Block") {
                            if blocked { Task { await unblock() } } else { showBlock = true }
                        }
                        dangerRow(icon: "exclamationmark.bubble", title: "Report Contact") { showReport = true }
                    }
                    Color.clear.frame(height: 40)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(pinned ? "Unpin chat" : "Pin chat") { pinned.toggle(); savePref("is_pinned", pinned) }
                        if !userId.isEmpty {
                            Button(blocked ? "Unblock" : "Block") {
                                if blocked { Task { await unblock() } } else { showBlock = true }
                            }
                            Button("Report") { showReport = true }
                            ShareLink(item: phone.isEmpty ? title : "\(title)\n\(phone)") { Text("Share contact") }
                        }
                    } label: {
                        Image(systemName: "ellipsis")
                    }
                }
            }
        }
        .task { await load() }
        .alert("Block \(title)?", isPresented: $showBlock) {
            Button("Block", role: .destructive) { Task { await block() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("They won't be able to message or call you. Existing messages stay on your device until you delete the chat.")
        }
        .confirmationDialog("Report \(title)", isPresented: $showReport, titleVisibility: .visible) {
            Button("Spam") { reportReason = "Spam"; Task { await report() } }
            Button("Harassment") { reportReason = "Harassment"; Task { await report() } }
            Button("Hate speech") { reportReason = "Hate speech"; Task { await report() } }
            Button("Scam or fraud") { reportReason = "Scam or fraud"; Task { await report() } }
            Button("Inappropriate content") { reportReason = "Inappropriate content"; Task { await report() } }
            Button("Other") { reportReason = "Other"; Task { await report() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This report is sent to TJ. The contact is not notified.")
        }
        .confirmationDialog("Disappearing messages", isPresented: $showDisappear, titleVisibility: .visible) {
            Button("Off") { setDisappear(0) }
            Button("24 hours") { setDisappear(24 * 60 * 60) }
            Button("7 days") { setDisappear(7 * 24 * 60 * 60) }
            Button("90 days") { setDisappear(90 * 24 * 60 * 60) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("New messages in this chat disappear after the selected duration on this device.")
        }
        .alert("Encryption", isPresented: $showEncryption) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Messages and calls in this chat use end-to-end encryption when both people have keys set. TJ cannot read the contents of those messages.")
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

    private var contactAvatar: some View {
        Group {
            if let raw = chat.avatarUrl, let url = FeedPost.resolve(raw) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image { image.resizable().scaledToFill() } else { avatarFallback }
                }
            } else {
                avatarFallback
            }
        }
        .frame(width: 110, height: 110)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color(uiColor: .systemBackground), lineWidth: 3))
        .shadow(radius: 6)
    }

    private var avatarFallback: some View {
        Circle().fill(Color.gray.opacity(0.25)).overlay(Text(String(title.prefix(1)).uppercased()).font(.system(size: 42, weight: .bold)))
    }

    private func actionButton(_ icon: String, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon).font(.system(size: 18)).foregroundColor(Color.appPrimary)
                Text(label).font(.system(size: 11, weight: .medium)).foregroundColor(.primary)
            }
            .frame(width: 64)
        }
    }

    private func labeledRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon).foregroundColor(Color.appAccent).frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 16)).foregroundColor(.primary)
                Text(subtitle).font(.system(size: 12)).foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 4)
    }

    private func settingRow<Trailing: View>(icon: String, title: String, subtitle: String = "", @ViewBuilder trailing: () -> Trailing) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon).foregroundColor(Color.appAccent).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundColor(.primary)
                if !subtitle.isEmpty { Text(subtitle).font(.system(size: 12)).foregroundColor(.secondary) }
            }
            Spacer()
            trailing()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func settingLabel(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon).foregroundColor(Color.appAccent).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundColor(.primary)
                if !subtitle.isEmpty { Text(subtitle).font(.system(size: 12)).foregroundColor(.secondary).multilineTextAlignment(.leading) }
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func dangerRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                Text(title).fontWeight(.bold)
                Spacer()
            }
            .foregroundColor(.red)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
    }

    private func mediaTile(_ raw: String, video: Bool) -> some View {
        ZStack {
            if let url = FeedPost.resolve(raw) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image { image.resizable().scaledToFill() } else { Color.gray.opacity(0.15) }
                }
            } else {
                Color.gray.opacity(0.15)
            }
            if video {
                Image(systemName: "play.fill").foregroundColor(.white)
            }
        }
        .frame(width: 80, height: 80)
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    private var disappearLabel: String {
        switch disappearSeconds {
        case 0: return "Off"
        case 24 * 60 * 60: return "24 hours"
        case 7 * 24 * 60 * 60: return "7 days"
        case 90 * 24 * 60 * 60: return "90 days"
        default: return disappearSeconds > 0 ? "\(disappearSeconds / 3600) hours" : "Off"
        }
    }

    private func setMediaVisible(_ value: Bool) {
        mediaVisible = value
        UserDefaults.standard.set(value, forKey: "tj_media_visible_\(chat.id)")
    }

    private func setDisappear(_ seconds: Int) {
        disappearSeconds = seconds
        UserDefaults.standard.set(seconds, forKey: "tj_disappear_\(chat.id)")
    }

    private func placeCall(_ type: CallType) {
        let target = userId
        let label = chat.consistentName()
        Task { @MainActor in
            guard !target.isEmpty else {
                note = "No contact to call"
                return
            }
            ChatViewModel().startVoiceOrVideoCall(receiverId: target, callType: type, name: label)
        }
    }

    private static func lastSeen(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let calendar = Calendar.current
        let time = DateFormatter()
        time.dateFormat = "h:mm a"
        let clock = time.string(from: date)
        if calendar.isDateInToday(date) { return "last seen today at \(clock)" }
        if calendar.isDateInYesterday(date) { return "last seen yesterday at \(clock)" }
        let day = DateFormatter()
        day.dateFormat = "MMM d"
        return "last seen \(day.string(from: date)) at \(clock)"
    }

    private func load() async {
        if !userId.isEmpty, let data = try? await API.data("users/\(userId)") {
            let root = InfoJSON.object(data)
            let user = (root["user"] as? [String: Any]) ?? root
            let loadedPhone = LiveJSON.string(user["phone_number"])
            if !loadedPhone.isEmpty { phone = loadedPhone }
            username = LiveJSON.string(user["username"])
        }
        if let blockedData = try? await API.data("users/blocked") {
            let rows = LiveJSON.list(blockedData, keys: ["users", "blocked"])
            blocked = rows.contains { LiveJSON.string($0["id"]) == userId }
        }
        guard ChatExpiry.mediaVisible(chat.id) else {
            media = []
            return
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

    private func unblock() async {
        guard !userId.isEmpty else { return }
        do {
            _ = try await API.data("users/\(userId)/unblock", method: "POST")
            blocked = false
        } catch { note = error.localizedDescription }
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
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: 8) {
                        Circle()
                            .fill(Color.appAccent.opacity(0.2))
                            .frame(width: 110, height: 110)
                            .overlay(Text(String(name.prefix(1)).uppercased()).font(.system(size: 42, weight: .bold)).foregroundColor(Color.appAccent))
                        Text(name.isEmpty ? "Channel" : name).font(.system(size: 22, weight: .bold))
                        Text("\(followers) followers").font(.system(size: 14)).foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(LinearGradient(colors: [Color.appAccent.opacity(0.22), Color(uiColor: .systemBackground)], startPoint: .top, endPoint: .bottom))
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Channel info").font(.system(size: 14, weight: .bold)).foregroundColor(Color.appAccent)
                        Text("This is a broadcast channel for \(name).")
                        HStack(spacing: 8) {
                            Image(systemName: "person.3").foregroundColor(.secondary)
                            Text("\(followers) followers").foregroundColor(.secondary)
                        }
                        if !details.isEmpty { Text(details).foregroundColor(.secondary) }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    Rectangle().fill(Color.primary.opacity(0.06)).frame(height: 8)
                    HStack {
                        Image(systemName: "bell.slash").foregroundColor(Color.appAccent).frame(width: 24)
                        Text("Mute notifications")
                        Spacer()
                        Toggle("", isOn: Binding(get: { muted }, set: { value in Task { await setMuted(value) } })).labelsHidden().tint(Color.appAccent)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    Button { Task { await follow() } } label: {
                        HStack {
                            Image(systemName: subscribed ? "minus.circle" : "plus.circle").foregroundColor(Color.appAccent).frame(width: 24)
                            Text(subscribed ? "Unfollow channel" : "Follow channel").foregroundColor(.primary)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                    Button { showDescription = true } label: {
                        HStack {
                            Image(systemName: "pencil").foregroundColor(Color(hex: "00A884")).frame(width: 24)
                            Text("Add description").foregroundColor(Color(hex: "00A884"))
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                    Button { Task { await shareStatus() } } label: {
                        HStack {
                            Image(systemName: "target").foregroundColor(Color(hex: "00A884")).frame(width: 24)
                            Text("Share to my status").foregroundColor(Color(hex: "00A884"))
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                    ShareLink(item: "Follow \(name) on TJ: firstapp://channel/\(conversationId)") {
                        HStack {
                            Image(systemName: "square.and.arrow.up").foregroundColor(Color.appAccent).frame(width: 24)
                            Text("Share channel").foregroundColor(.primary)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                }
            }
            .navigationTitle("Channel info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    ShareLink(item: "Follow \(name) on TJ: firstapp://channel/\(conversationId)") {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
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
            Button("OK", role: .cancel) {}
        } message: {
            Text(note)
        }
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

    private func setMuted(_ value: Bool) async {
        let previous = muted
        muted = value
        do {
            _ = try await API.data("chats/channels/\(conversationId)/mute", method: "POST", json: ["is_muted": value])
        } catch {
            muted = previous
            note = error.localizedDescription
        }
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
            _ = try await API.data("chats/channels/\(conversationId)/description", method: "PATCH", json: ["description": text])
            details = text
            showDescription = false
            note = "Description saved"
            return
        } catch {}
        do {
            _ = try await API.data("conversations/\(conversationId)/messages", method: "POST", json: [
                "client_uuid": UUID().uuidString,
                "message_text": text,
                "media_type": "TEXT"
            ])
            showDescription = false
            note = "Description posted to the channel. The server did not save it on the channel yet."
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
            ScrollView {
                VStack(spacing: 16) {
                    Text(channel.name).font(.system(size: 22, weight: .bold))
                    Text("\(channel.followers) followers").foregroundColor(.secondary)
                    if !channel.description.isEmpty {
                        Text(channel.description).foregroundColor(.secondary)
                    }
                    TextField("Add description", text: $draft, axis: .vertical)
                        .padding(10)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.35)))
                    Button("Post description to channel") {
                        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !text.isEmpty else { return }
                        onPostDescription(text)
                    }
                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Button(action: onShareStatus) {
                        Text("Share to my status")
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.secondary.opacity(0.3)))
                    }
                    if channel.isAdmin {
                        Button("Invite admins", action: onInviteAdmins)
                    }
                }
                .padding(20)
            }
            .navigationTitle("Channel info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close", action: onClose) } }
        }
        .onAppear { draft = channel.description }
    }
}
