import SwiftUI

struct ShortcutItemView: View {
    let title: String
    let iconName: String
    let color: Color
    let isEnabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(color.opacity(isEnabled ? 0.15 : 0.05))
                        .frame(width: 64, height: 64)

                    Image(systemName: iconName)
                        .font(.system(size: 26))
                        .foregroundColor(isEnabled ? color : AppColors.greyMedium.opacity(0.5))
                }

                Text(isEnabled ? title : "\(title) (Soon)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(isEnabled ? .primary : AppColors.greyMedium)
                    .lineLimit(1)
            }
        }
    }
}

struct MoreShortcutsSheetView: View {
    @Binding var isPresented: Bool
    var onSelectTab: (BottomNavItem) -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.4)
                .edgesIgnoringSafeArea(.all)
                .onTapGesture {
                    withAnimation { isPresented = false }
                }

            VStack(spacing: 20) {
                HStack {
                    Text("Shortcuts")
                        .font(.title3)
                        .fontWeight(.bold)
                    Spacer()
                    Button(action: {
                        withAnimation { isPresented = false }
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.greyMedium)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 16)

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 16), count: 3), spacing: 24) {
                    ShortcutItemView(title: "Stream", iconName: "music.note", color: .purple, isEnabled: true) {
                        isPresented = false
                        onSelectTab(.stream)
                    }
                    ShortcutItemView(title: "Ride", iconName: "car.fill", color: .cyan, isEnabled: true) {
                        isPresented = false
                        onSelectTab(.ride)
                    }
                    ShortcutItemView(title: "Wallet", iconName: "creditcard.fill", color: .orange, isEnabled: false) { }
                    ShortcutItemView(title: "Vocab", iconName: "book.fill", color: .blue, isEnabled: false) { }
                    ShortcutItemView(title: "Calendar", iconName: "calendar", color: .green, isEnabled: false) { }
                    ShortcutItemView(title: "Files", iconName: "folder.fill", color: .indigo, isEnabled: false) { }
                }
                .padding(.horizontal)
                .padding(.bottom, 40)
            }
            .background(
                RoundedRectangle(cornerRadius: 28)
                    .fill(Color(UIColor.systemBackground))
            )
            .padding(.horizontal, 8)
            .padding(.bottom, 80)
        }
    }
}
