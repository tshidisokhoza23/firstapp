import SwiftUI

struct WalletView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var adManager = AdManager.shared

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Balance Card
                    ZStack {
                        RoundedRectangle(cornerRadius: 24)
                            .fill(LinearGradient(colors: [Color.orange, Color.red], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(height: 180)
                            .shadow(radius: 6)

                        VStack(spacing: 8) {
                            Text("TJ Digital Wallet")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.8))
                            Text("\(adManager.userPoints) Points")
                                .font(.system(size: 36, weight: .bold))
                                .foregroundColor(.white)
                            Text("Equivalent value: $\(Double(adManager.userPoints) * 0.02, specifier: "%.2f") USD")
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.9))
                        }
                    }
                    .padding(.horizontal)

                    HStack(spacing: 16) {
                        Button(action: {}) {
                            VStack(spacing: 6) {
                                Image(systemName: "arrow.up.right.circle.fill")
                                    .font(.title2)
                                Text("Send")
                                    .font(.caption)
                            }
                            .foregroundColor(AppColors.brandPrimary)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                        }
                        Button(action: {}) {
                            VStack(spacing: 6) {
                                Image(systemName: "arrow.down.left.circle.fill")
                                    .font(.title2)
                                Text("Receive")
                                    .font(.caption)
                            }
                            .foregroundColor(AppColors.brandPrimary)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)

                    List {
                        Section(header: Text("Transaction History")) {
                            HStack {
                                Image(systemName: "gift.fill")
                                    .foregroundColor(.green)
                                VStack(alignment: .leading) {
                                    Text("Daily Reward Bonus")
                                        .font(.headline)
                                    Text("Today • Earned")
                                        .font(.caption)
                                        .foregroundColor(AppColors.greyMedium)
                                }
                                Spacer()
                                Text("+50 pts")
                                    .font(.headline)
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .frame(height: 200)
                }
                .padding(.top)
            }
            .navigationTitle("Wallet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
