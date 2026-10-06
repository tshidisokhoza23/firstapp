import SwiftUI

struct UserProfileView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = ProfileViewModel()
    @State private var selectedTab: Int = 0
    @State private var showingHeaderMenu: Bool = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 16) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .frame(width: 80, height: 80)
                            .foregroundColor(AppColors.greyMedium)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Tshidiso Khoza")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text("@tshidisokhoza23")
                                .font(.subheadline)
                                .foregroundColor(AppColors.greyMedium)

                            HStack(spacing: 16) {
                                HStack(spacing: 4) {
                                    Text("\(viewModel.followersCount)")
                                        .fontWeight(.bold)
                                    Text("Followers")
                                        .foregroundColor(AppColors.greyMedium)
                                }
                                HStack(spacing: 4) {
                                    Text("340")
                                        .fontWeight(.bold)
                                    Text("Following")
                                        .foregroundColor(AppColors.greyMedium)
                                }
                            }
                            .font(.caption)
                            .padding(.top, 2)
                        }

                        Spacer()
                    }
                    .padding(.horizontal)

                    Button(action: { viewModel.toggleFollowState() }) {
                        Text(viewModel.isFollowing ? "Following" : "Follow")
                            .font(.headline)
                            .foregroundColor(viewModel.isFollowing ? .primary : .white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(viewModel.isFollowing ? AppColors.greyLight : AppColors.brandPrimary)
                            .clipShape(Capsule())
                    }
                    .padding(.horizontal)

                    Picker("Profile Segment", selection: $selectedTab) {
                        Text("Posts").tag(0)
                        Text("Replies").tag(1)
                        Text("Media").tag(2)
                        Text("Shorts").tag(3)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal)

                    if selectedTab == 0 {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Elon Musk must be shaking in his boots")
                                .font(.body)
                                .padding()
                                .background(Color(UIColor.secondarySystemBackground))
                                .cornerRadius(12)
                        }
                        .padding(.horizontal)
                    } else if selectedTab == 2 {
                        VStack(spacing: 16) {
                            Spacer().frame(height: 40)
                            Image(systemName: "photo.on.rectangle.angled")
                                .font(.system(size: 64))
                                .foregroundColor(AppColors.greyMedium)
                            Text("No media items yet")
                                .font(.headline)
                                .foregroundColor(AppColors.greyMedium)
                            Spacer().frame(height: 40)
                        }
                        .frame(maxWidth: .infinity)
                    } else if selectedTab == 3 {
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                            ForEach(1...6, id: \.self) { index in
                                Rectangle()
                                    .fill(LinearGradient(colors: [AppColors.brandPrimary.opacity(0.3), AppColors.brandAccent.opacity(0.15)], startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .aspectRatio(9/16, contentMode: .fill)
                                    .cornerRadius(12)
                                    .overlay(
                                        VStack {
                                            Spacer()
                                            HStack {
                                                Image(systemName: "play.fill")
                                                    .font(.caption2)
                                                    .foregroundColor(.white)
                                                Text("1.2K")
                                                    .font(.caption2)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(.white)
                                                Spacer()
                                            }
                                            .padding(6)
                                        }
                                    )
                            }
                        }
                        .padding(.horizontal)
                    } else {
                        Text("No replies yet")
                            .font(.subheadline)
                            .foregroundColor(AppColors.greyMedium)
                            .padding()
                    }
                }
                .padding(.top)
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.primary)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingHeaderMenu = true }) {
                        Image(systemName: "ellipsis")
                            .foregroundColor(.primary)
                    }
                }
            }
            .confirmationDialog("Profile Options", isPresented: $showingHeaderMenu, titleVisibility: .visible) {
                Button("Share Profile") { }
                Button("Copy Profile Link") { }
                Button("Block User", role: .destructive) { }
                Button("Report Profile", role: .destructive) { }
            }
        }
    }
}
