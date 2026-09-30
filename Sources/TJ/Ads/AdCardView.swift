import SwiftUI

struct AdCardView: View {
    let ad: AdItem

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Sponsor Header
            HStack(spacing: 8) {
                Image(systemName: ad.sponsorLogo)
                    .font(.title3)
                    .foregroundColor(AppColors.brandPrimary)
                    .frame(width: 32, height: 32)
                    .background(AppColors.brandPrimary.opacity(0.1))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(ad.sponsorName)
                            .font(.system(size: 14, weight: .bold))
                        Text("Sponsored")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.orange)
                            .cornerRadius(4)
                    }
                    Text("Ad • Promoted Content")
                        .font(.system(size: 11))
                        .foregroundColor(AppColors.greyMedium)
                }

                Spacer()
            }

            Text(ad.headline)
                .font(.system(size: 15, weight: .semibold))

            Text(ad.bodyText)
                .font(.system(size: 13))
                .foregroundColor(AppColors.greyMedium)

            // Image Banner
            Rectangle()
                .fill(LinearGradient(colors: [AppColors.brandPrimary, AppColors.brandAccent], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 180)
                .cornerRadius(12)
                .overlay(
                    VStack(spacing: 8) {
                        Image(systemName: "sparkles.tv.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                        Text("Sponsored Ad Banner")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                )

            // CTA Button
            Button(action: {
                if let url = URL(string: ad.destinationUrl) {
                    UIApplication.shared.open(url)
                }
            }) {
                HStack {
                    Text(ad.ctaText)
                        .font(.system(size: 14, weight: .bold))
                    Spacer()
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 14, weight: .bold))
                }
                .foregroundColor(.white)
                .padding()
                .background(AppColors.brandPrimary)
                .cornerRadius(12)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1.5)
        )
    }
}
