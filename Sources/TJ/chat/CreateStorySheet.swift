import SwiftUI

struct CreateStorySheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatViewModel: ChatViewModel
    @State private var storyText: String = ""

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                ZStack {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(LinearGradient(colors: [AppColors.brandPrimary, AppColors.brandAccent], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(height: 300)

                    Text(storyText.isEmpty ? "Tap to add text..." : storyText)
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding()
                }

                TextField("Add a caption...", text: $storyText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                Spacer()
            }
            .padding()
            .navigationTitle("Add to Story")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Share") {
                        let newStory = StoryItem(
                            id: UUID().uuidString,
                            userName: "Your Story",
                            avatarUrl: "https://i.pravatar.cc/150?u=usr_1",
                            isLive: false,
                            hasUnseenStory: true
                        )
                        chatViewModel.stories.insert(newStory, at: 0)
                        dismiss()
                    }
                    .font(.headline)
                }
            }
        }
    }
}
