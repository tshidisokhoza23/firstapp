import SwiftUI

public struct StoriesHeaderRow: View {
    public let stories: [UserStory]
    public var currentUserId: String? = nil
    public var onStoryClick: (UserStory) -> Void = { _ in }
    public var onMediaStoryClick: () -> Void = {}
    public var onTextStoryClick: () -> Void = {}
    public var onPostedStoriesClick: () -> Void = {}

    public init(
        stories: [UserStory],
        currentUserId: String? = nil,
        onStoryClick: @escaping (UserStory) -> Void = { _ in },
        onMediaStoryClick: @escaping () -> Void = {},
        onTextStoryClick: @escaping () -> Void = {},
        onPostedStoriesClick: @escaping () -> Void = {}
    ) {
        self.stories = stories
        self.currentUserId = currentUserId
        self.onStoryClick = onStoryClick
        self.onMediaStoryClick = onMediaStoryClick
        self.onTextStoryClick = onTextStoryClick
        self.onPostedStoriesClick = onPostedStoriesClick
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                // My Story button
                VStack(spacing: 6) {
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(Color.gray.opacity(0.15))
                            .frame(width: 64, height: 64)
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.accentColor)

                        Circle()
                            .fill(Color.accentColor)
                            .frame(width: 20, height: 20)
                            .overlay(
                                Image(systemName: "plus")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white)
                            )
                    }

                    Text("My Story")
                        .font(.system(size: 11, weight: .bold))
                }
                .onTapGesture {
                    onMediaStoryClick()
                }

                // Other user stories
                ForEach(stories) { story in
                    VStack(spacing: 6) {
                        ZStack {
                            Circle()
                                .stroke(story.allViewed ? Color.gray.opacity(0.4) : Color.accentColor, lineWidth: 2)
                                .frame(width: 68, height: 68)

                            if let urlString = story.userAvatarUrl, let url = URL(string: urlString) {
                                AsyncImage(url: url) { phase in
                                    if let image = phase.image {
                                        image.resizable().scaledToFill()
                                    } else {
                                        storyInitials(story.userName)
                                    }
                                }
                                .frame(width: 62, height: 62)
                                .clipShape(Circle())
                            } else {
                                storyInitials(story.userName)
                            }
                        }

                        Text(story.userName.components(separatedBy: " ").first ?? story.userName)
                            .font(.system(size: 11, weight: story.allViewed ? .regular : .bold))
                            .lineLimit(1)
                            .frame(width: 64)
                    }
                    .onTapGesture {
                        onStoryClick(story)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private func storyInitials(_ name: String) -> some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.2))
                .frame(width: 62, height: 62)
            Text(String(name.prefix(1)).uppercased())
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.accentColor)
        }
    }
}
