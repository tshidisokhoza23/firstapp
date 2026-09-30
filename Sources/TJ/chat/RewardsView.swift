import SwiftUI

struct RewardsView: View {
    @ObservedObject var adManager = AdManager.shared
    @State private var dailyBonusClaimed: Bool = false
    @State private var showingRewardedAdModal: Bool = false

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "gift.fill")
                .font(.system(size: 64))
                .foregroundColor(AppColors.brandAccent)
            Text("TJ Rewards & Points")
                .font(.title2)
                .fontWeight(.bold)
            Text("Your Current Balance: \(adManager.userPoints) pts")
                .font(.headline)
                .foregroundColor(AppColors.brandPrimary)
            Text("Earn points by staying active, watching video ads, chatting, and inviting friends!")
                .font(.subheadline)
                .foregroundColor(AppColors.greyMedium)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(spacing: 12) {
                Button(action: {
                    if !dailyBonusClaimed {
                        adManager.userPoints += 50
                        dailyBonusClaimed = true
                    }
                }) {
                    Text(dailyBonusClaimed ? "Daily Bonus Claimed! (+50 pts)" : "Claim Daily Bonus (+50 pts)")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(dailyBonusClaimed ? Color.green : AppColors.brandPrimary)
                        .cornerRadius(16)
                }

                Button(action: {
                    showingRewardedAdModal = true
                }) {
                    HStack {
                        Image(systemName: "play.tv.fill")
                        Text("Watch Video Ad (+50 pts)")
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.orange)
                    .cornerRadius(16)
                }
            }
            .padding(.horizontal, 32)

            Spacer()
        }
        .fullScreenCover(isPresented: $showingRewardedAdModal) {
            RewardedAdModalView()
        }
    }
}
