import SwiftUI

struct ShortsView: View {
    @EnvironmentObject var shortsViewModel: ShortsViewModel
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var showingComments: Bool = false
    @State private var showingOptionsSheet: Bool = false

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .bottomTrailing) {
                // Background Video Placeholder Container
                Color.black
                    .edgesIgnoringSafeArea(.all)

                VStack {
                    Spacer()
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 72))
                        .foregroundColor(.white.opacity(0.8))
                    Text("Reels & Shorts Feed")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Spacer()
                }

                // Bottom Caption & Creator Info
                if shortsViewModel.currentVideoIndex < shortsViewModel.shortVideos.count {
                    let video = shortsViewModel.shortVideos[shortsViewModel.currentVideoIndex]

                    HStack(alignment: .bottom) {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack(spacing: 8) {
                                Image(systemName: "person.crop.circle.fill")
                                    .resizable()
                                    .frame(width: 36, height: 36)
                                    .foregroundColor(.white)
                                Text(video.creatorName)
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                Button("Follow") { }
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .overlay(Capsule().stroke(Color.white, lineWidth: 1))
                            }

                            Text(video.videoTitle)
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            HStack(spacing: 6) {
                                Image(systemName: "music.note")
                                    .foregroundColor(.white)
                                Text(video.audioTrack)
                                    .font(.system(size: 12))
                                    .foregroundColor(.white)
                            }
                        }
                        .padding()

                        Spacer()

                        // Right Side Action Bar
                        VStack(spacing: 20) {
                            Button(action: {
                                shortsViewModel.toggleLike(for: video)
                            }) {
                                VStack(spacing: 4) {
                                    Image(systemName: video.isLiked ? "heart.fill" : "heart")
                                        .font(.title)
                                        .foregroundColor(video.isLiked ? .red : .white)
                                    Text("\(video.likesCount)")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }

                            Button(action: { showingComments = true }) {
                                VStack(spacing: 4) {
                                    Image(systemName: "bubble.right.fill")
                                        .font(.title)
                                        .foregroundColor(.white)
                                    Text("\(video.commentsCount)")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }

                            Button(action: { showingOptionsSheet = true }) {
                                Image(systemName: "ellipsis")
                                    .font(.title)
                                    .foregroundColor(.white)
                            }

                            // Next Video Button
                            Button(action: {
                                shortsViewModel.currentVideoIndex = (shortsViewModel.currentVideoIndex + 1) % shortsViewModel.shortVideos.count
                            }) {
                                Circle()
                                    .fill(AppColors.brandPrimary.opacity(0.8))
                                    .frame(width: 44, height: 44)
                                    .overlay(
                                        Image(systemName: "chevron.down")
                                            .font(.headline)
                                            .foregroundColor(.white)
                                    )
                            }
                        }
                        .padding(.trailing, 16)
                        .padding(.bottom, 90)
                    }
                }
            }
        }
        .sheet(isPresented: $showingComments) {
            if shortsViewModel.currentVideoIndex < shortsViewModel.shortVideos.count {
                CommentsSheetView(post: SocialPost(id: "v", authorName: "", authorAvatar: "", timestamp: "", textContent: "", imageUrl: nil, likesCount: 0, commentsCount: 0, isLiked: false))
            }
        }
        .sheet(isPresented: $showingOptionsSheet) {
            if shortsViewModel.currentVideoIndex < shortsViewModel.shortVideos.count {
                let video = shortsViewModel.shortVideos[shortsViewModel.currentVideoIndex]
                ShortOptionsBottomSheet(
                    video: video,
                    isOwner: video.creatorName == "@dev_vibes",
                    onMessageAuthor: {
                        let newChat = ChatItem(
                            id: UUID().uuidString,
                            name: video.creatorName,
                            lastMessage: "Hi! Saw your video: \(video.videoTitle)",
                            timestamp: "Just now",
                            unreadCount: 0,
                            avatarUrl: video.creatorAvatar,
                            isOnline: true
                        )
                        chatViewModel.chats.insert(newChat, at: 0)
                        chatViewModel.activeChat = newChat
                    },
                    onCopyLink: {},
                    onSaveToGallery: {},
                    onNotInterested: {
                        shortsViewModel.shortVideos.removeAll(where: { $0.id == video.id })
                    },
                    onBlockAuthor: {
                        shortsViewModel.shortVideos.removeAll(where: { $0.creatorName == video.creatorName })
                    },
                    onReport: {},
                    onDelete: {
                        shortsViewModel.shortVideos.removeAll(where: { $0.id == video.id })
                    }
                )
            }
        }
    }
}
