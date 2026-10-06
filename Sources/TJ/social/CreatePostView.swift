import SwiftUI

enum ReplyAudience: String, CaseIterable, Identifiable {
    case everyone = "Everyone can reply"
    case following = "People you follow"
    case mentioned = "Only mentioned accounts"

    var id: String { rawValue }

    var iconName: String {
        switch self {
        case .everyone: return "globe"
        case .following: return "person.2.fill"
        case .mentioned: return "at"
        }
    }
}

struct CreatePostView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var socialViewModel: SocialViewModel

    @State private var postText: String = ""
    @State private var selectedAudience: ReplyAudience = .everyone
    @State private var showAudienceSheet: Bool = false
    @State private var attachedImages: [String] = []

    private let maxCharacters = 280

    private var remainingCharacters: Int {
        maxCharacters - postText.count
    }

    private var isValidPost: Bool {
        let trimmed = postText.trimmingCharacters(in: .whitespacesAndNewlines)
        return (!trimmed.isEmpty || !attachedImages.isEmpty) && remainingCharacters >= 0
    }

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .frame(width: 44, height: 44)
                        .foregroundColor(AppColors.greyMedium)

                    TextEditor(text: $postText)
                        .frame(height: 140)
                        .font(.body)
                        .overlay(
                            Group {
                                if postText.isEmpty {
                                    Text("What's happening?")
                                        .foregroundColor(AppColors.greyMedium)
                                        .padding(.leading, 4)
                                        .padding(.top, 8)
                                }
                            },
                            alignment: .topLeading
                        )
                }
                .padding(.horizontal)

                if !attachedImages.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(attachedImages.indices, id: \.self) { idx in
                                ZStack(alignment: .topTrailing) {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(AppColors.brandPrimary.opacity(0.15))
                                        .frame(width: 90, height: 90)
                                        .overlay(
                                            Image(systemName: "photo.fill")
                                                .foregroundColor(AppColors.brandPrimary)
                                        )

                                    Button(action: { attachedImages.remove(at: idx) }) {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.white)
                                            .background(Circle().fill(Color.black.opacity(0.6)))
                                    }
                                    .padding(4)
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()

                Divider()

                Button(action: { showAudienceSheet = true }) {
                    HStack(spacing: 6) {
                        Image(systemName: selectedAudience.iconName)
                        Text(selectedAudience.rawValue)
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    .foregroundColor(AppColors.brandPrimary)
                    .padding(.horizontal)
                }

                HStack(spacing: 20) {
                    Button(action: {
                        attachedImages.append("sample_photo_\(attachedImages.count + 1)")
                    }) {
                        Image(systemName: "photo.on.rectangle.angled")
                            .font(.system(size: 20))
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {}) {
                        Image(systemName: "chart.bar.fill")
                            .font(.system(size: 20))
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {}) {
                        Image(systemName: "location.fill")
                            .font(.system(size: 20))
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Spacer()

                    Text("\(remainingCharacters)")
                        .font(.caption)
                        .foregroundColor(remainingCharacters < 0 ? .red : AppColors.greyMedium)

                    Button(action: submitPost) {
                        Text("Post")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                            .background(isValidPost ? AppColors.brandPrimary : AppColors.greyMedium.opacity(0.4))
                            .clipShape(Capsule())
                    }
                    .disabled(!isValidPost)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.primary)
                    }
                }
            }
            .confirmationDialog("Who can reply?", isPresented: $showAudienceSheet, titleVisibility: .visible) {
                ForEach(ReplyAudience.allCases) { audience in
                    Button(audience.rawValue) {
                        selectedAudience = audience
                    }
                }
            }
        }
    }

    private func submitPost() {
        let newPost = SocialPost(
            id: UUID().uuidString,
            authorName: "Alex Morgan",
            authorAvatar: "https://i.pravatar.cc/150?u=usr_1",
            timestamp: "Just now",
            textContent: postText,
            imageUrl: attachedImages.isEmpty ? nil : "attached_image",
            likesCount: 0,
            commentsCount: 0,
            isLiked: false
        )
        socialViewModel.posts.insert(newPost, at: 0)
        dismiss()
    }
}
