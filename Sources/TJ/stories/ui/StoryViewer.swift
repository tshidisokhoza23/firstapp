import SwiftUI
import AVKit

// MARK: - Story Item Models

public struct StorySlide: Identifiable, Hashable {
    public let id: String
    public let mediaSymbol: String
    public let caption: String?
    public let backgroundGradient: [Color]
    public let durationSeconds: Double
    public let mediaURL: String?

    public init(id: String, mediaSymbol: String, caption: String? = nil, backgroundGradient: [Color], durationSeconds: Double = 5.0, mediaURL: String? = nil) {
        self.id = id
        self.mediaSymbol = mediaSymbol
        self.caption = caption
        self.backgroundGradient = backgroundGradient
        self.durationSeconds = durationSeconds
        self.mediaURL = mediaURL
    }
}

public struct UserStoryGroup: Identifiable {
    public let id: String
    public let username: String
    public let avatarGradient: [Color]
    public let timeAgo: String
    public let slides: [StorySlide]

    public init(id: String, username: String, avatarGradient: [Color], timeAgo: String, slides: [StorySlide]) {
        self.id = id
        self.username = username
        self.avatarGradient = avatarGradient
        self.timeAgo = timeAgo
        self.slides = slides
    }
}

// MARK: - StoryViewer

public struct StoryViewer: View {
    public var onClose: (() -> Void)?

    @State private var activeSlideIndex: Int = 0
    @State private var slideProgress: Double = 0.0
    @State private var isPaused: Bool = false
    @State private var replyText: String = ""
    @State private var storyPlayer: AVPlayer?
    @State private var showViewersSheet: Bool = false
    @State private var sentReactionEmoji: String? = nil
    @State private var videoHoldSeconds: Double = 0
    @State private var confirmDelete = false

    private let liveStory: UserStory?
    private let startIndex: Int
    private let onFinished: (() -> Void)?
    private let onRetreat: (() -> Void)?
    @State private var viewers: [ApiViewer] = []
    @State private var viewersNote = ""

    private let quickEmojis = ["❤️", "🔥", "😂", "👏", "😮", "🎉"]

    private let timer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()

    public init(story: UserStory, startIndex: Int = 0, onClose: (() -> Void)? = nil, onFinished: (() -> Void)? = nil, onRetreat: (() -> Void)? = nil) {
        self.liveStory = story
        self.startIndex = startIndex
        self.onClose = onClose
        self.onFinished = onFinished
        self.onRetreat = onRetreat
    }

    /// Older call sites pass the live story under this name. It is not sample data.
    public init(sampleStoryGroup: UserStory, startIndex: Int = 0, onClose: (() -> Void)? = nil, onFinished: (() -> Void)? = nil, onRetreat: (() -> Void)? = nil) {
        self.liveStory = sampleStoryGroup
        self.startIndex = startIndex
        self.onClose = onClose
        self.onFinished = onFinished
        self.onRetreat = onRetreat
    }

    private var displayGroup: UserStoryGroup {
        guard let liveStory else {
            return UserStoryGroup(id: "", username: "", avatarGradient: [.black], timeAgo: "", slides: [])
        }let hold: Double
            if item.type == .video {
                hold = 0
            } else {
                let seconds = item.durationMs > 0 ? Double(item.durationMs) / 1000.0 : 6
                hold = min(15, max(6, seconds))
            }
            StorySlide(
                id: item.id,
                mediaSymbol: item.type == .video ? "video.fill" : (item.type == .image ? "photo.fill" : "text.alignleft"),
                caption: item.content.isEmpty ? nil : item.content,
                backgroundGradient: [item.backgroundColor, .black],
                durationSeconds: hold
                backgroundGradient: [item.backgroundColor, .black],
                durationSeconds: max(1, Double(item.durationMs) / 1000.0),
                mediaURL: item.contentUrl
            )
        }
        return UserStoryGroup(
            id: liveStory.id,
            username: liveStory.userName,
            avatarGradient: [liveStory.stories.first?.backgroundColor ?? .black, .black],
            timeAgo: liveStory.lastUpdated,
            slides: slides
        )
    }

    private var isOwnStory: Bool {
        guard let liveStory else { return false }
        let mine = SessionStore.shared.userId ?? ""
        let bare = mine.replacingOccurrences(of: "user_", with: "")
        return [liveStory.id, liveStory.userId].contains { key in
            key == mine || key.replacingOccurrences(of: "user_", with: "") == bare
        }
    }

    private var headerTime: String {
        let item = liveStory?.stories.indices.contains(activeSlideIndex) == true ? liveStory?.stories[activeSlideIndex] : liveStory?.stories.last
        if let item {
            return StoryTime.ago(item.createdAt, fallback: item.timestamp)
        }
        if let ms = Int64(displayGroup.timeAgo) {
            return StoryTime.ago(ms)
        }
        return displayGroup.timeAgo
    }

    private var currentViews: Int {
        guard let liveStory, liveStory.stories.indices.contains(activeSlideIndex) else { return 0 }
        return liveStory.stories[activeSlideIndex].viewsCount
    }

    private var currentSlide: StorySlide {
        let slides = displayGroup.slides
        guard !slides.isEmpty else {
            return StorySlide(id: "empty", mediaSymbol: "circle", backgroundGradient: [.black, .black])
        }
        return slides[min(max(activeSlideIndex, 0), slides.count - 1)]
    }

    public var body: some View {
        if displayGroup.slides.isEmpty {
            ZStack {
                Color.black.ignoresSafeArea()
                Text("This story has expired or is no longer available.")
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(32)
            }
        } else {
            playback
        }
    }

    private var playback: some View {
        ZStack {
            // Full Screen Background & Media Content
            LinearGradient(colors: currentSlide.backgroundGradient, startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()

            if let player = storyPlayer {
                VideoPlayer(player: player)
                    .ignoresSafeArea()
            } else if let raw = currentSlide.mediaURL, let url = FeedPost.resolve(raw), currentSlide.mediaSymbol != "video.fill" {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill().ignoresSafeArea()
                    } else {
                        Color.black.ignoresSafeArea()
                    }
                }
            }

            VStack {
                Spacer()

                if storyPlayer == nil && currentSlide.mediaURL == nil {
                    Image(systemName: currentSlide.mediaSymbol)
                        .font(.system(size: 130))
                        .foregroundColor(.white.opacity(0.85))
                        .shadow(radius: 12)
                }

                if let caption = currentSlide.caption {
                    Text(caption)
                        .font(.title3.bold())
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                        .padding(.top, 20)
                }

                Spacer()
            }

            // Interactive Touch Zones
            HStack(spacing: 0) {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        goToPreviousSlide()
                    }

                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        goToNextSlide()
                    }
            }
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in isPaused = true }
                    .onEnded { _ in isPaused = false }
            )

            // Overlays (Top Header & Bottom Reply)
            VStack {
                // Top Progress Bars & User Info
                VStack(spacing: 10) {
                    // Segmented Progress Bar
                    HStack(spacing: 4) {
                        ForEach(0..<displayGroup.slides.count, id: \.self) { idx in
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Rectangle()
                                        .fill(Color.white.opacity(0.35))

                                    Rectangle()
                                        .fill(Color.white)
                                        .frame(width: progressWidth(for: idx, totalWidth: geo.size.width))
                                }
                                .cornerRadius(2)
                            }
                            .frame(height: 3)
                        }
                    }

                    // User Header
                    HStack(spacing: 10) {
                        storyAvatar

                        VStack(alignment: .leading, spacing: 2) {
                            Text(displayGroup.username)
                                .font(.subheadline.bold())
                                .foregroundColor(.white)

                            Text(headerTime)
                                .font(.caption2)
                                .foregroundColor(.white.opacity(0.8))
                        }

                        Spacer()

                        if isOwnStory {
                            Button(action: { showViewersSheet = true }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "eye.fill")
                                    Text("\(currentViews)")
                                }
                                .font(.caption.bold())
                                .foregroundColor(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.black.opacity(0.3))
                                .cornerRadius(12)
                            }
                            Button(action: { confirmDelete = true }) {
                                Image(systemName: "trash")
                                    .font(.caption.bold())
                                    .foregroundColor(.white)
                                    .padding(8)
                                    .background(Color.black.opacity(0.3))
                                    .clipShape(Circle())
                            }
                        }

                        Button(action: { onClose?() }) {
                            Image(systemName: "xmark")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(8)
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.top, 12)

                Spacer()

                // Reaction Toast Animation
                if let emoji = sentReactionEmoji {
                    Text(emoji)
                        .font(.system(size: 60))
                        .transition(.scale.combined(with: .opacity))
                        .padding(.bottom, 20)
                }

                if !isOwnStory {
                VStack(spacing: 12) {
                    // Emoji Pills
                    HStack(spacing: 12) {
                        ForEach(quickEmojis, id: \.self) { emoji in
                            Button(action: { sendEmojiReaction(emoji) }) {
                                Text(emoji)
                                    .font(.title2)
                                    .padding(8)
                                    .background(Color.black.opacity(0.3))
                                    .clipShape(Circle())
                            }
                        }
                    }

                    // Text Field Row
                    HStack(spacing: 10) {
                        TextField("Send reply to @\(displayGroup.username)...", text: $replyText)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.35))
                            .cornerRadius(20)
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.3), lineWidth: 1))

                        if !replyText.isEmpty {
                            Button(action: {
                                let text = replyText
                                replyText = ""
                                sendEmojiReaction("📩")
                                Task { await sendStoryReply(text) }
                            }) {
                                Image(systemName: "paperplane.fill")
                                    .font(.headline)
                                    .foregroundColor(.white)
                                    .padding(10)
                                    .background(Color.blue)
                                    .clipShape(Circle())
                            }
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 16)
                }
            }
        }
        .onReceive(timer) { _ in
            guard !isPaused else { return }
            let duration = currentSlide.mediaSymbol == "video.fill" ? videoHoldSeconds : currentSlide.durationSeconds
            guard duration > 0 else { return }
            let step = 0.1 / duration
            if slideProgress + step >= 1.0 {
                goToNextSlide()
            } else {
                slideProgress += step
            }
        }
        .alert("Delete story update?", isPresented: $confirmDelete) {
            Button("Delete", role: .destructive) { Task { await deleteCurrentStory() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This story will be removed.")
        }
        .sheet(isPresented: $showViewersSheet) {
            viewersSheetView
        }
        .onAppear {
            if displayGroup.slides.indices.contains(startIndex) {
                activeSlideIndex = startIndex
            }
            markCurrentViewed()
            loadSlideMedia()
        }
        .onChange(of: activeSlideIndex) { _ in
            markCurrentViewed()
            loadSlideMedia()
        }
        .onDisappear {
        videoHoldSeconds = 0
        guard currentSlide.mediaSymbol == "video.fill",
              let raw = currentSlide.mediaURL,
              let url = FeedPost.resolve(raw) else { return }
        let player = AVPlayer(url: url)
        storyPlayer = player
        player.play()
        Task {
            for _ in 0..<40 {
                let seconds = player.currentItem?.duration.seconds ?? 0
                if seconds.isFinite, seconds > 0 {
                    await MainActor.run {
                        videoHoldSeconds = min(120, max(3, seconds))
                    }
                    return
                }
                try? await Task.sleep(nanoseconds: 250_000_000)
            }
        }
    }

    private func deleteCurrentStory() async {
        guard let item = liveStory?.stories.indices.contains(activeSlideIndex) == true ? liveStory?.stories[activeSlideIndex] : nil else { return }
        let removed = (try? await StoryRepository.shared.deleteStory(storyId: item.id)) ?? false
        if removed {
            onClose?()
        }SlideMedia() {
        storyPlayer?.pause()
        storyPlayer = nil
        guard currentSlide.mediaSymbol == "video.fill",
              let raw = currentSlide.mediaURL,
              let url = FeedPost.resolve(raw) else { return }
        let player = AVPlayer(url: url)
        storyPlayer = player
        player.play()
    }

    private func markCurrentViewed() {
        guard let liveStory, liveStory.stories.indices.contains(activeSlideIndex) else { return }
        let item = liveStory.stories[activeSlideIndex]
        Task {
            _ =if let onRetreat {
            onRetreat()
        } else  try? await StoryRepository.shared.markStoryAsViewed(userId: liveStory.userId, storyId: item.id)
        }
    }

    // MARK: - Navigation Logic

    private func goToNextSlide() {
        if activeSlideIndex < displayGroup.slides.count - 1 {
            activeSlideIndex += 1
            slideProgress = 0.0
        } else if let onFinished {
            onFinished()
        } else {
            onClose?()
        }
    }

    private func goToPreviousSlide() {
        if activeSlideIndex > 0 {
            activeSlideIndex -= 1
            slideProgress = 0.0
        } else {
            slideProgress = 0.0
        }
    }

    private func progressWidth(for index: Int, totalWidth: CGFloat) -> CGFloat {
        if index < activeSlideIndex {
            return totalWidth
        } else if index == activeSlideIndex {
            return totalWidth * CGFloat(slideProgress)
        } else {
            return 0
        }
    }

    private func sendStoryReply(_ text: String) async {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let userId = liveStory?.userId, !userId.isEmpty else { return }
        guard let data = try? await API.data("chats/dm", method: "POST", json: ["contact_id": userId]),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let convID = json["conversation_id"] as? String, !convID.isEmpty else { return }
        let item = liveStory?.stories.indices.contains(activeSlideIndex) == true ? liveStory?.stories[activeSlideIndex] : nil
        let storyId = (item?.id ?? "").replacingOccurrences(of: "story_", with: "").components(separatedBy: "|").first ?? ""
        let bareUser = userId.replacingOccurrences(of: "user_", with: "")
        let replyId = bareUser.isEmpty ? "story_\(storyId)" : "story_\(storyId)|\(bareUser)"
        let rawCaption = item?.content ?? ""
        let caption = (rawCaption.hasPrefix("http") || rawCaption.hasPrefix("uploads/") || rawCaption.hasPrefix("/")) ? "Story" : rawCaption
        let summary = caption.isEmpty ? "Story" : caption
        var replyText = "[Story] \(summary)"
        if let media = item?.contentUrl, !media.isEmpty {
            replyText += " [mediaUrl:\(media)]"
        }
        let author = liveStory?.userName ?? ""
        let wire = MessageMarkers.storyWire(text: trimmed, replyToId: replyId, replyToText: replyText, author: author)
        _ = try? await API.data("conversations/\(convID)/messages", method: "POST", json: [
            "client_uuid": UUID().uuidString,
            "message_text": wire,
            "media_type": "TEXT",
            "reply_to_id": replyId,
            "reply_to_text": replyText,
            "reply_to_sender_name": author
        ])
    }

    private func sendEmojiReaction(_ emoji: String) {
        withAnimation(.spring()) {
            sentReactionEmoji = emoji
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            withAnimation { sentReactionEmoji = nil }
        }
    }

    private var storyAvatar: some View {
        Group {
            if let raw = liveStory?.userAvatarUrl, let url = FeedPost.resolve(raw) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.white.opacity(0.2)
                }
            } else {
                Text(String(displayGroup.username.prefix(1)).uppercased())
                    .font(.caption.bold())
                    .foregroundColor(.white)
            }
        }
        .frame(width: 38, height: 38)
        .background(Color.white.opacity(0.2))
        .clipShape(Circle())
    }

    private var viewersSheetView: some View {
        NavigationStack {
            List {
                if viewers.isEmpty {
                    Text(viewersNote.isEmpty ? "No views yet" : viewersNote)
                        .foregroundColor(.secondary)
                } else {
                    ForEach(viewers) { viewer in
                        HStack(spacing: 12) {
                            if let raw = viewer.avatarUrl, let url = FeedPost.resolve(raw) {
                                AsyncImage(url: url) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Color.gray.opacity(0.2)
                                }
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(viewer.userName)
                                    .font(.subheadline.bold())
                                if !viewer.viewedLabel.isEmpty {
                                    Text(viewer.viewedLabel)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Viewers")
            .navigationBarItems(trailing: Button("Done") { showViewersSheet = false })
            .task {
                guard let item = liveStory?.stories.indices.contains(activeSlideIndex) == true ? liveStory?.stories[activeSlideIndex] : nil else { return }
                do {
                    viewers = try await StoryRepository.shared.getStoryViewers(storyId: item.id)
                } catch {
                    viewersNote = error.localizedDescription
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
