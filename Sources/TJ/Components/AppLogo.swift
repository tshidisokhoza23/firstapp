import SwiftUI

struct AppLogo: View {
    var size: CGFloat = 36

    var body: some View {
        HStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppColors.brandPrimary, AppColors.brandAccent],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: size, height: size)

                Image(systemName: "sparkles")
                    .font(.system(size: size * 0.5, weight: .bold))
                    .foregroundColor(.white)
            }

            Text("TJ")
                .font(.system(size: size * 0.75, weight: .black, design: .rounded))
                .foregroundStyle(
                    LinearGradient(
                        colors: [AppColors.brandPrimary, AppColors.brandAccent],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
        }
    }
}
