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

    private let liveStory: UserStory?
    private let sampleStoryGroup = UserStoryGroup(
        id: "g1",
        username: "alex_morgan",
        avatarGradient: [.blue, .purple],
        timeAgo: "2 hours ago",
        slides: [
            StorySlide(id: "s1", mediaSymbol: "mountain.2.fill", caption: "Sunrise hike up Mt. Tamalpais! 🌄🥾", backgroundGradient: [.indigo, .purple]),
            StorySlide(id: "s2", mediaSymbol: "cup.and.saucer.fill", caption: "Espresso & SwiftUI coding session ☕📱", backgroundGradient: [.brown, .orange]),
            StorySlide(id: "s3", mediaSymbol: "sparkles", caption: "Exciting new TJ app release candidate is live! 🚀", backgroundGradient: [.blue, .cyan])
        ]
    )

    private let quickEmojis = ["❤️", "🔥", "😂", "👏", "😮", "🎉"]

    private let timer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()

    public init(onClose: (() -> Void)? = nil) {
        self.liveStory = nil
        self.onClose = onClose
    }

    public init(story: UserStory, onClose: (() -> Void)? = nil) {
        self.liveStory = story
        self.onClose = onClose
    }

    /// Kept so older call sites can open a real story group.
    public init(sampleStoryGroup: UserStory, onClose: (() -> Void)? = nil) {
        self.liveStory = sampleStoryGroup
        self.onClose = onClose
    }

    private var displayGroup: UserStoryGroup {
        guard let liveStory else { return sampleStoryGroup }
        let slides = liveStory.stories.map { item in
            StorySlide(
                id: item.id,
                mediaSymbol: item.type == .video ? "video.fill" : (item.type == .image ? "photo.fill" : "text.alignleft"),
                caption: item.content.isEmpty ? nil : item.content,
                backgroundGradient: [item.backgroundColor, .black],
                durationSeconds: max(1, Double(item.durationMs) / 1000.0),
                mediaURL: item.contentUrl
            )
        }
        return UserStoryGroup(
            id: liveStory.id,
            username: liveStory.userName,
            avatarGradient: [.blue, .purple],
            timeAgo: liveStory.lastUpdated,
            slides: slides.isEmpty ? [StorySlide(id: liveStory.id, mediaSymbol: "text.alignleft", caption: "No story", backgroundGradient: [.gray, .black])] : slides
        )
    }

    private var currentSlide: StorySlide {
        let slides = displayGroup.slides
        return slides[min(activeSlideIndex, max(slides.count - 1, 0))]
    }

    public var body: some View {
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
                        Circle()
                            .fill(LinearGradient(colors: displayGroup.avatarGradient, startPoint: .top, endPoint: .bottom))
                            .frame(width: 38, height: 38)
                            .overlay(Image(systemName: "person.fill").foregroundColor(.white).font(.caption))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(displayGroup.username)
                                .font(.subheadline.bold())
                                .foregroundColor(.white)

                            Text(displayGroup.timeAgo)
                                .font(.caption2)
                                .foregroundColor(.white.opacity(0.8))
                        }

                        Spacer()

                        Button(action: { showViewersSheet = true }) {
                            HStack(spacing: 4) {
                                Image(systemName: "eye.fill")
                                Text("24")
                            }
                            .font(.caption.bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.black.opacity(0.3))
                            .cornerRadius(12)
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

                // Quick Reply & Emoji Reactions Bar
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
        .onReceive(timer) { _ in
            guard !isPaused else { return }
            let step = 0.1 / currentSlide.durationSeconds
            if slideProgress + step >= 1.0 {
                goToNextSlide()
            } else {
                slideProgress += step
            }
        }
        .sheet(isPresented: $showViewersSheet) {
            viewersSheetView
        }
        .onAppear {
            markCurrentViewed()
            loadSlideMedia()
        }
        .onChange(of: activeSlideIndex) { _ in
            markCurrentViewed()
            loadSlideMedia()
        }
        .onDisappear {
            storyPlayer?.pause()
            storyPlayer = nil
        }
    }

    private func loadSlideMedia() {
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
            _ = try? await StoryRepository.shared.markStoryAsViewed(userId: liveStory.userId, storyId: item.id)
        }
    }

    // MARK: - Navigation Logic

    private func goToNextSlide() {
        if activeSlideIndex < displayGroup.slides.count - 1 {
            activeSlideIndex += 1
            slideProgress = 0.0
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
        _ = try? await API.data("conversations/\(convID)/messages", method: "POST", json: [
            "message_text": trimmed,
            "message_type": "text"
        ])
    }

    private func sendEmojiReaction(_ emoji: String) {
        withAnimation(.spring()) {
            sentReactionEmoji = emoji
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            withAnimation {
                sentReactionEmoji = nil
            }
        }
    }

    private var viewersSheetView: some View {
        NavigationStack {
            List {
                Section(header: Text("STORY VIEWERS (24)")) {
                    ForEach(1...8, id: \.self) { i in
                        HStack(spacing: 12) {
                            Circle()
                                .fill(Color.blue.opacity(0.2))
                                .frame(width: 40, height: 40)
                                .overlay(Text("U\(i)").font(.caption.bold()))

                            VStack(alignment: .leading, spacing: 2) {
                                Text("Viewer User #\(i)")
                                    .font(.subheadline.bold())
                                Text("@viewer_\(i)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(i * 5)m ago")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Viewers")
            .navigationBarItems(trailing: Button("Done") { showViewersSheet = false })
        }
        .presentationDetents([.medium, .large])
    }
}

#Preview {
    StoryViewer()
}
