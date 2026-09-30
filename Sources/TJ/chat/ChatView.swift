import SwiftUI

struct ChatView: View {
    @EnvironmentObject var chatViewModel: ChatViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showingSettings: Bool = false
    @State private var showingNewChatContactPicker: Bool = false
    @State private var showingCreateStory: Bool = false
    @State private var showingCreateChannel: Bool = false
    @State private var showingPostJob: Bool = false

    var body: some View {
        NavigationView {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    // Top App Bar
                    HStack {
                        AppLogo(size: 32)

                        Spacer()

                        HStack(spacing: 16) {
                            Button(action: { showingNewChatContactPicker = true }) {
                                Image(systemName: "magnifyingglass")
                                    .font(.title3)
                                    .foregroundColor(.primary)
                            }
                            Button(action: { showingCreateStory = true }) {
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
                        VStack(spacing: 0) {
                            HStack {
                                Spacer()
                                Button(action: { showingCreateChannel = true }) {
                                    Label("Create Channel", systemImage: "plus.circle.fill")
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(AppColors.brandPrimary)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 8)

                            ChannelsListView()
                        }
                    } else if chatViewModel.activeSubTab == 2 {
                        VStack(spacing: 0) {
                            HStack {
                                Spacer()
                                Button(action: { showingPostJob = true }) {
                                    Label("Post a Job", systemImage: "plus.circle.fill")
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(AppColors.brandPrimary)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 8)

                            JobsListView()
                        }
                    } else {
                        RewardsView()
                    }
                }

                // Floating Action Button for New Chat
                if chatViewModel.activeSubTab == 0 {
                    Button(action: { showingNewChatContactPicker = true }) {
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
            .sheet(isPresented: $showingNewChatContactPicker) {
                ContactPickerView()
            }
            .sheet(isPresented: $showingCreateStory) {
                CreateStorySheet()
            }
            .sheet(isPresented: $showingCreateChannel) {
                CreateChannelSheet()
            }
            .sheet(isPresented: $showingPostJob) {
                PostJobSheet()
            }
        }
    }
}
