import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var mainViewModel: MainViewModel
    @EnvironmentObject var chatViewModel: ChatViewModel
    @EnvironmentObject var socialViewModel: SocialViewModel
    @EnvironmentObject var shortsViewModel: ShortsViewModel
    @EnvironmentObject var mingleViewModel: MingleViewModel
    @EnvironmentObject var streamViewModel: StreamViewModel
    @EnvironmentObject var driveViewModel: DriveViewModel
    @EnvironmentObject var authViewModel: AuthViewModel

    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        ZStack(alignment: .bottom) {
            // Main Active View
            Group {
                switch mainViewModel.selectedTab {
                case .chat:
                    ChatView()
                case .social:
                    SocialView()
                case .scroll:
                    ShortsView()
                case .mingle:
                    MingleView()
                case .stream:
                    StreamView()
                case .ride:
                    RideView()
                case .more:
                    activePriorView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Custom Bottom Navigation Bar
            VStack(spacing: 0) {
                Divider()
                    .background(AppColors.dividerColor)

                HStack {
                    ForEach(BottomNavItem.mainItems) { item in
                        Spacer()
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                mainViewModel.selectTab(item)
                            }
                        }) {
                            VStack(spacing: 4) {
                                Image(systemName: item.iconName)
                                    .font(.system(size: 20, weight: mainViewModel.selectedTab == item ? .bold : .regular))
                                Text(item.title)
                                    .font(.system(size: 10, weight: mainViewModel.selectedTab == item ? .bold : .medium))
                            }
                            .foregroundColor(mainViewModel.selectedTab == item ? AppColors.brandPrimary : AppColors.greyMedium)
                            .frame(maxWidth: .infinity)
                        }
                        Spacer()
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, 12)
                .background(colorScheme == .dark ? AppColors.surfaceColor : Color.white)
            }

            // Shortcuts Sheet Overlay
            if mainViewModel.isMoreSheetVisible {
                MoreShortcutsSheetView(isPresented: $mainViewModel.isMoreSheetVisible) { targetTab in
                    mainViewModel.selectTab(targetTab)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .edgesIgnoringSafeArea(.bottom)
    }

    @ViewBuilder
    private func activePriorView() -> some View {
        switch mainViewModel.previousTab {
        case .social: SocialView()
        case .scroll: ShortsView()
        case .mingle: MingleView()
        case .stream: StreamView()
        case .ride: RideView()
        default: ChatView()
        }
    }
}
