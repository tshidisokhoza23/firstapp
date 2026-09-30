import SwiftUI

struct GifStickerPickerView: View {
    @Environment(\.dismiss) var dismiss
    var onSelectMedia: (String) -> Void
    @State private var selectedTab: Int = 0 // 0: Emojis, 1: GIFs, 2: Stickers

    let emojis = ["😂", "❤️", "🔥", "👍", "🚀", "🎉", "😍", "🙌", "💯", "🙏", "😎", "✨", "👏", "🥳", "🌟", "💡"]

    var body: some View {
        VStack(spacing: 12) {
            Capsule()
                .fill(AppColors.greyMedium)
                .frame(width: 40, height: 5)
                .padding(.top, 8)

            Picker("Expression", selection: $selectedTab) {
                Text("Emojis").tag(0)
                Text("GIFs").tag(1)
                Text("Stickers").tag(2)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal)

            if selectedTab == 0 {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 16) {
                    ForEach(emojis, id: \.self) { emoji in
                        Button(action: {
                            onSelectMedia(emoji)
                            dismiss()
                        }) {
                            Text(emoji)
                                .font(.system(size: 32))
                        }
                    }
                }
                .padding()
            } else if selectedTab == 1 {
                ScrollView {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                        ForEach(1...4, id: \.self) { index in
                            RoundedRectangle(cornerRadius: 12)
                                .fill(AppColors.brandPrimary.opacity(0.15))
                                .frame(height: 120)
                                .overlay(
                                    VStack {
                                        Image(systemName: "film.fill")
                                            .font(.title)
                                            .foregroundColor(AppColors.brandPrimary)
                                        Text("Trending GIF #\(index)")
                                            .font(.caption)
                                            .foregroundColor(AppColors.greyMedium)
                                    }
                                )
                                .onTapGesture {
                                    onSelectMedia("GIF #\(index)")
                                    dismiss()
                                }
                        }
                    }
                    .padding()
                }
            } else {
                ScrollView {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 16) {
                        ForEach(1...6, id: \.self) { index in
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.yellow.opacity(0.2))
                                .frame(height: 90)
                                .overlay(
                                    Image(systemName: "face.smiling.fill")
                                        .font(.largeTitle)
                                        .foregroundColor(.orange)
                                )
                                .onTapGesture {
                                    onSelectMedia("Sticker #\(index)")
                                    dismiss()
                                }
                        }
                    }
                    .padding()
                }
            }

            Spacer()
        }
        .frame(height: 320)
    }
}
