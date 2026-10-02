import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import AVFoundation
import UIKit

/// Same screens Android opens from the story menu: New Story, Text Story, Share Story, Posted stories.
public struct AndroidStoryFlow: View {
    public enum Start: String, Identifiable {
        case media
        case text
        case posted
        public var id: String { rawValue }
    }

    @State private var step: Start
    @State private var share: ShareDraft?
    @State private var playing: UserStory?
    @State private var playIndex = 0
    @State private var note = ""
    private let stories: [UserStory]
    private let currentUserId: String?
    private let onClose: () -> Void
    private let onPosted: () -> Void

    public init(start: Start, stories: [UserStory], currentUserId: String?, onClose: @escaping () -> Void, onPosted: @escaping () -> Void) {
        self._step = State(initialValue: start)
        self.stories = stories
        self.currentUserId = currentUserId
        self.onClose = onClose
        self.onPosted = onPosted
    }

    public var body: some View {
        Group {
            if let playing {
                StoryViewer(story: playing, startIndex: playIndex, onClose: {
                    self.playing = nil
                    if step == .posted { return }
                    onClose()
                })
            } else if let share {
                ShareStoryScreen(draft: share, onBack: { self.share = nil }, onPosted: {
                    onPosted()
                    let latest = StoryRepository.shared.userStories
                    if let mine = AndroidStoryFlow.myStory(in: latest.isEmpty ? stories : latest, userId: currentUserId) {
                        playIndex = 0
                        playing = mine
                    } else {
                        onClose()
                    }
                })
            } else {
                switch step {
                case .media:
                    NewStoryScreen(
                        onBack: onClose,
                        onNext: { draft in share = draft }
                    )
                case .text:
                    TextStoryScreen(
                        onBack: onClose,
                        onShare: { text in
                            share = ShareDraft(content: text, type: "text", data: nil, filename: "", mime: "")
                        }
                    )
                case .posted:
                    PostedStoriesScreen(
                        currentUserId: currentUserId,
                        onBack: onClose,
                        onOpen: { story, index in
                            playIndex = index
                            playing = story
                        },
                        onText: { step = .text },
                        onMedia: { step = .media }
                    )
                }
            }
        }
        .alert("Story", isPresented: Binding(get: { !note.isEmpty }, set: { if !$0 { note = "" } })) {
            Button("OK", role: .cancel) { note = "" }
        } message: {
            Text(note)
        }
    }

    static func myStory(in stories: [UserStory], userId: String?) -> UserStory? {
        guard let userId, !userId.isEmpty else { return nil }
        let bare = userId.replacingOccurrences(of: "user_", with: "")
        return stories.first { story in
            let ids = [story.id, story.userId]
            return ids.contains(userId) || ids.contains(bare) || ids.contains("user_\(bare)")
        }
    }
}

struct ShareDraft: Identifiable {
    let id = UUID()
    var content: String
    var type: String
    var data: Data?
    var filename: String
    var mime: String
}

struct NewStoryScreen: View {
    var onBack: () -> Void
    var onNext: (ShareDraft) -> Void

    @State private var pickerItem: PhotosPickerItem?
    @State private var showPicker = true
    @State private var caption = ""
    @State private var preview: Data?
    @State private var mime = "image/jpeg"
    @State private var filename = "story.jpg"
    @State private var mediaType = "image"
    @State private var tooLong = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let preview, mime.hasPrefix("image"), let image = UIImage(data: preview) {
                Image(uiImage: image).resizable().scaledToFit()
            } else if preview != nil {
                Image(systemName: mediaType == "video" ? "video.fill" : "doc.fill")
                    .font(.system(size: 72))
                    .foregroundColor(.white)
            }
            VStack {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left").foregroundColor(.white)
                    }
                    VStack(alignment: .leading) {
                        Text("New Story").foregroundColor(.white).font(.headline)
                        if tooLong {
                            Text("Video exceeds 2m limit").foregroundColor(.red).font(.caption)
                        }
                    }
                    Spacer()
                }
                .padding()
                Spacer()
                HStack {
                    TextField("Add a caption...", text: $caption)
                        .foregroundColor(.white)
                    Button("NEXT") {
                        guard let preview, !tooLong else { return }
                        onNext(ShareDraft(content: caption, type: mediaType, data: preview, filename: filename, mime: mime))
                    }
                    .disabled(preview == nil || tooLong)
                    .foregroundColor(.white)
                    .font(.headline)
                }
                .padding()
                .background(Color.black.opacity(0.5))
            }
        }
        .photosPicker(isPresented: $showPicker, selection: $pickerItem, matching: .any(of: [.images, .videos]))
        .onChange(of: pickerItem) { item in
            guard let item else { return }
            Task { await load(item) }
        }
    }

    private func load(_ item: PhotosPickerItem) async {
        let type = item.supportedContentTypes.first
        mime = type?.preferredMIMEType ?? "application/octet-stream"
        mediaType = mime.hasPrefix("video") ? "video" : "image"
        filename = mediaType == "video" ? "story.mp4" : "story.jpg"
        preview = try? await item.loadTransferable(type: Data.self)
        if mediaType == "video", let preview {
            tooLong = await videoLongerThanTwoMinutes(preview)
        }
    }

    private func videoLongerThanTwoMinutes(_ data: Data) async -> Bool {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("story-check.mp4")
        try? data.write(to: url)
        let asset = AVURLAsset(url: url)
        let duration = (try? await asset.load(.duration)) ?? .zero
        return duration.seconds > 120
    }
}

struct TextStoryScreen: View {
    var onBack: () -> Void
    var onShare: (String) -> Void
    @State private var text = ""

    var body: some View {
        VStack {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "xmark")
                }
                Text("Text Story").font(.headline)
                Spacer()
            }
            .padding()
            Spacer()
            TextField("Type a story...", text: $text, axis: .vertical)
                .font(.title2)
                .multilineTextAlignment(.center)
                .padding()
            Spacer()
            Button("Share story") {
                let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmed.isEmpty else { return }
                onShare(trimmed)
            }
            .buttonStyle(.borderedProminent)
            .padding()
        }
    }
}

struct ShareStoryScreen: View {
    let draft: ShareDraft
    var onBack: () -> Void
    var onPosted: () -> Void
    @State private var privacy = "Contacts"
    @State private var allowScreenshots = true
    @State private var postToProfile = true
    @State private var posting = false
    @State private var error = ""

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left").foregroundColor(.white)
                    }
                    .disabled(posting)
                    Text("New Story").foregroundColor(.white).font(.headline)
                    Spacer()
                }
                .padding()
                VStack(alignment: .leading, spacing: 16) {
                    Text("Share story").font(.title3.bold()).foregroundColor(.white)
                    Text("Choose who can view your story").foregroundColor(.gray)
                    privacyRow("All Contacts", "Visible to all contacts", selected: privacy == "Contacts") { privacy = "Contacts" }
                    privacyRow("All Contacts except…", "exclude specific people >", selected: privacy == "ContactsExcept") { privacy = "ContactsExcept" }
                    Text("Select people who will never see your stories.").foregroundColor(Color(red: 0.13, green: 0.59, blue: 0.95)).font(.footnote)
                    Toggle("Allow Screenshots", isOn: $allowScreenshots).foregroundColor(.white)
                    Toggle("Post to My Profile", isOn: $postToProfile).foregroundColor(.white)
                    HStack {
                        Text("Album").foregroundColor(.white)
                        Spacer()
                        Text("All Stories").foregroundColor(Color(red: 0.13, green: 0.59, blue: 0.95))
                    }
                    Text("Keep this story on your profile even after it expires in 24 hours. Privacy settings will apply.")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .padding(20)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .background(Color(red: 0.11, green: 0.11, blue: 0.12))
                .clipShape(RoundedRectangle(cornerRadius: 24))
                Button(action: post) {
                    if posting {
                        ProgressView()
                    } else {
                        Text("Post Story").font(.headline)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.accentColor)
                .foregroundColor(.black)
                .clipShape(Capsule())
                .disabled(posting)
                .padding()
            }
        }
        .alert("Story", isPresented: Binding(get: { !error.isEmpty }, set: { if !$0 { error = "" } })) {
            Button("OK", role: .cancel) { error = "" }
        } message: {
            Text(error)
        }
    }

    private func privacyRow(_ title: String, _ subtitle: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading) {
                    Text(title).foregroundColor(.white)
                    Text(subtitle).font(.caption).foregroundColor(.gray)
                }
                Spacer()
                Image(systemName: selected ? "checkmark.circle.fill" : "circle").foregroundColor(.white)
            }
        }
    }

    private func post() {
        posting = true
        let wirePrivacy = privacy == "ContactsExcept" ? "my_contacts_except" : "contacts"
        Task {
            do {
                var url = ""
                if let data = draft.data, !data.isEmpty {
                    url = try await MediaActions.upload(data: data, filename: draft.filename, mime: draft.mime, folder: "stories")
                }
                _ = try await StoryRepository.shared.createStory(
                    content: draft.content,
                    contentUrl: url.isEmpty ? nil : url,
                    type: draft.type,
                    privacy: wirePrivacy
                )
                posting = false
                onPosted()
            } catch {
                posting = false
                self.error = error.localizedDescription
            }
        }
    }
}

struct PostedStoriesScreen: View {
    var currentUserId: String?
    var onBack: () -> Void
    var onOpen: (UserStory, Int) -> Void
    var onText: () -> Void
    var onMedia: () -> Void

    @ObservedObject private var repository = StoryRepository.shared
    @State private var query = ""

    private var mine: UserStory? {
        AndroidStoryFlow.myStory(in: repository.userStories, userId: currentUserId)
    }

    private var items: [StoryItem] {
        let stories = mine?.stories ?? []
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return stories }
        return stories.filter { $0.content.localizedCaseInsensitiveContains(trimmed) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let mine, !items.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 20) {
                                ForEach(Array(items.enumerated()), id: \.element.id) { offset, item in
                                    Button(action: { onOpen(mine, index(of: item)) }) {
                                        postedItem(item, name: mine.userName)
                                    }
                                    .buttonStyle(.plain)
                                    .id(offset)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    } else {
                        Button(action: onMedia) {
                            HStack(spacing: 16) {
                                ZStack(alignment: .bottomTrailing) {
                                    Circle()
                                        .fill(Color.accentColor)
                                        .frame(width: 54, height: 54)
                                        .overlay(Text(String((mine?.userName ?? "M").prefix(1)).uppercased()).foregroundColor(.white).font(.title3.bold()))
                                    Image(systemName: "plus")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(width: 20, height: 20)
                                        .background(Color.accentColor)
                                        .clipShape(Circle())
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("My Story").font(.system(size: 16, weight: .bold))
                                    Text("Tap to add to your story").font(.system(size: 14)).foregroundColor(.secondary)
                                }
                                Spacer()
                            }
                        }
                        .buttonStyle(.plain)
                        .padding(.vertical, 8)
                    }
                }
                .padding(.horizontal, 16)
            }
            .navigationTitle("Posted stories")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, prompt: "Search stories...")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onBack) { Image(systemName: "chevron.left") }
                }
            }
            .overlay(alignment: .bottomTrailing) {
                VStack(spacing: 16) {
                    Button(action: onText) {
                        Image(systemName: "pencil")
                            .frame(width: 40, height: 40)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(Circle())
                    }
                    Button(action: onMedia) {
                        Image(systemName: "camera.fill")
                            .frame(width: 56, height: 56)
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .clipShape(Circle())
                    }
                }
                .padding()
            }
            .task { _ = await repository.syncStories() }
        }
    }

    private func index(of item: StoryItem) -> Int {
        mine?.stories.firstIndex(where: { $0.id == item.id }) ?? 0
    }

    private func postedItem(_ item: StoryItem, name: String) -> some View {
        VStack(spacing: 6) {
            ZStack {
                Circle().stroke(Color.accentColor, lineWidth: 2).frame(width: 70, height: 70)
                if let raw = item.contentUrl, let url = FeedPost.resolve(raw) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.black.opacity(0.8)
                    }
                    .frame(width: 62, height: 62)
                    .clipShape(Circle())
                } else if item.type == .text, !item.content.isEmpty {
                    Text(item.content)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .padding(4)
                        .frame(width: 62, height: 62)
                        .background(item.backgroundColor)
                        .clipShape(Circle())
                } else {
                    Text(String(name.prefix(1)).uppercased())
                        .foregroundColor(.white)
                        .frame(width: 62, height: 62)
                        .background(item.backgroundColor)
                        .clipShape(Circle())
                }
            }
            HStack(spacing: 4) {
                Image(systemName: "eye.fill").font(.system(size: 12))
                Text("\(item.viewsCount) views").font(.system(size: 12, weight: .bold))
            }
            .foregroundColor(.accentColor)
            Text(StoryTime.ago(item.createdAt, fallback: item.timestamp))
                .font(.system(size: 11))
                .foregroundColor(.secondary)
        }
    }
}

enum StoryTime {
    static func ago(_ createdAt: Int64, fallback: String = "") -> String {
        guard createdAt > 0 else { return fallback.isEmpty ? "recently" : fallback }
        let seconds = max(0, Int(Date().timeIntervalSince1970) - Int(createdAt / 1000))
        if seconds < 60 { return "just now" }
        if seconds < 3600 { return "\(seconds / 60)m" }
        if seconds < 86400 { return "\(seconds / 3600)h" }
        return "\(seconds / 86400)d"
    }
}

struct StoryLaunch: Identifiable {
    let id = UUID().uuidString
    let userId: String
    let storyId: String?
}

struct StoryRowPlayer: View {
    let startUserId: String
    var startStoryId: String? = nil
    var archive = false
    var onClose: () -> Void
    @State private var queue: [UserStory] = []
    @State private var position = 0
    @State private var slideIndex = 0
    @State private var savedSlides: [String: Int] = [:]
    @State private var loaded = false
    @State private var missing = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if loaded, queue.indices.contains(position) {
                StoryViewer(
                    story: queue[position],
                    startIndex: slideIndex,
                    onClose: onClose,
                    onFinished: { move(1) },
                    onRetreat: { move(-1) }
                )
                .id("\(queue[position].id)-\(slideIndex)-\(position)")
            } else if missing {
                VStack(spacing: 12) {
                    Text("This story has expired or is no longer available.")
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    Button("Close", action: onClose).foregroundColor(.white)
                }
                .padding(32)
            } else {
                ProgressView().tint(.white)
            }
        }
        .task { await load() }
    }

    private func load() async {
        _ = await StoryRepository.shared.syncStories()
        let all = StoryRepository.shared.userStories
        let mine = AndroidStoryFlow.myStory(in: all, userId: SessionStore.shared.userId)
        let ordered: [UserStory]
        if archive {
            ordered = mine.map { [$0] } ?? []
        } else {
            var list: [UserStory] = []
            if let mine, !mine.stories.isEmpty { list.append(mine) }
            let others = all.filter { story in
                story.id != mine?.id && story.userId != mine?.userId && !story.isMuted
            }
            list.append(contentsOf: others.filter { !$0.allViewed })
            list.append(contentsOf: others.filter { $0.allViewed })
            ordered = list
        }
        let wanted = startUserId.replacingOccurrences(of: "user_", with: "")
        if let index = ordered.firstIndex(where: { story in
            [story.id, story.userId].contains { key in
                let bare = key.replacingOccurrences(of: "user_", with: "")
                return key == startUserId || bare == wanted
            }
        }) {
            queue = ordered
            position = index
            slideIndex = slideIndex(in: ordered[index], storyId: startStoryId)
            loaded = true
        } else {
            missing = true
            loaded = true
        }
    }

    private func move(_ direction: Int) {
        guard queue.indices.contains(position) else {
            onClose()
            return
        }
        savedSlides[queue[position].userId] = slideIndex
        let next = position + direction
        guard queue.indices.contains(next) else {
            onClose()
            return
        }
        position = next
        if direction < 0 {
            slideIndex = savedSlides[queue[next].userId] ?? max(0, queue[next].stories.count - 1)
        } else {
            slideIndex = savedSlides[queue[next].userId] ?? 0
        }
    }

    private func slideIndex(in story: UserStory, storyId: String?) -> Int {
        guard let storyId, !storyId.isEmpty else { return 0 }
        let wanted = storyId.replacingOccurrences(of: "story_", with: "").components(separatedBy: "|").first ?? storyId
        let index = story.stories.firstIndex { item in
            let id = item.id.replacingOccurrences(of: "story_", with: "").components(separatedBy: "|").first ?? item.id
            return item.id == storyId || id == wanted || item.id.hasSuffix(wanted) || wanted.hasSuffix(id)
        }
        return index ?? 0
    }
}
