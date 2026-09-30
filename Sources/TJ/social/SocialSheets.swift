import SwiftUI

struct CreatePostSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var socialViewModel: SocialViewModel
    @State private var textContent: String = ""

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                ZStack(alignment: .topLeading) {
                    if textContent.isEmpty {
                        Text("What's on your mind?")
                            .foregroundColor(AppColors.greyMedium)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 12)
                    }
                    TextEditor(text: $textContent)
                        .frame(height: 150)
                        .padding(4)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                }

                HStack {
                    Button(action: {}) {
                        Label("Photo/Video", systemImage: "photo.on.rectangle")
                            .font(.subheadline)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                    Spacer()
                }
                .padding(.horizontal)

                Spacer()
            }
            .padding()
            .navigationTitle("New Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Post") {
                        if !textContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            let newPost = SocialPost(
                                id: UUID().uuidString,
                                authorName: "Alex Morgan",
                                authorAvatar: "https://i.pravatar.cc/150?u=usr_1",
                                timestamp: "Just now",
                                textContent: textContent,
                                imageUrl: nil,
                                likesCount: 0,
                                commentsCount: 0,
                                isLiked: false
                            )
                            socialViewModel.posts.insert(newPost, at: 0)
                            dismiss()
                        }
                    }
                    .font(.headline)
                    .disabled(textContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

struct CommentsSheetView: View {
    let post: SocialPost
    @Environment(\.dismiss) var dismiss
    @State private var commentText: String = ""
    @State private var comments: [String] = ["Great post! 🔥", "Totally agree with this 💯"]

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                List {
                    ForEach(comments, id: \.self) { comment in
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "person.crop.circle.fill")
                                .font(.title2)
                                .foregroundColor(AppColors.greyMedium)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Community Member")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                Text(comment)
                                    .font(.subheadline)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                Divider()

                HStack(spacing: 12) {
                    TextField("Add a comment...", text: $commentText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(20)

                    Button(action: {
                        if !commentText.isEmpty {
                            comments.append(commentText)
                            commentText = ""
                        }
                    }) {
                        Image(systemName: "paperplane.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
                .padding()
            }
            .navigationTitle("Comments")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
