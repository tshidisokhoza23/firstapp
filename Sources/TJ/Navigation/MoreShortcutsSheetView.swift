import SwiftUI

struct MoreShortcutsSheetView: View {
    @Binding var isPresented: Bool
    var onSelectTab: (BottomNavItem) -> Void

    @State private var showingWallet: Bool = false
    @State private var showingVocab: Bool = false
    @State private var showingCalendar: Bool = false
    @State private var showingFiles: Bool = false

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
                    ShortcutItemView(title: "Wallet", iconName: "creditcard.fill", color: .orange, isEnabled: true) {
                        showingWallet = true
                    }
                    ShortcutItemView(title: "Vocab", iconName: "book.fill", color: .blue, isEnabled: true) {
                        showingVocab = true
                    }
                    ShortcutItemView(title: "Calendar", iconName: "calendar", color: .green, isEnabled: true) {
                        showingCalendar = true
                    }
                    ShortcutItemView(title: "Files", iconName: "folder.fill", color: .indigo, isEnabled: true) {
                        showingFiles = true
                    }
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
        .sheet(isPresented: $showingWallet) { WalletView() }
        .sheet(isPresented: $showingVocab) { VocabView() }
        .sheet(isPresented: $showingCalendar) { CalendarView() }
        .sheet(isPresented: $showingFiles) { FilesView() }
    }
}
