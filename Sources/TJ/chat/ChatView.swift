import SwiftUI

struct ChatView: View {
    @EnvironmentObject var chatViewModel: ChatViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showingSettings: Bool = false

    var body: some View {
        NavigationView {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    // Top App Bar
                    HStack {
                        AppLogo(size: 32)

                        Spacer()

                        HStack(spacing: 16) {
                            Button(action: {}) {
                                Image(systemName: "magnifyingglass")
                                    .font(.title3)
                                    .foregroundColor(.primary)
                            }
                            Button(action: {}) {
                                Image(systemName: "camera")
                                    .font(.title3)
                                    .foregroundColor(.primary)
                            }
                            Button(action: { showingSettings = true }) {
                                Image(systemName: "gearshape")
                                    .font(.title3)
                                    .foregroundColor(.primary)
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                    // Sub-tab Segmented Picker
                    Picker("SubTab", selection: $chatViewModel.activeSubTab) {
                        Text("Chats").tag(0)
                        Text("Channels").tag(1)
                        Text("Jobs").tag(2)
                        Text("Rewards").tag(3)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal)
                    .padding(.bottom, 8)

                    if chatViewModel.activeSubTab == 0 {
                        // Stories Row & Chats List
                        StoriesRowView(stories: chatViewModel.stories)
                        Divider().padding(.vertical, 4)
                        ChatsListView()
                    } else if chatViewModel.activeSubTab == 1 {
                        ChannelsListView()
                    } else if chatViewModel.activeSubTab == 2 {
                        JobsListView()
                    } else {
                        RewardsView()
                    }
                }

                // Floating Action Button
                if chatViewModel.activeSubTab == 0 {
                    Button(action: {}) {
                        Image(systemName: "square.and.pencil")
                            .font(.title2)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(AppColors.brandPrimary)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 80)
                }
            }
            .navigationBarHidden(true)
            .sheet(item: $chatViewModel.activeChat) { chat in
                ChatDetailView(chat: chat)
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView()
            }
        }
    }
}
