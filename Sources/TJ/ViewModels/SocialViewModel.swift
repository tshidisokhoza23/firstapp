import SwiftUI
import Combine

class SocialViewModel: ObservableObject {
    @Published var posts: [SocialPost] = SocialPost.samples

    init() {
        Task { @MainActor in
            let remotePosts = await NetworkClient.shared.fetchSocialPosts()
            if !remotePosts.isEmpty {
                self.posts = remotePosts
            }
        }
    }

    func toggleLike(for post: SocialPost) {
        if let index = posts.firstIndex(where: { $0.id == post.id }) {
            posts[index].isLiked.toggle()
            posts[index].likesCount += posts[index].isLiked ? 1 : -1
        }
    }
}
