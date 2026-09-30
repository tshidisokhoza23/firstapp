import SwiftUI

struct SocialView: View {
    @EnvironmentObject var socialViewModel: SocialViewModel
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var showingCreatePost: Bool = false
    @State private var selectedPostForComments: SocialPost? = nil

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("Social Feed")
                        .font(.title2)
                        .fontWeight(.bold)
                    Spacer()
                    Button(action: { showingCreatePost = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 12)

                ScrollView {
                    VStack(spacing: 16) {
                        // Stories Row
                        StoriesRowView(stories: chatViewModel.stories)

                        Divider()

                        // Feed Posts + Interleaved Sponsored Ad Cards
                        ForEach(Array(socialViewModel.posts.enumerated()), id: \.element.id) { index, post in
                            VStack(spacing: 16) {
                                // User Social Post
                                VStack(alignment: .leading, spacing: 12) {
                                    HStack(spacing: 10) {
                                        Image(systemName: "person.crop.circle.fill")
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 40, height: 40)
                                            .foregroundColor(AppColors.greyMedium)

                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(post.authorName)
                                                .font(.system(size: 15, weight: .semibold))
                                            Text(post.timestamp)
                                                .font(.system(size: 12))
                                                .foregroundColor(AppColors.greyMedium)
                                        }
                                        Spacer()
                                        Button(action: {}) {
                                            Image(systemName: "ellipsis")
                                                .foregroundColor(AppColors.greyMedium)
                                        }
                                    }

                                    Text(post.textContent)
                                        .font(.system(size: 15))

                                    if let _ = post.imageUrl {
                                        Rectangle()
                                            .fill(AppColors.brandPrimary.opacity(0.1))
                                            .frame(height: 220)
                                            .cornerRadius(12)
                                            .overlay(
                                                Image(systemName: "photo.fill")
                                                    .font(.largeTitle)
                                                    .foregroundColor(AppColors.brandPrimary)
                                            )
                                    }

                                    HStack(spacing: 24) {
                                        Button(action: {
                                            socialViewModel.toggleLike(for: post)
                                        }) {
                                            HStack(spacing: 6) {
                                                Image(systemName: post.isLiked ? "heart.fill" : "heart")
                                                    .foregroundColor(post.isLiked ? .red : .primary)
                                                Text("\(post.likesCount)")
                                                    .font(.system(size: 13, weight: .medium))
                                            }
                                        }

                                        Button(action: {
                                            selectedPostForComments = post
                                        }) {
                                            HStack(spacing: 6) {
                                                Image(systemName: "bubble.left")
                                                Text("\(post.commentsCount)")
                                                    .font(.system(size: 13, weight: .medium))
                                            }
                                        }

                                        Button(action: {}) {
                                            Image(systemName: "arrow.2.squarepath")
                                        }

                                        Spacer()

                                        Button(action: {}) {
                                            Image(systemName: "paperplane")
                                        }
                                    }
                                    .foregroundColor(.primary)
                                    .padding(.top, 4)
                                }
                                .padding()
                                .background(Color(UIColor.secondarySystemBackground))
                                .cornerRadius(16)
                                .padding(.horizontal)

                                // Interleaved Native Sponsored Ad Card every 2 posts
                                if index % 2 == 0, let ad = AdItem.samples.first {
                                    AdCardView(ad: ad)
                                        .padding(.horizontal)
                                }
                            }
                        }
                    }
                    .padding(.bottom, 80)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showingCreatePost) {
                CreatePostSheet()
            }
            .sheet(item: $selectedPostForComments) { post in
                CommentsSheetView(post: post)
            }
        }
    }
}
