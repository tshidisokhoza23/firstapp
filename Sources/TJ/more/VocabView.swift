import SwiftUI

struct VocabView: View {
    @Environment(\.dismiss) var dismiss
    @State private var streakDays: Int = 12

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                HStack {
                    Image(systemName: "flame.fill")
                        .font(.title)
                        .foregroundColor(.orange)
                    Text("\(streakDays) Day Learning Streak!")
                        .font(.headline)
                        .fontWeight(.bold)
                }
                .padding()
                .background(Color.orange.opacity(0.15))
                .cornerRadius(16)

                // Flashcard Placeholder
                ZStack {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(AppColors.brandPrimary.opacity(0.1))
                        .frame(height: 240)

                    VStack(spacing: 12) {
                        Text("Word of the Day")
                            .font(.caption)
                            .foregroundColor(AppColors.greyMedium)
                        Text("Resilient")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(AppColors.brandPrimary)
                        Text("adj. Able to withstand or recover quickly from difficult conditions.")
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
                .padding(.horizontal)

                Button("Mark as Learned ✓") {
                    streakDays += 1
                }
                .buttonStyle(.borderedProminent)
                .tint(AppColors.brandPrimary)

                Spacer()
            }
            .padding(.top)
            .navigationTitle("Vocab Builder")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
