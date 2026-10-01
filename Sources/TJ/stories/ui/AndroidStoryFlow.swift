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
                StoryViewer(sampleStoryGroup: playing, onClose: {
                    self.playing = nil
                    if step == .posted { return }
                    onClose()
                })
            } else if let share {
                ShareStoryScreen(draft: share, onBack: { self.share = nil }, onPosted: {
                    onPosted()
                    let latest = StoryRepository.shared.userStories
                    if let mine = AndroidStoryFlow.myStory(in: latest.isEmpty ? stories : latest, userId: currentUserId) {
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
                        story: AndroidStoryFlow.myStory(in: stories, userId: currentUserId),
                        onBack: onClose,
                        onOpen: { playing = $0 },
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
            guard let item else {
                if preview == nil { onBack() }
                return
            }
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
    let story: UserStory?
    var onBack: () -> Void
    var onOpen: (UserStory) -> Void
    var onText: () -> Void
    var onMedia: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView(.horizontal) {
                HStack(spacing: 20) {
                    if let story {
                        ForEach(story.stories) { item in
                            VStack {
                                if let raw = item.contentUrl, let url = FeedPost.resolve(raw) {
                                    AsyncImage(url: url) { image in
                                        image.resizable().scaledToFill()
                                    } placeholder: {
                                        Color.gray
                                    }
                                    .frame(width: 92, height: 140)
                                    .clipped()
                                } else {
                                    Text(item.content)
                                        .font(.caption)
                                        .frame(width: 92, height: 140)
                                        .background(item.backgroundColor)
                                        .foregroundColor(.white)
                                }
                            }
                            .onTapGesture { onOpen(story) }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Posted stories")
            .navigationBarTitleDisplayMode(.inline)
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
        }
    }
}
