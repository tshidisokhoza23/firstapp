import SwiftUI

// MARK: - Story Item Models

public struct StorySlide: Identifiable, Hashable {
    public let id: String
    public let mediaSymbol: String
    public let caption: String?
    public let backgroundGradient: [Color]
    public let durationSeconds: Double

    public init(id: String, mediaSymbol: String, caption: String? = nil, backgroundGradient: [Color], durationSeconds: Double = 5.0) {
        self.id = id
        self.mediaSymbol = mediaSymbol
        self.caption = caption
        self.backgroundGradient = backgroundGradient
        self.durationSeconds = durationSeconds
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
    @State private var showViewersSheet: Bool = false
    @State private var sentReactionEmoji: String? = nil

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
        self.onClose = onClose
    }

    private var currentSlide: StorySlide {
        sampleStoryGroup.slides[min(activeSlideIndex, sampleStoryGroup.slides.count - 1)]
    }

    public var body: some View {
        ZStack {
            // Full Screen Background & Media Content
            LinearGradient(colors: currentSlide.backgroundGradient, startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()

            VStack {
                Spacer()

                Image(systemName: currentSlide.mediaSymbol)
                    .font(.system(size: 130))
                    .foregroundColor(.white.opacity(0.85))
                    .shadow(radius: 12)

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
                        ForEach(0..<sampleStoryGroup.slides.count, id: \.self) { idx in
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
                            .fill(LinearGradient(colors: sampleStoryGroup.avatarGradient, startPoint: .top, endPoint: .bottom))
                            .frame(width: 38, height: 38)
                            .overlay(Image(systemName: "person.fill").foregroundColor(.white).font(.caption))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(sampleStoryGroup.username)
                                .font(.subheadline.bold())
                                .foregroundColor(.white)

                            Text(sampleStoryGroup.timeAgo)
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
                        TextField("Send reply to @\(sampleStoryGroup.username)...", text: $replyText)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.35))
                            .cornerRadius(20)
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.3), lineWidth: 1))

                        if !replyText.isEmpty {
                            Button(action: {
                                replyText = ""
                                sendEmojiReaction("📩")
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
    }

    // MARK: - Navigation Logic

    private func goToNextSlide() {
        if activeSlideIndex < sampleStoryGroup.slides.count - 1 {
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
