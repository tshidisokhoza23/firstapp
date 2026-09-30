import SwiftUI

struct RewardsView: View {
    @State private var dailyBonusClaimed: Bool = false
    @State private var points: Int = 350

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "gift.fill")
                .font(.system(size: 64))
                .foregroundColor(AppColors.brandAccent)
            Text("TJ Rewards & Points")
                .font(.title2)
                .fontWeight(.bold)
            Text("Your Current Balance: \(points) pts")
                .font(.headline)
                .foregroundColor(AppColors.brandPrimary)
            Text("Earn points by staying active, chatting, and inviting friends!")
                .font(.subheadline)
                .foregroundColor(AppColors.greyMedium)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(action: {
                if !dailyBonusClaimed {
                    points += 50
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
            .padding(.horizontal, 32)

            Spacer()
        }
    }
}
