import SwiftUI

// MARK: - Radius Filter Sheet
struct MingleRadiusSheet: View {
    @Binding var radiusKm: Double
    @Environment(\.dismiss) var dismiss
    var onApply: (Double) -> Void

    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Text("Search Radius")
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                Button("Done") {
                    onApply(radiusKm)
                    dismiss()
                }
                .font(.headline)
                .foregroundColor(AppColors.brandPrimary)
            }
            .padding(.top)

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Maximum Distance:")
                        .font(.subheadline)
                        .foregroundColor(AppColors.greyMedium)
                    Spacer()
                    Text(radiusKm >= 100 ? "100+ km" : "\(Int(radiusKm)) km")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.brandPrimary)
                }

                Slider(value: $radiusKm, in: 1...100, step: 1)
                    .tint(AppColors.brandPrimary)
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16)

            Spacer()
        }
        .padding()
    }
}

// MARK: - Profile Detail Preview Sheet
struct MingleProfileDetailView: View {
    let profile: MingleProfileModel
    @Environment(\.dismiss) var dismiss
    var onLike: () -> Void
    var onPass: () -> Void
    var onSecretIntro: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                ZStack(alignment: .topTrailing) {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(LinearGradient(colors: [AppColors.brandPrimary, AppColors.darkBackgroundTop], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(height: 380)
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.system(size: 100))
                                .foregroundColor(.white.opacity(0.3))
                        )

                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(.white)
                            .padding()
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("\(profile.name), \(profile.age)")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                        Spacer()
                        Label(String(format: "%.1f km", profile.distanceKm), systemImage: "location.fill")
                            .font(.subheadline)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(AppColors.brandPrimary.opacity(0.1))
                            .foregroundColor(AppColors.brandPrimary)
                            .cornerRadius(12)
                    }

                    Text(profile.occupation)
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.brandAccent)

                    Divider()

                    Text("About Me")
                        .font(.headline)
                    Text(profile.bio)
                        .font(.body)
                        .foregroundColor(AppColors.greyMedium)

                    Divider()

                    Text("Passions & Interests")
                        .font(.headline)

                    HFlowStack(items: ["Travel ✈️", "Music 🎵", "Coding 💻", "Coffee ☕️", "Fitness 🏋️‍♀️"])

                    Spacer(minLength: 20)

                    // Action Bar
                    HStack(spacing: 20) {
                        Button(action: {
                            onPass()
                            dismiss()
                        }) {
                            Image(systemName: "xmark")
                                .font(.title)
                                .foregroundColor(.red)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(16)
                        }

                        Button(action: {
                            onSecretIntro()
                            dismiss()
                        }) {
                            Image(systemName: "envelope.fill")
                                .font(.title2)
                                .foregroundColor(.purple)
                                .frame(width: 60, height: 54)
                                .background(Color.purple.opacity(0.1))
                                .cornerRadius(16)
                        }

                        Button(action: {
                            onLike()
                            dismiss()
                        }) {
                            Image(systemName: "heart.fill")
                                .font(.title)
                                .foregroundColor(.green)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green.opacity(0.1))
                                .cornerRadius(16)
                        }
                    }
                }
                .padding()
            }
        }
    }
}

struct HFlowStack: View {
    let items: [String]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(items, id: \.self) { item in
                    Text(item)
                        .font(.system(size: 13, weight: .medium))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(AppColors.greyLight)
                        .cornerRadius(12)
                }
            }
        }
    }
}

// MARK: - Secret Chats View
struct MingleChatsView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                HStack(spacing: 12) {
                    Circle()
                        .fill(AppColors.brandAccent.opacity(0.2))
                        .frame(width: 50, height: 50)
                        .overlay(Image(systemName: "lock.fill").foregroundColor(AppColors.brandAccent))
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Secret Candidate Match")
                            .font(.headline)
                        Text("You both liked each other! Start chatting anonymously.")
                            .font(.caption)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
            }
            .navigationTitle("Mingle Secret Chats")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Secret Notifications View
struct MingleNotificationsView: View {
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            List {
                HStack(spacing: 12) {
                    Image(systemName: "heart.circle.fill")
                        .font(.title)
                        .foregroundColor(.pink)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Someone liked your profile!")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        Text("Upgrade or claim daily points to reveal who liked you.")
                            .font(.caption)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
            }
            .navigationTitle("Mingle Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
