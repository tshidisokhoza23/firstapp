import SwiftUI

struct RewardedAdModalView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var adManager = AdManager.shared
    @State private var countdownSeconds: Int = 3
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)

            VStack(spacing: 24) {
                HStack {
                    Text("Rewarded Ad")
                        .font(.headline)
                        .foregroundColor(.white)
                    Spacer()
                    if countdownSeconds == 0 {
                        Button("Close X") {
                            adManager.userPoints += 50
                            dismiss()
                        }
                        .font(.headline)
                        .foregroundColor(.yellow)
                    } else {
                        Text("Reward in \(countdownSeconds)s")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                .padding()

                Spacer()

                VStack(spacing: 16) {
                    Image(systemName: "play.tv.fill")
                        .font(.system(size: 80))
                        .foregroundColor(AppColors.brandAccent)

                    Text("Watch Video Ad & Earn Points")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Get +50 TJ points instantly when the ad finishes.")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Spacer()

                ProgressView(value: Double(3 - countdownSeconds), total: 3.0)
                    .tint(AppColors.brandAccent)
                    .padding()
            }
        }
        .onReceive(timer) { _ in
            if countdownSeconds > 0 {
                countdownSeconds -= 1
            }
        }
    }
}
