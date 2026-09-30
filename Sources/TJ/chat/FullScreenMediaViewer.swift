import SwiftUI

struct FullScreenMediaViewer: View {
    let mediaUrl: String
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.edgesIgnoringSafeArea(.all)

            VStack {
                Spacer()
                RoundedRectangle(cornerRadius: 16)
                    .fill(AppColors.brandPrimary.opacity(0.2))
                    .frame(height: 380)
                    .overlay(
                        VStack(spacing: 12) {
                            Image(systemName: "photo.fill")
                                .font(.system(size: 80))
                                .foregroundColor(.white)
                            Text("Full Resolution Media View")
                                .font(.headline)
                                .foregroundColor(.white)
                        }
                    )
                    .padding()
                Spacer()
            }

            Button(action: { dismiss() }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.white)
                    .padding()
            }
        }
    }
}
