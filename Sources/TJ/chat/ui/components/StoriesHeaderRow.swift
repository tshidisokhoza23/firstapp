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

    private var myStory: UserStory? {
        AndroidStoryFlow.myStory(in: stories, userId: currentUserId)
    }

    private var otherStories: [UserStory] {
        let mine = myStory
        return stories.filter { story in
            story.id != mine?.id && story.userName != "My Story" && story.userName != "My Number"
        }
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                if let myStory, !myStory.stories.isEmpty {
                    myStoryItem(myStory)
                } else {
                    addStoryItem
                }
                ForEach(otherStories) { story in
                    storyItem(story)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private var addStoryItem: some View {
        Menu {
            storyMenu
        } label: {
            VStack(spacing: 6) {
                ZStack {
                    Circle()
                        .stroke(Color.accentColor, lineWidth: 2)
                        .frame(width: 64, height: 64)
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.accentColor)
                }
                Text("Add Story")
                    .font(.system(size: 11, weight: .bold))
            }
        }
    }

    private func myStoryItem(_ story: UserStory) -> some View {
        VStack(spacing: 6) {
            ZStack(alignment: .bottomTrailing) {
                storyRing(story)
                    .onTapGesture { onStoryClick(story) }
                Menu {
                    storyMenu
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 20, height: 20)
                        .background(Color.accentColor)
                        .clipShape(Circle())
                }
            }
            Text("My Story")
                .font(.system(size: 11, weight: .bold))
        }
    }

    private func storyItem(_ story: UserStory) -> some View {
        VStack(spacing: 6) {
            storyRing(story)
            Text(story.userName.components(separatedBy: " ").first ?? story.userName)
                .font(.system(size: 11, weight: story.allViewed ? .regular : .bold))
                .lineLimit(1)
                .frame(width: 64)
        }
        .onTapGesture { onStoryClick(story) }
    }

    private func storyRing(_ story: UserStory) -> some View {
        ZStack {
            Circle()
                .stroke(story.allViewed ? Color.gray.opacity(0.4) : Color.accentColor, lineWidth: 2)
                .frame(width: 68, height: 68)
            if let latest = story.stories.last, let raw = latest.contentUrl, let url = FeedPost.resolve(raw) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    storyInitials(story.userName)
                }
                .frame(width: 62, height: 62)
                .clipShape(Circle())
            } else {
                storyInitials(story.userName)
            }
        }
    }

    @ViewBuilder
    private var storyMenu: some View {
        Button("Media story", action: onMediaStoryClick)
        Button("Text story", action: onTextStoryClick)
        Button("Posted stories", action: onPostedStoriesClick)
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
