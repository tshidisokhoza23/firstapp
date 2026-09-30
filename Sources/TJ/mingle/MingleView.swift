import SwiftUI

struct MingleView: View {
    @EnvironmentObject var mingleViewModel: MingleViewModel
    @State private var showingRadiusSheet: Bool = false
    @State private var showingSecretChats: Bool = false
    @State private var showingNotifications: Bool = false
    @State private var selectedDetailProfile: MingleProfileModel? = nil
    @State private var radiusKm: Double = 50.0

    var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                // Left Controls: Secret Chats, Notifications, Rewind
                HStack(spacing: 12) {
                    Button(action: { showingSecretChats = true }) {
                        Image(systemName: "envelope.fill")
                            .font(.title3)
                            .foregroundColor(.primary)
                    }
                    Button(action: { showingNotifications = true }) {
                        Image(systemName: "bell.fill")
                            .font(.title3)
                            .foregroundColor(.primary)
                    }
                    Button(action: {
                        if mingleViewModel.currentCardIndex > 0 {
                            mingleViewModel.currentCardIndex -= 1
                        }
                    }) {
                        Image(systemName: "arrow.uturn.backward.circle.fill")
                            .font(.title3)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }

                Spacer()

                // Center App Logo
                AppLogo(size: 28)

                Spacer()

                // Right Controls: Radius Filter Pill & Camera
                HStack(spacing: 12) {
                    Button(action: { showingRadiusSheet = true }) {
                        HStack(spacing: 4) {
                            Image(systemName: "location.fill")
                                .font(.caption)
                                .foregroundColor(AppColors.brandPrimary)
                            Text(radiusKm >= 100 ? "100+ km" : "\(Int(radiusKm)) km")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.primary)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                    }

                    Button(action: {}) {
                        Image(systemName: "camera.circle.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)

            // Profile Card Stack
            if mingleViewModel.currentCardIndex < mingleViewModel.profiles.count {
                let profile = mingleViewModel.profiles[mingleViewModel.currentCardIndex]

                ZStack(alignment: .bottom) {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(
                            LinearGradient(colors: [AppColors.brandPrimary.opacity(0.85), AppColors.darkBackgroundTop], startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.system(size: 120))
                                .foregroundColor(.white.opacity(0.2))
                        )
                        .shadow(radius: 8)
                        .onTapGesture {
                            selectedDetailProfile = profile
                        }

                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("\(profile.name), \(profile.age)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            Spacer()
                            Text(String(format: "%.1f km away", profile.distanceKm))
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.8))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.black.opacity(0.4))
                                .cornerRadius(8)
                        }

                        Text(profile.occupation)
                            .font(.headline)
                            .foregroundColor(AppColors.brandAccent)

                        Text(profile.bio)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.9))
                            .lineLimit(2)

                        HStack {
                            Text("Tap card to view full profile")
                                .font(.caption2)
                                .foregroundColor(.white.opacity(0.6))
                            Spacer()
                            Image(systemName: "info.circle")
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .padding(.top, 4)
                    }
                    .padding(24)
                    .background(
                        LinearGradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.85)], startPoint: .top, endPoint: .bottom)
                    )
                    .cornerRadius(24)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)

                // Bottom Floating Action Buttons
                HStack(spacing: 32) {
                    Button(action: { mingleViewModel.passCurrent() }) {
                        Image(systemName: "xmark")
                            .font(.title)
                            .foregroundColor(.red)
                            .frame(width: 60, height: 60)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }

                    Button(action: { mingleViewModel.likeCurrent() }) {
                        Image(systemName: "star.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                            .frame(width: 50, height: 50)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }

                    Button(action: { mingleViewModel.likeCurrent() }) {
                        Image(systemName: "heart.fill")
                            .font(.title)
                            .foregroundColor(.green)
                            .frame(width: 60, height: 60)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                }
                .padding(.vertical, 20)
                .padding(.bottom, 60)
            } else {
                Spacer()
                VStack(spacing: 16) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(AppColors.brandAccent)
                    Text("That's everyone for now!")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Check back later or expand your search radius.")
                        .font(.subheadline)
                        .foregroundColor(AppColors.greyMedium)

                    Button("Reset Candidates") {
                        mingleViewModel.currentCardIndex = 0
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.brandPrimary)
                }
                Spacer()
            }
        }
        .sheet(isPresented: $showingRadiusSheet) {
            MingleRadiusSheet(radiusKm: $radiusKm) { newRadius in
                radiusKm = newRadius
            }
        }
        .sheet(isPresented: $showingSecretChats) {
            MingleChatsView()
        }
        .sheet(isPresented: $showingNotifications) {
            MingleNotificationsView()
        }
        .sheet(item: $selectedDetailProfile) { profile in
            MingleProfileDetailView(
                profile: profile,
                onLike: { mingleViewModel.likeCurrent() },
                onPass: { mingleViewModel.passCurrent() },
                onSecretIntro: { showingSecretChats = true }
            )
        }
    }
}
