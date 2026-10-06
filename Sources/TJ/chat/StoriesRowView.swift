import SwiftUI

struct StoriesRowView: View {
    let stories: [StoryItem]
    @State private var selectedStoryForViewer: StoryItem? = nil

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                ForEach(stories) { story in
                    Button(action: {
                        selectedStoryForViewer = story
                    }) {
                        VStack(spacing: 6) {
                            ZStack(alignment: .bottomTrailing) {
                                ZStack {
                                    Circle()
                                        .stroke(
                                            story.hasUnseenStory ?
                                            LinearGradient(colors: [AppColors.brandPrimary, AppColors.brandAccent], startPoint: .topLeading, endPoint: .bottomTrailing) :
                                                LinearGradient(colors: [AppColors.greyMedium.opacity(0.3)], startPoint: .center, endPoint: .center),
                                            lineWidth: 2.5
                                        )
                                        .frame(width: 64, height: 64)

                                    Image(systemName: "person.crop.circle.fill")
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 56, height: 56)
                                        .foregroundColor(AppColors.greyMedium)
                                        .clipShape(Circle())
                                }

                                if story.isLive {
                                    Text("LIVE")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 4)
                                        .padding(.vertical, 2)
                                        .background(Color.red)
                                        .cornerRadius(4)
                                }
                            }

                            Text(story.userName)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.primary)
                                .lineLimit(1)
                                .frame(width: 68)
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .fullScreenCover(item: $selectedStoryForViewer) { story in
            StoryViewerSheet(story: story)
        }
    }
}
